package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.client.MarkerTypeOrder;
import hunternif.mc.impl.atlas.client.MarkerVisibility;
import hunternif.mc.impl.atlas.client.gui.core.GuiComponent;
import hunternif.mc.impl.atlas.client.gui.core.GuiScrollingContainer;
import hunternif.mc.impl.atlas.marker.Marker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Search panel displayed over the atlas without changing marker visibility. */
final class GuiMarkerSearch extends GuiComponent {
    private static final int PANEL_WIDTH = 238;
    private static final int PANEL_HEIGHT = 180;
    private static final int PANEL_PADDING = 8;
    private static final int RESULTS_TOP = 51;
    private static final int RESULTS_HEIGHT = 112;
    private static final int BOOK_WIDTH = 310;
    private static final int BOOK_HEIGHT = 218;
    private static final int SEARCH_BUTTON_X = 300;
    private static final int SEARCH_BUTTON_Y = 137;
    private static final int SEARCH_BUTTON_WIDTH = 21;
    private static final int SEARCH_BUTTON_HEIGHT = 18;

    private final Consumer<Marker> selectionListener;
    private final List<Marker> markers = new ArrayList<>();
    private final List<Marker> filteredMarkers = new ArrayList<>();
    private EditBox searchField;
    private GuiScrollingContainer results;
    private int panelX;
    private int panelY;

    GuiMarkerSearch(Consumer<Marker> selectionListener) {
        this.selectionListener = selectionListener;
        setBlocksScreen(true);
    }

    void setMarkers(Collection<Marker> source) {
        markers.clear();
        if (source != null) {
            for (Marker marker : source) {
                if (!marker.isGlobal()) markers.add(marker);
            }
        }
    }

    @Override
    public void init() {
        super.init();
        setSize(width, height);
        clearWidgets();
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        searchField = new EditBox(font, panelX + PANEL_PADDING, panelY + 25,
                PANEL_WIDTH - PANEL_PADDING * 2, 18,
                Component.translatable("gui.antiqueatlas.markerSearch.placeholder"));
        searchField.setMaxLength(128);
        searchField.setResponder(ignored -> rebuildResults());
        addRenderableWidget(searchField);

        if (results == null) {
            results = new GuiScrollingContainer();
            results.setWheelScrollsVertically();
            addChild(results);
        } else {
            results.removeAllContent();
        }
        results.setViewportSize(PANEL_WIDTH - PANEL_PADDING * 2 - 7, RESULTS_HEIGHT);
        results.setGuiCoords(panelX + PANEL_PADDING, panelY + RESULTS_TOP);
        results.scrollTo(0, 0);

        rebuildResults();
        setFocused(searchField);
        searchField.setFocused(true);
    }

    private void rebuildResults() {
        if (results == null || searchField == null) return;
        String query = normalize(searchField.getValue().strip());
        filteredMarkers.clear();
        for (Marker marker : markers) {
            String searchable = normalize(marker.getLabel().getString() + " "
                    + marker.getX() + " " + marker.getZ() + " " + marker.getType());
            if (query.isEmpty() || searchable.contains(query)) filteredMarkers.add(marker);
        }
        filteredMarkers.sort(Comparator
                .comparing(Marker::getType, MarkerTypeOrder.idComparator())
                .thenComparing(marker -> normalize(marker.getLabel().getString()))
                .thenComparingInt(Marker::getId));

        results.removeAllContent();
        results.scrollTo(0, 0);
        int contentY = 0;
        int rowWidth = PANEL_WIDTH - PANEL_PADDING * 2 - 7;
        for (Marker marker : filteredMarkers) {
            GuiMarkerResultButton row = new GuiMarkerResultButton(marker, rowWidth,
                    !MarkerVisibility.isVisible(marker.getType()));
            row.addListener(button -> {
                closeChild();
                selectionListener.accept(marker);
            });
            results.addContent(row).setRelativeY(contentY);
            contentY += GuiMarkerResultButton.HEIGHT;
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
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
    public boolean charTyped(char character, int modifiers) {
        super.charTyped(character, modifiers);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bookX = (width - BOOK_WIDTH) / 2;
        int bookY = (height - BOOK_HEIGHT) / 2;
        if (mouseX >= bookX + SEARCH_BUTTON_X
                && mouseX < bookX + SEARCH_BUTTON_X + SEARCH_BUTTON_WIDTH
                && mouseY >= bookY + SEARCH_BUTTON_Y
                && mouseY < bookY + SEARCH_BUTTON_Y + SEARCH_BUTTON_HEIGHT) {
            closeChild();
            return true;
        }
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
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE6100C08);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFFB98A4C);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 1,
                panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFB98A4C);
        graphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFFB98A4C);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY,
                panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFB98A4C);
        drawCentered(graphics, Component.translatable("gui.antiqueatlas.markerSearch.title"),
                panelY + 8, 0xFFF1D49A, true);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (searchField.getValue().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.antiqueatlas.markerSearch.placeholder"),
                    panelX + PANEL_PADDING + 4, panelY + 30, 0xFF777777, false);
        }

        Component count = filteredMarkers.isEmpty()
                ? Component.translatable("gui.antiqueatlas.markerSearch.noResults")
                : Component.translatable("gui.antiqueatlas.markerSearch.results", filteredMarkers.size());
        graphics.drawString(font, count, panelX + PANEL_PADDING,
                panelY + PANEL_HEIGHT - 13, 0xFF9E8F77, false);
    }
}
