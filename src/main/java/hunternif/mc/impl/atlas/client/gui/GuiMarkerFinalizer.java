package hunternif.mc.impl.atlas.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import hunternif.mc.impl.atlas.client.MarkerTypeOrder;
import hunternif.mc.impl.atlas.client.gui.core.GuiComponent;
import hunternif.mc.impl.atlas.client.gui.core.GuiScrollingContainer;
import hunternif.mc.impl.atlas.client.gui.core.ToggleGroup;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkerColor;
import hunternif.mc.impl.atlas.registry.MarkerType;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * This GUI is used select marker icon and enter a label.
 * When the user clicks on the confirmation button, the call to MarkerAPI is made.
 *
 * @author Hunternif
 */
public class GuiMarkerFinalizer extends GuiComponent {
    private Level world;
    private int atlasID;
    private int markerX;
    private int markerZ;
    private Marker editingMarker;
    private String markerName = "";
    private MarkerColor selectedColor = MarkerColor.NONE;

    MarkerType selectedType = MarkerType.REGISTRY.get(MarkerType.REGISTRY.getDefaultKey());

    private static final int BUTTON_WIDTH = 100;
    private static final int EDIT_BUTTON_WIDTH = 80;
    private static final int BUTTON_SPACING = 4;

    private static final int TYPE_SPACING = 1;
    private static final int TYPE_BG_FRAME = 4;
    private static final int COLOR_COLUMNS = 9;
    private static final int COLOR_SPACING = 2;
    private static final int COLOR_TOP_OFFSET = 29;
    private static final int PREVIEW_SIZE = 30;
    private static final int CONTROLS_Y_OFFSET = 73;

    private Button btnDone;
    private Button btnCancel;
    private Button btnDelete;
    private EditBox textField;
    private GuiScrollingContainer scroller;
    private ToggleGroup<GuiMarkerInList> typeRadioGroup;
    private ToggleGroup<GuiMarkerColorButton> colorRadioGroup;
    private final List<GuiMarkerColorButton> colorButtons = new ArrayList<>();

    private final List<IMarkerTypeSelectListener> markerListeners = new ArrayList<>();

    GuiMarkerFinalizer() {
    }

    void resetNewMarkerSelection() {
        this.editingMarker = null;
        this.markerName = "";
        this.selectedType = MarkerType.REGISTRY.get(MarkerType.REGISTRY.getDefaultKey());
        this.selectedColor = MarkerColor.NONE;
    }

    void setMarkerData(Level world, int atlasID, int markerX, int markerZ) {
        this.world = world;
        this.atlasID = atlasID;
        this.markerX = markerX;
        this.markerZ = markerZ;
        resetNewMarkerSelection();
        setBlocksScreen(true);
    }

    void setMarkerDataForEditing(Level world, int atlasID, Marker marker) {
        this.world = world;
        this.atlasID = atlasID;
        this.markerX = marker.getX();
        this.markerZ = marker.getZ();
        this.editingMarker = marker;
        this.markerName = marker.getLabel().getString();
        this.selectedColor = marker.getColor();
        MarkerType markerType = MarkerType.REGISTRY.get(marker.getType());
        if (markerType != null) selectedType = markerType;
        setBlocksScreen(true);
    }

    boolean isEditing() {
        return editingMarker != null;
    }

    void addMarkerListener(IMarkerTypeSelectListener listener) {
        markerListeners.add(listener);
    }

    void removeMarkerListener(IMarkerTypeSelectListener listener) {
        markerListeners.remove(listener);
    }

    void removeAllMarkerListeners() {
        markerListeners.clear();
    }

