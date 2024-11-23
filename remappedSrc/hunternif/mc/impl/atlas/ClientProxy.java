package hunternif.mc.impl.atlas;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import hunternif.mc.impl.atlas.client.*;
import hunternif.mc.impl.atlas.marker.MarkerTextureConfig;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Environment(EnvType.CLIENT)
public class ClientProxy implements PreparableReloadListener {
    public void initClient() {
        // read Textures first from assets
        TextureConfig textureConfig = new TextureConfig(Textures.TILE_TEXTURES_MAP);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, textureConfig, textureConfig.getId(), textureConfig.getDependencies());

        // then read TextureSets
        TextureSetMap textureSetMap = TextureSetMap.instance();
        TextureSetConfig textureSetConfig = new TextureSetConfig(textureSetMap);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, textureSetConfig, textureSetConfig.getId(), textureSetConfig.getDependencies());

        // After that, we can read the tile mappings
        TileTextureMap tileTextureMap = TileTextureMap.instance();
        TileTextureConfig tileTextureConfig = new TileTextureConfig(tileTextureMap, textureSetMap);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, tileTextureConfig, tileTextureConfig.getId(), tileTextureConfig.getDependencies());

        // Legacy file name:
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, this);

        MarkerTextureConfig markerTextureConfig = new MarkerTextureConfig();
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, markerTextureConfig, markerTextureConfig.getId(), markerTextureConfig.getDependencies());

        for (MarkerType type : MarkerType.REGISTRY) {
            type.initMips();
        }

        if (!AntiqueAtlasMod.CONFIG.itemNeeded) {
            KeyHandler.registerBindings();
            ClientTickEvent.CLIENT_POST.register(KeyHandler::onClientTick);
        }

    }

    /**
     * Assign default textures to biomes defined in the client world, but
     * not part of the BuiltinRegistries.BIOME. This happens for all biomes
     * defined in data packs. Also, as these are only available per world,
     * we need the ClientWorld loaded here.
     */
    public static void assignCustomBiomeTextures(ClientLevel world) {
        for (Map.Entry<ResourceKey<Biome>, Biome> biome : BuiltinRegistries.BIOME.entrySet()) {
            ResourceLocation id = BuiltinRegistries.BIOME.getKey(biome.getValue());
            if (!TileTextureMap.instance().isRegistered(id)) {
                TileTextureMap.instance().autoRegister(id, biome.getKey());
            }
        }

        for (Map.Entry<ResourceKey<Biome>, Biome> entry : world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).entrySet()) {
            ResourceLocation id = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(entry.getValue());
            if (!TileTextureMap.instance().isRegistered(id)) {
                TileTextureMap.instance().autoRegister(id, entry.getKey());
            }
        }
    }

    @Override
    public String getName() {
        return AntiqueAtlasMod.id("proxy").toString();
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.completedFuture(null).thenCompose(synchronizer::wait).thenCompose(t -> CompletableFuture.runAsync(() -> {
            for (MarkerType type : MarkerType.REGISTRY) {
                type.initMips();
            }
        }, applyExecutor));
    }
}
