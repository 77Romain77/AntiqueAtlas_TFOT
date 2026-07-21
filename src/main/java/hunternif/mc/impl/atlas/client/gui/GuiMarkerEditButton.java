package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.gui.core.GuiComponentButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Small pencil button shown at the end of marker search rows. */
final class GuiMarkerEditButton extends GuiComponentButton {
    static final int SIZE = 16;

    private static final String[] PENCIL = {
            ".........DD.",
            "........DRRD",
            ".......DYYRD",
            "......DLYYD.",
            ".....DLYYD..",
            "....DLYYD...",
            "...DLYYD....",
            "..DLYYD.....",
            ".DWWYD......",
            "DWWWD.......",
            ".DD.........",
            "............"
    };

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

        // Crisp 12x12 pixel-art pencil, matching the atlas' parchment palette.
        for (int py = 0; py < PENCIL.length; py++) {
            String row = PENCIL[py];
            for (int px = 0; px < row.length(); px++) {
                int color = switch (row.charAt(px)) {
                    case 'D' -> 0xFF2B1B11; // dark outline
                    case 'Y' -> 0xFFD89A36; // pencil body
                    case 'L' -> 0xFFFFD36B; // highlight
                    case 'R' -> 0xFFB85D58; // eraser
                    case 'W' -> 0xFFE8D3A6; // sharpened wood
                    default -> 0;
                };
                if (color != 0) {
                    graphics.fill(x + 2 + px, y + 2 + py,
                            x + 3 + px, y + 3 + py, color);
                }
            }
        }

        if (isMouseOver) {
            drawTooltip(List.of(Component.translatable("gui.antiqueatlas.markerEdit.title")),
                    Minecraft.getInstance().font);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
