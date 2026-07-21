package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.gui.core.GuiComponentButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Small pencil button shown at the end of marker search rows. */
final class GuiMarkerEditButton extends GuiComponentButton {
    static final int SIZE = 16;

    GuiMarkerEditButton() {
        setSize(SIZE, SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiX();
        int y = getGuiY();
        int border = isMouseOver ? 0xFFE6BE72 : 0xFF80613D;
        int background = isMouseOver ? 0xDD5A4630 : 0xCC2B2118;
        graphics.fill(x, y, x + SIZE, y + SIZE, border);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, background);

        // Compact pixel-art pencil, pointing towards the lower-left corner.
        graphics.fill(x + 3, y + 11, x + 5, y + 13, 0xFFE9D3A5);
        graphics.fill(x + 2, y + 13, x + 4, y + 15, 0xFF30251B);
        for (int step = 0; step < 4; step++) {
            int px = x + 4 + step * 2;
            int py = y + 10 - step * 2;
            graphics.fill(px, py, px + 3, py + 3, 0xFFD99A36);
            graphics.fill(px + 1, py, px + 3, py + 1, 0xFFFFD06A);
        }
        graphics.fill(x + 11, y + 3, x + 14, y + 6, 0xFFB75B58);

        if (isMouseOver) {
            drawTooltip(List.of(Component.translatable("gui.antiqueatlas.markerEdit.title")),
                    Minecraft.getInstance().font);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
