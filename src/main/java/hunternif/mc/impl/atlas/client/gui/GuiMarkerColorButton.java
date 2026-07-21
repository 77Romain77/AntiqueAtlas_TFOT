package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.gui.core.GuiToggleButton;
import hunternif.mc.impl.atlas.marker.MarkerColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** One color swatch in the marker creation/editing palette. */
final class GuiMarkerColorButton extends GuiToggleButton {
    static final int SIZE = 14;

    private final MarkerColor color;

    GuiMarkerColorButton(MarkerColor color) {
        this.color = color;
        setSize(SIZE, SIZE);
    }

    MarkerColor getColor() {
        return color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frame = isSelected() ? 0xFFFFCC55 : (isMouseOver ? 0xFFE7D5A8 : 0xFF473725);
        graphics.fill(getGuiX(), getGuiY(), getGuiX() + SIZE, getGuiY() + SIZE, frame);
        graphics.fill(getGuiX() + 1, getGuiY() + 1,
                getGuiX() + SIZE - 1, getGuiY() + SIZE - 1, 0xFF1D1712);
        if (color.isColored()) {
            MarkerColorRenderer.drawBadge(graphics, getGuiX() + 2, getGuiY() + 2,
                    SIZE - 4, SIZE - 4, color, 0xFF);
        } else {
            MarkerColorRenderer.drawNoColorSwatch(graphics, getGuiX(), getGuiY(), SIZE);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isMouseOver) {
            drawTooltip(List.of(Component.translatable(color.getTranslationKey())),
                    Minecraft.getInstance().font);
        }
    }
}
