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
    private static final int FORMAT_VERSION = 3;
    private static final int BINDING_VERSION = 1;
    private static final String HIDDEN_MARKER_TYPES_TAG = "hiddenMarkerTypes";
    private static final String AUTO_DEATH_MARKER_TAG = "autoDeathMarker";
    private static final String BINDING_VERSION_TAG = "aaBindingVersion";
    private static final String MAP_ID_TAG = "aaMapId";
    private static final String WORLD_ID_TAG = "aaWorldId";
    private static final String OWNER_NAME_TAG = "aaOwnerName";
    private static final String OWNER_FINGERPRINT_TAG = "aaOwnerFingerprint";
    private static final String WORLD_FINGERPRINT_TAG = "aaWorldFingerprint";
    private static final String REGION_FINGERPRINT_TAG = "aaRegionFingerprint";
    private static final String REGION_MIGRATION_PENDING_TAG = "aaRegionMigrationPending";
    private static final String TERRAIN_MIGRATION_COMPLETE_TAG = "aaTerrainMigrationComplete";
    private static final String OWNER_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-owner-v1";
    private static final String PROFILE_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-profile-v1";
    private static final String LEGACY_REGION_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-region-v1";
    private static final String WORLD_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-world-v1";
    private static final String SHARED_REGION_FINGERPRINT_CONTEXT = "antiqueatlas-tfot-shared-region-v1";
    private static final int SAVE_INTERVAL_TICKS = 100;
    private static final String DEFAULT_PROFILE_ID = "map_0001";
    private static final String DEFAULT_WORLD_ID = "world_0001";
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
    private AtlasData sharedAtlasData;
    private volatile WorldBinding worldBinding;
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
        return activeProfile == null ? null : sharedAtlasData;
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

    public boolean isAutoDeathMarkerEnabled() {
        return activeProfile != null && activeProfile.autoDeathMarker;
    }

    public void setAutoDeathMarkerEnabled(boolean enabled) {
        if (activeProfile == null || activeProfile.autoDeathMarker == enabled) return;
        activeProfile.autoDeathMarker = enabled;
        stateDirty = true;
    }

    public int getActiveAtlasId() {
        return activeProfileId == null ? 0 : activeProfileId.hashCode();
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public String getActiveProfileName() {
        ProfileInfo info = profiles.get(activeProfileId);
        return info == null ? defaultProfileName(1) : info.name();
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

        ProfileInfo info = new ProfileInfo(id, defaultProfileName(profiles.size() + 1));
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
        ClientProfile loaded = loadProfile(profileId);
        if (loaded == null) return false;

        activeProfileId = profileId;
        activeProfile = loaded;
        applyActiveNavigation();
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
        if (activeProfile == null || sharedAtlasData == null || tile == null) return false;
        ResourceLocation canonical = tileIds.computeIfAbsent(tile.toString(), ignored -> tile);
        ResourceLocation old = sharedAtlasData.getWorldData(dimension).getTile(chunkX, chunkZ);
        if (canonical.equals(old)) return false;
        sharedAtlasData.setTile(dimension, chunkX, chunkZ, canonical);
        dirtyRegions.add(RegionKey.of(dimension, chunkX, chunkZ));
        return true;
    }

    public boolean removeTile(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (activeProfile == null || sharedAtlasData == null) return false;
        ResourceLocation removed = sharedAtlasData.removeTile(dimension, chunkX, chunkZ);
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
        sharedAtlasData = null;
        worldBinding = null;
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
        if (profiles.isEmpty()) {
            profiles.put(DEFAULT_PROFILE_ID, new ProfileInfo(DEFAULT_PROFILE_ID, defaultProfileName(1)));
        }
        if (activeProfileId == null || !profiles.containsKey(activeProfileId)) activeProfileId = profiles.keySet().iterator().next();

        // Claim the selected storage root before migrating any legacy profile.
        // This prevents another local nickname from taking it if the client is
        // closed in the middle of the one-time region migration.
        writeIndex();
        claimLegacyProfileBindings();

        ClientProfile loaded = loadProfile(activeProfileId);
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
            profiles.put(activeProfileId,
                    new ProfileInfo(activeProfileId, defaultProfileName(profiles.size() + 1)));
            loaded = loadProfile(activeProfileId);
        }
        if (loaded == null) throw new IllegalStateException("Could not create a local map owned by the current player");

        sharedAtlasData = new AtlasData();
        loadSharedTerrain(activeProfileId);
        activeProfile = loaded;
        applyActiveNavigation();
        dirtyRegions.clear();
        stateDirty = false;
        installActiveData();
        writeIndex();
        AntiqueAtlasClientSegment.resetAtlasGUI();
        AntiqueAtlasClientSegment.resetClientScanner();
        AntiqueAtlas.LOG.info("Loaded local atlas '{}' for {}", getActiveProfileName(), address);
    }

    private void installActiveData() {
        AntiqueAtlas.tileData.setClientData(activeProfile == null ? null : sharedAtlasData);
        AntiqueAtlas.markersData.setClientData(activeProfile == null ? null : activeProfile.markersData);
    }

    private void captureActiveNavigation() {
        if (activeProfile == null || sharedAtlasData == null) return;
        Set<ResourceKey<Level>> dimensions = new LinkedHashSet<>(sharedAtlasData.getVisitedWorlds());
        dimensions.addAll(activeProfile.navigationData.getVisitedWorlds());
        for (ResourceKey<Level> dimension : dimensions) {
            var shared = sharedAtlasData.getWorldData(dimension);
            activeProfile.navigationData.getWorldData(dimension).setBrowsingPosition(
                    shared.getBrowsingX(), shared.getBrowsingY(), shared.getBrowsingZoom());
        }
    }

    private void applyActiveNavigation() {
        if (activeProfile == null || sharedAtlasData == null) return;
        Set<ResourceKey<Level>> dimensions = new LinkedHashSet<>(sharedAtlasData.getVisitedWorlds());
        dimensions.addAll(activeProfile.navigationData.getVisitedWorlds());
        for (ResourceKey<Level> dimension : dimensions) {
            var shared = sharedAtlasData.getWorldData(dimension);
            if (activeProfile.navigationData.getVisitedWorlds().contains(dimension)) {
                var saved = activeProfile.navigationData.getWorldData(dimension);
                shared.setBrowsingPosition(saved.getBrowsingX(), saved.getBrowsingY(), saved.getBrowsingZoom());
            } else {
                shared.setBrowsingPosition(0, 0, AntiqueAtlas.CONFIG.defaultScale);
            }
        }
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

    private ClientProfile loadProfile(String profileId) {
        AtlasData navigationData = new AtlasData();
        MarkersData markersData = new MarkersData();
        Set<String> hiddenMarkerTypes = new LinkedHashSet<>();
        Path folder = profileFolder(profileId);
        Path stateFile = folder.resolve("profile.dat");
        Path dimensions = folder.resolve("dimensions");
        CompoundTag savedState = null;
        boolean hadStateFile = Files.isRegularFile(stateFile);
        boolean autoDeathMarker = true;

        if (hadStateFile) {
            try {
                savedState = NbtIo.readCompressed(stateFile.toFile());
                if (savedState.contains("atlas", Tag.TAG_COMPOUND)) {
                    navigationData.updateFromNbt(savedState.getCompound("atlas"));
                }
                if (savedState.contains("markers", Tag.TAG_COMPOUND)) {
                    markersData = MarkersData.fromNbt(savedState.getCompound("markers"));
                }
                if (savedState.contains(AUTO_DEATH_MARKER_TAG, Tag.TAG_BYTE)) {
                    autoDeathMarker = savedState.getBoolean(AUTO_DEATH_MARKER_TAG);
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

        ClientProfile profile = new ClientProfile(navigationData, markersData, hiddenMarkerTypes,
                autoDeathMarker, binding, migrationPending);

        // A small synchronous checkpoint makes the owner and random map ID
        // durable before any legacy terrain is moved into shared storage.
        if (!declaresBinding) {
            writeNbtAtomic(stateFile, createStateTag(profile, migrationPending));
        }
        return profile;
    }

    private RegionReadResult readLegacyRegion(Path path, AtlasData atlasData, ProfileBinding binding,
                                              boolean allowUnboundRegion, boolean fillMissingOnly) {
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
                        legacyRegionFingerprint(binding, key, paletteTag, tiles))) {
                    return new RegionReadResult(key, true);
                }
            } else if (!allowUnboundRegion) {
                return new RegionReadResult(key, true);
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
                if (fillMissingOnly && atlasData.getWorldData(dimension).hasTileAt(x, z)) continue;
                atlasData.setTile(dimension, x, z, palette.get(paletteIndex));
            }
            return new RegionReadResult(key, false);
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load local atlas region {}", path, exception);
            return null;
        }
    }

    /**
     * Loads the one terrain dataset shared by every map profile for this
     * player/server pair. Existing per-map regions are merged once, with the
     * active map taking priority and the other maps only filling missing tiles.
     */
    private void loadSharedTerrain(String preferredProfileId) {
        worldBinding = readWorldBinding();

        int rejectedSharedRegions = 0;
        Path sharedDimensions = worldFolder().resolve("dimensions");
        for (Path path : listRegionFiles(sharedDimensions)) {
            RegionReadResult result = readSharedRegion(path, sharedAtlasData, worldBinding);
            if (result == null || result.rejected) rejectedSharedRegions++;
        }
        if (rejectedSharedRegions > 0) {
            AntiqueAtlas.LOG.warn("Skipped {} shared atlas region(s) whose owner or contents did not match player '{}'",
                    rejectedSharedRegions, currentOwnerName);
        }

        if (worldBinding.terrainMigrationComplete) return;

        LinkedHashSet<String> orderedProfiles = new LinkedHashSet<>();
        if (preferredProfileId != null) orderedProfiles.add(preferredProfileId);
        orderedProfiles.addAll(profiles.keySet());

        Set<RegionKey> migratedRegions = new LinkedHashSet<>();
        int rejectedLegacyRegions = 0;
        for (String profileId : orderedProfiles) {
            LegacyProfileAccess access = readLegacyProfileAccess(profileId);
            if (access == null) continue;
            Path dimensions = profileFolder(profileId).resolve("dimensions");
            for (Path path : listRegionFiles(dimensions)) {
                RegionReadResult result = readLegacyRegion(path, sharedAtlasData, access.binding,
                        access.allowUnboundRegions, true);
                if (result == null || result.rejected) {
                    rejectedLegacyRegions++;
                } else {
                    migratedRegions.add(result.key);
                }
            }
        }
        if (rejectedLegacyRegions > 0) {
            AntiqueAtlas.LOG.warn("Skipped {} legacy atlas region(s) whose owner or contents were invalid",
                    rejectedLegacyRegions);
        }

        startSharedTerrainMigration(migratedRegions);
    }

    private WorldBinding readWorldBinding() {
        String expectedFingerprint = worldFingerprint(currentOwnerFingerprint, DEFAULT_WORLD_ID);
        WorldBinding expected = new WorldBinding(DEFAULT_WORLD_ID, expectedFingerprint,
                currentOwnerName, false);
        Path stateFile = worldFolder().resolve("world.dat");
        if (!Files.isRegularFile(stateFile)) {
            writeNbtAtomic(stateFile, createWorldStateTag(expected));
            return expected;
        }

        try {
            CompoundTag root = NbtIo.readCompressed(stateFile.toFile());
            String worldId = root.getString(WORLD_ID_TAG);
            String storedFingerprint = root.getString(WORLD_FINGERPRINT_TAG);
            if (root.getInt(BINDING_VERSION_TAG) != BINDING_VERSION
                    || !DEFAULT_WORLD_ID.equals(worldId)
                    || !expectedFingerprint.equals(storedFingerprint)) {
                AntiqueAtlas.LOG.warn("Ignoring shared atlas terrain in {} because it belongs to another player",
                        stateFile);
                writeNbtAtomic(stateFile, createWorldStateTag(expected));
                return expected;
            }
            String storedOwner = root.getString(OWNER_NAME_TAG);
            return new WorldBinding(worldId, storedFingerprint,
                    storedOwner.isBlank() ? currentOwnerName : storedOwner,
                    root.getBoolean(TERRAIN_MIGRATION_COMPLETE_TAG));
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load shared atlas world state {}", stateFile, exception);
            writeNbtAtomic(stateFile, createWorldStateTag(expected));
            return expected;
        }
    }

    private LegacyProfileAccess readLegacyProfileAccess(String profileId) {
        Path stateFile = profileFolder(profileId).resolve("profile.dat");
        if (!Files.isRegularFile(stateFile)) return null;
        try {
            CompoundTag root = NbtIo.readCompressed(stateFile.toFile());
            if (!declaresProfileBinding(root)
                    || root.getInt(BINDING_VERSION_TAG) != BINDING_VERSION) return null;
            String mapId = root.getString(MAP_ID_TAG);
            String storedFingerprint = root.getString(OWNER_FINGERPRINT_TAG);
            if (mapId.isBlank()
                    || !profileFingerprint(currentOwnerFingerprint, mapId).equals(storedFingerprint)) {
                return null;
            }
            String storedOwner = root.getString(OWNER_NAME_TAG);
            return new LegacyProfileAccess(new ProfileBinding(mapId, storedFingerprint,
                    storedOwner.isBlank() ? currentOwnerName : storedOwner),
                    root.getBoolean(REGION_MIGRATION_PENDING_TAG));
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not inspect legacy atlas profile {}", stateFile, exception);
            return null;
        }
    }

    private RegionReadResult readSharedRegion(Path path, AtlasData atlasData, WorldBinding binding) {
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            ResourceLocation dimensionId = ResourceLocation.tryParse(root.getString("dimension"));
            if (dimensionId == null) return null;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            RegionKey key = new RegionKey(dimension, root.getInt("regionX"), root.getInt("regionZ"));
            ListTag paletteTag = root.getList("palette", Tag.TAG_STRING);
            int[] tiles = root.getIntArray("tiles");
            if (root.getInt(BINDING_VERSION_TAG) != BINDING_VERSION
                    || !root.contains(REGION_FINGERPRINT_TAG, Tag.TAG_STRING)
                    || !root.getString(REGION_FINGERPRINT_TAG).equals(
                    sharedRegionFingerprint(binding, key, paletteTag, tiles))) {
                return new RegionReadResult(key, true);
            }

            List<ResourceLocation> palette = new ArrayList<>(paletteTag.size());
            for (int index = 0; index < paletteTag.size(); index++) {
                ResourceLocation id = ResourceLocation.tryParse(paletteTag.getString(index));
                palette.add(id == null ? AntiqueAtlas.id("unknown")
                        : tileIds.computeIfAbsent(id.toString(), ignored -> id));
            }
            int length = Math.min(tiles.length, REGION_SIZE * REGION_SIZE);
            for (int index = 0; index < length; index++) {
                int paletteIndex = tiles[index] - 1;
                if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                int x = key.regionX * REGION_SIZE + index % REGION_SIZE;
                int z = key.regionZ * REGION_SIZE + index / REGION_SIZE;
                atlasData.setTile(dimension, x, z, palette.get(paletteIndex));
            }
            return new RegionReadResult(key, false);
        } catch (Exception exception) {
            AntiqueAtlas.LOG.error("Could not load shared atlas region {}", path, exception);
            return null;
        }
    }

    private void startSharedTerrainMigration(Set<RegionKey> migratedRegions) {
        WorldBinding pendingBinding = worldBinding;
        List<RegionSnapshot> snapshots = new ArrayList<>(migratedRegions.size());
        for (RegionKey key : migratedRegions) snapshots.add(snapshotRegion(key));

        WorldBinding completedBinding = new WorldBinding(pendingBinding.worldId,
                pendingBinding.fingerprint, pendingBinding.ownerName, true);
        StateSnapshot completedState = new StateSnapshot(worldFolder().resolve("world.dat"),
                createWorldStateTag(completedBinding));

        AntiqueAtlas.LOG.info("Moving {} legacy atlas region(s) into shared terrain for player '{}'",
                snapshots.size(), currentOwnerName);
        ioExecutor.submit(() -> {
            boolean success = true;
            for (RegionSnapshot snapshot : snapshots) {
                if (!writeNbtAtomic(snapshot.path, snapshot.data)) success = false;
            }
            if (success) success = writeNbtAtomic(completedState.path, completedState.data);
            if (success) {
                if (worldBinding == pendingBinding) worldBinding = completedBinding;
            } else {
                AntiqueAtlas.LOG.warn("The shared atlas terrain migration will be retried next time");
            }
        });
    }

    private CompoundTag createWorldStateTag(WorldBinding binding) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        root.putInt(BINDING_VERSION_TAG, BINDING_VERSION);
        root.putString(WORLD_ID_TAG, binding.worldId);
        root.putString(OWNER_NAME_TAG, binding.ownerName);
        root.putString(WORLD_FINGERPRINT_TAG, binding.fingerprint);
        root.putBoolean(TERRAIN_MIGRATION_COMPLETE_TAG, binding.terrainMigrationComplete);
        return root;
    }

    private static List<Path> listRegionFiles(Path dimensions) {
        if (!Files.isDirectory(dimensions)) return List.of();
        try (var files = Files.walk(dimensions)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("r."))
                    .filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not enumerate local atlas regions in {}", dimensions, exception);
            return List.of();
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
            ResourceLocation tile = sharedAtlasData.getWorldData(key.dimension).getTile(x, z);
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
                sharedRegionFingerprint(worldBinding, key, paletteTag, tiles));
        return new RegionSnapshot(regionPath(key), root);
    }

    private StateSnapshot snapshotState() {
        if (!stateDirty || activeProfile == null) return null;
        captureActiveNavigation();
        stateDirty = false;
        return new StateSnapshot(profileFolder(activeProfileId).resolve("profile.dat"),
                createStateTag(activeProfile, activeProfile.legacyRegionsAllowed));
    }

    private CompoundTag createStateTag(ClientProfile profile, boolean migrationPending) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        root.putInt(BINDING_VERSION_TAG, BINDING_VERSION);
        root.putString(MAP_ID_TAG, profile.binding.mapId);
        root.putString(OWNER_NAME_TAG, profile.binding.ownerName);
        root.putString(OWNER_FINGERPRINT_TAG, profile.binding.ownerFingerprint);
        root.putBoolean(REGION_MIGRATION_PENDING_TAG, migrationPending);
        root.putBoolean(AUTO_DEATH_MARKER_TAG, profile.autoDeathMarker);
        root.put("atlas", profile.navigationData.writeToNBT(new CompoundTag(), false));
        root.put("markers", profile.markersData.save(new CompoundTag()));
        ListTag hiddenTypes = new ListTag();
        for (String id : profile.hiddenMarkerTypes) hiddenTypes.add(StringTag.valueOf(id));
        root.put(HIDDEN_MARKER_TYPES_TAG, hiddenTypes);
        return root;
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
        String legacyOwner = readIndexOwnerFingerprint(baseServerFolder.resolve("maps.json"));
        Path playersFolder = baseServerFolder.resolve("players");
        String slug = normalizePlayerName(currentOwnerName).replaceAll("[^a-z0-9._-]+", "_");
        if (slug.isBlank()) slug = "player";
        if (slug.length() > 32) slug = slug.substring(0, 32);
        String baseName = slug + "-" + currentOwnerFingerprint.substring(0, 12);
        for (int suffix = 0; suffix < 100; suffix++) {
            Path candidate = playersFolder.resolve(suffix == 0 ? baseName : baseName + "-" + suffix);
            String candidateOwner = readIndexOwnerFingerprint(candidate.resolve("maps.json"));
            if (currentOwnerFingerprint.equals(candidateOwner)) return candidate;
            if (candidateOwner == null) {
                boolean legacyBelongsHere = suffix == 0
                        && (legacyOwner == null || currentOwnerFingerprint.equals(legacyOwner))
                        && hasLegacyRoot(baseServerFolder);
                if (legacyBelongsHere && !migrateLegacyRoot(baseServerFolder, candidate)) {
                    // Keep using the original location for this session rather
                    // than risking a partial migration or data loss. If even
                    // the rollback failed, follow the index wherever it ended up.
                    return Files.isRegularFile(baseServerFolder.resolve("maps.json"))
                            ? baseServerFolder : candidate;
                }
                AntiqueAtlas.LOG.info("Local atlas storage for '{}' uses its own player folder",
                        currentOwnerName);
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate an owned local atlas folder for " + currentOwnerName);
    }

    private static boolean hasLegacyRoot(Path baseServerFolder) {
        return Files.isRegularFile(baseServerFolder.resolve("maps.json"))
                || Files.isDirectory(baseServerFolder.resolve("maps"))
                || Files.isDirectory(baseServerFolder.resolve("worlds"));
    }

    private static boolean migrateLegacyRoot(Path source, Path target) {
        List<String> moved = new ArrayList<>();
        try {
            Files.createDirectories(target);
            for (String name : List.of("maps", "worlds", "maps.json")) {
                Path from = source.resolve(name);
                Path to = target.resolve(name);
                if (Files.exists(from) && !Files.exists(to)) {
                    moveAtomic(from, to);
                    moved.add(name);
                }
            }
            AntiqueAtlas.LOG.info("Moved legacy local atlas files into {}", target);
            return true;
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not move legacy local atlas files from {} to {}",
                    source, target, exception);
            for (int index = moved.size() - 1; index >= 0; index--) {
                String name = moved.get(index);
                Path from = target.resolve(name);
                Path to = source.resolve(name);
                try {
                    if (Files.exists(from) && !Files.exists(to)) moveAtomic(from, to);
                } catch (IOException rollbackException) {
                    AntiqueAtlas.LOG.error("Could not roll back local atlas path {}", from, rollbackException);
                }
            }
            return false;
        }
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

    private static String defaultProfileName(int number) {
        return Component.translatable("gui.antiqueatlas.maps.defaultName", Math.max(1, number)).getString();
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

    private Path worldFolder() {
        return serverFolder.resolve("worlds").resolve(DEFAULT_WORLD_ID);
    }

    private Path regionPath(RegionKey key) {
        ResourceLocation dimension = key.dimension.location();
        return worldFolder()
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

    private static String worldFingerprint(String ownerFingerprint, String worldId) {
        return sha256Hex(WORLD_FINGERPRINT_CONTEXT, ownerFingerprint, worldId);
    }

    private static String legacyRegionFingerprint(ProfileBinding binding, RegionKey key,
                                                  ListTag palette, int[] tiles) {
        MessageDigest digest = newSha256Digest();
        updateDigest(digest, LEGACY_REGION_FINGERPRINT_CONTEXT);
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

    private static String sharedRegionFingerprint(WorldBinding binding, RegionKey key,
                                                  ListTag palette, int[] tiles) {
        MessageDigest digest = newSha256Digest();
        updateDigest(digest, SHARED_REGION_FINGERPRINT_CONTEXT);
        updateDigest(digest, binding.fingerprint);
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
        private final AtlasData navigationData;
        private final MarkersData markersData;
        private final Set<String> hiddenMarkerTypes;
        private boolean autoDeathMarker;
        private final ProfileBinding binding;
        private final boolean legacyRegionsAllowed;

        private ClientProfile(AtlasData navigationData, MarkersData markersData,
                              Set<String> hiddenMarkerTypes, boolean autoDeathMarker,
                              ProfileBinding binding, boolean legacyRegionsAllowed) {
            this.navigationData = navigationData;
            this.markersData = markersData;
            this.hiddenMarkerTypes = hiddenMarkerTypes;
            this.autoDeathMarker = autoDeathMarker;
            this.binding = binding;
            this.legacyRegionsAllowed = legacyRegionsAllowed;
        }
    }

    private record ProfileBinding(String mapId, String ownerFingerprint, String ownerName) {
    }

    private record WorldBinding(String worldId, String fingerprint, String ownerName,
                                boolean terrainMigrationComplete) {
    }

    private record LegacyProfileAccess(ProfileBinding binding, boolean allowUnboundRegions) {
    }

    private record RegionReadResult(RegionKey key, boolean rejected) {
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
