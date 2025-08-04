package hunternif.mc.impl.atlas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.stereowalker.unionlib.api.collectors.ConfigCollector;
import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.PacketCollector;
import com.stereowalker.unionlib.api.collectors.ReloadListeners;
import com.stereowalker.unionlib.api.creativetabs.CreativeTabPopulator;
import com.stereowalker.unionlib.api.registries.RegistryCollector;
import com.stereowalker.unionlib.insert.Inserts;
import com.stereowalker.unionlib.mod.MinecraftMod;
import com.stereowalker.unionlib.mod.PacketHolder;
import com.stereowalker.unionlib.mod.ServerSegment;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.core.AtlasIdData;
import hunternif.mc.impl.atlas.core.GlobalTileDataHandler;
import hunternif.mc.impl.atlas.core.PlayerEventHandler;
import hunternif.mc.impl.atlas.core.TileDataHandler;
import hunternif.mc.impl.atlas.core.scaning.TileDetectorBase;
import hunternif.mc.impl.atlas.core.scaning.WorldScanner;
//import hunternif.mc.impl.atlas.event.RecipeCraftedHandler;
import hunternif.mc.impl.atlas.item.AntiqueAtlasItems;
import hunternif.mc.impl.atlas.marker.GlobalMarkersDataHandler;
import hunternif.mc.impl.atlas.marker.MarkersDataHandler;
import hunternif.mc.impl.atlas.network.packet.c2s.play.DeleteMarkerC2SPacket;
import hunternif.mc.impl.atlas.network.packet.c2s.play.PutBrowsingPositionC2SPacket;
import hunternif.mc.impl.atlas.network.packet.c2s.play.PutMarkerC2SPacket;
import hunternif.mc.impl.atlas.network.packet.c2s.play.PutTileC2SPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteGlobalTileS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteMarkerS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DimensionUpdateS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.MapDataS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.PutGlobalTileS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.PutMarkersS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.PutTileS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.TileGroupsS2CPacket;
import hunternif.mc.impl.atlas.structure.EndCity;
import hunternif.mc.impl.atlas.structure.JigsawConfig;
import hunternif.mc.impl.atlas.structure.NetherFortress;
import hunternif.mc.impl.atlas.structure.Overworld;
import hunternif.mc.impl.atlas.structure.StructureHandler;
import hunternif.mc.impl.atlas.structure.Village;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.common.Mod;

@Mod(value = AntiqueAtlas.ID)
public class AntiqueAtlas extends MinecraftMod implements PacketHolder {
	public static AntiqueAtlas instance;
	
    public AntiqueAtlas() {
    	super("antiqueatlas", () -> new AntiqueAtlasClientSegment(), () -> new ServerSegment());
    	instance = this;
	}

	public static final String ID = "antiqueatlas";
    public static final String NAME = "Antique Atlas";

    public static Logger LOG = LogManager.getLogger(NAME);

    public static final WorldScanner worldScanner = new WorldScanner();
    public static final TileDataHandler tileData = new TileDataHandler();
    public static final MarkersDataHandler markersData = new MarkersDataHandler();

    public static final GlobalTileDataHandler globalTileData = new GlobalTileDataHandler();
    public static final GlobalMarkersDataHandler globalMarkersData = new GlobalMarkersDataHandler();

    public static final AntiqueAtlasConfig CONFIG = new AntiqueAtlasConfig();

    public static ResourceLocation id(String... path) {
        return path[0].contains(":") ? VersionHelper.toLoc(String.join(".", path)) : VersionHelper.toLoc(ID, String.join(".", path));
    }

    public static AtlasIdData getAtlasIdData(Level world) {
        if (world.isClientSide()) {
            LOG.warn("Tried to access server only data from client.");
            return null;
        }

        return ((ServerLevel) world).getDataStorage().computeIfAbsent(AtlasIdData::fromNbt, AtlasIdData::new, "antiqueatlas_global_atlas_data");
    }
    
    @Override
    public void onModStartup() {

        AntiqueAtlasItems.register();

        //NewServerConnectionCallback.EVENT.register(tileData::onClientConnectedToServer);
        //NewServerConnectionCallback.EVENT.register(markersData::onClientConnectedToServer);
        //NewServerConnectionCallback.EVENT.register(globalMarkersData::onClientConnectedToServer);

//        NewPlayerConnectionCallback.EVENT.register(globalMarkersData::onPlayerLogin);
//        NewPlayerConnectionCallback.EVENT.register(globalTileData::onPlayerLogin);
//        NewPlayerConnectionCallback.EVENT.register(PlayerEventHandler::onPlayerLogin);

//        LifecycleEvent.SERVER_LEVEL_LOAD.register(globalMarkersData::onWorldLoad);
//        LifecycleEvent.SERVER_LEVEL_LOAD.register(globalTileData::onWorldLoad);

        //RecipeCraftedCallback.EVENT.register(new RecipeCraftedHandler());

        //StructurePieceAddedCallback.EVENT.register(StructureHandler::resolve);
        //StructureAddedCallback.EVENT.register(StructureHandler::resolve);

        NetherFortress.registerPieces();
        EndCity.registerMarkers();
        Village.registerMarkers();
        Overworld.registerPieces();
    }
    
