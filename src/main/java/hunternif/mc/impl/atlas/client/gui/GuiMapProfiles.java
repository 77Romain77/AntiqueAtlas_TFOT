package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small local-only menu used to select and name map profiles. */
public class GuiMapProfiles extends Screen {
    private static final int LIST_WIDTH = 300;
    private static final int OPEN_BUTTON_WIDTH = 82;
    private static final int COLUMN_GAP = 4;
    private static final int ROW_GAP = 3;
    private static final int ROW_HEIGHT = 20 + ROW_GAP;
    private static final int LIST_TOP = 44;
    private static final int LIST_BOTTOM_MARGIN = 76;
    private static final int ACTION_BUTTON_WIDTH = 96;
    private static final int ACTION_GAP = 6;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Screen parent;
    private final ClientMapManager maps = ClientMapManager.getInstance();
    private final ItemStack accessStack;
    private String selectedProfileId;
    private EditBox nameField;
    private boolean deleteArmed;
    private boolean draggingScrollbar;
    private int listX;
    private int listBottom;
    private int scrollOffset;
    private int visibleProfileCount;
    private int profileCount;

    public GuiMapProfiles(GuiAtlas parent) {
        super(Component.translatable("gui.antiqueatlas.maps.title"));
        this.parent = parent;
        this.accessStack = parent.copyAccessStack();
        this.selectedProfileId = maps.getActiveProfileId();
    }

    @Override
    protected void init() {
        refreshWidgets(true);
    }

    private void refreshWidgets() {
        refreshWidgets(false);
    }

    private void refreshWidgets(boolean keepSelectionVisible) {
        clearWidgets();
        List<ClientMapManager.ProfileInfo> profiles = maps.getProfiles();
        if (selectedProfileId == null || profiles.stream().noneMatch(profile -> profile.id().equals(selectedProfileId))) {
            selectedProfileId = maps.getActiveProfileId();
        }

        listX = width / 2 - LIST_WIDTH / 2;
        listBottom = height - LIST_BOTTOM_MARGIN;
        visibleProfileCount = Math.max(1, (listBottom - LIST_TOP + ROW_GAP) / ROW_HEIGHT);
        profileCount = profiles.size();
        scrollOffset = Math.max(0, Math.min(getMaxScroll(), scrollOffset));
        if (keepSelectionVisible) scrollSelectionIntoView(profiles);

        int nameButtonWidth = LIST_WIDTH - OPEN_BUTTON_WIDTH - COLUMN_GAP;
        String activeProfileId = maps.getActiveProfileId();
        int lastVisibleProfile = Math.min(profiles.size(), scrollOffset + visibleProfileCount);
        for (int profileIndex = scrollOffset; profileIndex < lastVisibleProfile; profileIndex++) {
            ClientMapManager.ProfileInfo profile = profiles.get(profileIndex);
            int row = profileIndex - scrollOffset;
            boolean selected = profile.id().equals(selectedProfileId);
            boolean active = profile.id().equals(activeProfileId);
            Component label = Component.literal((selected ? "▶ " : "  ") + profile.name());
            addRenderableWidget(Button.builder(label, button -> {
                selectedProfileId = profile.id();
                deleteArmed = false;
                refreshWidgets();
            }).bounds(listX, LIST_TOP + row * ROW_HEIGHT, nameButtonWidth, 20).build());

            Button openButton = Button.builder(Component.translatable(active
                            ? "gui.antiqueatlas.maps.active"
                            : "gui.antiqueatlas.maps.open"),
                    button -> openProfile(profile.id()))
                    .bounds(listX + nameButtonWidth + COLUMN_GAP, LIST_TOP + row * ROW_HEIGHT,
                            OPEN_BUTTON_WIDTH, 20)
                    .build();
            openButton.active = !active;
            addRenderableWidget(openButton);
        }

        ClientMapManager.ProfileInfo selected = profiles.stream()
                .filter(profile -> profile.id().equals(selectedProfileId))
                .findFirst()
                .orElse(null);
        int nameFieldWidth = LIST_WIDTH - ACTION_GAP - ACTION_BUTTON_WIDTH;
        nameField = new EditBox(font, listX, height - 66, nameFieldWidth, 20,
                Component.translatable("gui.antiqueatlas.maps.name"));
        nameField.setMaxLength(64);
        nameField.setValue(selected == null ? "" : selected.name());
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(Component.translatable("gui.antiqueatlas.maps.rename"), button -> {
            if (maps.renameProfile(selectedProfileId, nameField.getValue())) {
                deleteArmed = false;
                refreshWidgets();
            }
        }).bounds(listX + nameFieldWidth + ACTION_GAP, height - 66,
                ACTION_BUTTON_WIDTH, 20).build());

