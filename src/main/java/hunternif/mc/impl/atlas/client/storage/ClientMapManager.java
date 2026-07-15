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
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private static final int FORMAT_VERSION = 2;
    private static final int BINDING_VERSION = 1;
    private static final String HIDDEN_MARKER_TYPES_TAG = "hiddenMarkerTypes";
    private static final String BINDING_VERSION_TAG = "aaBindingVersion";
    private static final String MAP_ID_TAG = "aaMapId";
    private static final String OWNER_NAME_TAG = "aaOwnerName";
    private static final String OWNER_FINGERPRINT_TAG = "aaOwnerFingerprint";
    private static final String REGION_FINGERPRINT_TAG = "aaRegionFingerprint";
    private static final String REGION_MIGRATION_PENDING_TAG = "aaRegionMigrationPending";
    private static final String OWNER_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-owner-v1";
    private static final String PROFILE_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-profile-v1";
    private static final String REGION_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-region-v1";
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

    private String serverAddress;
    private String currentOwnerName;
    private String currentOwnerFingerprint;
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
        String detectedOwnerName = detectPlayerName(minecraft);
        boolean sameContext = detectedAddress.equals(serverAddress)
                && normalizePlayerName(detectedOwnerName).equals(normalizePlayerName(currentOwnerName));
        if (!sameContext) {
            String detectedServerIdentity = folderName(detectedAddress);
            String detectedOwnerFingerprint = ownerFingerprint(detectedOwnerName);
            activateServer(detectedServerIdentity, detectedAddress,
                    detectedOwnerName, detectedOwnerFingerprint, minecraft.gameDirectory.toPath());
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

    public boolean isMarkerTypeVisible(ResourceLocation type) {
        return activeProfile == null || type == null
                || !activeProfile.hiddenMarkerTypes.contains(type.toString());
    }

    public void setMarkerTypeVisible(ResourceLocation type, boolean visible) {
        if (activeProfile == null || type == null) return;
        String id = type.toString();
        boolean changed = visible
                ? activeProfile.hiddenMarkerTypes.remove(id)
                : activeProfile.hiddenMarkerTypes.add(id);
        if (changed) stateDirty = true;
    }

    public void showAllMarkerTypes() {
        if (activeProfile == null || activeProfile.hiddenMarkerTypes.isEmpty()) return;
        activeProfile.hiddenMarkerTypes.clear();
        stateDirty = true;
    }

    public void hideMarkerTypes(Collection<ResourceLocation> types) {
        if (activeProfile == null || types == null) return;
        boolean changed = false;
        for (ResourceLocation type : types) {
            if (type != null) changed |= activeProfile.hiddenMarkerTypes.add(type.toString());
        }
        if (changed) stateDirty = true;
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

    public int getMaxProfiles() {
        return Math.max(1, AntiqueAtlas.CONFIG.maxMaps);
    }

    public boolean canCreateProfile() {
        return serverFolder != null && profiles.size() < getMaxProfiles();
    }

    public String createProfile() {
        ensureReady();
        if (!canCreateProfile()) return null;

        String id = nextProfileId();

        // Parentheses are intentional: the first additional profile is Carte 2.
        ProfileInfo info = new ProfileInfo(id, "Carte " + (profiles.size() + 1));
        profiles.put(id, info);
        claimLegacyProfileBinding(id);
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
        ProfileLoad loaded = loadProfile(profileId);
        if (loaded == null) return false;

        activeProfileId = profileId;
        activeProfile = loaded.profile;
        dirtyRegions.clear();
        stateDirty = false;
        installActiveData();
        writeIndex();
        startRegionBindingMigration(profileId, loaded);
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

    public boolean removeTile(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (activeProfile == null) return false;
        ResourceLocation removed = activeProfile.atlasData.removeTile(dimension, chunkX, chunkZ);
        if (removed == null) return false;
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
        serverAddress = null;
        currentOwnerName = null;
        currentOwnerFingerprint = null;
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

    private void activateServer(String serverIdentity, String address,
                                String ownerName, String ownerFingerprint, Path gameDirectory) {
        if (activeProfile != null) {
            stateDirty = true;
            flushBlocking();
        }
        serverAddress = address;
        currentOwnerName = ownerName;
        currentOwnerFingerprint = ownerFingerprint;
        Path baseServerFolder = gameDirectory.resolve("config").resolve("antiqueatlas")
                .resolve("servers").resolve(serverIdentity);
        serverFolder = selectOwnedServerFolder(baseServerFolder);
        profiles.clear();
        readIndex();
        if (profiles.isEmpty()) profiles.put(DEFAULT_PROFILE_ID, new ProfileInfo(DEFAULT_PROFILE_ID, "Carte 1"));
        if (activeProfileId == null || !profiles.containsKey(activeProfileId)) activeProfileId = profiles.keySet().iterator().next();

        // Claim the selected storage root before migrating any legacy profile.
        // This prevents another local nickname from taking it if the client is
        // closed in the middle of the one-time region migration.
        writeIndex();
        claimLegacyProfileBindings();

        ProfileLoad loaded = loadProfile(activeProfileId);
        if (loaded == null) {
            for (String profileId : profiles.keySet()) {
                if (profileId.equals(activeProfileId)) continue;
                loaded = loadProfile(profileId);
                if (loaded != null) {
                    activeProfileId = profileId;
                    break;
                }
            }
        }
        if (loaded == null) {
            activeProfileId = nextProfileId();
            profiles.put(activeProfileId, new ProfileInfo(activeProfileId, "Carte " + (profiles.size() + 1)));
            loaded = loadProfile(activeProfileId);
        }
        if (loaded == null) throw new IllegalStateException("Could not create a local map owned by the current player");

        activeProfile = loaded.profile;
        dirtyRegions.clear();
        stateDirty = false;
        installActiveData();
        writeIndex();
        startRegionBindingMigration(activeProfileId, loaded);
        AntiqueAtlasClientSegment.resetAtlasGUI();
        AntiqueAtlasClientSegment.resetClientScanner();
        AntiqueAtlas.LOG.info("Loaded local atlas '{}' for {}", getActiveProfileName(), address);
    }

    private void installActiveData() {
        AntiqueAtlas.tileData.setClientData(activeProfile == null ? null : activeProfile.atlasData);
        AntiqueAtlas.markersData.setClientData(activeProfile == null ? null : activeProfile.markersData);
    }

    private void claimLegacyProfileBindings() {
        for (String profileId : profiles.keySet()) claimLegacyProfileBinding(profileId);
    }

    private void claimLegacyProfileBinding(String profileId) {
        Path folder = profileFolder(profileId);
        Path stateFile = folder.resolve("profile.dat");
        boolean hadStateFile = Files.isRegularFile(stateFile);
        boolean hasRegions = Files.isDirectory(folder.resolve("dimensions"));
        CompoundTag root = new CompoundTag();
        if (hadStateFile) {
            try {
                root = NbtIo.readCompressed(stateFile.toFile());
            } catch (Exception exception) {
                AntiqueAtlas.LOG.error("Could not inspect legacy local atlas profile {}", stateFile, exception);
                return;
            }
        }
        if (declaresProfileBinding(root)) return;

        String mapId = UUID.randomUUID().toString();
        root.putInt("version", FORMAT_VERSION);
        root.putInt(BINDING_VERSION_TAG, BINDING_VERSION);
        root.putString(MAP_ID_TAG, mapId);
        root.putString(OWNER_NAME_TAG, currentOwnerName);
        root.putString(OWNER_FINGERPRINT_TAG, profileFingerprint(currentOwnerFingerprint, mapId));
        root.putBoolean(REGION_MIGRATION_PENDING_TAG, hadStateFile || hasRegions);
        writeNbtAtomic(stateFile, root);
    }

    private ProfileLoad loadProfile(String profileId) {
        AtlasData atlasData = new AtlasData();
        MarkersData markersData = new MarkersData();
        Set<String> hiddenMarkerTypes = new LinkedHashSet<>();
        Path folder = profileFolder(profileId);
        Path stateFile = folder.resolve("profile.dat");
        Path dimensions = folder.resolve("dimensions");
        CompoundTag savedState = null;
        boolean hadStateFile = Files.isRegularFile(stateFile);

        if (hadStateFile) {
            try {
                savedState = NbtIo.readCompressed(stateFile.toFile());
                if (savedState.contains("atlas", Tag.TAG_COMPOUND)) {
                    atlasData.updateFromNbt(savedState.getCompound("atlas"));
                }
                if (savedState.contains("markers", Tag.TAG_COMPOUND)) {
                    markersData = MarkersData.fromNbt(savedState.getCompound("markers"));
                }
                ListTag hiddenTypes = savedState.getList(HIDDEN_MARKER_TYPES_TAG, Tag.TAG_STRING);
                for (int index = 0; index < hiddenTypes.size(); index++) {
                    String id = hiddenTypes.getString(index);
                    if (!id.isBlank()) hiddenMarkerTypes.add(id);
                }
            } catch (Exception exception) {
                AntiqueAtlas.LOG.error("Could not load local atlas profile {}", stateFile, exception);
            }
        }

        ProfileBinding binding;
        boolean declaresBinding = declaresProfileBinding(savedState);
        boolean migrationPending;
        if (declaresBinding) {
            if (savedState.getInt(BINDING_VERSION_TAG) != BINDING_VERSION
                    || !savedState.contains(MAP_ID_TAG, Tag.TAG_STRING)
                    || !savedState.contains(OWNER_FINGERPRINT_TAG, Tag.TAG_STRING)) {
                AntiqueAtlas.LOG.warn("Refusing local atlas profile {} because its owner binding is incomplete", stateFile);
                return null;
            }

            String mapId = savedState.getString(MAP_ID_TAG);
            String storedFingerprint = savedState.getString(OWNER_FINGERPRINT_TAG);
            String expectedFingerprint = profileFingerprint(currentOwnerFingerprint, mapId);
            if (mapId.isBlank() || !storedFingerprint.equals(expectedFingerprint)) {
                String storedOwner = savedState.getString(OWNER_NAME_TAG);
                AntiqueAtlas.LOG.warn("Refusing local atlas profile {} owned by '{}' for current player '{}'",
                        stateFile, storedOwner.isBlank() ? "unknown" : storedOwner, currentOwnerName);
                return null;
            }

            String storedOwner = savedState.getString(OWNER_NAME_TAG);
            binding = new ProfileBinding(mapId, storedFingerprint,
                    storedOwner.isBlank() ? currentOwnerName : storedOwner);
            migrationPending = savedState.getBoolean(REGION_MIGRATION_PENDING_TAG);
        } else {
            String mapId = UUID.randomUUID().toString();
            binding = new ProfileBinding(mapId,
                    profileFingerprint(currentOwnerFingerprint, mapId), currentOwnerName);
            migrationPending = hadStateFile || Files.isDirectory(dimensions);
        }

        ClientProfile profile = new ClientProfile(atlasData, markersData, hiddenMarkerTypes,
                binding, migrationPending);

        // A small synchronous checkpoint makes the owner and random map ID
        // durable before the larger legacy regions are rebound in the worker.
        if (!declaresBinding) {
            writeNbtAtomic(stateFile, createStateTag(profile, migrationPending));
        }

        Set<RegionKey> regionsToMigrate = new LinkedHashSet<>();
        int rejectedRegions = 0;
        if (Files.isDirectory(dimensions)) {
            try (var files = Files.walk(dimensions)) {
                List<Path> regionFiles = files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("r."))
                        .filter(path -> path.getFileName().toString().endsWith(".dat"))
                        .sorted()
                        .toList();
                for (Path path : regionFiles) {
                    RegionReadResult result = readRegion(path, atlasData, binding, migrationPending);
                    if (result == null || result.rejected) {
                        rejectedRegions++;
                    } else if (result.needsMigration) {
                        regionsToMigrate.add(result.key);
                    }
                }
            } catch (IOException exception) {
                AntiqueAtlas.LOG.error("Could not enumerate local atlas regions in {}", dimensions, exception);
            }
        }
        if (rejectedRegions > 0) {
            AntiqueAtlas.LOG.warn("Skipped {} local atlas region(s) whose owner or contents did not match profile {}",
                    rejectedRegions, profileId);
        }
        return new ProfileLoad(profile, List.copyOf(regionsToMigrate));
    }

    private RegionReadResult readRegion(Path path, AtlasData atlasData, ProfileBinding binding,
                                        boolean allowLegacyRegion) {
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            ResourceLocation dimensionId = ResourceLocation.tryParse(root.getString("dimension"));
            if (dimensionId == null) return null;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            int regionX = root.getInt("regionX");
            int regionZ = root.getInt("regionZ");
            RegionKey key = new RegionKey(dimension, regionX, regionZ);
            ListTag paletteTag = root.getList("palette", Tag.TAG_STRING);
            int[] tiles = root.getIntArray("tiles");

            boolean declaresBinding = root.contains(BINDING_VERSION_TAG)
                    || root.contains(REGION_FINGERPRINT_TAG);
            if (declaresBinding) {
                if (root.getInt(BINDING_VERSION_TAG) != BINDING_VERSION
                        || !root.contains(REGION_FINGERPRINT_TAG, Tag.TAG_STRING)
                        || !root.getString(REGION_FINGERPRINT_TAG).equals(
                        regionFingerprint(binding, key, paletteTag, tiles))) {
                    return new RegionReadResult(key, false, true);
                }
            } else if (!allowLegacyRegion) {
                return new RegionReadResult(key, false, true);
            }

            List<ResourceLocation> palette = new ArrayList<>(paletteTag.size());
            for (int i = 0; i < paletteTag.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(paletteTag.getString(i));
                palette.add(id == null ? AntiqueAtlas.id("unknown") : tileIds.computeIfAbsent(id.toString(), ignored -> id));
            }

            int length = Math.min(tiles.length, REGION_SIZE * REGION_SIZE);
            for (int index = 0; index < length; index++) {
                int paletteIndex = tiles[index] - 1;
                if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                int x = regionX * REGION_SIZE + index % REGION_SIZE;
                int z = regionZ * REGION_SIZE + index / REGION_SIZE;
                atlasData.setTile(dimension, x, z, palette.get(paletteIndex));
            }
            return new RegionReadResult(key, !declaresBinding, false);
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load local atlas region {}", path, exception);
            return null;
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
        waitForIo();

        // A binding migration may have deliberately postponed profile.dat.
        // Once its worker has finished, take one last current snapshot.
        scheduleSave();
        waitForIo();
    }

    private void waitForIo() {
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
        return snapshotRegion(activeProfileId, activeProfile, key);
    }

    private RegionSnapshot snapshotRegion(String profileId, ClientProfile profile, RegionKey key) {
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
            ResourceLocation tile = profile.atlasData.getWorldData(key.dimension).getTile(x, z);
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
        root.putInt(BINDING_VERSION_TAG, BINDING_VERSION);
        root.putString(REGION_FINGERPRINT_TAG,
                regionFingerprint(profile.binding, key, paletteTag, tiles));
        return new RegionSnapshot(regionPath(profileId, key), root);
    }

    private StateSnapshot snapshotState() {
        if (!stateDirty || activeProfile == null) return null;
        if (activeProfile.migrationInProgress) return null;
        stateDirty = false;
        return new StateSnapshot(profileFolder(activeProfileId).resolve("profile.dat"),
                createStateTag(activeProfile, activeProfile.migrationPending));
    }

    private CompoundTag createStateTag(ClientProfile profile, boolean migrationPending) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        root.putInt(BINDING_VERSION_TAG, BINDING_VERSION);
        root.putString(MAP_ID_TAG, profile.binding.mapId);
        root.putString(OWNER_NAME_TAG, profile.binding.ownerName);
        root.putString(OWNER_FINGERPRINT_TAG, profile.binding.ownerFingerprint);
        root.putBoolean(REGION_MIGRATION_PENDING_TAG, migrationPending);
        root.put("atlas", profile.atlasData.writeToNBT(new CompoundTag(), false));
        root.put("markers", profile.markersData.save(new CompoundTag()));
        ListTag hiddenTypes = new ListTag();
        for (String id : profile.hiddenMarkerTypes) hiddenTypes.add(StringTag.valueOf(id));
        root.put(HIDDEN_MARKER_TYPES_TAG, hiddenTypes);
        return root;
    }

    private void startRegionBindingMigration(String profileId, ProfileLoad loaded) {
        ClientProfile profile = loaded.profile;
        if (!profile.migrationPending || profile.migrationInProgress) return;

        profile.migrationInProgress = true;
        List<RegionSnapshot> regions = new ArrayList<>(loaded.regionsToMigrate.size());
        for (RegionKey key : loaded.regionsToMigrate) {
            regions.add(snapshotRegion(profileId, profile, key));
        }
        StateSnapshot completedState = new StateSnapshot(profileFolder(profileId).resolve("profile.dat"),
                createStateTag(profile, false));

        AntiqueAtlas.LOG.info("Binding {} legacy atlas region(s) in profile '{}' to player '{}'",
                regions.size(), profileId, currentOwnerName);
        ioExecutor.submit(() -> {
            boolean success = true;
            for (RegionSnapshot region : regions) {
                if (!writeNbtAtomic(region.path, region.data)) success = false;
            }
            if (success) success = writeNbtAtomic(completedState.path, completedState.data);
            if (success) {
                profile.migrationPending = false;
            } else {
                AntiqueAtlas.LOG.warn("The owner binding migration for local atlas profile '{}' will be retried later",
                        profileId);
            }
            profile.migrationInProgress = false;
        });
    }

    private void writeSnapshots(List<RegionSnapshot> regions, StateSnapshot state) {
        for (RegionSnapshot region : regions) writeNbtAtomic(region.path, region.data);
        if (state != null) writeNbtAtomic(state.path, state.data);
    }

    private boolean writeNbtAtomic(Path path, CompoundTag data) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            NbtIo.writeCompressed(data, temporary.toFile());
            moveAtomic(temporary, path);
            return true;
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not save local atlas data to {}", path, exception);
            return false;
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
        root.addProperty("bindingVersion", BINDING_VERSION);
        root.addProperty("serverAddress", serverAddress);
        root.addProperty("ownerName", currentOwnerName);
        root.addProperty("ownerFingerprint", currentOwnerFingerprint);
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

    private Path selectOwnedServerFolder(Path baseServerFolder) {
        String storedOwner = readIndexOwnerFingerprint(baseServerFolder.resolve("maps.json"));
        if (storedOwner == null || storedOwner.equals(currentOwnerFingerprint)) {
            return baseServerFolder;
        }

        // Keep another nickname's maps untouched and transparently give the
        // current nickname a separate local root for this same server.
        Path playersFolder = baseServerFolder.resolve("players");
        String slug = normalizePlayerName(currentOwnerName).replaceAll("[^a-z0-9._-]+", "_");
        if (slug.isBlank()) slug = "player";
        if (slug.length() > 32) slug = slug.substring(0, 32);
        String baseName = slug + "-" + currentOwnerFingerprint.substring(0, 12);
        for (int suffix = 0; suffix < 100; suffix++) {
            Path candidate = playersFolder.resolve(suffix == 0 ? baseName : baseName + "-" + suffix);
            String candidateOwner = readIndexOwnerFingerprint(candidate.resolve("maps.json"));
            if (candidateOwner == null || candidateOwner.equals(currentOwnerFingerprint)) {
                AntiqueAtlas.LOG.info("Local atlas storage for '{}' is isolated from another nickname's maps",
                        currentOwnerName);
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate an owned local atlas folder for " + currentOwnerName);
    }

    private static String readIndexOwnerFingerprint(Path index) {
        if (!Files.isRegularFile(index)) return null;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(index, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.has("ownerFingerprint") ? root.get("ownerFingerprint").getAsString() : null;
        } catch (Exception exception) {
            AntiqueAtlas.LOG.warn("Could not inspect local atlas owner in {}", index, exception);
            return null;
        }
    }

    private String nextProfileId() {
        int number = 1;
        String id;
        do {
            id = "map_%04d".formatted(number++);
        } while (profiles.containsKey(id));
        return id;
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

    private static String detectPlayerName(Minecraft minecraft) {
        String name = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
        return name == null || name.isBlank() ? "unknown-player" : name.strip();
    }

    private static String normalizePlayerName(String name) {
        return name == null ? "unknown-player" : name.strip().toLowerCase(Locale.ROOT);
    }

    private static String folderName(String address) {
        String slug = address.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        if (slug.isBlank()) slug = "server";
        if (slug.length() > 48) slug = slug.substring(0, 48);
        return slug + "-" + shortHash(address);
    }

    private static String shortHash(String value) {
        return sha256Hex(value).substring(0, 8);
    }

    private static boolean declaresProfileBinding(CompoundTag root) {
        return root != null && (root.contains(BINDING_VERSION_TAG)
                || root.contains(MAP_ID_TAG)
                || root.contains(OWNER_FINGERPRINT_TAG));
    }

    private static String ownerFingerprint(String playerName) {
        return sha256Hex(OWNER_FINGERPRINT_CONTEXT, normalizePlayerName(playerName));
    }

    private static String profileFingerprint(String ownerFingerprint, String mapId) {
        return sha256Hex(PROFILE_FINGERPRINT_CONTEXT, ownerFingerprint, mapId);
    }

    private static String regionFingerprint(ProfileBinding binding, RegionKey key,
                                            ListTag palette, int[] tiles) {
        MessageDigest digest = newSha256Digest();
        updateDigest(digest, REGION_FINGERPRINT_CONTEXT);
        updateDigest(digest, binding.ownerFingerprint);
        updateDigest(digest, key.dimension.location().toString());
        updateDigest(digest, key.regionX);
        updateDigest(digest, key.regionZ);
        updateDigest(digest, palette.size());
        for (int index = 0; index < palette.size(); index++) {
            updateDigest(digest, palette.getString(index));
        }
        updateDigest(digest, tiles.length);
        for (int tile : tiles) updateDigest(digest, tile);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256Hex(String... values) {
        MessageDigest digest = newSha256Digest();
        for (String value : values) updateDigest(digest, value);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateDigest(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateDigest(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
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

    private static final class ClientProfile {
        private final AtlasData atlasData;
        private final MarkersData markersData;
        private final Set<String> hiddenMarkerTypes;
        private final ProfileBinding binding;
        private volatile boolean migrationPending;
        private volatile boolean migrationInProgress;

        private ClientProfile(AtlasData atlasData, MarkersData markersData,
                              Set<String> hiddenMarkerTypes, ProfileBinding binding,
                              boolean migrationPending) {
            this.atlasData = atlasData;
            this.markersData = markersData;
            this.hiddenMarkerTypes = hiddenMarkerTypes;
            this.binding = binding;
            this.migrationPending = migrationPending;
        }
    }

    private record ProfileBinding(String mapId, String ownerFingerprint, String ownerName) {
    }

    private record ProfileLoad(ClientProfile profile, List<RegionKey> regionsToMigrate) {
    }

    private record RegionReadResult(RegionKey key, boolean needsMigration, boolean rejected) {
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
