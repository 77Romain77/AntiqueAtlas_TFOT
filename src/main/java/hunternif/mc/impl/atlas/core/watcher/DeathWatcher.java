package hunternif.mc.impl.atlas.core.watcher;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Puts an skull marker to the player's death spot.
 *
 * @author Hunternif, Haven King
 */
public class DeathWatcher {
    public static void onPlayerDeath(Player player) {
        if (player.level().isClientSide() && AntiqueAtlas.CONFIG.autoDeathMarker) {
            ClientMapManager.getInstance().createMarker(
                    player.level().dimension(), AntiqueAtlas.id("tomb"),
                    Component.translatable("gui.antiqueatlas.marker.tomb", player.getName()),
                    (int) player.getX(), (int) player.getZ(), true);
        }
    }
}
