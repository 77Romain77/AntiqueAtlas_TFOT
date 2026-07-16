package hunternif.mc.impl.atlas.core.watcher;

import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Puts one tomb marker at the local player's death spot.
 *
 * @author Hunternif, Haven King
 */
public class DeathWatcher {
    private static Player lastPlayer;
    private static ResourceKey<Level> lastDimension;
    private static long lastDeathTick = Long.MIN_VALUE;
    private static int lastDeathX;
    private static int lastDeathZ;

    public static void onPlayerDeath(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientMapManager maps = ClientMapManager.getInstance();
        if (!player.level().isClientSide() || minecraft.player != player
                || !maps.isReady() || !maps.isAutoDeathMarkerEnabled()) return;

        ResourceKey<Level> dimension = player.level().dimension();
        long deathTick = player.level().getGameTime();
        int x = player.getBlockX();
        int z = player.getBlockZ();
        boolean duplicate = lastPlayer == player && dimension.equals(lastDimension)
                && deathTick == lastDeathTick
                && x == lastDeathX && z == lastDeathZ;
        if (duplicate) return;

        lastPlayer = player;
        lastDimension = dimension;
        lastDeathTick = deathTick;
        lastDeathX = x;
        lastDeathZ = z;
        maps.createMarker(dimension, AntiqueAtlas.id("tomb"),
                Component.translatable("gui.antiqueatlas.marker.death"), x, z, true);
    }
}
