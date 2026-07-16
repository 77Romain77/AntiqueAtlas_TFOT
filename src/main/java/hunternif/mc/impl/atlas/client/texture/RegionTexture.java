package hunternif.mc.impl.atlas.client.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws a fixed sub-region from a larger texture canvas. */
public final class RegionTexture extends Texture {
    private final int u;
    private final int v;
    private final int regionWidth;
    private final int regionHeight;
    private final int drawWidth;
    private final int drawHeight;

    public RegionTexture(ResourceLocation texture, int textureWidth, int textureHeight,
                         int u, int v, int regionWidth, int regionHeight,
                         int drawWidth, int drawHeight) {
        super(texture, textureWidth, textureHeight);
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        int actualWidth = Math.min(width, drawWidth);
        int actualHeight = Math.min(height, drawHeight);
        int centeredX = x + (width - actualWidth) / 2;
        int centeredY = y + (height - actualHeight) / 2;
        super.draw(graphics, centeredX, centeredY, actualWidth, actualHeight,
                u, v, regionWidth, regionHeight);
    }
}
