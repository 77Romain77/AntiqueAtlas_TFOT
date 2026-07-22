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
import java.util.function.Consumer;

/** Compact marker row shared by search results and overlap selection. */
final class GuiMarkerResultButton extends GuiComponentButton {
    static final int HEIGHT = 24;
    private static final int ICON_SIZE = 24;
    private static final int ICON_X = 3;
    private static final int TEXT_GAP = 4;

    private final Marker marker;
    private final boolean hidden;
    private final GuiMarkerEditButton editButton;

    GuiMarkerResultButton(Marker marker, int width, boolean hidden) {
        this(marker, width, hidden, null);
    }

    GuiMarkerResultButton(Marker marker, int width, boolean hidden, Consumer<Marker> editListener) {
        this.marker = marker;
        this.hidden = hidden;
        setSize(width, HEIGHT);
        if (editListener == null) {
            editButton = null;
        } else {
            editButton = new GuiMarkerEditButton();
            editButton.addListener(button -> editListener.accept(marker));
            addChild(editButton).setRelativeCoords(width - GuiMarkerEditButton.SIZE - 3,
                    (HEIGHT - GuiMarkerEditButton.SIZE) / 2);
        }
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
        int iconX = getGuiX() + ICON_X;
        int iconY = getGuiY();
        if (texture != null) texture.draw(graphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        MarkerColorRenderer.drawCompactMarkerBanner(graphics, iconX, iconY,
                ICON_SIZE, ICON_SIZE, marker.getColor(), hidden ? 0x72 : 0xE8);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textX = iconX + ICON_SIZE + TEXT_GAP;
        int textWidth = Math.max(8, getWidth() - (textX - getGuiX()) - 4
                - (editButton == null ? 0 : GuiMarkerEditButton.SIZE + 4));
        String markerName = marker.getLabel().getString();
        if (markerName.isBlank()) {
            markerName = Component.translatable("gui.antiqueatlas.markerSearch.unnamed").getString();
        }
        markerName = font.plainSubstrByWidth(markerName, textWidth);
        int textColor = hidden ? 0xFF8D8D8D : 0xFFF1E3C2;
        graphics.drawString(font, markerName, textX,
                getGuiY() + (HEIGHT - font.lineHeight) / 2, textColor, false);

        if (isMouseOver && hidden) {
            drawTooltip(List.of(Component.translatable("gui.antiqueatlas.markerSearch.hidden")), font);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
