package hunternif.mc.impl.atlas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import hunternif.mc.api.client.AtlasClientAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.client.*;
import hunternif.mc.impl.atlas.client.gui.core.*;
import hunternif.mc.impl.atlas.client.gui.core.GuiStates.IState;
import hunternif.mc.impl.atlas.client.gui.core.GuiStates.SimpleState;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.client.texture.TileRenderBatch;
import hunternif.mc.impl.atlas.client.texture.TileTexture;
import hunternif.mc.impl.atlas.client.texture.UnexploredCloudBatch;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.core.WorldData;
import hunternif.mc.impl.atlas.event.MarkerClickedCallback;
import hunternif.mc.impl.atlas.event.MarkerHoveredCallback;
import hunternif.mc.impl.atlas.marker.DimensionMarkersData;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.registry.MarkerRenderInfo;
import hunternif.mc.impl.atlas.registry.MarkerType;
import hunternif.mc.impl.atlas.util.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public class GuiAtlas extends GuiComponent {
    public static final int WIDTH = 310;
    public static final int HEIGHT = 218;

    private static final int MAP_BORDER_WIDTH = 17;
    private static final int MAP_BORDER_HEIGHT = 11;
    private static final int MAP_WIDTH = WIDTH - MAP_BORDER_WIDTH * 2;
    private static final int MAP_HEIGHT = 194;

    private static final float PLAYER_ROTATION_STEPS = 16;
    private static final int PLAYER_ICON_WIDTH = 7;
    private static final int PLAYER_ICON_HEIGHT = 8;

    public static final int MARKER_SIZE = 32;

    /**
     * If the map scale goes below this value, the tiles will not scale down
     * visually, but will instead span greater area.
     */
    private static final double MIN_SCALE_THRESHOLD = 0.5;

    private static final int DIAGNOSTIC_SAMPLE_COUNT = 60;
    private static final int DIAGNOSTIC_PADDING = 4;
    private static final int DIAGNOSTIC_MARGIN = 5;
    private static final long DIAGNOSTIC_HOLD_NANOS = 5_000_000_000L;

    private final long[] renderTimes = new long[30];

    /**
     * Hidden, session-only performance overlay. It is intentionally absent
     * from the config and normal controls. Hold Ctrl+D for five seconds to
     * reveal it while the atlas is open, then press Ctrl+D once to hide it.
     */
    private boolean diagnosticVisible;
    private boolean diagnosticShortcutDown;
    private long diagnosticHoldStartNanos;
    private final long[] diagnosticFrameTimes = new long[DIAGNOSTIC_SAMPLE_COUNT];
    private final long[] diagnosticTerrainTimes = new long[DIAGNOSTIC_SAMPLE_COUNT];
    private int diagnosticSampleIndex;
    private int diagnosticSamples;
    private long diagnosticFrameTotal;
    private long diagnosticTerrainTotal;
    private int diagnosticTilesVisited;
    private int diagnosticSubtilesRendered;
    private int diagnosticBatchCount;
    private int diagnosticMarkersRendered;

    /** Prepared terrain geometry reused until the visible page actually changes. */
    private final Map<TileTexture, TileRenderBatch> terrainCacheBatches = new LinkedHashMap<>();
    private final UnexploredCloudBatch terrainCacheClouds = new UnexploredCloudBatch();
    private boolean terrainCacheValid;
    private boolean terrainCacheCloudsEnabled;
    private WorldData terrainCacheWorld;
    private int terrainCacheStartX;
    private int terrainCacheStartZ;
    private int terrainCacheEndX;
    private int terrainCacheEndZ;
    private int terrainCacheGuiX;
    private int terrainCacheGuiY;
    private int terrainCacheTileHalfSize;
    private int terrainCacheTile2ChunkScale;
    private long terrainCacheGlobalWorldRevision = Long.MIN_VALUE;
    private long terrainCacheWorldRevision = Long.MIN_VALUE;
    private long terrainCacheTextureRevision = Long.MIN_VALUE;
    private int terrainCacheTilesVisited;
    private int terrainCacheSubtilesRendered;
    private String terrainCacheFrameState = "EMPTY";
    private String terrainCacheLastReason = "initialisation";
    private long terrainCacheLastBuildNanos;
    private long terrainCacheRebuildCount;

    private int renderTimesIndex = 0;

    // States ==================================================================

    private final GuiStates state = new GuiStates();

    /**
     * If on, navigate the map normally.
     */
    private final IState NORMAL = new SimpleState();

    /**
     * If on, a semi-transparent marker is attached to the cursor, and the
     * player's icon becomes semi-transparent as well.
     */
    private final IState PLACING_MARKER = new IState() {
        @Override
        public void onEnterState() {
            btnMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            btnMarker.setSelected(false);
        }
    };

    /**
     * If on, the closest marker will be deleted upon mouseclick.
     */
    private final IState DELETING_MARKER = new IState() {
        @Override
        public void onEnterState() {
            // GuiComponent.v.a();
            addChild(eraser);
            btnDelMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            // mc.v.b();
            removeChild(eraser);
            btnDelMarker.setSelected(false);
        }
    };
    private final GuiCursor eraser = new GuiCursor();

    // Buttons =================================================================

    /**
     * Arrow buttons for navigating the map view via mouse clicks.
     */
    private final GuiArrowButton btnUp, btnDown, btnLeft, btnRight;

    /** Button opening the local map-profile manager. */
    private final GuiBookmarkButton btnMaps;

    /**
     * Button for placing a marker at current position, local to this Atlas instance.
     */
    private final GuiBookmarkButton btnMarker;

    /**
     * Button for deleting local markers.
     */
    private final GuiBookmarkButton btnDelMarker;

    /** Button opening the per-type marker visibility filter. */
    private final GuiBookmarkButton btnMarkerFilter;

    /** Per-map toggle for automatically placing a tomb at the death position. */
    private final GuiBookmarkButton btnDeathMarker;

    /** One-shot refresh of already mapped chunks loaded around the player. */
    private final GuiBookmarkButton btnRescan;

    /** Opens the local marker search panel. */
    private final GuiBookmarkButton btnMarkerSearch;

    /**
     * Button for restoring player's position at the center of the Atlas.
     */
    private final GuiPositionButton btnPosition;


    // Navigation ==============================================================

    /**
     * Pause between after the arrow button is pressed and continuous
     * navigation starts, in ticks.
     */
    private static final int BUTTON_PAUSE = 8;

    /**
     * How much the map view is offset, in blocks, per click (or per tick).
     */
    private static final int navigateStep = 24;

    /**
     * The button which is currently being pressed. Used for continuous
     * navigation using the arrow buttons. Also used to prevent immediate
     * canceling of placing marker.
     */
    private GuiComponentButton selectedButton = null;

    /**
     * Time in world ticks when the button was pressed. Used to create a pause
     * before continuous navigation using the arrow buttons.
     */
    private long timeButtonPressed = 0;

    /**
     * Set to true when dragging the map view.
     */
    private boolean isDragging = false;

    /**
     * Offset to the top left corner of the tile at (0, 0) from the center of
     * the map drawing area, in pixels.
     */
    private int mapOffsetX, mapOffsetY;

    /**
     * When dragging, this saves the partly updates of the mapOffset.
     * Turns out, mouse dragging events are too precise.
     */
    private float mapOffsetDeltaX, mapOffsetDeltaY;

    private Integer targetOffsetX, targetOffsetY;
    /**
     * If true, the player's icon will be in the center of the GUI, and the
     * offset of the tiles will be calculated accordingly. Otherwise it's the
     * position of the player that will be calculated with respect to the
     * offset.
     */
    private boolean followPlayer;

    private final GuiScaleBar scaleBar = new GuiScaleBar();

    /** Marker-type categories attached directly to the left side of the atlas. */
    private final GuiScrollingContainer markerGroups = new GuiScrollingContainer();

    /** Markers from the selected category, displayed to the left of the categories. */
    private final GuiScrollingContainer markerBookmarks = new GuiScrollingContainer();

    private final Map<ResourceLocation, List<Marker>> groupedMarkers = new LinkedHashMap<>();
    private final Map<ResourceLocation, GuiMarkerGroupBookmark> markerGroupBookmarks = new LinkedHashMap<>();
    private ResourceLocation selectedMarkerGroup;

    /**
     * Pixel-to-block ratio.
     */
    private double mapScale;
    /**
     * The visual size of a tile in pixels.
     */
    private int tileHalfSize;
    /**
     * The number of chunks a tile spans.
     */
    private int tile2ChunkScale;


    // Markers =================================================================

    /**
     * Local markers in the current dimension
     */
    private DimensionMarkersData localMarkersData;
    /**
     * Global markers in the current dimension
     */
    private DimensionMarkersData globalMarkersData;
    /**
     * The marker highlighted by the eraser. Even though multiple markers may
     * be highlighted at the same time, only one of them will be deleted.
     */
    private Marker hoveredMarker;
    private final List<Marker> hoveredLocalMarkers = new ArrayList<>();

    private final GuiMarkerFinalizer markerFinalizer = new GuiMarkerFinalizer();
    private final GuiMarkerFilter markerFilter = new GuiMarkerFilter();
    private final GuiMarkerSearch markerSearch = new GuiMarkerSearch(
            this::focusSearchResult, this::openMarkerEditor);
    private final GuiMarkerPicker markerPicker = new GuiMarkerPicker(this::openMarkerEditor);
    /**
     * Displayed where the marker is about to be placed when the Finalizer GUI is on.
     */
    private final GuiBlinkingMarker blinkingIcon = new GuiBlinkingMarker();

    // Misc stuff ==============================================================

    private Player player;
    private ItemStack stack;
    private WorldData biomeData;

    /**
     * Coordinate scale factor relative to the actual screen size.
     */
    private double screenScale;

    private long lastUpdateMillis = System.currentTimeMillis();
    private int scaleAlpha = 255;
    private int scaleClipIndex = 0;
    private final int zoomLevelOne = 8;
    private int zoomLevel = zoomLevelOne;
    private final String[] zoomNames = new String[]{"256", "128", "64", "32", "16", "8", "4", "2", "1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64", "1/128", "1/256"};

    @SuppressWarnings("rawtypes")
    public GuiAtlas() {
        setSize(WIDTH, HEIGHT);
        setMapScale(0.5);
        followPlayer = true;
        setInterceptKeyboard(true);

        btnUp = GuiArrowButton.up();
        addChild(btnUp).offsetGuiCoords(148, 10);
        btnDown = GuiArrowButton.down();
        addChild(btnDown).offsetGuiCoords(148, 194);
        btnLeft = GuiArrowButton.left();
        addChild(btnLeft).offsetGuiCoords(15, 100);
        btnRight = GuiArrowButton.right();
        addChild(btnRight).offsetGuiCoords(283, 100);
        btnPosition = new GuiPositionButton();
        btnPosition.setEnabled(!followPlayer);
        addChild(btnPosition).offsetGuiCoords(283, 194);
        IButtonListener positionListener = button -> {
            selectedButton = button;
            if (button.equals(btnPosition)) {
                followPlayer = true;
                targetOffsetX = null;
                targetOffsetY = null;
                btnPosition.setEnabled(false);
            } else {
                // Navigate once, before enabling pause:
                navigateByButton(selectedButton);
                timeButtonPressed = player.getCommandSenderWorld().getGameTime();
            }
        };
        btnUp.addListener(positionListener);
        btnDown.addListener(positionListener);
        btnLeft.addListener(positionListener);
        btnRight.addListener(positionListener);
        btnPosition.addListener(positionListener);

        btnMaps = new GuiBookmarkButton(1, Textures.ICON_MAPS,
                Component.translatable("gui.antiqueatlas.maps.manage"));
        addChild(btnMaps).offsetGuiCoords(300, 113);
        btnMaps.addListener(button -> {
            if (biomeData != null) {
                biomeData.setBrowsingPosition(mapOffsetX, mapOffsetY, mapScale);
                ClientMapManager.getInstance().markStateDirty();
            }
            closeMarkerGroupList();
            AntiqueAtlasClientSegment.openMapProfiles(this);
        });

        btnMarker = new GuiBookmarkButton(0, Textures.ICON_ADD_MARKER, Component.translatable("gui.antiqueatlas.addMarker"));
        addChild(btnMarker).offsetGuiCoords(300, 14);
        btnMarker.addListener(button -> {
            if (state.is(PLACING_MARKER)) {
                selectedButton = null;
                state.switchTo(NORMAL);
            } else if (stack != null || !AntiqueAtlas.CONFIG.itemNeeded) {
                // Reset before entering placement mode so the translucent icon
                // attached to the cursor never reuses the previous marker.
                markerFinalizer.resetNewMarkerSelection();
                selectedButton = button;
                state.switchTo(PLACING_MARKER);

                // While holding shift, we create a marker on the player's position
                if (hasShiftDown()) {
                    markerFinalizer.setMarkerData(player.getCommandSenderWorld(),
                            getAtlasID(),
                            player.getBlockX(), player.getBlockZ());
                    addChild(markerFinalizer);

                    blinkingIcon.setTexture(markerFinalizer.selectedType.getTexture(),
                            MARKER_SIZE, MARKER_SIZE);
                    addChildBehind(markerFinalizer, blinkingIcon)
                            .setRelativeCoords(worldXToScreenX((int) player.getX()) - getGuiX() - MARKER_SIZE / 2,
                                    worldZToScreenY((int) player.getZ()) - getGuiY() - MARKER_SIZE / 2);

                    // Need to intercept keyboard events to type in the label:
                    setInterceptKeyboard(true);

                    // Un-press all keys to prevent player from walking infinitely:
                    KeyMapping.releaseAll();

                    selectedButton = null;
                    state.switchTo(NORMAL);
                }
            }
        });
        btnDelMarker = new GuiBookmarkButton(2, Textures.ICON_DELETE_MARKER, Component.translatable("gui.antiqueatlas.delMarker"));
        addChild(btnDelMarker).offsetGuiCoords(300, 33);
        btnDelMarker.addListener(button -> {
            if (state.is(DELETING_MARKER)) {
                selectedButton = null;
                state.switchTo(NORMAL);
            } else if (stack != null || !AntiqueAtlas.CONFIG.itemNeeded) {
                selectedButton = button;
                state.switchTo(DELETING_MARKER);
            }
        });
        btnMarkerFilter = new GuiBookmarkButton(3, Textures.ICON_HIDE_MARKERS,
                Component.translatable("gui.antiqueatlas.markerFilter.title"));
        addChild(btnMarkerFilter).offsetGuiCoords(300, 52);
        btnMarkerFilter.addListener(button -> {
            selectedButton = null;
            if (stack != null || !AntiqueAtlas.CONFIG.itemNeeded) addChild(markerFilter);
        });

        btnDeathMarker = new GuiBookmarkButton(0, Textures.ICON_DEATH_MARKER,
                Component.translatable("gui.antiqueatlas.deathMarker.title"));
        addChild(btnDeathMarker).offsetGuiCoords(300, 90);
        btnDeathMarker.addListener(button -> {
            ClientMapManager maps = ClientMapManager.getInstance();
            maps.setAutoDeathMarkerEnabled(!maps.isAutoDeathMarkerEnabled());
            updateDeathMarkerButton();
        });
        updateDeathMarkerButton();

        btnRescan = new GuiBookmarkButton(1, Component.literal("↻"),
                Component.translatable("gui.antiqueatlas.rescan.title"));
        addChild(btnRescan).offsetGuiCoords(300, 137);
        btnRescan.addListener(button -> requestAreaRescan());
        updateRescanButton();

        btnMarkerSearch = new GuiBookmarkButton(3, Textures.ICON_MARKER_SEARCH,
                Component.translatable("gui.antiqueatlas.markerSearch.title"));
        addChild(btnMarkerSearch).offsetGuiCoords(300, 71);
        btnMarkerSearch.addListener(button -> {
            selectedButton = null;
            if (markerSearch.getParent() != null) {
                markerSearch.closeChild();
                return;
            }
            markerSearch.setMarkers(localMarkersData == null
                    ? List.of()
                    : localMarkersData.getAllMarkers());
            addChild(markerSearch);
            btnMarkerSearch.setSelected(true);
            KeyMapping.releaseAll();
        });

        addChild(scaleBar).offsetGuiCoords(20, 198);
        scaleBar.setMapScale(1);

        addChild(markerBookmarks).setRelativeCoords(-38, 14);
        markerBookmarks.setViewportSize(21, 180);
        markerBookmarks.setWheelScrollsVertically();

        addChild(markerGroups).setRelativeCoords(-10, 14);
        markerGroups.setViewportSize(21, 180);
        markerGroups.setWheelScrollsVertically();

        markerFinalizer.addMarkerListener(blinkingIcon);

        eraser.setTexture(Textures.ERASER, 12, 14, 2, 11);

        state.switchTo(NORMAL);
    }

    public GuiAtlas prepareToOpen(ItemStack stack) {
        this.stack = stack;

        return prepareToOpen();
    }

    /**
     * Keeps the vanilla book context while the map-profile screen temporarily
     * replaces this atlas. This is required when atlas access needs an item.
     */
    public ItemStack copyAccessStack() {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public void openMarkerFinalizer(Component name) {
        if (isMapInteractionBlocked()) return;
        markerFinalizer.setMarkerData(player.getCommandSenderWorld(),
                getAtlasID(),
                (int) player.getX(), (int) player.getZ());
        addChild(markerFinalizer);

        if (name != null) {
            markerFinalizer.setMarkerName(name);
        }

        blinkingIcon.setTexture(markerFinalizer.selectedType.getTexture(),
                MARKER_SIZE, MARKER_SIZE);
        addChildBehind(markerFinalizer, blinkingIcon)
                .setRelativeCoords(worldXToScreenX((int) player.getX()) - getGuiX() - MARKER_SIZE / 2,
                        worldZToScreenY((int) player.getZ()) - getGuiY() - MARKER_SIZE / 2);

        // Need to intercept keyboard events to type in the label:
        setInterceptKeyboard(true);

        // Un-press all keys to prevent player from walking infinitely:
        KeyMapping.releaseAll();

        selectedButton = null;
        state.switchTo(NORMAL);
    }

    private void openMarkerEditor(Marker marker) {
        if (marker == null || marker.isGlobal() || isMapInteractionBlocked()) return;
        markerFinalizer.setMarkerDataForEditing(player.getCommandSenderWorld(), getAtlasID(), marker);
        addChild(markerFinalizer);

        blinkingIcon.setTexture(markerFinalizer.selectedType.getTexture(), MARKER_SIZE, MARKER_SIZE);
        addChildBehind(markerFinalizer, blinkingIcon)
                .setRelativeCoords(worldXToScreenX(marker.getX()) - getGuiX() - MARKER_SIZE / 2,
                        worldZToScreenY(marker.getZ()) - getGuiY() - MARKER_SIZE / 2);

        setInterceptKeyboard(true);
        KeyMapping.releaseAll();
        selectedButton = null;
        state.switchTo(NORMAL);
    }

    private void focusSearchResult(Marker marker) {
        if (marker == null) return;
        setTargetPosition(marker.getX(), marker.getZ());
        followPlayer = false;
        btnPosition.setEnabled(true);
    }

    private void editHoveredMarker() {
        if (hoveredLocalMarkers.isEmpty()) return;
        List<Marker> candidates = hoveredLocalMarkers.stream()
                .distinct()
                .sorted(Comparator.comparingInt(Marker::getId))
                .toList();
        if (candidates.size() == 1) {
            openMarkerEditor(candidates.get(0));
        } else {
            markerPicker.setMarkers(candidates);
            addChild(markerPicker);
            KeyMapping.releaseAll();
        }
    }

    public GuiAtlas prepareToOpen() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));

        this.player = Minecraft.getInstance().player;
        updateDeathMarkerButton();
        updateRescanButton();
        updateAtlasData();
        if (!followPlayer && AntiqueAtlas.CONFIG.doSaveBrowsingPos) {
            loadSavedBrowsingPosition();
        }

        return this;
    }

    public void loadSavedBrowsingPosition() {
        // Apply zoom first, because browsing position depends on it:
        setMapScale(biomeData.getBrowsingZoom());
        mapOffsetX = biomeData.getBrowsingX();
        mapOffsetY = biomeData.getBrowsingY();
        isDragging = false;
    }

    @Override
    public void init() {
        super.init();
        //Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(true);
        screenScale = Minecraft.getInstance().getWindow().getGuiScale();
        // The marker lists extend beyond the book and must not influence its
        // position. Center the 310x218 book itself on the screen.
        setGuiCoords((width - WIDTH) / 2, (height - HEIGHT) / 2);

        updateBookmarkerList();
    }

    public void updateBookmarkerList() {
        markerGroups.removeAllContent();
        markerGroups.scrollTo(0, 0);
        markerGroupBookmarks.clear();
        groupedMarkers.clear();

        if (localMarkersData != null) {
            List<Marker> visibleMarkers = new ArrayList<>();
            for (Marker marker : localMarkersData.getAllMarkers()) {
                if (marker.isVisibleAhead() && !marker.isGlobal()
                        && MarkerVisibility.isVisible(marker.getType())) {
                    visibleMarkers.add(marker);
                }
            }
            visibleMarkers.sort(Comparator
                    .comparing(Marker::getType, MarkerTypeOrder.idComparator())
                    .thenComparingInt(Marker::getId));
            for (Marker marker : visibleMarkers) {
                groupedMarkers.computeIfAbsent(marker.getType(), ignored -> new ArrayList<>())
                        .add(marker);
            }
        }

        if (selectedMarkerGroup != null && !groupedMarkers.containsKey(selectedMarkerGroup)) {
            selectedMarkerGroup = null;
        }

        int contentY = 0;
        for (Map.Entry<ResourceLocation, List<Marker>> entry : groupedMarkers.entrySet()) {
            ResourceLocation markerTypeId = entry.getKey();
            MarkerType markerType = MarkerType.REGISTRY.get(markerTypeId);
            GuiMarkerGroupBookmark bookmark = new GuiMarkerGroupBookmark(markerType,
                    entry.getValue().size());
            bookmark.setSelected(markerTypeId.equals(selectedMarkerGroup));
            bookmark.addListener(button -> selectMarkerGroup(markerTypeId));

            markerGroupBookmarks.put(markerTypeId, bookmark);
            markerGroups.addContent(bookmark).setRelativeY(contentY);
            contentY += 20;
        }

        updateSelectedMarkerBookmarks();
    }

    private void selectMarkerGroup(ResourceLocation markerTypeId) {
        if (markerTypeId.equals(selectedMarkerGroup)) {
            closeMarkerGroupList();
            return;
        }
        selectedMarkerGroup = markerTypeId;
        markerGroupBookmarks.forEach((id, bookmark) ->
                bookmark.setSelected(id.equals(selectedMarkerGroup)));
        updateSelectedMarkerBookmarks();
    }

    private void closeMarkerGroupList() {
        selectedMarkerGroup = null;
        markerGroupBookmarks.values().forEach(bookmark -> bookmark.setSelected(false));
        markerBookmarks.removeAllContent();
    }

    private void updateSelectedMarkerBookmarks() {
        markerBookmarks.removeAllContent();
        markerBookmarks.scrollTo(0, 0);

        List<Marker> selectedMarkers = groupedMarkers.get(selectedMarkerGroup);
        if (selectedMarkers == null) return;

        int contentY = 0;
        for (Marker marker : selectedMarkers) {
            if (!marker.isVisibleAhead() || marker.isGlobal() || !MarkerVisibility.isVisible(marker.getType())) {
                continue;
            }
            GuiMarkerBookmark bookmark = new GuiMarkerBookmark(marker);

            bookmark.addListener(button -> {
                if (state.is(NORMAL)) {
                    setTargetPosition(marker.getX(), marker.getZ());
                    followPlayer = false;
                    btnPosition.setEnabled(true);
                } else if (state.is(DELETING_MARKER)) {
                    AtlasClientAPI.getMarkerAPI().deleteMarker(player.getCommandSenderWorld(),
                            getAtlasID(), marker.getId());
                    player.getCommandSenderWorld().playSound(player, player.blockPosition(),
                            SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.AMBIENT,
                            1F, 0.5F);
                    state.switchTo(NORMAL);
                }
            });

            markerBookmarks.addContent(bookmark).setRelativeY(contentY);
            contentY += 20;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseState) {
        boolean result = super.mouseClicked(mouseX, mouseY, mouseState);
        if (result) {
            return true;
        }

        int mapX = (width - MAP_WIDTH) / 2;
        int mapY = (height - MAP_HEIGHT) / 2;
        boolean isMouseOverMap = mouseX >= mapX && mouseX <= mapX + MAP_WIDTH &&
                mouseY >= mapY && mouseY <= mapY + MAP_HEIGHT;

        // Right-click edits a local marker. A right-click on empty map space is
        // deliberately consumed so it cannot close the atlas or start dragging.
        if (mouseState == 1 && state.is(NORMAL)) {
            if (isMouseOverMap) {
                if (!hoveredLocalMarkers.isEmpty()) {
                    editHoveredMarker();
                }
                return true;
            }

            // Preserve the original shortcut when right-clicking outside the map.
            onClose();
            return true;
        }

        // If clicked on the map, start dragging
        if (!state.is(NORMAL)) {
            int atlasID = getAtlasID();

            if (state.is(PLACING_MARKER) // If clicked on the map, place marker:
                    && isMouseOverMap && mouseState == 0 /* left click */) {
                markerFinalizer.setMarkerData(player.getCommandSenderWorld(), atlasID,
                        screenXToWorldX((int) mouseX), screenYToWorldZ((int) mouseY));
                addChild(markerFinalizer);

                blinkingIcon.setTexture(markerFinalizer.selectedType.getTexture(),
                        MARKER_SIZE, MARKER_SIZE);
                addChildBehind(markerFinalizer, blinkingIcon)
                        .setRelativeCoords((int) mouseX - getGuiX() - MARKER_SIZE / 2,
                                (int) mouseY - getGuiY() - MARKER_SIZE / 2);

                // Need to intercept keyboard events to type in the label:
                setInterceptKeyboard(true);

                // Un-press all keys to prevent player from walking infinitely:
                KeyMapping.releaseAll();

                state.switchTo(NORMAL);
                return true;
            } else if (state.is(DELETING_MARKER) // If clicked on a marker, delete it:
                    && hoveredMarker != null && !hoveredMarker.isGlobal() && isMouseOverMap && mouseState == 0) {
                AtlasClientAPI.getMarkerAPI().deleteMarker(player.getCommandSenderWorld(),
                        atlasID, hoveredMarker.getId());
                hoveredMarker = null;
                player.getCommandSenderWorld().playSound(player, player.blockPosition(),
                        SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.AMBIENT,
                        1F, 0.5F);
            }
            state.switchTo(NORMAL);
        } else if (isMouseOverMap && selectedButton == null) {
			if (hoveredMarker == null /*
										 * || !MarkerClickedCallback.EVENT.invoker().onClicked(player, hoveredMarker,
										 * mouseState).interruptsFurtherEvaluation()
										 */) {
                isDragging = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Modal children get exclusive keyboard control. This also prevents
        // navigation and zoom shortcuts from affecting the atlas behind them.
        if (isMapInteractionBlocked()) {
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_D && hasControlDown()) {
            // Ignore GLFW key-repeat events. Opening requires a five-second
            // hold, while a fresh press closes an already visible overlay.
            if (!diagnosticShortcutDown) {
                diagnosticShortcutDown = true;
                if (diagnosticVisible) {
                    diagnosticVisible = false;
                    diagnosticHoldStartNanos = 0L;
                } else {
                    diagnosticHoldStartNanos = System.nanoTime();
                }
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && (state.is(PLACING_MARKER) || state.is(DELETING_MARKER))) {
            // Escape first cancels the temporary map tool. A second Escape can
            // then close the atlas normally.
            selectedButton = null;
            state.switchTo(NORMAL);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            navigateMap(0, navigateStep);
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            navigateMap(0, -navigateStep);
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            navigateMap(navigateStep, 0);
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            navigateMap(-navigateStep, 0);
        } else if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            setMapScale(mapScale * 2);
        } else if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            setMapScale(mapScale / 2);
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
        } else {
            KeyMapping[] hotbarKeys = Minecraft.getInstance().options.keyHotbarSlots;
            for (KeyMapping bind : hotbarKeys) {
                // only handle hotbarkeys when marker gui isn't shown1
                if (bind.matches(keyCode, scanCode) && this.markerFinalizer.getParent() == null) {
                    onClose();
                    // if we close the gui, then don't handle the event
                    return false;
                }
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_D) {
            diagnosticShortcutDown = false;
            diagnosticHoldStartNanos = 0L;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double wheelMove) {
        if (isMapInteractionBlocked()) {
            super.mouseScrolled(mx, my, wheelMove);
            return true;
        }
        double origWheelMove = wheelMove;

        boolean handled = super.mouseScrolled(mx, my, origWheelMove);

        if (!handled && wheelMove != 0) {
            wheelMove = wheelMove > 0 ? 1 : -1;
            if (AntiqueAtlas.CONFIG.doReverseWheelZoom) {
                wheelMove *= -1;
            }

            double mouseOffsetX = getMapCenterScreenX() - getMouseX();
            double mouseOffsetY = getMapCenterScreenY() - getMouseY();
            double newScale = mapScale * Math.pow(2, wheelMove);
            double addOffsetX = 0;
            double addOffsetY = 0;
            if (Math.abs(mouseOffsetX) < MAP_WIDTH / 2f && Math.abs(mouseOffsetY) < MAP_HEIGHT / 2f) {
                addOffsetX = mouseOffsetX * wheelMove;
                addOffsetY = mouseOffsetY * wheelMove;

                if (wheelMove > 0) {
                    addOffsetX *= mapScale / newScale;
                    addOffsetY *= mapScale / newScale;
                }
            }

            setMapScale(newScale, (int) addOffsetX, (int) addOffsetY);

            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));

            return true;
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseState) {
        if (isMapInteractionBlocked()) {
            return super.mouseReleased(mouseX, mouseY, mouseState);
        }
        boolean result = false;
        if (mouseState != -1) {
            result = selectedButton != null || isDragging;
            selectedButton = null;
            isDragging = false;
            mapOffsetDeltaX = 0;
            mapOffsetDeltaY = 0;
        }
        return super.mouseReleased(mouseX, mouseY, mouseState) || result;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int lastMouseButton, double deltaX, double deltaY) {
        if (isMapInteractionBlocked()) {
            return super.mouseDragged(mouseX, mouseY, lastMouseButton, deltaX, deltaY);
        }
        boolean result = false;
        if (isDragging) {
            followPlayer = false;
            btnPosition.setEnabled(true);

            mapOffsetDeltaX += deltaX;
            mapOffsetDeltaY += deltaY;

            int offsetX = (int) (Math.signum(mapOffsetDeltaX) * Math.floor(Math.abs(mapOffsetDeltaX)));
            int offsetY = (int) (Math.signum(mapOffsetDeltaY) * Math.floor(Math.abs(mapOffsetDeltaY)));

            if (Math.abs(mapOffsetDeltaX) >= 1) {
                mapOffsetDeltaX = mapOffsetDeltaX - offsetX;
                mapOffsetX += offsetX;
            }

            if (Math.abs(mapOffsetDeltaY) >= 1) {
                mapOffsetDeltaY = mapOffsetDeltaY - offsetY;
                mapOffsetY += offsetY;
            }

            result = true;
        }
        return super.mouseDragged(mouseX, mouseY, lastMouseButton, deltaX, deltaY) || result;
    }

    @Override
    public void tick() {
        super.tick();
        updateRescanButton();
        updateDiagnosticShortcut();
        if (player == null) return;
        if (followPlayer) {
            setMapPosition(player.getBlockX(), player.getBlockZ());
        }
        if (player.getCommandSenderWorld().getGameTime() > timeButtonPressed + BUTTON_PAUSE) {
            navigateByButton(selectedButton);
        }

        if (targetOffsetX != null) {
            if (Math.abs(getTargetPositionX() - mapOffsetX) > navigateStep) {
                navigateMap(getTargetPositionX() > mapOffsetX ? navigateStep : -navigateStep, 0);
            } else {
                mapOffsetX = getTargetPositionX();
                targetOffsetX = null;
            }
        }

        if (targetOffsetY != null) {
            if (Math.abs(getTargetPositionY() - mapOffsetY) > navigateStep) {
                navigateMap(0, getTargetPositionY() > mapOffsetY ? navigateStep : -navigateStep);
            } else {
                mapOffsetY = getTargetPositionY();
                targetOffsetY = null;
            }
        }

        updateAtlasData();
    }

    private void requestAreaRescan() {
        ClientWorldScanner.RescanRequest request = AntiqueAtlasClientSegment.requestRescan();
        Component message = switch (request.result()) {
            case STARTED -> Component.translatable(
                    "message.antiqueatlas.rescan.started", request.queuedChunks());
            case ALREADY_RUNNING -> Component.translatable("message.antiqueatlas.rescan.alreadyRunning");
            case NOTHING_TO_SCAN -> Component.translatable("message.antiqueatlas.rescan.nothing");
            case UNAVAILABLE -> Component.translatable("message.antiqueatlas.rescan.unavailable");
        };
        if (player != null) player.displayClientMessage(message, true);
        updateRescanButton();
    }

    private void updateDeathMarkerButton() {
        if (btnDeathMarker == null) return;
        boolean enabled = ClientMapManager.getInstance().isAutoDeathMarkerEnabled();
        btnDeathMarker.setSelected(enabled);
        btnDeathMarker.setDimmed(!enabled);
        btnDeathMarker.setTooltip(List.of(
                Component.translatable("gui.antiqueatlas.deathMarker.title"),
                Component.translatable(enabled
                        ? "gui.antiqueatlas.deathMarker.enabled"
                        : "gui.antiqueatlas.deathMarker.disabled"),
                Component.translatable("gui.antiqueatlas.deathMarker.help.1"),
                Component.translatable("gui.antiqueatlas.deathMarker.help.2")));
    }

    private void updateRescanButton() {
        if (btnRescan == null) return;
        ClientWorldScanner.RescanStatus status = AntiqueAtlasClientSegment.getRescanStatus();
        btnRescan.setEnabled(!status.running());
        btnRescan.setSelected(status.running());
        if (status.running()) {
            btnRescan.setTooltip(List.of(
                    Component.translatable("gui.antiqueatlas.rescan.title"),
                    Component.translatable("gui.antiqueatlas.rescan.progress",
                            status.completedChunks(), status.totalChunks())));
        } else {
            btnRescan.setTooltip(List.of(
                    Component.translatable("gui.antiqueatlas.rescan.title"),
                    Component.translatable("gui.antiqueatlas.rescan.help.1"),
                    Component.translatable("gui.antiqueatlas.rescan.help.2"),
                    Component.translatable("gui.antiqueatlas.rescan.help.3")));
        }
    }

    private void updateDiagnosticShortcut() {
        if (diagnosticVisible || !diagnosticShortcutDown) return;

        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean dDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        if (!dDown || !hasControlDown() || isMapInteractionBlocked()) {
            diagnosticShortcutDown = false;
            diagnosticHoldStartNanos = 0L;
            return;
        }

        if (diagnosticHoldStartNanos != 0L
                && System.nanoTime() - diagnosticHoldStartNanos >= DIAGNOSTIC_HOLD_NANOS) {
            diagnosticVisible = true;
            diagnosticHoldStartNanos = 0L;
            resetDiagnosticSamples();
        }
    }

    /**
     * Update {@link #biomeData}, {@link #localMarkersData},
     * {@link #globalMarkersData}
     */
    private void updateAtlasData() {
        int atlasID = getAtlasID();

        biomeData = AntiqueAtlas.tileData
                .getData(atlasID, player.getCommandSenderWorld())
                .getWorldData(player.getCommandSenderWorld().dimension());
        globalMarkersData = AntiqueAtlas.globalMarkersData.getData()
                .getMarkersDataInWorld(player.getCommandSenderWorld().dimension());
        MarkersData markersData = AntiqueAtlas.markersData
                .getMarkersData(atlasID, player.getCommandSenderWorld());
        if (markersData != null) {
            localMarkersData = markersData
                    .getMarkersDataInWorld(player.getCommandSenderWorld().dimension());
        } else {
            localMarkersData = null;
        }
    }

    /**
     * Offset the map view depending on which button was pressed.
     */
    private void navigateByButton(GuiComponentButton btn) {
        if (btn == null) return;
        if (btn.equals(btnUp)) {
            navigateMap(0, navigateStep);
        } else if (btn.equals(btnDown)) {
            navigateMap(0, -navigateStep);
        } else if (btn.equals(btnLeft)) {
            navigateMap(navigateStep, 0);
        } else if (btn.equals(btnRight)) {
            navigateMap(-navigateStep, 0);
        }
    }

    /**
     * Offset the map view by given values, in blocks.
     */
    private void navigateMap(int dx, int dy) {
        mapOffsetX += dx;
        mapOffsetY += dy;
        followPlayer = false;
        btnPosition.setEnabled(true);
    }

    private void setMapPosition(int x, int z) {
        mapOffsetX = (int) (-x * mapScale);
        mapOffsetY = (int) (-z * mapScale);
    }

    private void setTargetPosition(int x, int z) {
        targetOffsetX = x;
        targetOffsetY = z;
    }

    private int getTargetPositionX() {
        return (int) (-targetOffsetX * mapScale);
    }

    private int getTargetPositionY() {
        return (int) (-targetOffsetY * mapScale);
    }


    /**
     * Set the pixel-to-block ratio, maintaining the current center of the screen.
     */
    public void setMapScale(double scale) {
        setMapScale(scale, 0, 0);
    }

    /**
     * Set the pixel-to-block ratio, maintaining the current center of the screen with additional offset.
     */
    private void setMapScale(double scale, int addOffsetX, int addOffsetY) {
        double oldScale = mapScale;
        mapScale = Math.min(Math.max(scale, AntiqueAtlas.CONFIG.minScale), AntiqueAtlas.CONFIG.maxScale);

        // Scaling not needed
        if (oldScale == mapScale) {
            return;
        }

        if (mapScale >= MIN_SCALE_THRESHOLD) {
            tileHalfSize = (int) Math.round(8 * mapScale);
            tile2ChunkScale = 1;
        } else {
            tileHalfSize = (int) Math.round(8 * MIN_SCALE_THRESHOLD);
            tile2ChunkScale = (int) Math.round(MIN_SCALE_THRESHOLD / mapScale);
        }

        // Times 2 because the contents of the Atlas are rendered at resolution 2 times smaller:
        scaleBar.setMapScale(mapScale * 2);
        mapOffsetX = (int) ((mapOffsetX + addOffsetX) * (mapScale / oldScale));
        mapOffsetY = (int) ((mapOffsetY + addOffsetY) * (mapScale / oldScale));
        scaleClipIndex = Mth.log2((int) (mapScale * 8192)) + 1 - 13;
        zoomLevel = -scaleClipIndex + zoomLevelOne;
        scaleAlpha = 255;

        if (followPlayer && (addOffsetX != 0 || addOffsetY != 0)) {
            followPlayer = false;
            btnPosition.setEnabled(true);
        }
    }

    @Override
    public void render(GuiGraphics matrices, int mouseX, int mouseY, float par3) {
        final boolean collectDiagnostics = diagnosticVisible;
        final long diagnosticFrameStart = collectDiagnostics ? System.nanoTime() : 0L;
        long diagnosticTerrainTime = 0L;
        if (collectDiagnostics) {
            diagnosticTilesVisited = 0;
            diagnosticSubtilesRendered = 0;
            diagnosticBatchCount = 0;
            diagnosticMarkersRendered = 0;
        }

        long currentMillis = System.currentTimeMillis();
        long deltaMillis = currentMillis - lastUpdateMillis;
        lastUpdateMillis = currentMillis;

        if (AntiqueAtlas.CONFIG.debugRender) {
            renderTimes[renderTimesIndex++] = System.currentTimeMillis();
            if (renderTimesIndex == renderTimes.length) {
                renderTimesIndex = 0;
                double elapsed = 0;
                for (int i = 0; i < renderTimes.length - 1; i++) {
                    elapsed += renderTimes[i + 1] - renderTimes[i];
                }
                System.out.printf("GuiAtlas avg. render time: %.3f\n", elapsed / renderTimes.length);
            }
        }

        super.renderBackground(matrices);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        // TODO fix me for 1.17
//        RenderSystem.enableAlphaTest();
//        RenderSystem.alphaFunc(GL11.GL_GREATER, 0); // So light detail on tiles is visible
        ITexture bookTexture = AntiqueAtlas.CONFIG.showBookSpine
                ? Textures.BOOK : Textures.BOOK_FLAT;
        bookTexture.draw(matrices, getGuiX(), getGuiY());

        if ((stack == null && AntiqueAtlas.CONFIG.itemNeeded) || biomeData == null)
            return;

        if (state.is(DELETING_MARKER)) {
            RenderSystem.setShaderColor(1, 1, 1, 0.5f);
        }
        RenderSystem.enableScissor(
                (int) ((getGuiX() + MAP_BORDER_WIDTH) * screenScale),
                (int) ((Minecraft.getInstance().getWindow().getHeight() - (getGuiY() + MAP_BORDER_HEIGHT + MAP_HEIGHT) * screenScale)),
                (int) (MAP_WIDTH * screenScale), (int) (MAP_HEIGHT * screenScale));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        final long diagnosticTerrainStart = collectDiagnostics ? System.nanoTime() : 0L;
        // Find chunk coordinates of the top left corner of the map.
        // The 'roundToBase' is required so that when the map scales below the
        // threshold the tiles don't change when map position changes slightly.
        // The +-2 at the end provide margin so that tiles at the edges of
        // the page have their stitched texture correct.
        int mapStartX = MathUtil.roundToBase((int) Math.floor(-((double) MAP_WIDTH / 2d + mapOffsetX + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapStartZ = MathUtil.roundToBase((int) Math.floor(-((double) MAP_HEIGHT / 2d + mapOffsetY + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapEndX = MathUtil.roundToBase((int) Math.ceil(((double) MAP_WIDTH / 2d - mapOffsetX + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapEndZ = MathUtil.roundToBase((int) Math.ceil(((double) MAP_HEIGHT / 2d - mapOffsetY + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapStartScreenX = worldXToScreenX(mapStartX << 4);
        int mapStartScreenY = worldZToScreenY(mapStartZ << 4);
        prepareTerrainCache(mapStartX, mapStartZ, mapEndX, mapEndZ,
                mapStartScreenX, mapStartScreenY);

        matrices.pose().pushPose();
        matrices.pose().translate(mapStartScreenX, mapStartScreenY, 0);
        terrainCacheClouds.draw(matrices, tileHalfSize);
        terrainCacheBatches.values().forEach(batch -> batch.draw(matrices, tileHalfSize));
        if (collectDiagnostics) {
            diagnosticTilesVisited = terrainCacheTilesVisited;
            diagnosticSubtilesRendered = terrainCacheSubtilesRendered;
            diagnosticBatchCount = terrainCacheBatches.size();
            diagnosticTerrainTime = System.nanoTime() - diagnosticTerrainStart;
        }

        matrices.pose().popPose();

        int markersStartX = MathUtil.roundToBase(mapStartX, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP - 1;
        int markersStartZ = MathUtil.roundToBase(mapStartZ, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP - 1;
        int markersEndX = MathUtil.roundToBase(mapEndX, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP + 1;
        int markersEndZ = MathUtil.roundToBase(mapEndZ, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP + 1;

        // Overlay the frame so that edges of the map are smooth:
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ITexture bookFrameTexture = AntiqueAtlas.CONFIG.showBookSpine
                ? Textures.BOOK_FRAME : Textures.BOOK_FRAME_FLAT;
        bookFrameTexture.draw(matrices, getGuiX(), getGuiY());

        double iconScale = getIconScale();

        hoveredMarker = null;
        hoveredLocalMarkers.clear();
        // Draw global markers:
        renderMarkers(matrices, markersStartX, markersStartZ, markersEndX, markersEndZ, globalMarkersData);
        renderMarkers(matrices, markersStartX, markersStartZ, markersEndX, markersEndZ, localMarkersData);

        RenderSystem.disableScissor();

        ITexture narrowBookFrameTexture = AntiqueAtlas.CONFIG.showBookSpine
                ? Textures.BOOK_FRAME_NARROW : Textures.BOOK_FRAME_NARROW_FLAT;
        narrowBookFrameTexture.draw(matrices, getGuiX(), getGuiY());

        renderScaleOverlay(matrices, deltaMillis);

        // The marker filter never hides the player icon.
        renderPlayer(matrices, iconScale);

        // Draw buttons:
        super.render(matrices, mouseX, mouseY, par3);

        // Draw the semi-transparent marker attached to the cursor when placing a new marker:
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (state.is(PLACING_MARKER)) {
            RenderSystem.setShaderColor(1, 1, 1, 0.5f);
            markerFinalizer.selectedType.calculateMip(iconScale, mapScale, screenScale);
            MarkerRenderInfo renderInfo = markerFinalizer.selectedType.getRenderInfo(iconScale, mapScale, screenScale);
            markerFinalizer.selectedType.resetMip();
            renderInfo.tex.draw(matrices, mouseX + renderInfo.x, mouseY + renderInfo.y);
            MarkerColorRenderer.drawMarkerBanner(matrices,
                    mouseX + renderInfo.x,
                    mouseY + renderInfo.y,
                    renderInfo.width,
                    renderInfo.height,
                    markerFinalizer.getSelectedColor(), 0x88);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        RenderSystem.disableBlend();

        if (AntiqueAtlas.CONFIG.debugRender && !isMapInteractionBlocked() && !isDragging && isMouseOver) {
            int x = screenXToWorldX((int) getMouseX());
            int z = screenYToWorldZ((int) getMouseY());

            String coords = String.format("Coords: %d / %d", x, z);

            ChunkPos pos = new ChunkPos(new BlockPos(x, 0, z));
            String chunks = String.format("Chunks: %d / %d", pos.x, pos.z);
            ResourceLocation tile = biomeData.getTile(pos.x, pos.z);

            if (tile == null) {
                drawTooltip(Arrays.asList(Component.literal(coords), Component.literal(chunks)), font);
            } else {
                String texture_set = TileTextureMap.instance().getTextureSet(tile).name.toString();
                drawTooltip(Arrays.asList(
                		Component.literal(coords),
                		Component.literal(chunks),
                		Component.literal("Tile: " + tile),
                		Component.literal("TSet: " + texture_set)),
                        font);
            }
        }

        if (collectDiagnostics) {
            recordDiagnosticSample(System.nanoTime() - diagnosticFrameStart, diagnosticTerrainTime);
            renderDiagnosticOverlay(matrices);
        }
    }

    private void prepareTerrainCache(int mapStartX, int mapStartZ, int mapEndX, int mapEndZ,
                                     int mapStartScreenX, int mapStartScreenY) {
        int stitchMargin = tile2ChunkScale * 2;
        boolean worldChanged = terrainCacheWorld != biomeData;
        boolean scopeChanged = terrainCacheStartX != mapStartX || terrainCacheStartZ != mapStartZ
                || terrainCacheEndX != mapEndX || terrainCacheEndZ != mapEndZ;
        boolean viewportChanged = terrainCacheGuiX != getGuiX() || terrainCacheGuiY != getGuiY();
        long globalWorldRevision = biomeData.getRenderRevision();
        long worldRevision = terrainCacheWorldRevision;
        if (!terrainCacheValid || worldChanged || scopeChanged
                || terrainCacheGlobalWorldRevision != globalWorldRevision) {
            worldRevision = biomeData.getRenderRevision(
                    mapStartX - stitchMargin, mapStartZ - stitchMargin,
                    mapEndX + stitchMargin, mapEndZ + stitchMargin);
        }
        long textureRevision = TileTextureMap.instance().getRenderRevision();

        String rebuildReason = null;
        if (!terrainCacheValid) {
            rebuildReason = "initialisation";
        } else if (worldChanged) {
            rebuildReason = "monde";
        } else if (terrainCacheTileHalfSize != tileHalfSize
                || terrainCacheTile2ChunkScale != tile2ChunkScale) {
            rebuildReason = "zoom";
        } else if (terrainCacheCloudsEnabled != AntiqueAtlas.CONFIG.showUnexploredClouds) {
            rebuildReason = "nuages";
        } else if (viewportChanged) {
            rebuildReason = "fenetre";
        } else if (scopeChanged) {
            rebuildReason = "deplacement";
        } else if (terrainCacheTextureRevision != textureRevision) {
            rebuildReason = "textures";
        } else if (terrainCacheWorldRevision != worldRevision) {
            rebuildReason = "terrain";
        }

        if (rebuildReason == null) {
            terrainCacheFrameState = "HIT";
            terrainCacheGlobalWorldRevision = globalWorldRevision;
            terrainCacheWorldRevision = worldRevision;
            return;
        }

        long buildStart = System.nanoTime();
        terrainCacheBatches.clear();
        terrainCacheClouds.clear();
        terrainCacheTilesVisited = 0;
        terrainCacheSubtilesRendered = 0;

        TileRenderIterator tiles = new TileRenderIterator(biomeData);
        tiles.setScope(new Rect(mapStartX, mapStartZ, mapEndX, mapEndZ));
        tiles.setStep(tile2ChunkScale);

        // TileRenderIterator reuses its four SubTile instances. Store only
        // primitive quad data. Keep one chunk-screen-width around the page so
        // it can move within the same chunk scope without revealing a gap.
        int cachePanMargin = tileHalfSize * 2;
        int mapLeft = getGuiX() + MAP_BORDER_WIDTH - cachePanMargin;
        int mapTop = getGuiY() + MAP_BORDER_HEIGHT - cachePanMargin;
        int mapRight = getGuiX() + MAP_BORDER_WIDTH + MAP_WIDTH + cachePanMargin;
        int mapBottom = getGuiY() + MAP_BORDER_HEIGHT + MAP_HEIGHT + cachePanMargin;
        for (SubTileQuartet subtiles : tiles) {
            terrainCacheTilesVisited++;
            for (SubTile subtile : subtiles) {
                if (subtile == null) continue;
                int drawX = subtile.x * tileHalfSize;
                int drawY = subtile.y * tileHalfSize;
                int screenX = mapStartScreenX + drawX;
                int screenY = mapStartScreenY + drawY;
                if (screenX >= mapRight || screenY >= mapBottom
                        || screenX + tileHalfSize <= mapLeft
                        || screenY + tileHalfSize <= mapTop) continue;
                if (subtile.tile == null) {
                    if (AntiqueAtlas.CONFIG.showUnexploredClouds) {
                        terrainCacheClouds.add(drawX, drawY, tileHalfSize,
                                subtile.variationNumber ^ (subtile.part.ordinal() * 0x9E3779B9));
                    }
                    continue;
                }
                ITexture texture = TileTextureMap.instance().getTexture(subtile);
                if (!(texture instanceof TileTexture tileTexture)) continue;
                terrainCacheBatches.computeIfAbsent(tileTexture, TileRenderBatch::new).add(
                        drawX, drawY,
                        subtile.getTextureU() * 8, subtile.getTextureV() * 8);
                terrainCacheSubtilesRendered++;
            }
        }

        terrainCacheWorld = biomeData;
        terrainCacheStartX = mapStartX;
        terrainCacheStartZ = mapStartZ;
        terrainCacheEndX = mapEndX;
        terrainCacheEndZ = mapEndZ;
        terrainCacheGuiX = getGuiX();
        terrainCacheGuiY = getGuiY();
        terrainCacheTileHalfSize = tileHalfSize;
        terrainCacheTile2ChunkScale = tile2ChunkScale;
        terrainCacheCloudsEnabled = AntiqueAtlas.CONFIG.showUnexploredClouds;
        terrainCacheGlobalWorldRevision = globalWorldRevision;
        terrainCacheWorldRevision = worldRevision;
        terrainCacheTextureRevision = textureRevision;
        terrainCacheValid = true;
        terrainCacheFrameState = "REBUILD";
        terrainCacheLastReason = rebuildReason;
        terrainCacheLastBuildNanos = System.nanoTime() - buildStart;
        terrainCacheRebuildCount++;
    }

    private void resetDiagnosticSamples() {
        Arrays.fill(diagnosticFrameTimes, 0L);
        Arrays.fill(diagnosticTerrainTimes, 0L);
        diagnosticSampleIndex = 0;
        diagnosticSamples = 0;
        diagnosticFrameTotal = 0L;
        diagnosticTerrainTotal = 0L;
    }

    private void recordDiagnosticSample(long frameTime, long terrainTime) {
        if (diagnosticSamples == DIAGNOSTIC_SAMPLE_COUNT) {
            diagnosticFrameTotal -= diagnosticFrameTimes[diagnosticSampleIndex];
            diagnosticTerrainTotal -= diagnosticTerrainTimes[diagnosticSampleIndex];
        } else {
            diagnosticSamples++;
        }

        diagnosticFrameTimes[diagnosticSampleIndex] = frameTime;
        diagnosticTerrainTimes[diagnosticSampleIndex] = terrainTime;
        diagnosticFrameTotal += frameTime;
        diagnosticTerrainTotal += terrainTime;
        diagnosticSampleIndex = (diagnosticSampleIndex + 1) % DIAGNOSTIC_SAMPLE_COUNT;
    }

    private void renderDiagnosticOverlay(GuiGraphics graphics) {
        if (diagnosticSamples == 0) return;

        double atlasMillis = diagnosticFrameTotal / (diagnosticSamples * 1_000_000.0);
        double terrainMillis = diagnosticTerrainTotal / (diagnosticSamples * 1_000_000.0);
        List<String> lines = List.of(
                "ATLAS DIAGNOSTIC",
                "FPS : " + Minecraft.getInstance().getFps(),
                String.format("Atlas : %.2f ms", atlasMillis),
                String.format("Terrain : %.2f ms", terrainMillis),
                "Zoom : x" + zoomNames[zoomLevel],
                "Tuiles : " + diagnosticTilesVisited,
                "Sous-tuiles : " + diagnosticSubtilesRendered,
                "Textures / lots : " + diagnosticBatchCount + " / " + diagnosticBatchCount,
                "Marqueurs : " + diagnosticMarkersRendered,
                "Cache terrain : " + terrainCacheFrameState,
                "Reconstructions : " + terrainCacheRebuildCount,
                String.format("Derniere : %.2f ms (%s)",
                        terrainCacheLastBuildNanos / 1_000_000.0, terrainCacheLastReason),
                "Ctrl + D pour fermer"
        );

        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }

        int panelWidth = textWidth + DIAGNOSTIC_PADDING * 2;
        int panelHeight = lines.size() * font.lineHeight + DIAGNOSTIC_PADDING * 2;
        int panelX = DIAGNOSTIC_MARGIN;
        int panelY = height - panelHeight - DIAGNOSTIC_MARGIN;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB0000000);
        int textY = panelY + DIAGNOSTIC_PADDING;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFFFB85C : 0xFFE5E5E5;
            graphics.drawString(font, lines.get(i), panelX + DIAGNOSTIC_PADDING, textY, color, false);
            textY += font.lineHeight;
        }
    }

    private void renderPlayer(GuiGraphics matrices, double iconScale) {
        int playerOffsetX = worldXToScreenX(player.getBlockX());
        int playerOffsetY = worldZToScreenY(player.getBlockZ());

        playerOffsetX = Mth.clamp(playerOffsetX, getGuiX() + MAP_BORDER_WIDTH, getGuiX() + MAP_WIDTH + MAP_BORDER_WIDTH);
        playerOffsetY = Mth.clamp(playerOffsetY, getGuiY() + MAP_BORDER_HEIGHT, getGuiY() + MAP_HEIGHT + MAP_BORDER_HEIGHT);

        // Draw the icon:
        RenderSystem.setShaderColor(1, 1, 1, state.is(PLACING_MARKER) ? 0.5f : 1);
        float playerRotation = (float) Math.round(player.getYRot() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS * 360f;

        Textures.PLAYER.drawCenteredWithRotation(matrices, playerOffsetX, playerOffsetY, (int) Math.round(PLAYER_ICON_WIDTH * iconScale), (int) Math.round(PLAYER_ICON_HEIGHT * iconScale), playerRotation);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private void renderScaleOverlay(GuiGraphics matrices, long deltaMillis) {
        if (scaleAlpha > 3) {
            matrices.pose().pushPose();
            matrices.pose().translate(getGuiX() + WIDTH - 13, getGuiY() + 12, 0);

            int color = scaleAlpha << 24;

            String text;
            int textWidth, xWidth;

            text = "x";
            xWidth = textWidth = this.font.width(text);
            xWidth++;
            matrices.drawString(font, text, -textWidth, 0, color);

            text = zoomNames[zoomLevel];
            if (text.contains("/")) {
                String[] parts = text.split("/");

                int centerXtranslate = Math.max(this.font.width(parts[0]), this.font.width(parts[1])) / 2;
                matrices.pose().translate(-xWidth - centerXtranslate, (float) -this.font.lineHeight / 2, 0);

                matrices.fill(-centerXtranslate - 1, this.font.lineHeight - 1, centerXtranslate, this.font.lineHeight, color);

                textWidth = this.font.width(parts[0]);
                matrices.drawString(font, parts[0], -textWidth / 2, 0, color);

                textWidth = this.font.width(parts[1]);
                matrices.drawString(font, parts[1], -textWidth / 2, 10, color);
            } else {
                textWidth = this.font.width(text);
                matrices.drawString(font, text, -textWidth - xWidth + 1, 2, color);
            }

            matrices.pose().popPose();

            int deltaScaleAlpha = (int) (deltaMillis * 0.256);
            // because of some crazy high frame rate
            if (deltaScaleAlpha == 0) {
                deltaScaleAlpha = 1;
            }

            scaleAlpha -= deltaScaleAlpha;

            if (scaleAlpha < 0)
                scaleAlpha = 0;

        }
    }

    private void renderMarkers(GuiGraphics matrices, int markersStartX, int markersStartZ,
                               int markersEndX, int markersEndZ, DimensionMarkersData markersData) {
        if (markersData == null) return;

        for (int x = markersStartX; x <= markersEndX; x++) {
            for (int z = markersStartZ; z <= markersEndZ; z++) {
                List<Marker> markers = markersData.getMarkersAtChunk(x, z);
                if (markers == null) continue;
                for (Marker marker : markers) {
                    renderMarker(matrices, marker, getIconScale());
                }
            }
        }
    }

    private void renderMarker(GuiGraphics matrices, Marker marker, double scale) {
        if (!MarkerVisibility.isVisible(marker.getType())) return;
        MarkerType type = MarkerType.REGISTRY.get(marker.getType());
        if (type.shouldHide(false, scaleClipIndex)) {
            return;
        }

        int markerX = worldXToScreenX(marker.getX());
        int markerY = worldZToScreenY(marker.getZ());
        if (!marker.isVisibleAhead() &&
                !biomeData.hasTileAt(marker.getChunkX(), marker.getChunkZ())) {
            return;
        }
        type.calculateMip(scale, mapScale, screenScale);
        MarkerRenderInfo info = type.getRenderInfo(scale, mapScale, screenScale);

        boolean mouseIsOverMarker = !isMapInteractionBlocked()
                && type.shouldHover((getMouseX() - (markerX + info.x)) / info.tex.width(),
                (getMouseY() - (markerY + info.y)) / info.tex.height());
        type.resetMip();

        if (mouseIsOverMarker) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
            hoveredMarker = marker;
            if (!marker.isGlobal()) hoveredLocalMarkers.add(marker);
//            MarkerHoveredCallback.EVENT.invoker().onHovered(player, marker);
        } else {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (hoveredMarker == marker) {
                hoveredMarker = null;
            }
        }

        if (state.is(PLACING_MARKER)) {
            RenderSystem.setShaderColor(1, 1, 1, 0.5f);
        } else if (state.is(DELETING_MARKER) && marker.isGlobal()) {
            RenderSystem.setShaderColor(1, 1, 1, 0.5f);
        } else {
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        if (AntiqueAtlas.CONFIG.debugRender) {
            System.out.println("Rendering Marker: " + info.tex);
        }

        if (markerX <= getGuiX() + MAP_BORDER_WIDTH || markerX >= getGuiX() + MAP_WIDTH + MAP_BORDER_WIDTH
                || markerY <= getGuiY() + MAP_BORDER_HEIGHT || markerY >= getGuiY() + MAP_HEIGHT + MAP_BORDER_HEIGHT
        ) {
            if (!type.isTechnical()) {
                RenderSystem.setShaderColor(1, 1, 1, 0.5f);
                info.scale(0.8);
            }
        }

        if (!type.isTechnical()) {
            markerX = Mth.clamp(markerX, getGuiX() + MAP_BORDER_WIDTH, getGuiX() + MAP_WIDTH + MAP_BORDER_WIDTH);
            markerY = Mth.clamp(markerY, getGuiY() + MAP_BORDER_HEIGHT, getGuiY() + MAP_HEIGHT + MAP_BORDER_HEIGHT);
        }

        int badgeAlpha = state.is(PLACING_MARKER)
                || (state.is(DELETING_MARKER) && marker.isGlobal()) ? 0x78 : 0xD8;
        info.tex.draw(matrices, markerX + info.x, markerY + info.y, info.width, info.height);
        // At zoom levels below x1 the fixed-size banner no longer tracks the
        // shrinking marker cleanly, so keep it visible only from x1 upwards.
        if (zoomLevel <= zoomLevelOne) {
            MarkerColorRenderer.drawMarkerBanner(matrices,
                    markerX + info.x,
                    markerY + info.y,
                    info.width,
                    info.height,
                    marker.getColor(), badgeAlpha);
        }
        if (diagnosticVisible) {
            diagnosticMarkersRendered++;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (isMouseOver && mouseIsOverMarker && marker.getLabel().getString().length() > 0) {
            drawTooltip(Collections.singletonList(marker.getLabel()), font);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closeMarkerGroupList();
        markerFinalizer.closeChild();
        if (markerFilter.getParent() != null) markerFilter.closeChild();
        if (markerSearch.getParent() != null) markerSearch.closeChild();
        if (markerPicker.getParent() != null) markerPicker.closeChild();
        removeChild(blinkingIcon);
        // Keyboard.enableRepeatEvents(false);
        if (biomeData != null && (biomeData.getBrowsingX() != mapOffsetX
                || biomeData.getBrowsingY() != mapOffsetY
                || Double.compare(biomeData.getBrowsingZoom(), mapScale) != 0)) {
            biomeData.setBrowsingPosition(mapOffsetX, mapOffsetY, mapScale);
            ClientMapManager.getInstance().markStateDirty();
        }
        super.onClose();
    }

    /**
     * Returns the Y coordinate that the cursor is pointing at.
     */
    private int screenXToWorldX(int mouseX) {
        return (int) Math.round((double) (mouseX - getMapCenterScreenX() - mapOffsetX) / mapScale);
    }

    /**
     * Returns the Y block coordinate that the cursor is pointing at.
     */
    private int screenYToWorldZ(int mouseY) {
        return (int) Math.round((double) (mouseY - getMapCenterScreenY() - mapOffsetY) / mapScale);
    }

    private int worldXToScreenX(int x) {
        return (int) Math.round((double) x * mapScale + getMapCenterScreenX() + mapOffsetX);
    }

    private int worldZToScreenY(int z) {
        return (int) Math.round((double) z * mapScale + getMapCenterScreenY() + mapOffsetY);
    }

    /**
     * The map is anchored to the explicitly centered book. Side panels extend
     * beyond its bounds but do not participate in this coordinate system.
     */
    private int getMapCenterScreenX() {
        return getGuiX() + WIDTH / 2;
    }

    private int getMapCenterScreenY() {
        return getGuiY() + HEIGHT / 2;
    }

    @Override
    protected void onChildClosed(GuiComponent child) {
        if (child.equals(markerFinalizer)) {
            setInterceptKeyboard(true);
            removeChild(blinkingIcon);
            hoveredMarker = null;
            hoveredLocalMarkers.clear();
            updateBookmarkerList();
        } else if (child.equals(markerFilter)) {
            btnMarkerFilter.setSelected(false);
            hoveredMarker = null;
            updateBookmarkerList();
        } else if (child.equals(markerSearch)) {
            btnMarkerSearch.setSelected(false);
            hoveredMarker = null;
        }
    }

    /**
     * The atlas must be inert while one of its modal panels is displayed.
     * Keeping this explicit also prevents map marker hover state from being
     * computed and rendered behind a modal.
     */
    private boolean isMapInteractionBlocked() {
        return markerFinalizer.getParent() != null
                || markerFilter.getParent() != null
                || markerSearch.getParent() != null
                || markerPicker.getParent() != null;
    }

    /**
     * Update all text labels to current localization.
     */
    public void updateL18n() {
        btnMaps.setTitle(Component.translatable("gui.antiqueatlas.maps.manage"));
        btnMarker.setTitle(Component.translatable("gui.antiqueatlas.addMarker"));
        btnDelMarker.setTitle(Component.translatable("gui.antiqueatlas.delMarker"));
        btnMarkerFilter.setTitle(Component.translatable("gui.antiqueatlas.markerFilter.title"));
        btnMarkerSearch.setTitle(Component.translatable("gui.antiqueatlas.markerSearch.title"));
        updateDeathMarkerButton();
        updateRescanButton();
    }

    /**
     * Returns the scale of markers and player icon at given mapScale.
     */
    private double getIconScale() {
        if (AntiqueAtlas.CONFIG.doScaleMarkers) {
            if (mapScale < 0.5) return 0.5;
            if (mapScale > 1) return 2;
        }
        return 1;
    }

    /**
     * The map belongs to the player. The atlas item only controls access.
     */
    private int getAtlasID() {
        return ClientMapManager.getInstance().getActiveAtlasId();
    }
}
