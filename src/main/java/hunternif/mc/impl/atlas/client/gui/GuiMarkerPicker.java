package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.gui.core.GuiComponent;
import hunternif.mc.impl.atlas.client.gui.core.GuiScrollingContainer;
import hunternif.mc.impl.atlas.marker.Marker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Lets the player choose which local marker to edit when icons overlap. */
final class GuiMarkerPicker extends GuiComponent {
    private static final int PANEL_WIDTH = 238;
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int PANEL_PADDING = 8;

    private final Consumer<Marker> selectionListener;
    private final List<Marker> markers = new ArrayList<>();
    private GuiScrollingContainer results;
    private int panelX;
    private int panelY;
    private int panelHeight;

    GuiMarkerPicker(Consumer<Marker> selectionListener) {
        this.selectionListener = selectionListener;
        setBlocksScreen(true);
    }

    void setMarkers(List<Marker> source) {
        markers.clear();
        if (source != null) markers.addAll(source);
    }

    @Override
    public void init() {
        super.init();
        setSize(width, height);
        int visibleRows = Math.max(1, Math.min(MAX_VISIBLE_ROWS, markers.size()));
        int resultsHeight = visibleRows * GuiMarkerResultButton.HEIGHT;
        panelHeight = resultsHeight + 38;
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - panelHeight) / 2;

        if (results == null) {
            results = new GuiScrollingContainer();
            results.setWheelScrollsVertically();
            addChild(results);
        } else {
            results.removeAllContent();
        }
        int rowWidth = PANEL_WIDTH - PANEL_PADDING * 2 - 7;
        results.setViewportSize(rowWidth, resultsHeight);
        results.setGuiCoords(panelX + PANEL_PADDING, panelY + 27);
        results.scrollTo(0, 0);

        int contentY = 0;
        for (Marker marker : markers) {
            GuiMarkerResultButton row = new GuiMarkerResultButton(marker, rowWidth, false);
            row.addListener(button -> {
                closeChild();
                selectionListener.accept(marker);
            });
            results.addContent(row).setRelativeY(contentY);
            contentY += GuiMarkerResultButton.HEIGHT;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) closeChild();
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
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xE6100C08);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFFB98A4C);
        graphics.fill(panelX, panelY + panelHeight - 1,
                panelX + PANEL_WIDTH, panelY + panelHeight, 0xFFB98A4C);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFFB98A4C);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY,
                panelX + PANEL_WIDTH, panelY + panelHeight, 0xFFB98A4C);
        drawCentered(graphics, Component.translatable("gui.antiqueatlas.markerEdit.choose"),
                panelY + 8, 0xFFF1D49A, true);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
