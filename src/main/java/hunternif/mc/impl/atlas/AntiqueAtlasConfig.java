package hunternif.mc.impl.atlas;

import com.stereowalker.unionlib.config.ConfigObject;
import com.stereowalker.unionlib.config.ConfigSide;
import com.stereowalker.unionlib.config.UnionConfig;


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
    @UnionConfig.Comment(comment = {"Legacy option kept for config compatibility. The client-only build does not inspect portal structures."})
    public boolean autoNetherPortalMarkers = false;

    @UnionConfig.Entry(group = "Gameplay", name = "itemNeeded", 
    		translatable = "text.autoconfig.antiqueatlas.option.itemNeeded", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Require a vanilla book renamed 'Antique Atlas' to scan and open the map."})
    public boolean itemNeeded = false;

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
    @UnionConfig.Comment(comment = {"Radius, in already-loaded client chunks, recorded around the player. This never loads server chunks."})
    @UnionConfig.Range(min = 0, max = 32)
    public int scanRadius = 11;

    @UnionConfig.Entry(group = "performance", name = "forceChunkLoading", 
    		translatable = "text.autoconfig.antiqueatlas.option.forceChunkLoading", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Legacy option kept for config compatibility. Client-only Atlas never force-loads chunks."})
    public boolean forceChunkLoading = false;

    @UnionConfig.Entry(group = "performance", name = "newScanInterval", 
    		translatable = "text.autoconfig.antiqueatlas.option.newScanInterval", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Legacy option kept for config compatibility. New client chunks are processed through a bounded queue."})
    @UnionConfig.Range(min = 0.05, max = 60.0)
    public float newScanInterval = 1f;

    @UnionConfig.Entry(group = "performance", name = "doRescan", 
    		translatable = "text.autoconfig.antiqueatlas.option.doRescan", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Legacy option kept for config compatibility. Already recorded chunks are not rescanned automatically."})
    public boolean doRescan = false;

    @UnionConfig.Entry(group = "performance", name = "rescanRate", 
    		translatable = "text.autoconfig.antiqueatlas.option.rescanRate", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The number of area scans between full rescans.\nHigher value gives better performance."})
    @UnionConfig.Range(min = 1, max = 1000)
    public int rescanRate = 4;

    @UnionConfig.Entry(group = "performance", name = "clientScanBudget",
            translatable = "text.autoconfig.antiqueatlas.option.clientScanBudget", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Maximum number of newly received chunks analysed per client tick. Lower values reduce client frame spikes."})
    @UnionConfig.Range(min = 1, max = 64)
    public int clientScanBudget = 8;

    @UnionConfig.Entry(group = "performance", name = "markerLimit", 
			translatable = "text.autoconfig.antiqueatlas.option.markerLimit", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The maximum number of markers a particular atlas can hold."})
    //@Setting.Constrain.Range(min = 0, max = 2147483647)
    public int markerLimit = 1024;

    @UnionConfig.Entry(group = "performance", name = "doScanPonds", 
    		translatable = "text.autoconfig.antiqueatlas.option.doScanPonds", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to perform additional scanning to locate small ponds of water or lava.\nDisable for better performance."})
    public boolean doScanPonds = false;

    @UnionConfig.Entry(group = "performance", name = "doScanRavines", 
    		translatable = "text.autoconfig.antiqueatlas.option.doScanRavines", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"Whether to perform additional scanning to locate ravines.\nDisable for better performance."})
    public boolean doScanRavines = false;

    @UnionConfig.Entry(group = "performance", name = "debugRender", 
    		translatable = "text.autoconfig.antiqueatlas.option.debugRender", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"If true, map render time will be output."})
    public boolean debugRender = false;

    @UnionConfig.Entry(group = "performance", name = "resourcePackLogging", 
    		translatable = "text.autoconfig.antiqueatlas.option.resourcePackLogging", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"If true, all resource pack loading information will be logged during start and reload."})
    public boolean resourcePackLogging = false;

    @UnionConfig.Entry(group = "appearance", name = "tileSize", 
    		translatable = "text.autoconfig.antiqueatlas.option.tileSize", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The size (in GUI pixels) of a map's tile.\nNote that this will change with Minecraft's GUI scale configuration.\nWhen using a small gui scale, the map may look better with a TILE_SIZE of 16 or more."})
    @UnionConfig.Range(min = 1, max = 10, useSlider = true)
    public int tileSize = 8;

    @UnionConfig.Entry(group = "appearance", name = "markerSize", 
    		translatable = "text.autoconfig.antiqueatlas.option.markerSize", side = ConfigSide.Shared)
    @UnionConfig.Comment(comment = {"The size (in GUI pixels) of a marker on the map.\nNote that this will change with Minecraft's GUI scale configuration."})
    //@Setting.Constrain.Range(min = 0)
    public int markerSize = 16;

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
