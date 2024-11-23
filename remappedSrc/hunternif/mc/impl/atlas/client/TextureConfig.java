package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.client.texture.ITexture;
import hunternif.mc.impl.atlas.client.texture.TileTexture;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import hunternif.mc.impl.atlas.util.Log;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reads all png files available under assets/(?modid)/textures/gui/tiles/(?tex).png as Textures that
 * are referenced by the TextureSets.
 * <p>
 * Note that each texture is represented by TWO Identifiers:
 * - The identifier of the physical location in modid:texture/gui/tiles/tex.png
 * - The logical identifier modid:tex referenced by TextureSets
 */
@Environment(EnvType.CLIENT)
public class TextureConfig implements ResourceReloadListener<Map<ResourceLocation, ITexture>> {
    public static final ResourceLocation ID = AntiqueAtlasMod.id("textures");
    private final Map<ResourceLocation, ITexture> texture_map;

    public TextureConfig(Map<ResourceLocation, ITexture> texture_map) {
        this.texture_map = texture_map;
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, ITexture>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ITexture> textures = new HashMap<>();

            for (ResourceLocation id : manager.listResources("textures/gui/tiles", (s) -> s.endsWith(".png"))) {
                // id now contains the physical file path of the texture
                try {

                    // texture_id is the logical identifier, as it will be referenced by TextureSets
                    ResourceLocation texture_id = new ResourceLocation(
                            id.getNamespace(),
                            id.getPath().replace("textures/gui/tiles/", "").replace(".png", "")
                    );

                    textures.put(texture_id, new TileTexture(id));
                } catch (ResourceLocationException e) {
                    AntiqueAtlasMod.LOG.warn("Failed to read texture!", e);
                }
            }

            return textures;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, ITexture> textures, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            texture_map.clear();
            for (Map.Entry<ResourceLocation, ITexture> entry : textures.entrySet()) {
                texture_map.put(entry.getKey(), entry.getValue());
                if (AntiqueAtlasMod.CONFIG.resourcePackLogging)
                    Log.info("Loaded texture %s with path %s", entry.getKey(), entry.getValue().getTexture());
            }
        }, executor);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }
}
