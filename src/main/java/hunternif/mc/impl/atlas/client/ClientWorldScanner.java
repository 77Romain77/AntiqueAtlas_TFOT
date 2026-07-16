package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.core.scaning.ITileDetector;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorBase;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorEnd;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Scans only chunks already received by this client, with a bounded per-tick budget. */
public final class ClientWorldScanner {
    private final ArrayDeque<ChunkKey> discoveryQueue = new ArrayDeque<>();
    private final Set<ChunkKey> discoveryQueued = new HashSet<>();
    private final ArrayDeque<ChunkKey> rescanQueue = new ArrayDeque<>();
    private final Set<ChunkKey> rescanQueued = new HashSet<>();
    private final TileDetectorBase overworldDetector = new TileDetectorBase();
    private final TileDetectorNether netherDetector = new TileDetectorNether();
    private final TileDetectorEnd endDetector = new TileDetectorEnd();

    private ResourceKey<Level> lastDimension;
    private ChunkPos lastPlayerChunk;
    private int discoveryTicker;
    private RescanProgress activeRescan;

    public void tick(Minecraft minecraft) {
        ClientMapManager maps = ClientMapManager.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !maps.isReady()) {
            reset();
            return;
        }
        if (AntiqueAtlas.CONFIG.itemNeeded && ClientAtlasItem.find(minecraft.player).isEmpty()) {
            clearDiscoveryQueue();
            cancelRescan();
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        ChunkPos playerChunk = minecraft.player.chunkPosition();
        boolean contextChanged = !dimension.equals(lastDimension);
        boolean movedChunk = !playerChunk.equals(lastPlayerChunk);

        if (contextChanged) {
            clearDiscoveryQueue();
            if (activeRescan != null && !activeRescan.matches(dimension)) cancelRescan();
            TileDetectorBase.scanBiomeTypes(level);
        }

        // Recheck once per second as chunks can finish loading while the player is stationary.
        if (contextChanged || movedChunk || ++discoveryTicker >= 20) {
            enqueueLoadedUnseenChunks(level, playerChunk, maps);
            discoveryTicker = 0;
            lastDimension = dimension;
            lastPlayerChunk = playerChunk;
        }

        int budget = Math.max(1, AntiqueAtlas.CONFIG.clientScanBudget);
        for (int i = 0; i < budget; i++) {
            // A manual operation is short-lived and gets the whole budget so
            // it finishes promptly; normal discovery resumes immediately after.
            if (activeRescan != null) {
                if (!processNextRescan(minecraft, level, maps)) break;
            } else if (!processNextDiscovery(level, maps)) {
                break;
            }
        }
    }

    /**
     * Queues one bounded pass over known chunks that are already loaded around
     * the player. Repeated requests are rejected until that pass is complete.
     */
    public RescanRequest requestRescan(Minecraft minecraft) {
        if (activeRescan != null) {
            return new RescanRequest(RescanRequestResult.ALREADY_RUNNING, activeRescan.totalChunks);
        }

        ClientMapManager maps = ClientMapManager.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !maps.isReady()) {
            return new RescanRequest(RescanRequestResult.UNAVAILABLE, 0);
        }
        if (AntiqueAtlas.CONFIG.itemNeeded && ClientAtlasItem.find(minecraft.player).isEmpty()) {
            return new RescanRequest(RescanRequestResult.UNAVAILABLE, 0);
        }

