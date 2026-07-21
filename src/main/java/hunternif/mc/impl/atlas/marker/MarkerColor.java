package hunternif.mc.impl.atlas.marker;

import java.util.Locale;

/**
 * Optional marker badge color. The RGB values match Minecraft's sixteen
 * legacy/chat colors exactly.
 */
public enum MarkerColor {
    NONE("none", -1),
    BLACK("black", 0x000000),
    DARK_BLUE("dark_blue", 0x0000AA),
    DARK_GREEN("dark_green", 0x00AA00),
    DARK_AQUA("dark_aqua", 0x00AAAA),
    DARK_RED("dark_red", 0xAA0000),
    DARK_PURPLE("dark_purple", 0xAA00AA),
    GOLD("gold", 0xFFAA00),
    GRAY("gray", 0xAAAAAA),
    DARK_GRAY("dark_gray", 0x555555),
    BLUE("blue", 0x5555FF),
    GREEN("green", 0x55FF55),
    AQUA("aqua", 0x55FFFF),
    RED("red", 0xFF5555),
    LIGHT_PURPLE("light_purple", 0xFF55FF),
    YELLOW("yellow", 0xFFFF55),
    WHITE("white", 0xFFFFFF);

    private final String serializedName;
    private final int rgb;

    MarkerColor(String serializedName, int rgb) {
        this.serializedName = serializedName;
        this.rgb = rgb;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public int getRgb() {
        return rgb;
    }

    public boolean isColored() {
        return this != NONE;
    }

    public String getTranslationKey() {
        return "gui.antiqueatlas.markerColor." + serializedName;
    }

    public static MarkerColor fromSerializedName(String name) {
        if (name == null || name.isBlank()) return NONE;
        String normalized = name.toLowerCase(Locale.ROOT);
        for (MarkerColor color : values()) {
            if (color.serializedName.equals(normalized)) return color;
        }
        return NONE;
    }
}
