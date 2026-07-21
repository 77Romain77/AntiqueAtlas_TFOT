package hunternif.mc.impl.atlas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import hunternif.mc.impl.atlas.client.gui.core.GuiComponentButton;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Compact marker row shared by search results and overlap selection. */
final class GuiMarkerResultButton extends GuiComponentButton {
    static final int HEIGHT = 22;

    private final Marker marker;
    private final boolean hidden;

    GuiMarkerResultButton(Marker marker, int width, boolean hidden) {
        this.marker = marker;
        this.hidden = hidden;
        setSize(width, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int background = isMouseOver ? 0xCC5A4630 : 0xB0201812;
        graphics.fill(getGuiX(), getGuiY(), getGuiX() + getWidth(), getGuiY() + HEIGHT - 1, background);

        MarkerType type = MarkerType.REGISTRY.get(marker.getType());
        ITexture texture = type == null ? null : type.getTexture();
        float tint = hidden ? 0.45F : 1.0F;
        RenderSystem.setShaderColor(tint, tint, tint, 1.0F);
        if (texture != null) texture.draw(graphics, getGuiX() + 3, getGuiY() + 3, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String coordinates = marker.getX() + ", " + marker.getZ();
        int coordinateWidth = font.width(coordinates);
        int textX = getGuiX() + 23;
        int textWidth = Math.max(8, getWidth() - 29 - coordinateWidth);
        String markerName = marker.getLabel().getString();
        if (markerName.isBlank()) {
            markerName = Component.translatable("gui.antiqueatlas.markerSearch.unnamed").getString();
        }
        markerName = font.plainSubstrByWidth(markerName, textWidth);
        int textColor = hidden ? 0xFF8D8D8D : 0xFFF1E3C2;
        graphics.drawString(font, markerName, textX, getGuiY() + 7, textColor, false);
        graphics.drawString(font, coordinates, getGuiX() + getWidth() - coordinateWidth - 4,
                getGuiY() + 7, hidden ? 0xFF777777 : 0xFFB9A98A, false);

        if (isMouseOver && hidden) {
            drawTooltip(List.of(Component.translatable("gui.antiqueatlas.markerSearch.hidden")), font);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