        Button createButton = Button.builder(Component.translatable("gui.antiqueatlas.maps.create"), button -> {
            String createdProfileId = maps.createProfile();
            if (createdProfileId != null) {
                selectedProfileId = createdProfileId;
                scrollOffset = Integer.MAX_VALUE;
                deleteArmed = false;
                refreshWidgets(true);
            }
        }).bounds(listX, height - 40, ACTION_BUTTON_WIDTH, 20).build();
        createButton.active = maps.canCreateProfile();
        if (!createButton.active) {
            createButton.setTooltip(Tooltip.create(Component.translatable(
                    "gui.antiqueatlas.maps.limitReached", maps.getMaxProfiles())));
        }
        addRenderableWidget(createButton);

        Button deleteButton = Button.builder(Component.translatable(deleteArmed
                        ? "gui.antiqueatlas.maps.deleteConfirm"
                        : "gui.antiqueatlas.maps.delete"), button -> {
            if (!deleteArmed) {
                deleteArmed = true;
                refreshWidgets();
            } else if (maps.deleteProfile(selectedProfileId)) {
                selectedProfileId = maps.getActiveProfileId();
                deleteArmed = false;
                refreshWidgets(true);
            }
        }).bounds(listX + ACTION_BUTTON_WIDTH + ACTION_GAP, height - 40,
                ACTION_BUTTON_WIDTH, 20).build();
        deleteButton.active = profiles.size() > 1;
        addRenderableWidget(deleteButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(listX + 2 * (ACTION_BUTTON_WIDTH + ACTION_GAP), height - 40,
                        ACTION_BUTTON_WIDTH, 20).build());
    }

    private void scrollSelectionIntoView(List<ClientMapManager.ProfileInfo> profiles) {
        int selectedIndex = -1;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(selectedProfileId)) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0) return;

        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleProfileCount) {
            scrollOffset = selectedIndex - visibleProfileCount + 1;
        }
        scrollOffset = Math.max(0, Math.min(getMaxScroll(), scrollOffset));
    }

    private int getMaxScroll() {
        return Math.max(0, profileCount - visibleProfileCount);
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= listX && mouseX < listX + LIST_WIDTH
                && mouseY >= LIST_TOP && mouseY < listBottom;
    }

    private int scrollbarX() {
        return listX + LIST_WIDTH + SCROLLBAR_GAP;
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        return getMaxScroll() > 0
                && mouseX >= scrollbarX() && mouseX < scrollbarX() + SCROLLBAR_WIDTH
                && mouseY >= LIST_TOP && mouseY < listBottom;
    }

    private int scrollbarThumbHeight() {
        int trackHeight = Math.max(1, listBottom - LIST_TOP);
        return Math.max(20, trackHeight * visibleProfileCount / Math.max(1, profileCount));
    }

    private boolean updateScrollFromMouse(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll == 0) return false;

        int trackHeight = listBottom - LIST_TOP;
        int thumbHeight = scrollbarThumbHeight();
        int travel = Math.max(1, trackHeight - thumbHeight);
        double position = (mouseY - LIST_TOP - thumbHeight / 2.0) / travel;
        int newOffset = (int) Math.round(Math.max(0.0, Math.min(1.0, position)) * maxScroll);
        if (newOffset == scrollOffset) return false;

        scrollOffset = newOffset;
        deleteArmed = false;
        refreshWidgets();
        return true;
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
        graphics.drawCenteredString(font, Component.translatable("gui.antiqueatlas.maps.count",
                profileCount, maps.getMaxProfiles()), width / 2, 28, 0xA0A0A0);
        renderScrollbar(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll == 0) return;

        int x = scrollbarX();
        int trackHeight = listBottom - LIST_TOP;
        int thumbHeight = scrollbarThumbHeight();
        int thumbY = LIST_TOP + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(x, LIST_TOP, x + SCROLLBAR_WIDTH, listBottom, 0x66000000);
        int thumbColor = draggingScrollbar || isMouseOverScrollbar(mouseX, mouseY)
                ? 0xFFFFFFFF : 0xFFAAAAAA;
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, thumbColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0 && isMouseOverList(mouseX, mouseY) && getMaxScroll() > 0) {
            int newOffset = Math.max(0, Math.min(getMaxScroll(),
                    scrollOffset + (delta > 0.0 ? -1 : 1)));
            if (newOffset != scrollOffset) {
                scrollOffset = newOffset;
                deleteArmed = false;
                refreshWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
