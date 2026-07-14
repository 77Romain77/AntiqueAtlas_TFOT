package hunternif.mc.impl.atlas.marker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Provides access to {@link MarkersData}. Maintains a cache on the client side,
 * because WorldClient is reset along with all WorldSavedData when the player
 * changes dimension (fixes #67).
 *
 * @author Hunternif
 */
public class MarkersDataHandler {
    private static final String MARKERS_DATA_PREFIX = "aaMarkers_";

    private final Map<String, MarkersData> markersDataClientCache = new ConcurrentHashMap<>();
    private MarkersData activeClientData;

    /**
     * Loads data for the given atlas or creates a new one.
     */
    public MarkersData getMarkersData(ItemStack stack, Level world) {
        if (stack.getItem() instanceof AtlasItem) {
            return getMarkersData(AtlasItem.getAtlasID(stack), world);
        } else {
            return null;
        }
    }

    /**
     * Loads data for the given atlas ID or creates a new one.
     */
    public MarkersData getMarkersData(int atlasID, Level world) {
        if (world == null) return null;
        String key = getMarkersDataKey(atlasID);
        if (world.isClientSide) {
            if (activeClientData == null) activeClientData = new MarkersData();
            return activeClientData;
        } else {
            DimensionDataStorage manager = ((ServerLevel) world).getDataStorage();
            return manager.computeIfAbsent(MarkersData::fromNbt, MarkersData::new, key);
        }
    }

    public MarkersData getMarkersDataCached(int atlasID, ResourceKey<Level> world)
    {
        if (activeClientData != null) return activeClientData;
        String key = getMarkersDataKey(atlasID);
        return markersDataClientCache.computeIfAbsent(key + world, s -> new MarkersData());
    }

    private String getMarkersDataKey(int atlasID) {
        return MARKERS_DATA_PREFIX + atlasID;
    }

    /**
     * This method resets the cache when the client loads a new world.
     * It is required in order that old markers data is not
     * transferred from a previous world the client visited.
     * <p>
     * Using a "connect" event instead of "disconnect" because according to a
     * form post, the latter event isn't actually fired on the client.
     * </p>
     */
    public void onClientConnectedToServer(boolean ignoredIsRemote) {
        markersDataClientCache.clear();
        activeClientData = null;
    }

    /** Installs the currently selected local map profile. Client side only. */
    public void setClientData(MarkersData data) {
        markersDataClientCache.clear();
        activeClientData = data;
    }
}
