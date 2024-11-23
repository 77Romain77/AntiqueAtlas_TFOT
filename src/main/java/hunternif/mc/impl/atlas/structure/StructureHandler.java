package hunternif.mc.impl.atlas.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Triple;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.util.MathUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

public class StructureHandler {
    private static final HashMultimap<ResourceLocation, Tuple<ResourceLocation, Setter>> STRUCTURE_PIECE_TO_TILE_MAP = HashMultimap.create();
    private static final Multimap<ResourceLocation, Tuple<ResourceLocation, Setter>> JIGSAW_TO_TILE_MAP = HashMultimap.create();
    private static final Map<ResourceLocation, Tuple<ResourceLocation, Component>> STRUCTURE_PIECE_TO_MARKER_MAP = new HashMap<>();
    private static final Map<ResourceLocation, Integer> STRUCTURE_PIECE_TILE_PRIORITY = new HashMap<>();
    public static final Setter ALWAYS = (world, element, box, rotation) -> Collections.singleton(new ChunkPos(MathUtil.getCenter(box).getX() >> 4, MathUtil.getCenter(box).getZ() >> 4));


    public static Collection<ChunkPos> IF_X_DIRECTION(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceX() == junctions.get(1).getSourceX() || junctions.get(0).getSourceZ() != junctions.get(1).getSourceZ()) {
                    return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
                }
            } else {
                return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
            }
        }
        return Collections.emptyList();
    }

    public static Collection<ChunkPos> IF_Z_DIRECTION(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceZ() == junctions.get(1).getSourceZ() || junctions.get(0).getSourceX() != junctions.get(1).getSourceX()) {
                    return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
                }
            } else {
                return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
            }
        }
        return Collections.emptyList();
    }

    private static final Set<Triple<Integer, Integer, ResourceLocation>> VISITED_STRUCTURES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void registerTile(StructurePieceType structurePieceType, int priority, ResourceLocation textureId, Setter setter) {
        ResourceLocation id = BuiltInRegistries.STRUCTURE_PIECE.getKey(structurePieceType);
        STRUCTURE_PIECE_TO_TILE_MAP.put(id, new Tuple<>(textureId, setter));
        STRUCTURE_PIECE_TILE_PRIORITY.put(textureId, priority);
    }

    public static void registerTile(StructurePieceType structurePieceType, int priority, ResourceLocation textureId) {
        registerTile(structurePieceType, priority, textureId, ALWAYS);
    }

    public static void registerJigsawTile(ResourceLocation jigsawPattern, int priority, ResourceLocation tileID, Setter setter) {
        JIGSAW_TO_TILE_MAP.put(jigsawPattern, new Tuple<>(tileID, setter));
        STRUCTURE_PIECE_TILE_PRIORITY.put(tileID, priority);
    }

    public static void registerJigsawTile(ResourceLocation jigsawPattern, int priority, ResourceLocation tileID) {
        registerJigsawTile(jigsawPattern, priority, tileID, ALWAYS);
    }

    public static void registerMarker(ResourceKey<Structure> structureFeature, ResourceLocation markerType, Component name) {
        STRUCTURE_PIECE_TO_MARKER_MAP.put(structureFeature.location(), new Tuple<>(markerType, name));
    }

    private static int getPriority(ResourceLocation structurePieceId) {
        return STRUCTURE_PIECE_TILE_PRIORITY.getOrDefault(structurePieceId, Integer.MAX_VALUE);
    }

    private static void put(Level world, int chunkX, int chunkZ, ResourceLocation textureId) {
        ResourceLocation existingTile = AtlasAPI.getTileAPI().getGlobalTile(world, chunkX, chunkZ);

        if (getPriority(textureId) < getPriority(existingTile)) {
            AtlasAPI.getTileAPI().putGlobalTile(world, textureId, chunkX, chunkZ);
        }
    }

    private static void resolveJigsaw(StructurePiece jigsawPiece, ServerLevel world) {
        if (jigsawPiece instanceof PoolElementStructurePiece pool) {
            if (pool.getElement() instanceof SinglePoolElement singlePoolElement) {
                Optional<ResourceLocation> left = singlePoolElement.template.left();

                if (left.isPresent()) {
                    for (Tuple<ResourceLocation, Setter> entry : JIGSAW_TO_TILE_MAP.get(left.get())) {
                        ResourceLocation tile = entry.getA();
                        Setter setter = entry.getB();
                        for (ChunkPos pos : setter.matches(world, singlePoolElement, pool.getBoundingBox(), jigsawPiece)) {
                            put(world, pos.x, pos.z, tile);
                        }
                    }
                }
            }

        }
    }

    public static void resolve(StructurePiece structurePiece, ServerLevel world) {
        if (structurePiece.getType() == StructurePieceType.JIGSAW) {
            resolveJigsaw(structurePiece, world);

            return;
        }

        ResourceLocation structurePieceId = world.registryAccess().registryOrThrow(Registries.STRUCTURE_PIECE).getKey(structurePiece.getType());
        if (STRUCTURE_PIECE_TO_TILE_MAP.containsKey(structurePieceId)) {
            for (Tuple<ResourceLocation, Setter> entry : STRUCTURE_PIECE_TO_TILE_MAP.get(structurePieceId)) {
                Collection<ChunkPos> matches;
                if (structurePiece instanceof PoolElementStructurePiece pool) {
                    matches = entry.getB().matches(world, pool.getElement(), pool.getBoundingBox(), structurePiece);
                } else {
                    matches = entry.getB().matches(world, null, structurePiece.getBoundingBox(), structurePiece);
                }

                for (ChunkPos pos : matches) {
                    put(world, pos.x, pos.z, entry.getA());
                }
            }
        }
    }

    public static void resolve(StructureStart structureStart, ServerLevel world) {
        ResourceLocation structureId = world.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structureStart.getStructure());
        if (STRUCTURE_PIECE_TO_MARKER_MAP.containsKey(structureId)) {
            Triple<Integer, Integer, ResourceLocation> key = Triple.of(
                    structureStart.getBoundingBox().getCenter().getX(),
                    structureStart.getBoundingBox().getCenter().getY(),
                    structureId);

            if (VISITED_STRUCTURES.contains(key)) return;
            VISITED_STRUCTURES.add(key);

            AtlasAPI.getMarkerAPI().putGlobalMarker(
                    world,
                    false,
                    STRUCTURE_PIECE_TO_MARKER_MAP.get(structureId).getA(),
                    STRUCTURE_PIECE_TO_MARKER_MAP.get(structureId).getB(),
                    structureStart.getBoundingBox().getCenter().getX(),
                    structureStart.getBoundingBox().getCenter().getZ()
            );
        }
    }

    interface Setter {
        Collection<ChunkPos> matches(Level world, StructurePoolElement element, BoundingBox box, StructurePiece rotation);
    }
}
