package hunternif.mc.impl.atlas.item;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeAtlasCloning implements CraftingRecipe {

    public RecipeAtlasCloning(CraftingBookCategory craftingBookCategory) {
    }

    @Override
    public String getGroup() {
        return AntiqueAtlas.ID + ":atlas";
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        int i = 0; // number of empty atlases
        ItemStack filledAtlas = ItemStack.EMPTY;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack stack = inv.getItem(j);

            if (!stack.isEmpty()) {
                if (stack.getItem() == AntiqueAtlasItems.Items.ATLAS) {
                    if (!filledAtlas.isEmpty()) {
                        return false;
                    }
                    filledAtlas = stack;
                } else {
                    if (stack.getItem() != AntiqueAtlasItems.Items.EMPTY_ATLAS) {
                        return false;
                    }
                    i++;
                }
            }
        }

        return !filledAtlas.isEmpty() && i > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, Provider provider) {
        int i = 0; // number of new copies
        ItemStack filledAtlas = ItemStack.EMPTY;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack stack = inv.getItem(j);

            if (!stack.isEmpty()) {
                if (stack.getItem() == AntiqueAtlasItems.Items.ATLAS) {
                    if (!filledAtlas.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    filledAtlas = stack;
                } else {
                    if (stack.getItem() != AntiqueAtlasItems.Items.EMPTY_ATLAS) {
                        return ItemStack.EMPTY;
                    }
                    i++;
                }
            }
        }

        if (!filledAtlas.isEmpty() && i >= 1) {
            ItemStack newAtlas = new ItemStack(AntiqueAtlasItems.Items.ATLAS, i + 1);
            newAtlas.set(AntiqueAtlasItems.Components.ATLAS_ID, filledAtlas.get(AntiqueAtlasItems.Components.ATLAS_ID));

            if (filledAtlas.has(DataComponents.CUSTOM_NAME)) {
                newAtlas.set(DataComponents.CUSTOM_NAME, filledAtlas.get(DataComponents.CUSTOM_NAME));
            }

            return newAtlas;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(Provider provider) {
        return ItemStack.EMPTY;
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
        return AntiqueAtlasItems.Recipes.CLONE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
