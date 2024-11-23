package hunternif.mc.impl.atlas.mixin.structure;

import hunternif.mc.impl.atlas.structure.StructureAddedCallback;
import hunternif.mc.impl.atlas.structure.StructurePieceAddedCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

@Mixin(StructureStart.class)
public class StructureStartMixin {

    @Shadow
    protected PiecesContainer children;


    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/structure/StructurePiece;generate(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockBox;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/util/math/BlockPos;)V"))
    private void structurePieceGenerated(StructurePiece structurePiece, WorldGenLevel serverWorldAccess, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, ChunkPos chunkPos, BlockPos blockPos) {
        ServerLevel world;

        if (serverWorldAccess instanceof ServerLevel) {
            world = (ServerLevel) serverWorldAccess;
        } else {
            world = ((WorldGenRegion) serverWorldAccess).level;
        }

        StructurePieceAddedCallback.EVENT.invoker().onStructurePieceAdded(structurePiece, world);
        structurePiece.postProcess(serverWorldAccess, structureAccessor, chunkGenerator, random, boundingBox, chunkPos, blockPos);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void structureGenerated(WorldGenLevel serverWorldAccess, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox blockBox, ChunkPos chunkPos, CallbackInfo ci) {
        ServerLevel world;

        if (serverWorldAccess instanceof ServerLevel) {
            world = (ServerLevel) serverWorldAccess;
        } else {
            world = ((WorldGenRegion) serverWorldAccess).level;
        }

        synchronized (this.children) {
            if(this.children.isEmpty()) return;

            StructureAddedCallback.EVENT.invoker().onStructureAdded((StructureStart) (Object) this, world);
        }
    }
}
