package hunternif.mc.impl.atlas.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Owns every client-side atlas save. Nothing in this class communicates with a
 * Minecraft server: the server address is only used to select a local folder.
 */
public final class ClientMapManager {
    public static final int REGION_SIZE = 32;
    private static final int FORMAT_VERSION = 1;
    private static final int SAVE_INTERVAL_TICKS = 100;
    private static final String DEFAULT_PROFILE_ID = "map_0001";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ClientMapManager INSTANCE = new ClientMapManager();

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Antique Atlas local save worker");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, ResourceLocation> tileIds = new LinkedHashMap<>();
    private final Set<RegionKey> dirtyRegions = new LinkedHashSet<>();

    private String contextIdentity;
    private String serverAddress;
    private Path serverFolder;
    private final LinkedHashMap<String, ProfileInfo> profiles = new LinkedHashMap<>();
    private String activeProfileId;
    private ClientProfile activeProfile;
    private boolean stateDirty;
    private int ticksUntilSave = SAVE_INTERVAL_TICKS;

    private ClientMapManager() {
    }

    public static ClientMapManager getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            if (activeProfile != null) disconnect();
            return;
        }

        String detectedAddress = detectServerAddress(minecraft);
        String detectedIdentity = folderName(detectedAddress);
        if (!detectedIdentity.equals(contextIdentity)) {
            activateServer(detectedIdentity, detectedAddress, minecraft.gameDirectory.toPath());
        }

        if (--ticksUntilSave <= 0) {
            scheduleSave();
            ticksUntilSave = SAVE_INTERVAL_TICKS;
        }
    }

    public boolean isReady() {
        return activeProfile != null;
    }

    public AtlasData getAtlasData() {
        return activeProfile == null ? null : activeProfile.atlasData;
    }

    public MarkersData getMarkersData() {
        return activeProfile == null ? null : activeProfile.markersData;
    }

    public int getActiveAtlasId() {
        return activeProfileId == null ? 0 : activeProfileId.hashCode();
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public String getActiveProfileName() {
        ProfileInfo info = profiles.get(activeProfileId);
        return info == null ? "Carte 1" : info.name();
    }

    public String getServerAddress() {
        return serverAddress == null ? "" : serverAddress;
    }

    public List<ProfileInfo> getProfiles() {
        return List.copyOf(profiles.values());
    }

    public String createProfile() {
        ensureReady();
        int number = 1;
        String id;
        do {
            id = "map_%04d".formatted(number++);
        } while (profiles.containsKey(id));

        // Parentheses are intentional: the first additional profile is Carte 2.
        ProfileInfo info = new ProfileInfo(id, "Carte " + (profiles.size() + 1));
        profiles.put(id, info);
        writeIndex();
        return id;
    }

    public boolean renameProfile(String profileId, String requestedName) {
        ProfileInfo old = profiles.get(profileId);
        if (old == null) return false;
        String name = requestedName == null ? "" : requestedName.strip();
        if (name.isEmpty()) return false;
        if (name.length() > 64) name = name.substring(0, 64);
        profiles.put(profileId, new ProfileInfo(profileId, name));
        writeIndex();
        return true;
    }

    public boolean activateProfile(String profileId) {
        if (serverFolder == null || !profiles.containsKey(profileId)) return false;
        if (profileId.equals(activeProfileId) && activeProfile != null) return true;

        stateDirty = activeProfile != null;
        flushBlocking();
        activeProfileId = profileId;
        activeProfile = loadProfile(profileId);
        dirtyRegions.clear();
        stateDirty = false;
        installActiveData();
        writeIndex();
        AntiqueAtlasClientSegment.resetAtlasGUI();
        return true;
    }

    public boolean deleteProfile(String profileId) {
        if (!profiles.containsKey(profileId) || profiles.size() <= 1) return false;
        flushBlocking();

        if (profileId.equals(activeProfileId)) {
            String replacement = profiles.keySet().stream()
                    .filter(id -> !id.equals(profileId))
                    .findFirst()
                    .orElse(DEFAULT_PROFILE_ID);
            activeProfileId = null;
            activeProfile = null;
            profiles.remove(profileId);
            activateProfile(replacement);
        } else {
            profiles.remove(profileId);
        }

        deleteRecursively(profileFolder(profileId));
        writeIndex();
        return true;
    }

    public boolean putTile(ResourceKey<Level> dimension, int chunkX, int chunkZ, ResourceLocation tile) {
        if (activeProfile == null || tile == null) return false;
        ResourceLocation canonical = tileIds.computeIfAbsent(tile.toString(), ignored -> tile);
        ResourceLocation old = activeProfile.atlasData.getWorldData(dimension).getTile(chunkX, chunkZ);
        if (canonical.equals(old)) return false;
        activeProfile.atlasData.setTile(dimension, chunkX, chunkZ, canonical);
        dirtyRegions.add(RegionKey.of(dimension, chunkX, chunkZ));
        return true;
    }

    public Marker createMarker(ResourceKey<Level> dimension, ResourceLocation type, Component label,
                               int x, int z, boolean visibleAhead) {
        if (activeProfile == null || type == null) return null;
        int markerCount = activeProfile.markersData.getVisitedDimensions().stream()
                .mapToInt(world -> activeProfile.markersData.getMarkersInWorld(world).size())
                .sum();
        if (markerCount >= Math.max(0, AntiqueAtlas.CONFIG.markerLimit)) {
            AntiqueAtlas.LOG.warn("Local map '{}' reached its marker limit of {}",
                    getActiveProfileName(), AntiqueAtlas.CONFIG.markerLimit);
            return null;
        }
        if (label == null) label = Component.empty();
        Marker marker = activeProfile.markersData.createAndSaveMarker(type, dimension, x, z, visibleAhead, label);
        stateDirty = true;
        return marker;
    }

    public boolean deleteMarker(int markerId) {
        if (activeProfile == null) return false;
        Marker removed = activeProfile.markersData.removeMarker(markerId);
        if (removed != null) stateDirty = true;
        return removed != null;
    }

    public void markStateDirty() {
        if (activeProfile != null) stateDirty = true;
    }

    public void disconnect() {
        stateDirty = activeProfile != null;
        flushBlocking();
        contextIdentity = null;
        serverAddress = null;
        serverFolder = null;
        profiles.clear();
        activeProfileId = null;
        activeProfile = null;
        dirtyRegions.clear();
        stateDirty = false;
        AntiqueAtlas.tileData.setClientData(null);
        AntiqueAtlas.markersData.setClientData(null);
        AntiqueAtlasClientSegment.resetAtlasGUI();
        AntiqueAtlasClientSegment.resetClientScanner();
    }

    private void activateServer(String identity, String address, Path gameDirectory) {
        if (activeProfile != null) {
            stateDirty = true;
            flushBlocking();
        }
        contextIdentity = identity;
        serverAddress = address;
        serverFolder = gameDirectory.resolve("config").resolve("antiqueatlas")
                .resolve("servers").resolve(identity);
        profiles.clear();
        readIndex();
        if (profiles.isEmpty()) profiles.put(DEFAULT_PROFILE_ID, new ProfileInfo(DEFAULT_PROFILE_ID, "Carte 1"));
        if (activeProfileId == null || !profiles.containsKey(activeProfileId)) activeProfileId = profiles.keySet().iterator().next();
        activeProfile = loadProfile(activeProfileId);
        dirtyRegions.clear();
        stateDirty = false;
        installActiveData();
        writeIndex();
        AntiqueAtlasClientSegment.resetAtlasGUI();
        AntiqueAtlasClientSegment.resetClientScanner();
        AntiqueAtlas.LOG.info("Loaded local atlas '{}' for {}", getActiveProfileName(), address);
    }

    private void installActiveData() {
        AntiqueAtlas.tileData.setClientData(activeProfile == null ? null : activeProfile.atlasData);
        AntiqueAtlas.markersData.setClientData(activeProfile == null ? null : activeProfile.markersData);
    }

    private ClientProfile loadProfile(String profileId) {
        AtlasData atlasData = new AtlasData();
        MarkersData markersData = new MarkersData();
        Path folder = profileFolder(profileId);
        Path stateFile = folder.resolve("profile.dat");

        if (Files.isRegularFile(stateFile)) {
            try {
                CompoundTag root = NbtIo.readCompressed(stateFile.toFile());
                if (root.contains("atlas", Tag.TAG_COMPOUND)) atlasData.updateFromNbt(root.getCompound("atlas"));
                if (root.contains("markers", Tag.TAG_COMPOUND)) markersData = MarkersData.fromNbt(root.getCompound("markers"));
            } catch (Exception exception) {
                AntiqueAtlas.LOG.error("Could not load local atlas profile {}", stateFile, exception);
            }
        }

        Path dimensions = folder.resolve("dimensions");
        if (Files.isDirectory(dimensions)) {
            try (var files = Files.walk(dimensions)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("r."))
                        .filter(path -> path.getFileName().toString().endsWith(".dat"))
                        .sorted()
                        .forEach(path -> readRegion(path, atlasData));
            } catch (IOException exception) {
                AntiqueAtlas.LOG.error("Could not enumerate local atlas regions in {}", dimensions, exception);
            }
        }
        return new ClientProfile(atlasData, markersData);
    }

    private void readRegion(Path path, AtlasData atlasData) {
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            ResourceLocation dimensionId = ResourceLocation.tryParse(root.getString("dimension"));
            if (dimensionId == null) return;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            int regionX = root.getInt("regionX");
            int regionZ = root.getInt("regionZ");
            ListTag paletteTag = root.getList("palette", Tag.TAG_STRING);
            List<ResourceLocation> palette = new ArrayList<>(paletteTag.size());
            for (int i = 0; i < paletteTag.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(paletteTag.getString(i));
                palette.add(id == null ? AntiqueAtlas.id("unknown") : tileIds.computeIfAbsent(id.toString(), ignored -> id));
            }

            int[] tiles = root.getIntArray("tiles");
            int length = Math.min(tiles.length, REGION_SIZE * REGION_SIZE);
            for (int index = 0; index < length; index++) {
                int paletteIndex = tiles[index] - 1;
                if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                int x = regionX * REGION_SIZE + index % REGION_SIZE;
                int z = regionZ * REGION_SIZE + index / REGION_SIZE;
                atlasData.setTile(dimension, x, z, palette.get(paletteIndex));
            }
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load local atlas region {}", path, exception);
        }
    }

    private void scheduleSave() {
        if (activeProfile == null) return;
        List<RegionSnapshot> regions = snapshotDirtyRegions();
        StateSnapshot state = snapshotState();
        if (regions.isEmpty() && state == null) return;
        ioExecutor.submit(() -> writeSnapshots(regions, state));
    }

    private void flushBlocking() {
        if (activeProfile == null) return;
        scheduleSave();
        try {
            Future<?> barrier = ioExecutor.submit(() -> { });
            barrier.get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Timed out while flushing local atlas data", exception);
        }
    }

    private List<RegionSnapshot> snapshotDirtyRegions() {
        if (dirtyRegions.isEmpty() || activeProfile == null) return List.of();
        List<RegionKey> keys = new ArrayList<>(dirtyRegions);
        dirtyRegions.clear();
        List<RegionSnapshot> snapshots = new ArrayList<>(keys.size());
        for (RegionKey key : keys) snapshots.add(snapshotRegion(key));
        return snapshots;
    }

    private RegionSnapshot snapshotRegion(RegionKey key) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        root.putString("dimension", key.dimension.location().toString());
        root.putInt("regionX", key.regionX);
        root.putInt("regionZ", key.regionZ);

        ListTag paletteTag = new ListTag();
        Map<ResourceLocation, Integer> palette = new LinkedHashMap<>();
        int[] tiles = new int[REGION_SIZE * REGION_SIZE];
        for (int index = 0; index < tiles.length; index++) {
            int x = key.regionX * REGION_SIZE + index % REGION_SIZE;
            int z = key.regionZ * REGION_SIZE + index / REGION_SIZE;
            ResourceLocation tile = activeProfile.atlasData.getWorldData(key.dimension).getTile(x, z);
            if (tile == null) continue;
            Integer paletteIndex = palette.get(tile);
            if (paletteIndex == null) {
                paletteIndex = palette.size();
                palette.put(tile, paletteIndex);
                paletteTag.add(StringTag.valueOf(tile.toString()));
            }
            tiles[index] = paletteIndex + 1;
        }
        root.put("palette", paletteTag);
        root.putIntArray("tiles", tiles);
        return new RegionSnapshot(regionPath(activeProfileId, key), root);
    }

    private StateSnapshot snapshotState() {
        if (!stateDirty || activeProfile == null) return null;
        stateDirty = false;
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        root.put("atlas", activeProfile.atlasData.writeToNBT(new CompoundTag(), false));
        root.put("markers", activeProfile.markersData.save(new CompoundTag()));
        return new StateSnapshot(profileFolder(activeProfileId).resolve("profile.dat"), root);
    }

    private void writeSnapshots(List<RegionSnapshot> regions, StateSnapshot state) {
        for (RegionSnapshot region : regions) writeNbtAtomic(region.path, region.data);
        if (state != null) writeNbtAtomic(state.path, state.data);
    }

    private void writeNbtAtomic(Path path, CompoundTag data) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            NbtIo.writeCompressed(data, temporary.toFile());
            moveAtomic(temporary, path);
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not save local atlas data to {}", path, exception);
        }
    }

    private void readIndex() {
        activeProfileId = null;
        Path index = serverFolder.resolve("maps.json");
        if (!Files.isRegularFile(index)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(index, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("activeMap")) activeProfileId = root.get("activeMap").getAsString();
            JsonObject maps = root.has("maps") && root.get("maps").isJsonObject() ? root.getAsJsonObject("maps") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : maps.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject value = entry.getValue().getAsJsonObject();
                String name = value.has("name") ? value.get("name").getAsString() : entry.getKey();
                profiles.put(entry.getKey(), new ProfileInfo(entry.getKey(), name));
            }
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load local atlas index {}", index, exception);
        }
    }

    private void writeIndex() {
        if (serverFolder == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        root.addProperty("serverAddress", serverAddress);
        root.addProperty("activeMap", activeProfileId);
        JsonObject maps = new JsonObject();
        profiles.forEach((id, info) -> {
            JsonObject value = new JsonObject();
            value.addProperty("name", info.name());
            maps.add(id, value);
        });
        root.add("maps", maps);

        Path index = serverFolder.resolve("maps.json");
        try {
            Files.createDirectories(index.getParent());
            Path temporary = index.resolveSibling("maps.json.tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            moveAtomic(temporary, index);
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not save local atlas index {}", index, exception);
        }
    }

    private Path profileFolder(String profileId) {
        return serverFolder.resolve("maps").resolve(profileId);
    }

    private Path regionPath(String profileId, RegionKey key) {
        ResourceLocation dimension = key.dimension.location();
        return profileFolder(profileId)
                .resolve("dimensions")
                .resolve(dimension.getNamespace())
                .resolve(dimension.getPath())
                .resolve("terrain")
                .resolve("r.%d.%d.dat".formatted(key.regionX, key.regionZ));
    }

    private static String detectServerAddress(Minecraft minecraft) {
        if (minecraft.getCurrentServer() != null) {
            return minecraft.getCurrentServer().ip.strip().toLowerCase(Locale.ROOT);
        }
        if (minecraft.getSingleplayerServer() != null) {
            return "singleplayer:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        return "unknown-server";
    }

    private static String folderName(String address) {
        String slug = address.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        if (slug.isBlank()) slug = "server";
        if (slug.length() > 48) slug = slug.substring(0, 48);
        return slug + "-" + shortHash(address);
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(8);
            for (int i = 0; i < 4; i++) out.append("%02x".formatted(digest[i]));
            return out.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void moveAtomic(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursively(Path folder) {
        if (!Files.exists(folder)) return;
        try (var paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not delete local atlas profile {}", folder, exception);
        }
    }

    private void ensureReady() {
        if (serverFolder == null) throw new IllegalStateException("No local atlas server context is active");
    }

    public record ProfileInfo(String id, String name) {
    }

    private record ClientProfile(AtlasData atlasData, MarkersData markersData) {
    }

    private record RegionSnapshot(Path path, CompoundTag data) {
    }

    private record StateSnapshot(Path path, CompoundTag data) {
    }

    private record RegionKey(ResourceKey<Level> dimension, int regionX, int regionZ) {
        private static RegionKey of(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
            return new RegionKey(dimension, Math.floorDiv(chunkX, REGION_SIZE), Math.floorDiv(chunkZ, REGION_SIZE));
        }
    }
}
