package hunternif.mc.impl.atlas.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AntiqueAtlasItems {
    public static final DeferredRegister<Item>
            ITEMS = DeferredRegister.create(AntiqueAtlasMod.ID, Registry.ITEM_REGISTRY);
    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPES = DeferredRegister.create(AntiqueAtlasMod.ID, Registry.RECIPE_SERIALIZER_REGISTRY);

    public static final RegistrySupplier<Item> EMPTY_ATLAS = ITEMS.register("empty_antique_atlas", () -> new EmptyAtlasItem(new Item.Settings().group(ItemGroup.MISC)));
    public static final RegistrySupplier<Item> ATLAS = ITEMS.register("antique_atlas", () -> new AtlasItem(new Item.Settings().maxCount(1)));

    public static ItemStack getAtlasFromId(int atlasID) {
        ItemStack atlas = new ItemStack(ATLAS.get());
        atlas.getOrCreateTag().putInt("atlasID", atlasID);

        return atlas;
    }

    public static void register() {
        if (AntiqueAtlasMod.CONFIG.itemNeeded) {
            ITEMS.register();

            RECIPES.register("atlas_clone", () -> RecipeAtlasCloning.SERIALIZER);
            RECIPES.register("atlas_combine", () -> RecipeAtlasCombining.SERIALIZER);

            RECIPES.register();
        }
    }
}
