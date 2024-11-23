package hunternif.mc.impl.atlas.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stereowalker.unionlib.resource.ReloadListener;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Saves texture set names with the lists of texture variations.
 */
public class TextureSetConfig implements ResourceReloadListener<Collection<TextureSet>>, ReloadListener {
    public static final ResourceLocation ID = AntiqueAtlas.id("texture_sets");
    private static final int VERSION = 1;
    private static final JsonParser PARSER = new JsonParser();
    private final TextureSetMap textureSetMap;

    public TextureSetConfig(TextureSetMap textureSetMap) {
        this.textureSetMap = textureSetMap;
    }

    @Override
    public CompletableFuture<Collection<TextureSet>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, TextureSet> sets = new HashMap<>();

            try {
                for (Entry<ResourceLocation, Resource> id : manager.listResources("atlas/texture_sets", (s) -> s.toString().endsWith(".json")).entrySet()) {
                    ResourceLocation texture_id = VersionHelper.toLoc(
                            id.getKey().getNamespace(),
                            id.getKey().getPath().replace("atlas/texture_sets/", "").replace(".json", "")
                    );

                    try {
                        Resource resource = id.getValue();
                        try (
                                InputStream stream = resource.open();
                                InputStreamReader reader = new InputStreamReader(stream)
                        ) {
                            JsonObject object = PARSER.parse(reader).getAsJsonObject();

                            int version = object.getAsJsonPrimitive("version").getAsInt();
                            if (version != VERSION) {
                                AntiqueAtlas.LOG.warn("The TextureSet " + texture_id + " is in the wrong version! Skipping.");
                                continue;
                            }

                            JsonObject data = object.getAsJsonObject("data");

                            List<ResourceLocation> textures = new ArrayList<>();

                            for (Entry<String, JsonElement> entry : data.getAsJsonObject("textures").entrySet()) {
                                for (int i = 0; i < entry.getValue().getAsInt(); i++) {
                                    textures.add(VersionHelper.toLoc(entry.getKey()));
                                }
                            }

                            ResourceLocation[] textureArray = new ResourceLocation[textures.size()];
                            TextureSet set;

                            if (!data.has("shore")) {
                                set = new TextureSet(texture_id, textures.toArray(textureArray));
                            } else {
                                JsonObject shore = data.getAsJsonObject("shore");

                                if (!shore.has("water")) {
                                    throw new RuntimeException("The `shore` entry is missing a water entry.");
                                }

                                set = new TextureSet.TextureSetShore(texture_id, VersionHelper.toLoc(shore.get("water").getAsString()), textures.toArray(textureArray));
                            }

                            if (data.has("stitch")) {
                                data.getAsJsonObject("stitch").entrySet().forEach(entry -> {
                                    String to = entry.getValue().getAsString();

                                    switch (to) {
                                        case "both":
                                            set.stitchTo(VersionHelper.toLoc(entry.getKey()));
                                            break;
                                        case "horizontal":
                                            set.stitchToHorizontal(VersionHelper.toLoc(entry.getKey()));
                                            break;
                                        case "vertical":
                                            set.stitchToVertical(VersionHelper.toLoc(entry.getKey()));
                                            break;
                                        default:
                                            throw new RuntimeException("Invalid stitch value (" + to + ") for `" + entry.getKey() + "`");
                                    }
                                });
                            }


                            sets.put(texture_id, set);
                        }
                    } catch (Exception e) {
                        AntiqueAtlas.LOG.warn("Error reading TextureSet " + texture_id + "!", e);
                    }
                }
            } catch (Throwable e) {
                Log.warn(e, "Failed to read texture sets!");
            }

            return sets.values();
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Collection<TextureSet> sets, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (TextureSet set : sets) {
                try {
                    set.loadTextures();
                    textureSetMap.register(set);
                    if (AntiqueAtlas.CONFIG.resourcePackLogging)
                        Log.info("Loaded texture set %s with %d custom texture(s)", set.name, set.getTexturePaths().length);
                } catch (Throwable e) {
                    Log.error(e, "Failed to load the texture set `%s`:", set.name);
                }

            }

            for (TextureSet set : sets) {
                set.checkStitching();

                if (set instanceof TextureSet.TextureSetShore) {
                    TextureSet.TextureSetShore texture = (TextureSet.TextureSetShore) set;
                    texture.loadWater();
                    if (AntiqueAtlas.CONFIG.resourcePackLogging)
                        Log.info("Loaded water texture `%s` for shore texture `%s` texture", texture.waterName, texture.name);
                }
            }
        }, executor);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singleton(TextureConfig.ID);
    }
}
