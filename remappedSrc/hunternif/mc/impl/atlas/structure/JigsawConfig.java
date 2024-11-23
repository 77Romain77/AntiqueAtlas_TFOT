package hunternif.mc.impl.atlas.structure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class JigsawConfig implements ResourceReloadListener<Map<ResourceLocation, StructurePieceTile>> {
    private static final ResourceLocation ID = AntiqueAtlasMod.id("structures");

    public static final Map<ResourceLocation, StructurePieceTile> PIECES = new ConcurrentHashMap<>();

    private static JsonObject readResource(ResourceManager manager, ResourceLocation id) throws IOException {
        Resource resource = manager.getResource(id);
        try (InputStream stream = resource.getInputStream(); InputStreamReader reader = new InputStreamReader(stream)) {
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
                for (ResourceLocation id : manager.listResources("atlas/structures", (s) -> s.endsWith(".json"))) {
                    // id now contains the physical file path of the structure piece
                    AntiqueAtlasMod.LOG.info("Found structure piece config: " + id);

                    try {
                        // strip parts to get a better id
                        ResourceLocation piece_id = new ResourceLocation(
                                id.getNamespace(),
                                id.getPath().replace("atlas/structures/", "").replace(".json", "")
                        );

                        JsonObject json = readResource(manager, id);
                        pieces.put(piece_id, parseJson(json));
                    } catch (Exception e) {
                        AntiqueAtlasMod.LOG.warn("Error reading structure piece config from " + id, e);
                    }
                }

            } catch (Throwable e) {
                AntiqueAtlasMod.LOG.warn("Failed to read structure piece mapping from data pack!", e);
            }

            return pieces;

        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, StructurePieceTile> pieces, ResourceManager
            manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            pieces.forEach((id, piece) -> {

                AntiqueAtlasMod.LOG.info("Apply structure piece config: " + id);
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
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }
}
