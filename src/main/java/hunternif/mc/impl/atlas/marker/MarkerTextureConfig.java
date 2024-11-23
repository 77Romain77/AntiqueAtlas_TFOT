package hunternif.mc.impl.atlas.marker;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stereowalker.unionlib.resource.ReloadListener;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.registry.MarkerType;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Maps marker type to texture.
 *
 * @author Hunternif
 */
public class MarkerTextureConfig implements ResourceReloadListener<Map<ResourceLocation, MarkerType>>, ReloadListener {
    public static final ResourceLocation ID = AntiqueAtlas.id("markers");
    private static final int VERSION = 1;
    private static final JsonParser parser = new JsonParser();

    @Override
    public CompletableFuture<Map<ResourceLocation, MarkerType>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, MarkerType> typeMap = new HashMap<>();

            for (Entry<ResourceLocation, Resource> id : manager.listResources("atlas/markers", (s) -> s.toString().endsWith(".json")).entrySet()) {
                ResourceLocation markerId = VersionHelper.toLoc(
                        id.getKey().getNamespace(),
                        id.getKey().getPath().replace("atlas/markers/", "").replace(".json", "")
                );

                try {
                    Resource resource = id.getValue();
                    try (
                            InputStream stream = resource.open();
                            InputStreamReader reader = new InputStreamReader(stream)
                    ) {
                        JsonObject object = parser.parse(reader).getAsJsonObject();

                        int version = object.getAsJsonPrimitive("version").getAsInt();

                        if (version != VERSION) {
                            throw new RuntimeException("Incompatible version (" + VERSION + " != " + version + ")");
                        }

                        MarkerType markerType = new MarkerType(markerId);
                        markerType.getJSONData().readFrom(object);
                        markerType.setIsFromJson(true);
                        typeMap.put(markerId, markerType);
                    }
                } catch (Exception e) {
                    AntiqueAtlas.LOG.warn("Error reading marker " + markerId + "!", e);
                }
            }

            return typeMap;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, MarkerType> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (Map.Entry<ResourceLocation, MarkerType> entry : data.entrySet()) {
                MarkerType.register(entry.getKey(), entry.getValue());
            }
        }, executor);
    }

    @Override
    public String getName() {
        return ID.toString();
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }
}