    @Override
    public void init() {
        super.init();

        Component doneLabel = Component.translatable(isEditing()
                ? "gui.antiqueatlas.markerEdit.save"
                : "gui.done");

        if (isEditing()) {
            int controlsWidth = EDIT_BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
            int controlsX = (this.width - controlsWidth) / 2;
            addRenderableWidget(btnDelete = Button.builder(
                    Component.translatable("gui.antiqueatlas.markerEdit.delete"), button -> {
                        if (ClientMapManager.getInstance().deleteMarker(editingMarker.getId())) {
                            LocalPlayer player = Minecraft.getInstance().player;
                            world.playSound(player, player.blockPosition(),
                                    SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.AMBIENT,
                                    1F, 0.5F);
                            closeChild();
                        }
                    }).bounds(controlsX, this.height / 2 + CONTROLS_Y_OFFSET, EDIT_BUTTON_WIDTH, 20).build());
            btnCancel = Button.builder(Component.translatable("gui.cancel"), button -> closeChild())
                    .bounds(controlsX + EDIT_BUTTON_WIDTH + BUTTON_SPACING,
                            this.height / 2 + CONTROLS_Y_OFFSET, EDIT_BUTTON_WIDTH, 20).build();
            btnDone = Button.builder(doneLabel, button -> saveMarker())
                    .bounds(controlsX + (EDIT_BUTTON_WIDTH + BUTTON_SPACING) * 2,
                            this.height / 2 + CONTROLS_Y_OFFSET, EDIT_BUTTON_WIDTH, 20).build();
        } else {
            btnCancel = Button.builder(Component.translatable("gui.cancel"), button -> closeChild())
                    .bounds(this.width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2,
                            this.height / 2 + CONTROLS_Y_OFFSET, BUTTON_WIDTH, 20).build();
            btnDone = Button.builder(doneLabel, button -> saveMarker())
                    .bounds(this.width / 2 + BUTTON_SPACING / 2,
                            this.height / 2 + CONTROLS_Y_OFFSET, BUTTON_WIDTH, 20).build();
        }
        addRenderableWidget(btnCancel);
        addRenderableWidget(btnDone);

        textField = new EditBox(Minecraft.getInstance().font, (this.width - 200) / 2, this.height / 2 - 81, 200, 20, Component.translatable("gui.antiqueatlas.marker.label"));
        textField.setEditable(true);
        textField.setMaxLength(128);
        textField.setValue(markerName);
        this.addRenderableWidget(this.textField);

        scroller = new GuiScrollingContainer();
        scroller.setWheelScrollsHorizontally();
        this.addChild(scroller);

        List<MarkerType> orderedTypes = MarkerTypeOrder.sortedNonTechnicalTypes();
        int typeCount = orderedTypes.size();
        int allTypesWidth = typeCount *
                (GuiMarkerInList.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
        int scrollerWidth = Math.min(allTypesWidth, 240);
        scroller.setViewportSize(scrollerWidth, GuiMarkerInList.FRAME_SIZE + TYPE_SPACING);
        scroller.setGuiCoords((this.width - scrollerWidth) / 2, this.height / 2 - 25);

        typeRadioGroup = new ToggleGroup<>();
        typeRadioGroup.addListener(button -> {
            selectedType = button.getMarkerType();
            for (IMarkerTypeSelectListener listener : markerListeners) {
                listener.onSelectMarkerType(button.getMarkerType());
            }
        });
        int contentX = 0;
        for (MarkerType markerType : orderedTypes) {
            GuiMarkerInList markerGui = new GuiMarkerInList(markerType);
            typeRadioGroup.addButton(markerGui);
            if (selectedType.equals(markerType)) {
                typeRadioGroup.setSelectedButton(markerGui);
            }
            scroller.addContent(markerGui).setRelativeX(contentX);
            contentX += GuiMarkerInList.FRAME_SIZE + TYPE_SPACING;
        }

        for (GuiMarkerColorButton colorButton : new ArrayList<>(colorButtons)) {
            removeChild(colorButton);
        }
        colorButtons.clear();
        colorRadioGroup = new ToggleGroup<>();
        colorRadioGroup.addListener(button -> selectedColor = button.getColor());
        int paletteWidth = COLOR_COLUMNS * GuiMarkerColorButton.SIZE
                + (COLOR_COLUMNS - 1) * COLOR_SPACING;
        int groupWidth = paletteWidth + 8 + PREVIEW_SIZE;
        int paletteX = (this.width - groupWidth) / 2;
        int paletteY = this.height / 2 + COLOR_TOP_OFFSET;
        MarkerColor[] colors = MarkerColor.values();
        for (int i = 0; i < colors.length; i++) {
            GuiMarkerColorButton colorButton = new GuiMarkerColorButton(colors[i]);
            colorButtons.add(colorButton);
            colorRadioGroup.addButton(colorButton);
            if (colors[i] == selectedColor) colorRadioGroup.setSelectedButton(colorButton);
            int column = i % COLOR_COLUMNS;
            int row = i / COLOR_COLUMNS;
            addChild(colorButton).setGuiCoords(
                    paletteX + column * (GuiMarkerColorButton.SIZE + COLOR_SPACING),
                    paletteY + row * (GuiMarkerColorButton.SIZE + COLOR_SPACING));
        }
    }

    private void saveMarker() {
        if (isEditing()) {
            Marker updated = ClientMapManager.getInstance().updateMarker(editingMarker.getId(),
                    MarkerType.REGISTRY.getKey(selectedType), Component.literal(textField.getValue()),
                    selectedColor);
            if (updated == null) return;
            Log.info("Updated marker #%d in Atlas #%d", editingMarker.getId(), atlasID);
        } else {
            Marker created = ClientMapManager.getInstance().createMarker(world.dimension(),
                    MarkerType.REGISTRY.getKey(selectedType), Component.literal(textField.getValue()),
                    markerX, markerZ, true, selectedColor);
            if (created == null) return;
            Log.info("Put marker in Atlas #%d \"%s\" at (%d, %d)",
                    atlasID, textField.getValue(), markerX, markerZ);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        world.playSound(player, player.blockPosition(),
                SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.AMBIENT,
                1F, 1F);
        closeChild();
    }

    public void setMarkerName(Component name) {
        markerName = name == null ? "" : name.getString();
        if (textField != null) textField.setValue(markerName);
    }

    MarkerColor getSelectedColor() {
        return selectedColor;
    }

    @Override
    public void closeChild() {
        if (scroller != null) {
            scroller.closeChild();
        }
        for (GuiMarkerColorButton colorButton : new ArrayList<>(colorButtons)) {
            colorButton.closeChild();
        }
        colorButtons.clear();
        super.closeChild();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button) || textField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int aa, int bb, int cc) {
        return super.keyPressed(aa, bb, cc) || textField.keyPressed(aa, bb, cc);
    }

    @Override
    public boolean charTyped(char aa, int bb) {
        return super.charTyped(aa, bb) || textField.charTyped(aa, bb);
    }

    @Override
    public void render(GuiGraphics matrices, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrices);
        if (isEditing()) {
            drawCentered(matrices, Component.translatable("gui.antiqueatlas.markerEdit.title"),
                    this.height / 2 - 119, 0xFFFFFF, true);
        }
        drawCentered(matrices, Component.translatable("gui.antiqueatlas.marker.label"), this.height / 2 - 97, 0xffffff, true);
        textField.render(matrices, mouseX, mouseY, partialTick);
        drawCentered(matrices, Component.translatable("gui.antiqueatlas.marker.type"), this.height / 2 - 44, 0xffffff, true);

        // Darker background for marker type selector
        matrices.fillGradient(scroller.getGuiX() - TYPE_BG_FRAME, scroller.getGuiY() - TYPE_BG_FRAME,
                scroller.getGuiX() + scroller.getWidth() + TYPE_BG_FRAME,
                scroller.getGuiY() + scroller.getHeight() + TYPE_BG_FRAME,
                0x88101010, 0x99101010);

        int paletteWidth = COLOR_COLUMNS * GuiMarkerColorButton.SIZE
                + (COLOR_COLUMNS - 1) * COLOR_SPACING;
        int groupWidth = paletteWidth + 8 + PREVIEW_SIZE;
        int paletteX = (this.width - groupWidth) / 2;
        Component colorLabel = Component.translatable("gui.antiqueatlas.markerColor.title");
        matrices.drawString(font, colorLabel,
                paletteX + (paletteWidth - font.width(colorLabel)) / 2,
                this.height / 2 + 16, 0xFFFFFF, true);

        int previewX = paletteX + paletteWidth + 8;
        int previewY = this.height / 2 + COLOR_TOP_OFFSET;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ITexture previewTexture = selectedType == null ? null : selectedType.getTexture();
        if (previewTexture != null) {
            previewTexture.draw(matrices, previewX + 3, previewY + 3,
                    PREVIEW_SIZE - 6, PREVIEW_SIZE - 6);
            MarkerColorRenderer.drawMarkerBanner(matrices, previewX + 3, previewY + 3,
                    PREVIEW_SIZE - 6, PREVIEW_SIZE - 6, selectedColor);
        }
        super.render(matrices, mouseX, mouseY, partialTick);
    }

    interface IMarkerTypeSelectListener {
        void onSelectMarkerType(MarkerType markerType);
    }
}
