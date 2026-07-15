package hunternif.mc.impl.atlas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import hunternif.mc.impl.atlas.client.Textures;
import hunternif.mc.impl.atlas.client.gui.core.GuiToggleButton;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;


/**
 * Bookmark-button in the journal. When a bookmark is selected, it will not
 * bulge on mouseover.
 */
public class GuiBookmarkButton extends GuiToggleButton {
    private static final int WIDTH = 21;
    private static final int HEIGHT = 18;
    private static final float TEXT_ICON_SCALE = 1.6F;

    private final int colorIndex;
    private ITexture iconTexture;
    private Component iconText;
    private Component title;
    private List<Component> tooltip;

    /**
     * @param colorIndex  0=red, 1=blue, 2=yellow, 3=green
     * @param iconTexture the path to the 16x16 texture to be drawn on top of the bookmark.
     * @param title       hovering text.
     */
    GuiBookmarkButton(int colorIndex, ITexture iconTexture, Component title) {
        this.colorIndex = colorIndex;
        setIconTexture(iconTexture);
        setTitle(title);
        setSize(WIDTH, HEIGHT);
    }

    /** Creates a bookmark whose icon is rendered by Minecraft's font. */
    GuiBookmarkButton(int colorIndex, Component iconText, Component title) {
        this.colorIndex = colorIndex;
        this.iconText = iconText;
        setTitle(title);
        setSize(WIDTH, HEIGHT);
    }

    void setIconTexture(ITexture iconTexture) {
        this.iconTexture = iconTexture;
        this.iconText = null;
    }

    public Component getTitle() {
        return title;
    }

    void setTitle(Component title) {
        this.title = title;
        this.tooltip = List.of(title);
    }

    void setTooltip(List<Component> tooltip) {
        this.tooltip = tooltip == null || tooltip.isEmpty() ? List.of(title) : List.copyOf(tooltip);
    }

    @Override
    public void render(GuiGraphics matrices, int mouseX, int mouseY, float partialTick) {
        float tint = isEnabled() ? 1.0F : 0.55F;
        RenderSystem.setShaderColor(tint, tint, tint, 1.0F);

        // Render background:
        int u = colorIndex * WIDTH;
        int v = isMouseOver || isSelected() ? 0 : HEIGHT;
        Textures.BOOKMARKS.draw(matrices, getGuiX(), getGuiY(), u, v, WIDTH, HEIGHT);

        // Render the icon:
        int hoverOffset = isMouseOver || isSelected() ? 1 : 0;
        if (iconTexture != null) {
            iconTexture.draw(matrices, getGuiX() + 2 + hoverOffset, getGuiY() + 1);
        } else if (iconText != null) {
            matrices.pose().pushPose();
            matrices.pose().translate(getGuiX() + WIDTH / 2F + hoverOffset,
                    getGuiY() + HEIGHT / 2F, 0);
            matrices.pose().scale(TEXT_ICON_SCALE, TEXT_ICON_SCALE, 1.0F);
            matrices.drawCenteredString(Minecraft.getInstance().font, iconText, 0,
                    -Minecraft.getInstance().font.lineHeight / 2,
                    isEnabled() ? 0xFFF2E2BD : 0xFF777777);
            matrices.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (isMouseOver) {
            drawTooltip(tooltip, Minecraft.getInstance().font);
        }
    }
}