        rescanQueue.clear();
        rescanQueued.clear();
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos center = minecraft.player.chunkPosition();
        int radius = Math.max(0, AntiqueAtlas.CONFIG.scanRadius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = center.x + dx;
                int z = center.z + dz;
                if (!level.getChunkSource().hasChunk(x, z)) continue;
                if (!maps.getAtlasData().getWorldData(dimension).hasTileAt(x, z)) continue;
                ChunkKey key = new ChunkKey(dimension, x, z);
                if (rescanQueued.add(key)) rescanQueue.addLast(key);
            }
        }

        int queuedChunks = rescanQueue.size();
        if (queuedChunks == 0) {
            return new RescanRequest(RescanRequestResult.NOTHING_TO_SCAN, 0);
        }

        TileDetectorBase.scanBiomeTypes(level);
        activeRescan = new RescanProgress(dimension, queuedChunks);
        return new RescanRequest(RescanRequestResult.STARTED, queuedChunks);
    }

    public RescanStatus getRescanStatus() {
        if (activeRescan == null) return new RescanStatus(false, 0, 0, 0, 0);
        return new RescanStatus(true, activeRescan.completedChunks, activeRescan.totalChunks,
                activeRescan.analyzedChunks, activeRescan.changedChunks);
    }

    public void reset() {
        clearDiscoveryQueue();
        cancelRescan();
        lastDimension = null;
        lastPlayerChunk = null;
        discoveryTicker = 0;
    }

    private void enqueueLoadedUnseenChunks(ClientLevel level, ChunkPos center, ClientMapManager maps) {
        int radius = Math.max(0, AntiqueAtlas.CONFIG.scanRadius);
        ResourceKey<Level> dimension = level.dimension();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = center.x + dx;
                int z = center.z + dz;
                if (!level.getChunkSource().hasChunk(x, z)) continue;
                if (maps.getAtlasData().getWorldData(dimension).hasTileAt(x, z)) continue;
                ChunkKey pending = new ChunkKey(dimension, x, z);
                if (discoveryQueued.add(pending)) discoveryQueue.addLast(pending);
            }
        }
    }

    private boolean processNextDiscovery(ClientLevel level, ClientMapManager maps) {
        ChunkKey pending = discoveryQueue.pollFirst();
        if (pending == null) return false;
        discoveryQueued.remove(pending);
        if (!pending.dimension.equals(level.dimension())
                || !level.getChunkSource().hasChunk(pending.x, pending.z)) return true;
        if (maps.getAtlasData().getWorldData(pending.dimension).hasTileAt(pending.x, pending.z)) return true;

        ChunkAccess chunk = level.getChunk(pending.x, pending.z);
        ResourceLocation tile = detectorFor(pending.dimension).getBiomeID(level, chunk);
        if (tile != null) maps.putTile(pending.dimension, pending.x, pending.z, tile);
        return true;
    }

    private boolean processNextRescan(Minecraft minecraft, ClientLevel level, ClientMapManager maps) {
        if (activeRescan == null) return false;
        if (!activeRescan.matches(level.dimension())) {
            cancelRescan();
            return false;
        }

        ChunkKey pending = rescanQueue.pollFirst();
        if (pending == null) {
            finishRescan(minecraft);
            return false;
        }
        rescanQueued.remove(pending);

        boolean analyzed = false;
        boolean changed = false;
        if (pending.dimension.equals(level.dimension())
                && level.getChunkSource().hasChunk(pending.x, pending.z)
                && maps.getAtlasData().getWorldData(pending.dimension).hasTileAt(pending.x, pending.z)) {
            ChunkAccess chunk = level.getChunk(pending.x, pending.z);
            ResourceLocation tile = detectorFor(pending.dimension).getBiomeID(level, chunk);
            analyzed = true;
            changed = tile == null
                    ? maps.removeTile(pending.dimension, pending.x, pending.z)
                    : maps.putTile(pending.dimension, pending.x, pending.z, tile);
        }

        activeRescan.completedChunks++;
        if (analyzed) activeRescan.analyzedChunks++;
        if (changed) activeRescan.changedChunks++;
        if (rescanQueue.isEmpty()) finishRescan(minecraft);
        return true;
    }

    private void finishRescan(Minecraft minecraft) {
        RescanProgress finished = activeRescan;
        activeRescan = null;
        rescanQueue.clear();
        rescanQueued.clear();
        if (finished != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    "message.antiqueatlas.rescan.complete",
                    finished.analyzedChunks, finished.changedChunks), true);
        }
    }

    private void clearDiscoveryQueue() {
        discoveryQueue.clear();
        discoveryQueued.clear();
    }

    private void cancelRescan() {
        activeRescan = null;
        rescanQueue.clear();
        rescanQueued.clear();
    }

    private ITileDetector detectorFor(ResourceKey<Level> dimension) {
        if (Level.NETHER.equals(dimension)) return netherDetector;
        if (Level.END.equals(dimension)) return endDetector;
        return overworldDetector;
    }

    public enum RescanRequestResult {
        STARTED,
        ALREADY_RUNNING,
        NOTHING_TO_SCAN,
        UNAVAILABLE
    }

    public record RescanRequest(RescanRequestResult result, int queuedChunks) {
    }

    public record RescanStatus(boolean running, int completedChunks, int totalChunks,
                               int analyzedChunks, int changedChunks) {
    }

    private static final class RescanProgress {
        private final ResourceKey<Level> dimension;
        private final int totalChunks;
        private int completedChunks;
        private int analyzedChunks;
        private int changedChunks;

        private RescanProgress(ResourceKey<Level> dimension, int totalChunks) {
            this.dimension = dimension;
            this.totalChunks = totalChunks;
        }

        private boolean matches(ResourceKey<Level> dimension) {
            return this.dimension.equals(dimension);
        }
    }

    private record ChunkKey(ResourceKey<Level> dimension, int x, int z) {
    }
}
