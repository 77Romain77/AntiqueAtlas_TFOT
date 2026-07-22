package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.marker.MarkerColor;
import net.minecraft.client.gui.GuiGraphics;

/** Draws marker color swatches and the small standing banner attached to icons. */
final class MarkerColorRenderer {
    private static final int OUTLINE_RGB = 0x24180F;
    private static final int POLE_RGB = 0x5A3A21;

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

    static void drawMarkerBanner(GuiGraphics graphics, int iconX, int iconY,
                                 int iconWidth, int iconHeight, MarkerColor color) {
        drawMarkerBanner(graphics, iconX, iconY, iconWidth, iconHeight, color, 0xE8);
    }

    /**
     * Draws a Minecraft-style banner planted at the lower-right of the visible
     * marker area. It is rendered after the icon so large marker textures can
     * never hide it, while remaining inside the icon's maximum footprint.
     */
    static void drawMarkerBanner(GuiGraphics graphics, int iconX, int iconY,
                                 int iconWidth, int iconHeight, MarkerColor color, int alpha) {
        if (color == null || !color.isColored() || iconWidth < 6 || iconHeight < 8) return;

        int referenceSize = Math.min(iconWidth, iconHeight);
        int bannerWidth = clamp(Math.round(referenceSize * 0.20F), 5, 6);
        int bannerHeight = clamp(Math.round(referenceSize * 0.34F), 8, 9);

        // Marker textures reserve transparent margins around their symbols. The
        // central half is the stable visual footprint shared by small and large
        // icons, so anchor the banner to its lower-right corner.
        int visualRight = iconX + iconWidth * 3 / 4;
        int visualBottom = iconY + iconHeight * 3 / 4;
        int bannerX = clamp(visualRight - bannerWidth - 1,
                iconX, iconX + iconWidth - bannerWidth);
        int bannerY = clamp(visualBottom - bannerHeight, iconY, iconY + iconHeight - bannerHeight);

        drawStandingBanner(graphics, bannerX, bannerY, bannerWidth, bannerHeight, color, alpha);
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

    private static void drawStandingBanner(GuiGraphics graphics, int x, int y,
                                           int width, int height, MarkerColor color, int alpha) {
        int outline = argb(alpha, OUTLINE_RGB);
        int pole = argb(alpha, POLE_RGB);
        int fabric = argb(alpha, color.getRgb());
        int fabricShadow = argb(alpha, darken(color.getRgb()));
        int poleX = x + width / 2;
        int clothBottom = y + height - 2;

        // The centered pole is drawn first so the cloth hangs naturally in
        // front of it. Only its tip, the notch and its planted foot stay visible.
        graphics.fill(poleX, y, poleX + 1, y + height - 1, pole);
        graphics.fill(poleX - 1, y + height - 1, poleX + 2, y + height, outline);

        // Slim Minecraft-style cloth with a one-pixel outline and a split lower
        // edge. Leaving the center of the last row empty reveals the pole behind.
        graphics.fill(x, y + 1, x + width, y + 2, outline);
        graphics.fill(x, y + 2, x + 1, clothBottom, outline);
        graphics.fill(x + width - 1, y + 2, x + width, clothBottom, outline);
        graphics.fill(x + 1, y + 2, x + width - 2, clothBottom - 1, fabric);
        graphics.fill(x + width - 2, y + 2, x + width - 1,
                clothBottom - 1, fabricShadow);
        graphics.fill(x, clothBottom - 1, x + 2, clothBottom, outline);
        graphics.fill(x + width - 2, clothBottom - 1, x + width, clothBottom, outline);
        graphics.fill(x + 1, clothBottom - 1, x + 2, clothBottom, fabric);
        graphics.fill(x + width - 2, clothBottom - 1,
                x + width - 1, clothBottom, fabricShadow);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int darken(int rgb) {
        int red = ((rgb >> 16) & 0xFF) * 3 / 4;
        int green = ((rgb >> 8) & 0xFF) * 3 / 4;
        int blue = (rgb & 0xFF) * 3 / 4;
        return (red << 16) | (green << 8) | blue;
    }
}
