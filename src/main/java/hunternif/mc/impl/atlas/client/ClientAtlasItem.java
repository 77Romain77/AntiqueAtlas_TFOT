package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/** Recognises a vanilla book renamed or tagged as an atlas. No custom item is registered. */
public final class ClientAtlasItem {
    public static final String ATLAS_NBT_TAG = "antiqueatlas";

    private static final List<String> ACCEPTED_NAMES = List.of(
            "antique atlas",
            "atlas antique",
            "atlas antiguo",
            "antyczny atlas",
            "античный атлас"
    );

    private ClientAtlasItem() {
    }

    public static boolean isAtlas(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.BOOK)) return false;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(ATLAS_NBT_TAG)) return true;
        if (!stack.hasCustomHoverName()) return false;

        String name = stack.getHoverName().getString().strip().toLowerCase(Locale.ROOT);
        return ACCEPTED_NAMES.stream().anyMatch(name::equals);
    }

    /**
     * Adds a model predicate to the vanilla book. The matching model override is
     * defined in {@code assets/minecraft/models/item/book.json}.
     */
    public static void registerModelProperty() {
        ItemProperties.register(Items.BOOK, AntiqueAtlas.id("atlas"),
                (stack, level, entity, seed) -> isAtlas(stack) ? 1.0F : 0.0F);
    }

    public static ItemStack find(Player player) {
        if (player == null) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (isAtlas(stack)) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isAtlas(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
