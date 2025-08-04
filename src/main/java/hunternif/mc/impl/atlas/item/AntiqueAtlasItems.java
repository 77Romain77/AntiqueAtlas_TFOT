package hunternif.mc.impl.atlas.item;

import com.mojang.serialization.Codec;
import com.stereowalker.unionlib.core.registries.RegistryHolder;
import com.stereowalker.unionlib.core.registries.RegistryObject;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class AntiqueAtlasItems {
	public record AtlasId(int id) {
	    public static final Codec<AtlasId> CODEC = Codec.INT.xmap(AtlasId::new, AtlasId::id);
//	    public static final StreamCodec<ByteBuf, AtlasId> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(AtlasId::new, AtlasId::id);

	    public String key() {
	        return "atlas_" + this.id;
	    }
	}
	@RegistryHolder(namespace = AntiqueAtlas.ID)
	public class Components {
//		@RegistryObject("atlas_id")
//		public static final DataComponentType<AtlasId> ATLAS_ID = register(
//		        type -> type.persistent(AtlasId.CODEC).networkSynchronized(AtlasId.STREAM_CODEC)
//		);
//	    private static <T> DataComponentType<T> register(UnaryOperator<DataComponentType.Builder<T>> pBuilder) {
//	        return pBuilder.apply(DataComponentType.builder()).build();
//	    }
	    public static final VersionHelper.Data<AtlasId> ATLAS_ID_DATA = new VersionHelper.Data<AtlasId>(
				(stack) -> stack.getTag() != null && stack.getTag().contains("atlasID"),
				(stack) -> new AtlasId(stack.getTag().getInt("atlasID")),
				(stack, dat) -> stack.getOrCreateTag().putInt("atlasID", dat.id),
				(stack) -> stack.removeTagKey("atlasID"));
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
        Components.ATLAS_ID_DATA.setData(atlas, new AtlasId(atlasID));
        return atlas;
    }

    public static void register() {
        if (AntiqueAtlas.CONFIG.itemNeeded) {
//            RECIPES.register("atlas_clone", () -> RecipeAtlasCloning.SERIALIZER);
//            RECIPES.register("atlas_combine", () -> RecipeAtlasCombining.SERIALIZER);
        }
    }
}
