package hunternif.mc.impl.atlas.client;

import java.util.HashMap;
import java.util.Map;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.client.texture.IconTexture;
import hunternif.mc.impl.atlas.client.texture.RegionTexture;
import hunternif.mc.impl.atlas.client.texture.Texture;
import hunternif.mc.impl.atlas.util.ResourceLocations;
import net.minecraft.resources.ResourceLocation;

public class Textures {
    public final static Map<ResourceLocation, ITexture> TILE_TEXTURES_MAP  = new HashMap<>();

    private static final String MOD_PREFIX = AntiqueAtlas.ID + ":";
    private static final String GUI = MOD_PREFIX + "textures/gui/";
    private static final String GUI_ICONS = GUI + "icons/";
    private static final String GUI_SCALEBAR = GUI + "scalebar/";

    public static final ITexture
            BOOK = gui("book.png", 310, 218),
            BOOK_FLAT = gui("book_flat.png", 310, 218),
            BOOK_FRAME = gui("book_frame.png", 310, 218),
            BOOK_FRAME_FLAT = gui("book_frame_flat.png", 310, 218),
            BOOK_FRAME_NARROW = gui("book_frame_narrow.png", 310, 218),
            BOOK_FRAME_NARROW_FLAT = gui("book_frame_narrow_flat.png", 310, 218),
            BTN_ARROWS = gui("navigate_arrows.png", 24, 24),
            BTN_POSITION = gui("position.png", 24, 24),
            BOOKMARKS = gui("bookmarks.png", 84, 36),
            BOOKMARKS_LEFT = gui("bookmarks_l.png", 84, 36),
            PLAYER = gui("player.png", 7, 8),
            SCROLLBAR_HOR = gui("scrollbar_hor.png", 8, 7),
            SCROLLBAR_VER = gui("scrollbar_ver.png", 7,8),
            MARKER_FRAME_ON = gui("marker_frame_on.png", 34, 34),
            MARKER_FRAME_OFF = gui("marker_frame_off.png", 34, 34),
            MARKER_BANNER_BASE = gui("marker_banner_base.png", 7, 10),
            MARKER_BANNER_FABRIC = gui("marker_banner_fabric.png", 7, 10),
            ERASER = gui("eraser.png", 24, 24),

            SCALEBAR_4 = scaleBar("scalebar_4.png"),
            SCALEBAR_8 = scaleBar("scalebar_8.png"),
            SCALEBAR_16 = scaleBar("scalebar_16.png"),
            SCALEBAR_32 = scaleBar("scalebar_32.png"),
            SCALEBAR_64 = scaleBar("scalebar_64.png"),
            SCALEBAR_128 = scaleBar("scalebar_128.png"),
            SCALEBAR_256 = scaleBar("scalebar_256.png"),
            SCALEBAR_512 = scaleBar("scalebar_512.png"),

            ICON_ADD_MARKER = icon("add_marker.png"),
            ICON_DELETE_MARKER = icon("del_marker.png"),
            ICON_HIDE_MARKERS = icon("hide_markers.png"),
            ICON_MARKER_SEARCH = new IconTexture(ResourceLocations.parse(
                    "minecraft:textures/item/spyglass.png")),
            ICON_DEATH_MARKER = new RegionTexture(ResourceLocations.parse(
                    MOD_PREFIX + "textures/gui/markers/tomb.png"), 32, 32,
                    10, 9, 12, 13, 15, 16),
            ICON_MAPS = new IconTexture(ResourceLocations.parse(MOD_PREFIX + "textures/item/antique_atlas.png"));

    public static final ResourceLocation EXPORTED_BG = ResourceLocations.parse(GUI + "exported_bg.png");
    public static final ResourceLocation MARKER_BANNER_BASE_LOCATION =
            ResourceLocations.parse(GUI + "marker_banner_base.png");
    public static final ResourceLocation MARKER_BANNER_FABRIC_LOCATION =
            ResourceLocations.parse(GUI + "marker_banner_fabric.png");

    // Constructor helpers:
    private static ITexture gui(String fileName, int width, int height) {
        return new Texture(ResourceLocations.parse(GUI + fileName), width, height);
    }

    private static ITexture scaleBar(String fileName) {
        return new Texture(ResourceLocations.parse(GUI_SCALEBAR + fileName), 20, 8);
    }

    private static ITexture icon(String fileName) {
        return new IconTexture(ResourceLocations.parse(GUI_ICONS + fileName));
    }
}
