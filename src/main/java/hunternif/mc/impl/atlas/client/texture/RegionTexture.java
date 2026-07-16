package hunternif.mc.impl.atlas.client.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws a fixed sub-region from a larger texture canvas. */
public final class RegionTexture extends Texture {
    private final int u;
    private final int v;
    private final int regionWidth;
    private final int regionHeight;

    public RegionTexture(ResourceLocation texture, int textureWidth, int textureHeight,
                         int u, int v, int regionWidth, int regionHeight) {
        super(texture, textureWidth, textureHeight);
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        super.draw(graphics, x, y, width, height, u, v, regionWidth, regionHeight);
    }
}