    @Override
    public void registerInserts(InsertCollector collector) {
    	collector.addInsert(Inserts.LEVEL_LOAD, (a) -> {
    		if (a instanceof ServerLevel server) {
    			globalMarkersData.onWorldLoad(server);
    			globalTileData.onWorldLoad(server);
    			TileDetectorBase.scanBiomeTypes(server);
    		}
    	});
    	collector.addInsert(Inserts.LOGGED_IN, (player) -> {
    		globalMarkersData.onPlayerLogin((ServerPlayer) player);
    		globalTileData.onPlayerLogin((ServerPlayer) player);
    		PlayerEventHandler.onPlayerLogin((ServerPlayer) player);
    	});
    	collector.addInsert(Inserts.STRUCTURE_ADDED, StructureHandler::resolve);
    	collector.addInsert(Inserts.STRUCTURE_PIECE_ADDED, StructureHandler::resolve);
    	
    	collector.addInsert(Inserts.LIVING_TICK, (living) -> {
    		if (living instanceof Player player) PlayerEventHandler.onPlayerTick(player);
    	});
    }
    
    @Override
    public void registerClientRelaodableResources(ReloadListeners reloadListener) {
        ClientProxy clientProxy = new ClientProxy();
        clientProxy.initClient(reloadListener);
    }
    
    @Override
    public void registerServerRelaodableResources(ReloadListeners reloadListener) {
        JigsawConfig jigsawConfig = new JigsawConfig();
        reloadListener.listenTo(jigsawConfig);
    }
    
    @Override
    public void populateCreativeTabs(CreativeTabPopulator populator) {
    	if (populator.isToolTab()) populator.addItems(AntiqueAtlasItems.Items.EMPTY_ATLAS);
    }
    
    @Override
    public void setupConfigs(ConfigCollector collector) {
    	collector.registerConfig(CONFIG);
    }
    
    @Override
    public void setupRegistries(RegistryCollector collector) {
    	if (AntiqueAtlas.CONFIG.itemNeeded) {    		
    		collector.addRegistryHolder(Registries.ITEM, AntiqueAtlasItems.Items.class);
    		collector.addRegistryHolder(Registries.RECIPE_SERIALIZER, AntiqueAtlasItems.Recipes.class);
//    		collector.addRegistryHolder(Registries.DATA_COMPONENT_TYPE, AntiqueAtlasItems.Components.class);
    	}
    }

	@Override
	public void registerPackets(PacketCollector collector) {
		collector.registerClientboundPacket(PutGlobalTileS2CPacket.ID, PutGlobalTileS2CPacket.class, PutGlobalTileS2CPacket::new);
		collector.registerClientboundPacket(DeleteGlobalTileS2CPacket.ID, DeleteGlobalTileS2CPacket.class, DeleteGlobalTileS2CPacket::new);
		collector.registerClientboundPacket(DeleteMarkerS2CPacket.ID, DeleteMarkerS2CPacket.class, DeleteMarkerS2CPacket::new);
		collector.registerClientboundPacket(DimensionUpdateS2CPacket.ID, DimensionUpdateS2CPacket.class, DimensionUpdateS2CPacket::new);
		collector.registerClientboundPacket(MapDataS2CPacket.ID, MapDataS2CPacket.class, MapDataS2CPacket::new);
		collector.registerClientboundPacket(PutMarkersS2CPacket.ID, PutMarkersS2CPacket.class, PutMarkersS2CPacket::new);
		collector.registerClientboundPacket(PutTileS2CPacket.ID, PutTileS2CPacket.class, PutTileS2CPacket::new);
		collector.registerClientboundPacket(TileGroupsS2CPacket.ID, TileGroupsS2CPacket.class, TileGroupsS2CPacket::new);
		
		collector.registerServerboundPacket(PutMarkerC2SPacket.ID, PutMarkerC2SPacket.class, PutMarkerC2SPacket::new);
		collector.registerServerboundPacket(PutBrowsingPositionC2SPacket.ID, PutBrowsingPositionC2SPacket.class, PutBrowsingPositionC2SPacket::new);
		collector.registerServerboundPacket(DeleteMarkerC2SPacket.ID, DeleteMarkerC2SPacket.class, DeleteMarkerC2SPacket::new);
		collector.registerServerboundPacket(PutTileC2SPacket.ID, PutTileC2SPacket.class, PutTileC2SPacket::new);
	}
}
