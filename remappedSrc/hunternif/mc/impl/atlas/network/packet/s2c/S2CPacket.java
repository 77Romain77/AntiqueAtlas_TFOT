package hunternif.mc.impl.atlas.network.packet.s2c;

import dev.architectury.networking.NetworkManager;
import hunternif.mc.impl.atlas.network.packet.AntiqueAtlasPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class S2CPacket extends AntiqueAtlasPacket {
	public void send(ServerPlayer playerEntity) {
		NetworkManager.sendToPlayer(playerEntity, this.getId(), this);
	}

	public void send(ServerLevel world) {
		NetworkManager.sendToPlayers(world.players(), this.getId(), this);
	}

	public void send(MinecraftServer server) {
		NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this.getId(), this);
	}
}
