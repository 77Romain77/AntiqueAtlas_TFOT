package hunternif.mc.impl.atlas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import hunternif.mc.impl.atlas.client.Textures;
import hunternif.mc.impl.atlas.client.gui.core.GuiToggleButton;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.registry.MarkerType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Bookmark representing all visible markers that use the same icon type. */
final class GuiMarkerGroupBookmark extends GuiToggleButton {
    private static final int WIDTH = 21;
    private static final int HEIGHT = 18;
    private static final int COLOR_INDEX = 3;

    private final ITexture iconTexture;
    private final Component name;
    private final int markerCount;

    GuiMarkerGroupBookmark(MarkerType markerType, Component name, int markerCount) {
        this.iconTexture = markerType.getTexture();
        this.name = name;
        this.markerCount = markerCount;
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        boolean raised = isMouseOver || isSelected();
        int u = COLOR_INDEX * WIDTH;
        int v = raised ? 0 : HEIGHT;
        Textures.BOOKMARKS_LEFT.draw(graphics, getGuiX(), getGuiY(), u, v, WIDTH, HEIGHT);

        if (iconTexture != null) {
            iconTexture.draw(graphics, getGuiX() - (raised ? 3 : 2), getGuiY() - 3, 24, 24);
        }

        if (isMouseOver) {
            Component count = markerCount == 1
                    ? Component.translatable("gui.antiqueatlas.markerGroup.count.one")
                    : Component.translatable("gui.antiqueatlas.markerGroup.count.many", markerCount);
            drawTooltip(List.of(name, count), Minecraft.getInstance().font);
        }
    }
}
