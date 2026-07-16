package hunternif.mc.impl.atlas;

/**
 * Plain client configuration serialized by {@code ClientConfigStorage}.
 * Keeping this as a simple data object avoids loading an external config mod.
 */
public class AntiqueAtlasConfig {
    // Gameplay
    public boolean doSaveBrowsingPos = true;
    public boolean autoVillageMarkers = false;
    public boolean itemNeeded = false;

    // Interface
    public boolean doScaleMarkers = false;
    public double defaultScale = 0.5;
    public double minScale = 1.0 / 32.0;
    public double maxScale = 4.0;
    public boolean doReverseWheelZoom = false;
    public int maxMaps = 10;

    // Performance and legacy-compatible values
    public int scanRadius = 11;
    public boolean forceChunkLoading = false;
    public float newScanInterval = 1.0F;
    // Kept only so the unused legacy server scanner still compiles. Manual
    // client rescans are deliberately one-shot and are not config toggles.
    public transient boolean doRescan = false;
    public transient int rescanRate = 4;
    public int clientScanBudget = 8;
    public int markerLimit = 1024;
    public boolean doScanPonds = false;
    public boolean doScanRavines = false;
    public boolean debugRender = false;
    public boolean resourcePackLogging = false;

    // Appearance
    public int tileSize = 8;
    public int markerSize = 16;
    public int playerIconWidth = 14;
    public int playerIconHeight = 16;
}
