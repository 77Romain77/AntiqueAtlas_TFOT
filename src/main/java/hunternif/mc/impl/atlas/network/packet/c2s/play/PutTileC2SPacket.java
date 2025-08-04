package hunternif.mc.impl.atlas.network.packet.c2s.play;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Puts biome tile into one atlas. When sent to server, forwards it to every
 * client that has this atlas' data synced.
 * @author Hunternif
 * @author Haven King
 */
public class PutTileC2SPacket extends ServerboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "tile", "put");
	int atlasID, x, z; 
	ResourceLocation tile;
	
	public PutTileC2SPacket(int atlasID, int x, int z, ResourceLocation tile) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.tile = tile;
		this.x = x;
		this.z = z;
	}

	public PutTileC2SPacket(FriendlyByteBuf packetBuffer) {
		super(packetBuffer, AntiqueAtlas.instance.channel);
		this.atlasID = packetBuffer.readVarInt();
		this.tile = packetBuffer.readResourceLocation();
		this.x = packetBuffer.readVarInt();
		this.z = packetBuffer.readVarInt();
	}

	@Override
	public void encode(final FriendlyByteBuf packetBuffer) {
		packetBuffer.writeVarInt(atlasID);
		packetBuffer.writeResourceLocation(tile);
		packetBuffer.writeVarInt(x);
		packetBuffer.writeVarInt(z);
	}

	@Override
	public boolean handleOnServer(ServerPlayer sender) {
		if (AntiqueAtlas.CONFIG.itemNeeded && !AtlasAPI.getPlayerAtlases(sender).contains(atlasID)) {
			Log.warn("Player %s attempted to modify someone else's Atlas #%d",
					sender.getName(), atlasID);
			return false;
		}

		AtlasAPI.getTileAPI().putTile(sender.level(), atlasID, tile, x, z);
		return true;
	}
	
	@Override
	public ResourceLocation id() {
		return ID;
	}
}
