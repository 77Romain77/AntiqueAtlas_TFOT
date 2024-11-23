package hunternif.mc.impl.atlas.item;

import java.util.ArrayList;
import java.util.List;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;

/**
 * 2 or more atlases combine into one with all biome and marker data copied.
 * All data is copied into a new atlas instance.
 *
 * @author Hunternif
 */
public class RecipeAtlasCombining implements CraftingRecipe {

    public RecipeAtlasCombining(CraftingBookCategory craftingBookCategory) {
    }

    @Override
    public String getGroup() {
        return AntiqueAtlas.ID + ":atlas_combine";
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        return matches(inv);
    }

    private boolean matches(CraftingInput inv) {
        int atlasesFound = 0;
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == AntiqueAtlasItems.Items.ATLAS) {
                    atlasesFound++;
                }
            }
        }
        return atlasesFound > 1;
    }
    
    @Override
    public ItemStack assemble(CraftingInput inv, Provider provider) {
        ItemStack firstAtlas = ItemStack.EMPTY;
        List<Integer> atlasIds = new ArrayList<>(9);
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof AtlasItem) {
                    if (firstAtlas.isEmpty()) {
                        firstAtlas = stack;
                    } else {
                        atlasIds.add(AtlasItem.getAtlasID(stack));
                    }
                }
            }
        }
        return atlasIds.size() < 1 ? ItemStack.EMPTY : firstAtlas.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }
    
    @Override
    public ItemStack getResultItem(Provider provider) {
        return new ItemStack(AntiqueAtlasItems.Items.ATLAS);
    }

//    @Override
//    public ResourceLocation getId() {
//        return id;
//    }

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AntiqueAtlasItems.Recipes.COMBINE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public ItemStack onCrafted(Level world, Container inventory, ItemStack result) {
        if (world.isClientSide) return result;
        // Until the first update, on the client the returned atlas ID is the same as the first Atlas on the crafting grid.
        int atlasID = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();

        AtlasData destBiomes = AntiqueAtlas.tileData.getData(atlasID, world);
        destBiomes.setDirty();
        MarkersData destMarkers = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        destMarkers.setDirty();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            AtlasData srcBiomes = AntiqueAtlas.tileData.getData(stack, world);
            if (destBiomes != null && srcBiomes != null && destBiomes != srcBiomes) {
                for (ResourceKey<Level> worldRegistryKey : srcBiomes.getVisitedWorlds()) {
                    destBiomes.getWorldData(worldRegistryKey).addData(srcBiomes.getWorldData(worldRegistryKey));
                }
            }
            MarkersData srcMarkers = AntiqueAtlas.markersData.getMarkersData(stack, world);
            if (destMarkers != null && srcMarkers != null && destMarkers != srcMarkers) {
                for (ResourceKey<Level> worldRegistryKey : srcMarkers.getVisitedDimensions()) {
                    for (Marker marker : srcMarkers.getMarkersDataInWorld(worldRegistryKey).getAllMarkers()) {
                        destMarkers.createAndSaveMarker(marker.getType(),
                                worldRegistryKey, marker.getX(), marker.getZ(), marker.isVisibleAhead(), marker.getLabel());
                    }
                }
            }
        }

        // Set atlas ID last, because otherwise we wouldn't be able copy the
        // data from the atlas which was used as a placeholder for the result.
        result.set(AntiqueAtlasItems.Components.ATLAS_ID, new MapId(atlasID));
        return result;
    }
}
