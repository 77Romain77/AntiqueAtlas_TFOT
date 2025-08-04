package hunternif.mc.impl.atlas.item;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class RecipeAtlasCloning extends CustomRecipe {

    public RecipeAtlasCloning(ResourceLocation pId, CraftingBookCategory craftingBookCategory) {
    	super(pId, craftingBookCategory);
    }

    @Override
    public String getGroup() {
        return AntiqueAtlas.ID + ":atlas";
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        int i = 0; // number of empty atlases
        ItemStack filledAtlas = ItemStack.EMPTY;

        for (int j = 0; j < inv.getContainerSize(); ++j) {
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
    public ItemStack assemble(CraftingContainer inv, RegistryAccess provider) {
        int i = 0; // number of new copies
        ItemStack filledAtlas = ItemStack.EMPTY;

        for (int j = 0; j < inv.getContainerSize(); ++j) {
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
            AntiqueAtlasItems.Components.ATLAS_ID_DATA.setData(newAtlas, AntiqueAtlasItems.Components.ATLAS_ID_DATA.getData(filledAtlas));

            if (filledAtlas.hasCustomHoverName()) {
                newAtlas.setHoverName(filledAtlas.getHoverName());
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
    public ItemStack getResultItem(RegistryAccess provider) {
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
