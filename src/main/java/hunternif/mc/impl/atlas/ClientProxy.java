package hunternif.mc.impl.atlas;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.stereowalker.unionlib.api.collectors.ReloadListeners;
import com.stereowalker.unionlib.resource.ReloadListener;

import hunternif.mc.impl.atlas.client.TextureConfig;
import hunternif.mc.impl.atlas.client.TextureSetConfig;
import hunternif.mc.impl.atlas.client.TextureSetMap;
import hunternif.mc.impl.atlas.client.Textures;
import hunternif.mc.impl.atlas.client.TileTextureConfig;
import hunternif.mc.impl.atlas.client.TileTextureMap;
import hunternif.mc.impl.atlas.marker.MarkerTextureConfig;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;

public class ClientProxy implements PreparableReloadListener, ReloadListener {
    public void initClient(ReloadListeners reloadListener) {
        // read Textures first from assets
        TextureConfig textureConfig = new TextureConfig(Textures.TILE_TEXTURES_MAP);
        reloadListener.listenTo(textureConfig);

        // then read TextureSets
        TextureSetMap textureSetMap = TextureSetMap.instance();
        TextureSetConfig textureSetConfig = new TextureSetConfig(textureSetMap);
        reloadListener.listenTo(textureSetConfig);
//        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, textureSetConfig, textureSetConfig.getId(), textureSetConfig.getDependencies());

        // After that, we can read the tile mappings
        TileTextureMap tileTextureMap = TileTextureMap.instance();
        TileTextureConfig tileTextureConfig = new TileTextureConfig(tileTextureMap, textureSetMap);
        reloadListener.listenTo(tileTextureConfig);
//        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, tileTextureConfig, tileTextureConfig.getId(), tileTextureConfig.getDependencies());

        // Legacy file name:
        reloadListener.listenTo(this);

        MarkerTextureConfig markerTextureConfig = new MarkerTextureConfig();
        reloadListener.listenTo(markerTextureConfig);

        for (MarkerType type : MarkerType.REGISTRY) {
            type.initMips();
        }
    }

    /**
     * Assign default textures to biomes defined in the client world, but
     * not part of the BuiltinRegistries.BIOME. This happens for all biomes
     * defined in data packs. Also, as these are only available per world,
     * we need the ClientWorld loaded here.
     */
    public static void assignCustomBiomeTextures(ClientLevel world) {
//        for (Map.Entry<ResourceKey<Biome>, Biome> biome : BuiltinRegistries.BIOME.entrySet()) {
//            ResourceLocation id = BuiltinRegistries.BIOME.getKey(biome.getValue());
//            if (!TileTextureMap.instance().isRegistered(id)) {
//                TileTextureMap.instance().autoRegister(id, biome.getKey());
//            }
//        }

        for (Map.Entry<ResourceKey<Biome>, Biome> entry : world.registryAccess().registryOrThrow(Registries.BIOME).entrySet()) {
            ResourceLocation id = world.registryAccess().registryOrThrow(Registries.BIOME).getKey(entry.getValue());
            if (!TileTextureMap.instance().isRegistered(id)) {
                TileTextureMap.instance().autoRegister(id, entry.getKey());
            }
        }
    }

    @Override
    public String getName() {
        return AntiqueAtlas.id("proxy").toString();
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.completedFuture(null).thenCompose(synchronizer::wait).thenCompose(t -> CompletableFuture.runAsync(() -> {
            for (MarkerType type : MarkerType.REGISTRY) {
                type.initMips();
            }
        }, applyExecutor));
    }

	@Override
	public ResourceLocation id() {
		return AntiqueAtlas.id("proxy");
	}
}
