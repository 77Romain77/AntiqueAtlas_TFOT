package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.core.scaning.ITileDetector;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorBase;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorEnd;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private final ArrayDeque<PendingChunk> queue = new ArrayDeque<>();
    private final Set<PendingChunk> queued = new HashSet<>();
    private final TileDetectorBase overworldDetector = new TileDetectorBase();
    private final TileDetectorNether netherDetector = new TileDetectorNether();
    private final TileDetectorEnd endDetector = new TileDetectorEnd();

    private ResourceKey<Level> lastDimension;
    private ChunkPos lastPlayerChunk;
    private String lastProfileId;
    private int discoveryTicker;

    public void tick(Minecraft minecraft) {
        ClientMapManager maps = ClientMapManager.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !maps.isReady()) {
            reset();
            return;
        }
        if (AntiqueAtlas.CONFIG.itemNeeded && ClientAtlasItem.find(minecraft.player).isEmpty()) {
            queue.clear();
            queued.clear();
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        ChunkPos playerChunk = minecraft.player.chunkPosition();
        String profileId = maps.getActiveProfileId();
        boolean contextChanged = !dimension.equals(lastDimension) || !profileId.equals(lastProfileId);
        boolean movedChunk = !playerChunk.equals(lastPlayerChunk);

        if (contextChanged) {
            queue.clear();
            queued.clear();
            TileDetectorBase.scanBiomeTypes(level);
        }

        // Recheck once per second as chunks can finish loading while the player is stationary.
        if (contextChanged || movedChunk || ++discoveryTicker >= 20) {
            enqueueLoadedUnseenChunks(level, playerChunk, maps);
            discoveryTicker = 0;
            lastDimension = dimension;
            lastPlayerChunk = playerChunk;
            lastProfileId = profileId;
        }

        int budget = Math.max(1, AntiqueAtlas.CONFIG.clientScanBudget);
        for (int i = 0; i < budget; i++) {
            PendingChunk pending = queue.pollFirst();
            if (pending == null) break;
            queued.remove(pending);
            if (!pending.dimension.equals(level.dimension()) || !level.getChunkSource().hasChunk(pending.x, pending.z)) continue;
            if (maps.getAtlasData().getWorldData(pending.dimension).hasTileAt(pending.x, pending.z)) continue;

            ChunkAccess chunk = level.getChunk(pending.x, pending.z);
            ResourceLocation tile = detectorFor(pending.dimension).getBiomeID(level, chunk);
            if (tile != null) maps.putTile(pending.dimension, pending.x, pending.z, tile);
        }
    }

    public void reset() {
        queue.clear();
        queued.clear();
        lastDimension = null;
        lastPlayerChunk = null;
        lastProfileId = null;
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
                PendingChunk pending = new PendingChunk(dimension, x, z);
                if (queued.add(pending)) queue.addLast(pending);
            }
        }
    }

    private ITileDetector detectorFor(ResourceKey<Level> dimension) {
        if (Level.NETHER.equals(dimension)) return netherDetector;
        if (Level.END.equals(dimension)) return endDetector;
        return overworldDetector;
    }

    private record PendingChunk(ResourceKey<Level> dimension, int x, int z) {
    }
}
