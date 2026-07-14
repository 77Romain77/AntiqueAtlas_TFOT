package hunternif.mc.impl.atlas.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/** Recognises a vanilla book renamed by the player. No custom item is registered. */
public final class ClientAtlasItem {
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
        if (stack == null || stack.isEmpty() || !stack.is(Items.BOOK) || !stack.hasCustomHoverName()) return false;
        String name = stack.getHoverName().getString().strip().toLowerCase(Locale.ROOT);
        return ACCEPTED_NAMES.stream().anyMatch(name::equals);
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
