package hunternif.mc.impl.atlas.structure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stereowalker.unionlib.resource.ReloadListener;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class JigsawConfig implements ResourceReloadListener<Map<ResourceLocation, StructurePieceTile>>, ReloadListener {
    private static final ResourceLocation ID = AntiqueAtlas.id("structures");

    public static final Map<ResourceLocation, StructurePieceTile> PIECES = new ConcurrentHashMap<>();

    private static JsonObject readResource(Resource resource) throws IOException {
        try (InputStream stream = resource.open(); InputStreamReader reader = new InputStreamReader(stream)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static StructurePieceTile parseJson(JsonObject json) {
        int version = json.getAsJsonPrimitive("version").getAsInt();

        if (version == 1) {
            return new StructurePieceTile(
                    ResourceLocation.tryParse(json.get("tile").getAsString()),
                    json.get("priority").getAsInt()
            );
        } else if (version == 2) {
            return new StructurePieceTileXZ(
                    ResourceLocation.tryParse(json.get("tile_x").getAsString()),
                    ResourceLocation.tryParse(json.get("tile_z").getAsString()),
                    json.get("priority").getAsInt()
            );
        } else {
            throw new RuntimeException("Unsupported JSON version: " + version + ". Only version 1 is supported.");
        }
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, StructurePieceTile>> load(ResourceManager manager, ProfilerFiller
            profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, StructurePieceTile> pieces = new HashMap<>();


            try {
                for (Entry<ResourceLocation, Resource> id : manager.listResources("atlas/structures", (s) -> s.toString().endsWith(".json")).entrySet()) {
                    // id now contains the physical file path of the structure piece
                    AntiqueAtlas.LOG.info("Found structure piece config: " + id.getKey());

                    try {
                        // strip parts to get a better id
                        ResourceLocation piece_id = VersionHelper.toLoc(
                                id.getKey().getNamespace(),
                                id.getKey().getPath().replace("atlas/structures/", "").replace(".json", "")
                        );

                        JsonObject json = readResource(id.getValue());
                        pieces.put(piece_id, parseJson(json));
                    } catch (Exception e) {
                        AntiqueAtlas.LOG.warn("Error reading structure piece config from " + id, e);
                    }
                }

            } catch (Throwable e) {
                AntiqueAtlas.LOG.warn("Failed to read structure piece mapping from data pack!", e);
            }

            return pieces;

        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, StructurePieceTile> pieces, ResourceManager
            manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            pieces.forEach((id, piece) -> {

                AntiqueAtlas.LOG.info("Apply structure piece config: " + id);
                if (piece instanceof StructurePieceTileXZ) {
                    StructureHandler.registerJigsawTile(id, piece.getPriority(), piece.getTileX(), StructureHandler::IF_X_DIRECTION);
                    StructureHandler.registerJigsawTile(id, piece.getPriority(), piece.getTileZ(), StructureHandler::IF_Z_DIRECTION);
                } else {
                    StructureHandler.registerJigsawTile(id, piece.getPriority(), piece.getTile());
                }
            });
        }, executor);
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
