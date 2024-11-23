package hunternif.mc.impl.atlas.core.scaning;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import hunternif.mc.impl.atlas.core.TileIdMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Detects seas of lava, cave ground and cave walls in the Nether.
 *
 * @author Hunternif
 */
public class TileDetectorNether extends TileDetectorBase implements ITileDetector {
    /**
     * The Nether will be checked for air/ground at this level.
     */
    private static final int airProbeLevel = 50;
    /**
     * The Nether will be checked for lava at this level.
     */
    private static final int lavaSeaLevel = 31;

    /**
     * Increment the counter for lava biomes by this much during iteration.
     * This is done so that rivers are more likely to be connected.
     */
    private static final int priorityLava = 1;

    @Override
    public ResourceLocation getBiomeID(Level world, ChunkAccess chunk) {
        Map<ResourceLocation, Integer> biomeOccurrences = new HashMap<>(world.registryAccess().registryOrThrow(Registries.BIOME).keySet().size());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Holder<Biome> biome = chunk.getNoiseBiome(x, lavaSeaLevel, z);
                if (biome.is(BiomeTags.IS_NETHER)) {
                    // The Nether!
                    Block seaLevelBlock = chunk.getBlockState(new BlockPos(x, lavaSeaLevel, z)).getBlock();
                    if (seaLevelBlock == Blocks.LAVA) {
                        updateOccurrencesMap(biomeOccurrences, TileIdMap.TILE_LAVA, priorityLava);
                    } else {
                        BlockState airProbeBlock = chunk.getBlockState(new BlockPos(x, airProbeLevel, z));
                        if (airProbeBlock.isAir()) {
                            updateOccurrencesMap(biomeOccurrences, TileIdMap.TILE_LAVA_SHORE, 1);
                        } else {
                            // cave walls
                            updateOccurrencesMap(biomeOccurrences, getBiomeIdentifier(world,biome.value()), 2);
                        }
                    }
                } else {
                    // In case there are custom biomes "modded in":
                    updateOccurrencesMap(biomeOccurrences, getBiomeIdentifier(world,biome.value()), priorityForBiome(getBiomeIdentifier(world,biome.value())));
                }
            }
        }

        if (biomeOccurrences.isEmpty()) return null;

        Map.Entry<ResourceLocation, Integer> meanBiome = Collections.max(biomeOccurrences.entrySet(), Comparator
                .comparingInt(Map.Entry::getValue));

        return meanBiome.getKey();
    }

    @Override
    public int getScanRadius() {
        return Math.min(super.getScanRadius(), 6);
    }
}
