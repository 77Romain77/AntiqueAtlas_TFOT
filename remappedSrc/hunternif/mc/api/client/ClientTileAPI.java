package hunternif.mc.api.client;

import hunternif.mc.api.TileAPI;
import hunternif.mc.impl.atlas.client.TileRenderIterator;
import hunternif.mc.impl.atlas.util.Rect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public interface ClientTileAPI extends TileAPI {
    TileRenderIterator getTiles(Level world, int atlasID, Rect scope, int step);
}
