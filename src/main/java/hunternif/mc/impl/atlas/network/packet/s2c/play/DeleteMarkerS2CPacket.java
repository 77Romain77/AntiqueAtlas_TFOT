package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.c2s.play.DeleteMarkerC2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Deletes a marker. A client sends a {@link DeleteMarkerC2SPacket}
 * to the server as a request, and the server sends this back to all players as a response, including the
 * original sender.
 * @author Hunternif
 * @author Haven King
 */
public class DeleteMarkerS2CPacket extends ClientboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "marker", "delete");

	private static final int GLOBAL = -1;
	int atlasID, markerID;

	public DeleteMarkerS2CPacket(int atlasID, int markerID) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.markerID = markerID;
	}
	
	public DeleteMarkerS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.markerID = byteBuf.readVarInt();
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeVarInt(atlasID);
		byteBuf.writeVarInt(markerID);
	}

	@Override
	public boolean runOnClient(Player sender) {
		MarkersData data = atlasID == GLOBAL ?
				AntiqueAtlas.globalMarkersData.getData() :
				AntiqueAtlas.markersData.getMarkersData(atlasID, sender.level());
		data.removeMarker(markerID);

		AntiqueAtlasClientSegment.getAtlasGUI().updateBookmarkerList();
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
