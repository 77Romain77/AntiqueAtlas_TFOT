package hunternif.mc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stereowalker.unionlib.util.RegistryHelper;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.api.impl.MarkerApiImpl;
import hunternif.mc.impl.atlas.api.impl.TileApiImpl;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        return RegistryHelper.items().get(VersionHelper.toLoc("antiqueatlas:antique_atlas"));
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
     * Convenience method that returns a list of atlas IDs for all atlas items
     * the player is currently carrying.
     **/
    public static List<Integer> getPlayerAtlases(Player player) {
        if (!AntiqueAtlas.CONFIG.itemNeeded) {
            return Collections.singletonList(player.getUUID().hashCode());
        }

        List<Integer> list = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof AtlasItem) {
                list.add(AtlasItem.getAtlasID(stack));
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() instanceof AtlasItem) {
                list.add(AtlasItem.getAtlasID(stack));
            }
        }

        return list;
    }
}
