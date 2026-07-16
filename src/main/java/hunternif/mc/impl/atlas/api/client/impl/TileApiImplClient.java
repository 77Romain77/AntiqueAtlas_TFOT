package hunternif.mc.impl.atlas.api.client.impl;

import hunternif.mc.api.client.ClientTileAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.TileRenderIterator;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileDataStorage;
import hunternif.mc.impl.atlas.util.Log;
import hunternif.mc.impl.atlas.util.Rect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class TileApiImplClient implements ClientTileAPI {
    @Override
    public void putTile(Level world, int atlasID, ResourceLocation tile, int chunkX, int chunkZ) {
        ClientMapManager.getInstance().putTile(world.dimension(), chunkX, chunkZ, tile);
    }

    @Override
    public ResourceLocation getTile(Level world, int atlasID, int chunkX, int chunkZ) {
        AtlasData data = AntiqueAtlas.tileData.getData(atlasID, world);
        return data.getWorldData(world.dimension()).getTile(chunkX, chunkZ);
    }

    @Override
    public void putGlobalTile(Level world, ResourceLocation tile, int chunkX, int chunkZ) {
        Log.warn("Client attempted to put global tile");
    }

    @Override
    public ResourceLocation getGlobalTile(Level world, int chunkX, int chunkZ) {
        TileDataStorage data = AntiqueAtlas.globalTileData.getData(world);
        return data.getTile(chunkX, chunkZ);
    }

    @Override
    public void deleteGlobalTile(Level world, int chunkX, int chunkZ) {
        Log.warn("Client attempted to delete global tile");
    }

    @Override
    public TileRenderIterator getTiles(Level world, int atlasID, Rect scope, int step) {
        TileRenderIterator iter = new TileRenderIterator(AntiqueAtlas.tileData
                .getData(atlasID, world)
                .getWorldData(world.dimension()));
        iter.setScope(scope);
        iter.setStep(step);
        return iter;
    }
}
