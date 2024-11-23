package hunternif.mc.impl.atlas.item;

import java.util.function.UnaryOperator;

import com.stereowalker.unionlib.core.registries.RegistryHolder;
import com.stereowalker.unionlib.core.registries.RegistryObject;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.saveddata.maps.MapId;

public class AntiqueAtlasItems {
	@RegistryHolder(namespace = AntiqueAtlas.ID)
	public class Components {
		@RegistryObject("atlas_id")
		public static final DataComponentType<MapId> ATLAS_ID = register(
		        type -> type.persistent(MapId.CODEC).networkSynchronized(MapId.STREAM_CODEC)
		);
	    private static <T> DataComponentType<T> register(UnaryOperator<DataComponentType.Builder<T>> pBuilder) {
	        return pBuilder.apply(DataComponentType.builder()).build();
	    }
	}
	
	@RegistryHolder(namespace = AntiqueAtlas.ID)
	public class Items {
		@RegistryObject("empty_antique_atlas")
		public static final Item EMPTY_ATLAS = new EmptyAtlasItem(new Item.Properties());
		@RegistryObject("antique_atlas")
		public static final Item ATLAS = new AtlasItem(new Item.Properties().stacksTo(1));
	}
	
	@RegistryHolder(namespace = AntiqueAtlas.ID)
	public class Recipes {
		@RegistryObject("atlas_clone")
	    public static final RecipeSerializer<?> CLONE = new SimpleCraftingRecipeSerializer<>(RecipeAtlasCloning::new);
		@RegistryObject("atlas_combine")
	    public static final RecipeSerializer<RecipeAtlasCombining> COMBINE = new SimpleCraftingRecipeSerializer<>(RecipeAtlasCombining::new);
	}
	
    public static ItemStack getAtlasFromId(int atlasID) {
        ItemStack atlas = new ItemStack(Items.ATLAS);
        atlas.set(Components.ATLAS_ID, new MapId(atlasID));
        return atlas;
    }

    public static void register() {
        if (AntiqueAtlas.CONFIG.itemNeeded) {
//            RECIPES.register("atlas_clone", () -> RecipeAtlasCloning.SERIALIZER);
//            RECIPES.register("atlas_combine", () -> RecipeAtlasCombining.SERIALIZER);
        }
    }
}
