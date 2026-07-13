package hunternif.mc.impl.atlas.core;

import java.util.Collection;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DimensionUpdateS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlayerEventHandler {
    public static void onPlayerLogin(ServerPlayer player) {
        if (AntiqueAtlas.CONFIG.itemNeeded) {
            return;
        }

        Level world = player.level();
        int atlasID = player.getUUID().hashCode();

        AtlasData data = AntiqueAtlas.tileData.getData(atlasID, world);
        // On the player join send the map from the server to the client:
        if (!data.isEmpty()) {
            data.syncToPlayer(atlasID, player);
        }

        // Same thing with the local markers:
        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        if (!markers.isEmpty()) {
            markers.syncToPlayer(atlasID, player);
        }
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide || AntiqueAtlas.CONFIG.itemNeeded || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int atlasID = player.getUUID().hashCode();
        AtlasData data = AntiqueAtlas.tileData.getData(atlasID, player.level());

        if (!data.isSyncedToPlayer(player) && !data.isEmpty()) {
            data.syncToPlayer(atlasID, player);
        }

        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(atlasID, player.level());
        if (!markers.isSyncedOnPlayer(player) && !markers.isEmpty()) {
            markers.syncToPlayer(atlasID, serverPlayer);
        }

        Collection<TileInfo> newTiles = AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(data, player);
        if (!newTiles.isEmpty()) {
            new DimensionUpdateS2CPacket(atlasID, player.level().dimension(), newTiles).send(serverPlayer);
        }
    }
}
