package hunternif.mc.impl.atlas.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import hunternif.mc.impl.atlas.util.Log;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Client-only config mapping biome IDs to texture sets.
 * <p>Must be loaded after {@link TextureSetConfig}!</p>
 *
 * @author Hunternif
 */
@Environment(EnvType.CLIENT)
public class TileTextureConfig implements ResourceReloadListener<Map<ResourceLocation, ResourceLocation>> {
    public static final ResourceLocation ID = AntiqueAtlasMod.id("tile_textures");
    private final TileTextureMap tileTextureMap;
    private final TextureSetMap textureSetMap;

    public TileTextureConfig(TileTextureMap biomeTextureMap, TextureSetMap textureSetMap) {
        this.tileTextureMap = biomeTextureMap;
        this.textureSetMap = textureSetMap;
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, ResourceLocation>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ResourceLocation> map = new HashMap<>();

            try {
                for (ResourceLocation id : manager.listResources("atlas/tiles", (s) -> s.endsWith(".json"))) {
                    ResourceLocation tile_id = new ResourceLocation(id.getNamespace(), id.getPath().replace("atlas/tiles/", "").replace(".json", ""));

                    try {
                        Resource resource = manager.getResource(id);
                        try (InputStream stream = resource.getInputStream(); InputStreamReader reader = new InputStreamReader(stream)) {
                            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();

                            int version = object.getAsJsonPrimitive("version").getAsInt();
                            if (version == 1) {
                                ResourceLocation texture_set = new ResourceLocation(object.get("texture_set").getAsString());

                                map.put(tile_id, texture_set);

                                for (TileHeightType layer : TileHeightType.values()) {
                                    map.put(ResourceLocation.tryParse(tile_id + "_" + layer.getName()), texture_set);
                                }
                            } else if (version == 2) {
                                ResourceLocation default_entry = TileTextureMap.DEFAULT_TEXTURE;

                                try {
                                    default_entry = new ResourceLocation(object.getAsJsonObject("texture_sets").get("default").getAsString());
                                } catch (Exception ignored) {
                                }

                                // insert the old-style texture set with the default one
                                map.put(tile_id, default_entry);

                                for (TileHeightType layer : TileHeightType.values()) {
                                    ResourceLocation texture_set = default_entry;

                                    try {
                                        texture_set = new ResourceLocation(object.getAsJsonObject("texture_sets").get(layer.getName()).getAsString());
                                    } catch (Exception ignored) {
                                    }

                                    map.put(ResourceLocation.tryParse(tile_id + "_" + layer), texture_set);
                                }
                            } else {
                                AntiqueAtlasMod.LOG.warn("The tile " + tile_id + " is in the wrong version! Skipping.");
                            }
                        }
                    } catch (Exception e) {
                        AntiqueAtlasMod.LOG.warn("Error reading tile mapping " + tile_id + "!", e);
                    }
                }
            } catch (Throwable e) {
                Log.warn(e, "Failed to read tile mappings!");
            }

            return map;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, ResourceLocation> tileMap, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : tileMap.entrySet()) {
                ResourceLocation tile_id = entry.getKey();
                ResourceLocation texture_set = entry.getValue();
                TextureSet set = textureSetMap.getByName(entry.getValue());

                if (set == null) {
                    AntiqueAtlasMod.LOG.error("Missing texture set `{}` for tile `{}`. Using default.", texture_set, tile_id);

                    set = tileTextureMap.getDefaultTexture();
                }

                tileTextureMap.setTexture(entry.getKey(), set);
                if (AntiqueAtlasMod.CONFIG.resourcePackLogging)
                    Log.info("Loaded tile %s with texture set %s", tile_id, set.name);
            }
        }, executor);
    }
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singleton(TextureSetConfig.ID);
    }
}
