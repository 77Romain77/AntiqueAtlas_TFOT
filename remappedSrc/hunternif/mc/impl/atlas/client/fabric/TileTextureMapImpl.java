package hunternif.mc.impl.atlas.client.fabric;

import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.client.TileTextureMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import java.util.Optional;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class TileTextureMapImpl {

    static public Optional<ResourceLocation> guessFittingTextureSet(ResourceKey<Biome> biome) {
        if (Minecraft.getInstance().level == null)
            return Optional.empty();

        Holder<Biome> biomeTag = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(biome);

        if (biomeTag.is(ConventionalBiomeTags.SWAMP)) {
            if (biomeTag.is(BiomeTags.IS_HILL)) {
                return Optional.of(AntiqueAtlasMod.id("swamp_hills"));
            } else {
                return Optional.of(AntiqueAtlasMod.id("swamp"));
            }
        }

        if (biomeTag.is(BiomeTags.IS_OCEAN)
                || biomeTag.is(BiomeTags.IS_DEEP_OCEAN)
                || biomeTag.is(BiomeTags.IS_RIVER)
                || biomeTag.is(ConventionalBiomeTags.AQUATIC)) {
            if (biomeTag.is(ConventionalBiomeTags.ICY))
                return Optional.of(AntiqueAtlasMod.id("ice"));

            return Optional.of(AntiqueAtlasMod.id("water"));
        }

        if (biomeTag.is(BiomeTags.IS_BEACH) || biomeTag.is(ConventionalBiomeTags.BEACH)) {
            return Optional.of(AntiqueAtlasMod.id("shore"));
        }

        if (biomeTag.is(BiomeTags.IS_JUNGLE)) {
            if (biomeTag.is(BiomeTags.IS_HILL)) {
                return Optional.of(AntiqueAtlasMod.id("jungle_hills"));
            } else {
                return Optional.of(AntiqueAtlasMod.id("jungle"));
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.SAVANNA) || biomeTag.is(ConventionalBiomeTags.TREE_SAVANNA)) {
            return Optional.of(AntiqueAtlasMod.id("savana"));
        }

        if (biomeTag.is((ConventionalBiomeTags.MESA))) {
            return Optional.of(AntiqueAtlasMod.id("plateau_mesa"));
        }

        if (biomeTag.is(BiomeTags.IS_FOREST) || biomeTag.is(ConventionalBiomeTags.TREE_DECIDUOUS)) {
            if (biomeTag.is(ConventionalBiomeTags.ICY) || biomeTag.is(ConventionalBiomeTags.SNOWY)) {
                if (biomeTag.is(BiomeTags.IS_HILL)) {
                    return Optional.of(AntiqueAtlasMod.id("snow_pines_hills"));
                } else {
                    return Optional.of(AntiqueAtlasMod.id("snow_pines"));
                }
            } else {
                if (biomeTag.is(BiomeTags.IS_HILL)) {
                    return Optional.of(AntiqueAtlasMod.id("forest_hills"));
                } else {
                    return Optional.of(AntiqueAtlasMod.id("forest"));
                }
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.PLAINS) || biomeTag.is(ConventionalBiomeTags.SNOWY_PLAINS)) {
            if (biomeTag.is(ConventionalBiomeTags.ICY)
                    || biomeTag.is(ConventionalBiomeTags.SNOWY)
            ) {
                if (biomeTag.is(BiomeTags.IS_HILL)) {
                    return Optional.of(AntiqueAtlasMod.id("snow_hills"));
                } else {
                    return Optional.of(AntiqueAtlasMod.id("snow"));
                }
            } else {
                if (biomeTag.is(BiomeTags.IS_HILL)) {
                    return Optional.of(AntiqueAtlasMod.id("hills"));
                } else {
                    return Optional.of(AntiqueAtlasMod.id("plains"));
                }
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.ICY)) {
            if (biomeTag.is(BiomeTags.IS_HILL)) {
                return Optional.of(AntiqueAtlasMod.id("mountains_snow_caps"));
            } else {
                return Optional.of(AntiqueAtlasMod.id("ice_spikes"));
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.DESERT)) {
            if (biomeTag.is(BiomeTags.IS_HILL)) {
                return Optional.of(AntiqueAtlasMod.id("desert_hills"));
            } else {
                return Optional.of(AntiqueAtlasMod.id("desert"));
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.TAIGA)) {
            return Optional.of(AntiqueAtlasMod.id("snow"));
        }

        if (biomeTag.is(ConventionalBiomeTags.EXTREME_HILLS)) {
            return Optional.of(AntiqueAtlasMod.id("hills"));
        }

        if (biomeTag.is(ConventionalBiomeTags.MOUNTAIN) || biomeTag.is(ConventionalBiomeTags.MOUNTAIN_SLOPE)) {
            return Optional.of(AntiqueAtlasMod.id("mountains"));
        }

        if (biomeTag.is(ConventionalBiomeTags.MOUNTAIN_PEAK)) {
            return Optional.of(AntiqueAtlasMod.id("mountains_snow_caps"));
        }

        if (biomeTag.is(ConventionalBiomeTags.IN_THE_END) || biomeTag.is(ConventionalBiomeTags.END_ISLANDS)) {
            if (biomeTag.is(ConventionalBiomeTags.VEGETATION_DENSE) || biomeTag.is(ConventionalBiomeTags.VEGETATION_SPARSE)) {
                return Optional.of(AntiqueAtlasMod.id("end_island_plants"));
            } else {
                return Optional.of(AntiqueAtlasMod.id("end_island"));
            }
        }

        if (biomeTag.is(ConventionalBiomeTags.MUSHROOM)) {
            return Optional.of(AntiqueAtlasMod.id("mushroom"));
        }

        if (biomeTag.is(ConventionalBiomeTags.IN_NETHER) || biomeTag.is(BiomeTags.IS_NETHER)) {
            return Optional.of(AntiqueAtlasMod.id("soul_sand_valley"));
        }

        if (biomeTag.is(ConventionalBiomeTags.VOID)) {
            return Optional.of(AntiqueAtlasMod.id("end_void"));
        }

        if (biomeTag.is(ConventionalBiomeTags.UNDERGROUND)) {
            AntiqueAtlasMod.LOG.warn("Underground biomes aren't supported yet.");
        }

        if (biomeTag.is(BiomeTags.IS_BADLANDS)) {
            return Optional.of(AntiqueAtlasMod.id("mesa"));
        }

        return TileTextureMap.guessFittingTextureSetFallback(biomeTag.value());
    }
}
