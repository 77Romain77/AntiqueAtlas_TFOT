package hunternif.mc.api;

import java.util.Collections;
import java.util.List;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.api.impl.MarkerApiImpl;
import hunternif.mc.impl.atlas.api.impl.TileApiImpl;
import hunternif.mc.impl.atlas.client.ClientAtlasItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Use this class to obtain a reference to the APIs.
 *
 * @author Hunternif
 */
public class AtlasAPI {
    private static final int VERSION = 5;
    private static final TileAPI tiles = new TileApiImpl();
    private static final MarkerAPI markers = new MarkerApiImpl();

    /**
     * Version of the API, meaning only this particular class. You might
     * want to check static field VERSION in the specific API interfaces.
     */
    public static int getVersion() {
        return VERSION;
    }

    public static Item getAtlasItem() {
        return Items.BOOK;
    }

    /**
     * API for biomes and custom tiles (i.e. dungeons, towns etc).
     */
    public static TileAPI getTileAPI() {
        return tiles;
    }

    /**
     * API for custom markers.
     */
    public static MarkerAPI getMarkerAPI() {
        return markers;
    }

    /**
     * Returns the player's personal atlas ID when access is allowed.
     **/
    public static List<Integer> getPlayerAtlases(Player player) {
        if (AntiqueAtlas.CONFIG.itemNeeded && !hasAtlas(player)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(player.getUUID().hashCode());
    }

    private static boolean hasAtlas(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (ClientAtlasItem.isAtlas(stack)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (ClientAtlasItem.isAtlas(stack)) return true;
        }
        return false;
    }
}
