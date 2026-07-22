package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.client.gui.GuiGraphics;

/** Draws only the visible portion of marker textures at a consistent UI size. */
final class MarkerIconRenderer {
    private MarkerIconRenderer() {
    }

    static Bounds drawNormalized(GuiGraphics graphics, MarkerType type,
                                 int boxX, int boxY, int boxWidth, int boxHeight) {
        if (type == null) return new Bounds(boxX, boxY, 0, 0);
        ITexture texture = type.getTexture();
        if (texture == null) return new Bounds(boxX, boxY, 0, 0);

        MarkerType.VisibleBounds source = type.getVisibleBounds();
        double scale = Math.min(boxWidth / (double) Math.max(1, source.width()),
                boxHeight / (double) Math.max(1, source.height()));
        int width = Math.max(1, (int) Math.round(source.width() * scale));
        int height = Math.max(1, (int) Math.round(source.height() * scale));
        int x = boxX + (boxWidth - width) / 2;
        int y = boxY + (boxHeight - height) / 2;

        texture.draw(graphics, x, y, width, height,
                source.x(), source.y(), source.width(), source.height());
        return new Bounds(x, y, width, height);
    }

    record Bounds(int x, int y, int width, int height) {
    }
}
