package hunternif.mc.impl.atlas.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.network.packet.s2c.play.MapDataS2CPacket;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * This class stores all the data
 */
public class AtlasData extends SavedData {
    public static final int VERSION = 4;
    public static final String TAG_VERSION = "aaVersion";
    public static final String TAG_WORLD_MAP_LIST = "qWorldMap";
    public static final String TAG_WORLD_ID = "qWorldID";
    public static final String TAG_VISITED_CHUNKS = "qVisitedChunks";

    // Navigation
    public static final String TAG_BROWSING_X = "qBrowseX";
    public static final String TAG_BROWSING_Y = "qBrowseY";
    public static final String TAG_BROWSING_ZOOM = "qBrowseZoom";

    /**
     * This map contains, for each dimension, a map of chunks the player
     * has seen. This map is thread-safe.
     * CAREFUL! Don't modify chunk coordinates that are already put in the map!
     */
    private final Map<ResourceKey<Level>, WorldData> worldMap =
            new ConcurrentHashMap<>(2, 0.75f, 2);

    /**
     * Set of players this Atlas data has been sent to.
     */
    private final Set<Player> playersSentTo = new HashSet<>();

    public AtlasData() {
    }

    public static AtlasData fromNbt(CompoundTag compound) {
        AtlasData data = new AtlasData();
        data.updateFromNbt(compound);
        return data;
    }

    public void updateFromNbt(CompoundTag compound) {
        int version = compound.getInt(TAG_VERSION);
        if (version < VERSION) {
            Log.warn("Outdated atlas data format! Was %d but current is %d.", version, VERSION);
            return;
        }

        ListTag worldMapList = compound.getList(TAG_WORLD_MAP_LIST, Tag.TAG_COMPOUND);
        for (int d = 0; d < worldMapList.size(); d++) {
            CompoundTag worldTag = worldMapList.getCompound(d);
            ResourceKey<Level> worldID;
            worldID = ResourceKey.create(Registries.DIMENSION, VersionHelper.toLoc(worldTag.getString(TAG_WORLD_ID)));
            ListTag dimensionTag = (ListTag) worldTag.get(TAG_VISITED_CHUNKS);
            WorldData dimData = this.getWorldData(worldID);
            dimData.readFromNBT(dimensionTag);
            double zoom = worldTag.getDouble(TAG_BROWSING_ZOOM);
            if (zoom == 0) zoom = 0.5;
            dimData.setBrowsingPosition(worldTag.getInt(TAG_BROWSING_X),
                    worldTag.getInt(TAG_BROWSING_Y), zoom);
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        return writeToNBT(compound, true);
    }

    public CompoundTag writeToNBT(CompoundTag compound, boolean includeTileData) {
        ListTag dimensionMapList = new ListTag();
        compound.putInt(TAG_VERSION, VERSION);
        for (Entry<ResourceKey<Level>, WorldData> dimensionEntry : worldMap.entrySet()) {
            CompoundTag dimTag = new CompoundTag();
            dimTag.putString(TAG_WORLD_ID, dimensionEntry.getKey().location().toString());
            WorldData dimData = dimensionEntry.getValue();
            if (includeTileData) {
                dimTag.put(TAG_VISITED_CHUNKS, dimData.writeToNBT());
            }
            dimTag.putInt(TAG_BROWSING_X, dimData.getBrowsingX());
            dimTag.putInt(TAG_BROWSING_Y, dimData.getBrowsingY());
            dimTag.putDouble(TAG_BROWSING_ZOOM, dimData.getBrowsingZoom());
            dimensionMapList.add(dimTag);
        }
        compound.put(TAG_WORLD_MAP_LIST, dimensionMapList);

        return compound;
    }

    /**
     * Puts a given tile into given map at specified coordinates and,
     * if tileStitcher is present, sets appropriate sectors on adjacent tiles.
     */
    public void setTile(ResourceKey<Level> world, int x, int y, ResourceLocation tile) {
        WorldData worldData = getWorldData(world);
        worldData.setTile(x, y, tile);
    }

    /**
     * Returns the Tile previously set at given coordinates.
     */
    public ResourceLocation removeTile(ResourceKey<Level> world, int x, int y) {
        WorldData dimData = getWorldData(world);
        return dimData.removeTile(x, y);
    }

    public Set<ResourceKey<Level>> getVisitedWorlds() {
        return worldMap.keySet();
    }

    /* TODO: Packet Rework
     *   Dimension data should check the server for updates*/

    /**
     * If this dimension is not yet visited, empty DimensionData will be created.
     */
    public WorldData getWorldData(ResourceKey<Level> world) {
        return worldMap.computeIfAbsent(world, k -> new WorldData(this, world));
    }

    public Map<ChunkPos, ResourceLocation> getSeenChunksInWorld(ResourceKey<Level> world) {
        return getWorldData(world).getSeenChunks();
    }

    /**
     * The set of players this AtlasData has already been sent to.
     */
    public Collection<Player> getSyncedPlayers() {
        return Collections.unmodifiableCollection(playersSentTo);
    }

    /**
     * Whether this AtlasData has already been sent to the specified player.
     */
    public boolean isSyncedToPlayer(Player player) {
        return playersSentTo.contains(player);
    }

    /**
     * Send all data to the player in several zipped packets. Called once
     * during the first run of ItemAtlas.onUpdate().
     */
    public void syncToPlayer(int atlasID, Player player) {
        CompoundTag data = new CompoundTag();

        // Before syncing make sure the changes are written to the nbt.
        // Do not include dimension tile data.  This will happen later.
        writeToNBT(data, false);
        new MapDataS2CPacket(atlasID, data).send((ServerPlayer) player);

        for (ResourceKey<Level> world : worldMap.keySet()) {
            worldMap.get(world).syncToPlayer(atlasID, player);
        }

        Log.info("Sent Atlas #%d data to player %s", atlasID, player.createCommandSourceStack().getTextName());
        playersSentTo.add(player);
    }

    public boolean isEmpty() {
        return worldMap.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AtlasData other)) return false;
        // TODO: This doesn't handle disjoint DimensionType keysets of equal size
        if (other.worldMap.size() != worldMap.size()) return false;
        for (ResourceKey<Level> key : worldMap.keySet()) {
            if (!worldMap.get(key).equals(other.worldMap.get(key))) return false;
        }
        return true;
    }
}
