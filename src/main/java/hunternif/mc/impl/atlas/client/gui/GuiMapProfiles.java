package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small local-only menu used to select and name map profiles. */
public class GuiMapProfiles extends Screen {
    private static final int LIST_WIDTH = 300;
    private static final int OPEN_BUTTON_WIDTH = 82;
    private static final int ROW_GAP = 4;

    private final Screen parent;
    private final ClientMapManager maps = ClientMapManager.getInstance();
    private final ItemStack accessStack;
    private String selectedProfileId;
    private EditBox nameField;
    private boolean deleteArmed;

    public GuiMapProfiles(GuiAtlas parent) {
        super(Component.translatable("gui.antiqueatlas.maps.title"));
        this.parent = parent;
        this.accessStack = parent.copyAccessStack();
        this.selectedProfileId = maps.getActiveProfileId();
    }

    @Override
    protected void init() {
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        List<ClientMapManager.ProfileInfo> profiles = maps.getProfiles();
        if (selectedProfileId == null || profiles.stream().noneMatch(profile -> profile.id().equals(selectedProfileId))) {
            selectedProfileId = maps.getActiveProfileId();
        }

        int listX = width / 2 - 150;
        int listY = 48;
        int nameButtonWidth = LIST_WIDTH - OPEN_BUTTON_WIDTH - ROW_GAP;
        String activeProfileId = maps.getActiveProfileId();
        int visibleProfiles = Math.min(8, profiles.size());
        for (int i = 0; i < visibleProfiles; i++) {
            ClientMapManager.ProfileInfo profile = profiles.get(i);
            boolean selected = profile.id().equals(selectedProfileId);
            boolean active = profile.id().equals(activeProfileId);
            Component label = Component.literal((selected ? "▶ " : "  ") + profile.name());
            addRenderableWidget(Button.builder(label, button -> {
                selectedProfileId = profile.id();
                deleteArmed = false;
                refreshWidgets();
            }).bounds(listX, listY + i * 23, nameButtonWidth, 20).build());

            Button openButton = Button.builder(Component.translatable(active
                            ? "gui.antiqueatlas.maps.active"
                            : "gui.antiqueatlas.maps.open"),
                    button -> openProfile(profile.id()))
                    .bounds(listX + nameButtonWidth + ROW_GAP, listY + i * 23,
                            OPEN_BUTTON_WIDTH, 20)
                    .build();
            openButton.active = !active;
            addRenderableWidget(openButton);
        }

        ClientMapManager.ProfileInfo selected = profiles.stream()
                .filter(profile -> profile.id().equals(selectedProfileId))
                .findFirst()
                .orElse(null);
        nameField = new EditBox(font, width / 2 - 150, height - 92, 200, 20,
                Component.translatable("gui.antiqueatlas.maps.name"));
        nameField.setMaxLength(64);
        nameField.setValue(selected == null ? "" : selected.name());
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(Component.translatable("gui.antiqueatlas.maps.rename"), button -> {
            if (maps.renameProfile(selectedProfileId, nameField.getValue())) {
                deleteArmed = false;
                refreshWidgets();
            }
        }).bounds(width / 2 + 55, height - 92, 95, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.antiqueatlas.maps.create"), button -> {
            selectedProfileId = maps.createProfile();
            deleteArmed = false;
            refreshWidgets();
        }).bounds(width / 2 - 150, height - 66, 95, 20).build());

        Button deleteButton = Button.builder(Component.translatable(deleteArmed
                        ? "gui.antiqueatlas.maps.deleteConfirm"
                        : "gui.antiqueatlas.maps.delete"), button -> {
            if (!deleteArmed) {
                deleteArmed = true;
                refreshWidgets();
            } else if (maps.deleteProfile(selectedProfileId)) {
                selectedProfileId = maps.getActiveProfileId();
                deleteArmed = false;
                refreshWidgets();
            }
        }).bounds(width / 2 - 50, height - 66, 95, 20).build();
        deleteButton.active = profiles.size() > 1;
        addRenderableWidget(deleteButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 50, height - 40, 100, 20).build());
    }

    private void openProfile(String profileId) {
        if (!maps.activateProfile(profileId)) return;

        minecraft.setScreen(null);
        if (accessStack.isEmpty()) {
            AntiqueAtlasClientSegment.openAtlasGUI();
        } else {
            AntiqueAtlasClientSegment.openAtlasGUI(accessStack);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
