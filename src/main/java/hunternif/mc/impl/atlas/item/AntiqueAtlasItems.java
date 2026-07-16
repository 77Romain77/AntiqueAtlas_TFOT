package hunternif.mc.impl.atlas.item;

import com.mojang.serialization.Codec;
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
	public static final class Components {
//		@RegistryObject("atlas_id")
//		public static final DataComponentType<AtlasId> ATLAS_ID = register(
//		        type -> type.persistent(AtlasId.CODEC).networkSynchronized(AtlasId.STREAM_CODEC)
//		);
//	    private static <T> DataComponentType<T> register(UnaryOperator<DataComponentType.Builder<T>> pBuilder) {
//	        return pBuilder.apply(DataComponentType.builder()).build();
//	    }
	    public static final AtlasIdData ATLAS_ID_DATA = new AtlasIdData();
	}

	/** Legacy NBT accessor retained for dormant migration code. */
	public static final class AtlasIdData {
		public boolean hasData(ItemStack stack) {
			return stack.getTag() != null && stack.getTag().contains("atlasID");
		}

		public AtlasId getData(ItemStack stack) {
			return new AtlasId(stack.getOrCreateTag().getInt("atlasID"));
		}

		public void setData(ItemStack stack, AtlasId data) {
			stack.getOrCreateTag().putInt("atlasID", data.id());
		}

		public void removeData(ItemStack stack) {
			stack.removeTagKey("atlasID");
		}
	}

	/** Historical objects are deliberately not registered in the client-only edition. */
	public static final class Items {
		public static final Item EMPTY_ATLAS = new EmptyAtlasItem(new Item.Properties());
		public static final Item ATLAS = new AtlasItem(new Item.Properties().stacksTo(1));
	}

	public static final class Recipes {
	    public static final RecipeSerializer<?> CLONE = new SimpleCraftingRecipeSerializer<>(RecipeAtlasCloning::new);
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
