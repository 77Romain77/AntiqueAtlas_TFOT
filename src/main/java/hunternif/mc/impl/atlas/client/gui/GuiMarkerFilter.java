package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.MarkerVisibility;
import hunternif.mc.impl.atlas.client.MarkerTypeOrder;
import hunternif.mc.impl.atlas.client.Textures;
import hunternif.mc.impl.atlas.client.gui.core.GuiComponent;
import hunternif.mc.impl.atlas.client.gui.core.GuiScrollingContainer;
import hunternif.mc.impl.atlas.client.gui.core.GuiToggleButton;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** In-atlas multi-select panel controlling which marker types are rendered. */
public final class GuiMarkerFilter extends GuiComponent {
    private static final int FRAME_SIZE = 34;
    private static final int TYPE_SPACING = 2;
    private static final int TYPE_BG_FRAME = 4;
    private static final int BUTTON_WIDTH = 96;

    private final List<MarkerFilterButton> markerButtons = new ArrayList<>();
    private GuiScrollingContainer scroller;

    public GuiMarkerFilter() {
        setBlocksScreen(true);
    }

    @Override
    public void init() {
        super.init();
        setSize(width, height);
        clearWidgets();
        markerButtons.clear();

        if (scroller == null) {
            scroller = new GuiScrollingContainer();
            scroller.setWheelScrollsHorizontally();
            addChild(scroller);
        } else {
            scroller.removeAllContent();
        }

        List<MarkerType> types = MarkerTypeOrder.sortedNonTechnicalTypes();

        int allTypesWidth = Math.max(FRAME_SIZE,
                types.size() * (FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING);
        int scrollerWidth = Math.min(allTypesWidth, 240);
        scroller.setViewportSize(scrollerWidth, FRAME_SIZE + TYPE_SPACING);
        scroller.setGuiCoords((width - scrollerWidth) / 2, height / 2 - 28);
        scroller.scrollTo(0, 0);

        int contentX = 0;
        List<ResourceLocation> typeIds = new ArrayList<>();
        for (MarkerType type : types) {
            ResourceLocation id = MarkerType.REGISTRY.getKey(type);
            typeIds.add(id);
            MarkerFilterButton markerButton = new MarkerFilterButton(type);
            markerButton.setSelected(MarkerVisibility.isVisible(id));
            markerButton.addListener(button -> MarkerVisibility.setVisible(id, markerButton.isMarkerVisible()));
            markerButtons.add(markerButton);
            scroller.addContent(markerButton).setRelativeX(contentX);
            contentX += FRAME_SIZE + TYPE_SPACING;
        }

        int controlsY = height / 2 + 20;
        addRenderableWidget(Button.builder(Component.translatable("gui.antiqueatlas.markerFilter.showAll"), button -> {
            MarkerVisibility.showAll();
            markerButtons.forEach(markerButton -> markerButton.setSelected(true));
        }).bounds(width / 2 - BUTTON_WIDTH - 3, controlsY, BUTTON_WIDTH, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.antiqueatlas.markerFilter.hideAll"), button -> {
            MarkerVisibility.hideAll(typeIds);
            markerButtons.forEach(markerButton -> markerButton.setSelected(false));
        }).bounds(width / 2 + 3, controlsY, BUTTON_WIDTH, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> closeChild())
                .bounds(width / 2 - 50, controlsY + 26, 100, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeChild();
            return true;
        }
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        super.mouseScrolled(mouseX, mouseY, delta);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawCentered(graphics, Component.translatable("gui.antiqueatlas.markerFilter.title"),
                height / 2 - 76, 0xFFFFFF, true);
        drawCentered(graphics, Component.translatable("gui.antiqueatlas.markerFilter.help"),
                height / 2 - 59, 0xA0A0A0, false);
        graphics.fillGradient(scroller.getGuiX() - TYPE_BG_FRAME, scroller.getGuiY() - TYPE_BG_FRAME,
                scroller.getGuiX() + scroller.getWidth() + TYPE_BG_FRAME,
                scroller.getGuiY() + scroller.getHeight() + TYPE_BG_FRAME,
                0xCC101010, 0xDD101010);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static final class MarkerFilterButton extends GuiToggleButton {
        private final MarkerType markerType;

        private MarkerFilterButton(MarkerType markerType) {
            this.markerType = markerType;
            setSize(FRAME_SIZE, FRAME_SIZE);
        }

        private boolean isMarkerVisible() {
            return isSelected();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ITexture frame = isSelected() ? Textures.MARKER_FRAME_ON : Textures.MARKER_FRAME_OFF;
            frame.draw(graphics, getGuiX() + 1, getGuiY() + 1);
            ITexture marker = markerType.getTexture();
            if (marker != null) marker.draw(graphics, getGuiX() + 1, getGuiY() + 1);

            if (isMouseOver) {
                Component status = Component.translatable(isSelected()
                        ? "gui.antiqueatlas.markerFilter.visible"
                        : "gui.antiqueatlas.markerFilter.hidden");
                drawTooltip(List.of(status), Minecraft.getInstance().font);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
