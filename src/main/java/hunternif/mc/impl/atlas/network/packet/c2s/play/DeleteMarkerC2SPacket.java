package hunternif.mc.impl.atlas.network.packet.c2s.play;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Deletes a marker. A client sends this packet to the server as a request,
 * and the server sends to all players as a response, including the
 * original sender.
 * @author Hunternif
 */
public class DeleteMarkerC2SPacket extends ServerboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "marker", "delete");

	private static final int GLOBAL = -1;
	int atlasID, markerID;
	
	public DeleteMarkerC2SPacket(int atlasID, int markerID) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.markerID = markerID;
	}

	public DeleteMarkerC2SPacket(FriendlyByteBuf packetBuffer) {
		super(packetBuffer, AntiqueAtlas.instance.channel);
		this.atlasID = packetBuffer.readInt();
		this.markerID = packetBuffer.readInt();
	}

	@Override
	public void encode(final FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(this.atlasID);
		packetBuffer.writeInt(this.markerID);
	}

	@Override
	public boolean handleOnServer(ServerPlayer sender) {
		if (AntiqueAtlas.CONFIG.itemNeeded && !AtlasAPI.getPlayerAtlases(sender).contains(atlasID)) {
			Log.warn("Player %s attempted to delete marker from someone else's Atlas #%d",
					sender.getName(), atlasID);
			return true;
		}

		AtlasAPI.getMarkerAPI().deleteMarker(sender.level(), atlasID, markerID);
		return true;
	}
	
	@Override
	public ResourceLocation id() {
		return ID;
	}
}
