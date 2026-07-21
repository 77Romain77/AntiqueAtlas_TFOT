package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.marker.MarkerColor;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the compact octagonal badge used behind colored marker icons. */
final class MarkerColorRenderer {
    private static final int OUTLINE_RGB = 0x24180F;

    private MarkerColorRenderer() {
    }

    static void drawBadge(GuiGraphics graphics, int x, int y, int width, int height,
                          MarkerColor color) {
        drawBadge(graphics, x, y, width, height, color, 0xD8);
    }

    static void drawBadge(GuiGraphics graphics, int x, int y, int width, int height,
                          MarkerColor color, int alpha) {
        if (color == null || !color.isColored() || width < 3 || height < 3) return;
        fillOctagon(graphics, x, y, width, height, argb(alpha, OUTLINE_RGB));
        fillOctagon(graphics, x + 1, y + 1, width - 2, height - 2,
                argb(alpha, color.getRgb()));
    }

    static void drawNoColorSwatch(GuiGraphics graphics, int x, int y, int size) {
        int inner = Math.max(2, size - 4);
        int left = x + 2;
        int top = y + 2;
        int half = Math.max(1, inner / 2);
        graphics.fill(left, top, left + inner, top + inner, 0xFFDDD2BA);
        graphics.fill(left + half, top, left + inner, top + half, 0xFF7A6B58);
        graphics.fill(left, top + half, left + half, top + inner, 0xFF7A6B58);
        for (int i = 0; i < inner; i++) {
            graphics.fill(left + i, top + inner - i - 1,
                    left + i + 1, top + inner - i, 0xFF9E2F2F);
        }
    }

    private static void fillOctagon(GuiGraphics graphics, int x, int y,
                                    int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        int corner = Math.max(1, Math.min(Math.min(width, height) / 5, 3));
        graphics.fill(x + corner, y, x + width - corner, y + corner, color);
        graphics.fill(x, y + corner, x + width, y + height - corner, color);
        graphics.fill(x + corner, y + height - corner,
                x + width - corner, y + height, color);
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
