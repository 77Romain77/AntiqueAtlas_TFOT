package hunternif.mc.impl.atlas;

import com.stereowalker.unionlib.config.ConfigObject;
import com.stereowalker.unionlib.config.ConfigSide;
import com.stereowalker.unionlib.config.UnionConfig;

import hunternif.mc.impl.atlas.client.gui.GuiAtlas;

@UnionConfig(name = "antiqueatlas", autoReload = true)
public class AntiqueAtlasConfig implements ConfigObject {
    //============ Gameplay settings ==============
    @UnionConfig.Entry(group = "Gameplay", name = "doSaveBrowsingPos", 
    		translatable = "text.autoconfig.antiqueatlas.option.doSaveBrowsingPos", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to remember last open browsing position and zoom level for each dimension in every atlas.\nIf disabled, all dimensions and all atlases will be \"synchronized\" at the same coordinates and zoom level, and map will \"follow\" player by default."})
    public boolean doSaveBrowsingPos = true;

    @UnionConfig.Entry(group = "Gameplay", name = "autoDeathMarker", 
    		translatable = "text.autoconfig.antiqueatlas.option.autoDeathMarker", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to add local marker for the spot where the player died."})
	public boolean autoDeathMarker = true;

    @UnionConfig.Entry(group = "Gameplay", name = "autoVillageMarkers", 
    		translatable = "text.autoconfig.antiqueatlas.option.autoVillageMarkers", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to add global markers for NPC villages."})
    public boolean autoVillageMarkers = false;

    @UnionConfig.Entry(group = "Gameplay", name = "autoNetherPortalMarkers", 
    		translatable = "text.autoconfig.antiqueatlas.option.autoNetherPortalMarkers", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to add global markers for Nether Portals."})
    public boolean autoNetherPortalMarkers = true;

    @UnionConfig.Entry(group = "Gameplay", name = "itemNeeded", 
    		translatable = "text.autoconfig.antiqueatlas.option.itemNeeded", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Player will need to craft atlas item to use atlas."})
    public boolean itemNeeded = true;

    @UnionConfig.Entry(group = "Gameplay", name = "minimap", 
    		translatable = "text.autoconfig.antiqueatlas.option.minimap", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Show a minimap if held in the mainhand"})
    public boolean minimap = false;

    //============ Interface settings =============
    @UnionConfig.Entry(group = "User Interface", name = "doScaleMarkers", 
    		translatable = "text.autoconfig.antiqueatlas.option.doScaleMarkers", side = ConfigSide.Shared)
    public boolean doScaleMarkers = false;

    @UnionConfig.Entry(group = "User Interface", name = "defaultScale", 
    		translatable = "text.autoconfig.antiqueatlas.option.defaultScale", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Default zoom level. The number corresponds to the size of a block on the map relative to the size of a GUI pixel. Preferrably a power of 2."})
    @UnionConfig.Range(min = 0.001953125, max = 16.0)
    public double defaultScale = 0.5f;

    @UnionConfig.Entry(group = "userInterface", name = "minScale", 
    		translatable = "text.autoconfig.antiqueatlas.option.minScale", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Minimum zoom level. The number corresponds to the size of a block on the map relative to the size of a GUI pixel. Preferrably a power of 2. Smaller values may decrease performance!"})
    @UnionConfig.Range(min = 0.0009765625, max = 1024.0)
    public double minScale = 1.0 / 32.0;

    @UnionConfig.Entry(group = "userInterface", name = "maxScale", 
    		translatable = "text.autoconfig.antiqueatlas.option.maxScale", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Maximum zoom level. The number corresponds to the size of a block on the map relative to the size of a GUI pixel. Preferrably a power of 2."})
    @UnionConfig.Range(min = 0.0009765625, max = 1024.0)
    public double maxScale = 4f;

    @UnionConfig.Entry(group = "User Interface", name = "doReverseWheelZoom", 
    		translatable = "text.autoconfig.antiqueatlas.option.doReverseWheelZoom", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"If false (by default), then mousewheel up is zoom in, mousewheel down is zoom out.\nIf true, then the direction is reversed."})
    public boolean doReverseWheelZoom = false;

    //=========== Performance settings ============
    @UnionConfig.Entry(group = "performance", name = "scanRadius", 
    		translatable = "text.autoconfig.antiqueatlas.option.scanRadius", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The radius of the area around the player which is scanned by the Atlas at regular intervals.\nNote that this will not force faraway chunks to load, unless force_chunk_loading is enabled.\nLower value gives better performance."})
    public int scanRadius = 11;

    @UnionConfig.Entry(group = "performance", name = "forceChunkLoading", 
    		translatable = "text.autoconfig.antiqueatlas.option.forceChunkLoading", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Force loading of chunks within scan radius even if it exceeds regular chunk loading distance.\nEnabling this may SEVERELY decrease performance!"})
    public boolean forceChunkLoading = false;

    @UnionConfig.Entry(group = "performance", name = "newScanInterval", 
    		translatable = "text.autoconfig.antiqueatlas.option.newScanInterval", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Time in seconds between two scans of the area.\nHigher value gives better performance."})
    public float newScanInterval = 1f;

    @UnionConfig.Entry(group = "performance", name = "doRescan", 
    		translatable = "text.autoconfig.antiqueatlas.option.doRescan", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to rescan chunks in the area that have been previously mapped. This is useful in case of changes in coastline (including small ponds of water and lava), or if land disappears completely (for sky worlds).\nDisable for better performance."})
    public boolean doRescan = true;

    @UnionConfig.Entry(group = "performance", name = "rescanRate", 
    		translatable = "text.autoconfig.antiqueatlas.option.rescanRate", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The number of area scans between full rescans.\nHigher value gives better performance."})
    //@Setting.Constrain.Range(min = 1, max = 1000)
    public int rescanRate = 4;

    @UnionConfig.Entry(group = "performance", name = "markerLimit", 
    		translatable = "text.autoconfig.antiqueatlas.option.maxScale", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The maximum number of markers a particular atlas can hold."})
    //@Setting.Constrain.Range(min = 0, max = 2147483647)
    public int markerLimit = 1024;

    @UnionConfig.Entry(group = "performance", name = "doScanPonds", 
    		translatable = "text.autoconfig.antiqueatlas.option.doScanPonds", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to perform additional scanning to locate small ponds of water or lava.\nDisable for better performance."})
    public boolean doScanPonds = true;

    @UnionConfig.Entry(group = "performance", name = "doScanRavines", 
    		translatable = "text.autoconfig.antiqueatlas.option.doScanRavines", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to perform additional scanning to locate ravines.\nDisable for better performance."})
    public boolean doScanRavines = true;

    @UnionConfig.Entry(group = "performance", name = "debugRender", 
    		translatable = "text.autoconfig.antiqueatlas.option.debugRender", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"If true, map render time will be output."})
    public boolean debugRender = false;

    @UnionConfig.Entry(group = "performance", name = "resourcePackLogging", 
    		translatable = "text.autoconfig.antiqueatlas.option.resourcePackLogging", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"If true, all resource pack loading information will be logged during start and reload."})
    public boolean resourcePackLogging = true;

    @UnionConfig.Entry(group = "appearance", name = "tileSize", 
    		translatable = "text.autoconfig.antiqueatlas.option.tileSize", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The size (in GUI pixels) of a map's tile.\nNote that this will change with Minecraft's GUI scale configuration.\nWhen using a small gui scale, the map may look better with a TILE_SIZE of 16 or more."})
    @UnionConfig.Range(min = 1, max = 10, useSlider = true)
    public int tileSize = 8;

    @UnionConfig.Entry(group = "appearance", name = "markerSize", 
    		translatable = "text.autoconfig.antiqueatlas.option.markerSize", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The size (in GUI pixels) of a marker on the map.\nNote that this will change with Minecraft's GUI scale configuration."})
    //@Setting.Constrain.Range(min = 0)
    public int markerSize = GuiAtlas.MARKER_SIZE / 2;

    @UnionConfig.Entry(group = "appearance", name = "playerIconWidth", 
    		translatable = "text.autoconfig.antiqueatlas.option.playerIconWidth", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The width (in GUI pixels) of the player's icon."})
    //@Setting.Constrain.Range(min = 0)
    public int playerIconWidth = 14;

    @UnionConfig.Entry(group = "appearance", name = "playerIconHeight", 
    		translatable = "text.autoconfig.antiqueatlas.option.playerIconHeight", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The height (in GUI pixels) of the player's icon."})
    //@Setting.Constrain.Range(min = 0)
    public int playerIconHeight = 16;
}
