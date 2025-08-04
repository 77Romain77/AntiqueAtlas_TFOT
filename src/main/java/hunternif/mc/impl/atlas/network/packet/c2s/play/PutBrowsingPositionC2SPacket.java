package hunternif.mc.impl.atlas.network.packet.c2s.play;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Packet used to save the last browsing position for a dimension in an atlas.
 * @author Hunternif
 * @author Haven King
 */
public class PutBrowsingPositionC2SPacket extends ServerboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "browsing_position", "put");
	int atlasID, x, y; 
	double zoom;
	ResourceKey<Level> world;
	
	public PutBrowsingPositionC2SPacket(int atlasID, ResourceKey<Level> world, int x, int y, double zoom) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.world = world;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
	}

	public PutBrowsingPositionC2SPacket(FriendlyByteBuf packetBuffer) {
		super(packetBuffer, AntiqueAtlas.instance.channel);
		this.atlasID = packetBuffer.readVarInt();
		this.world = ResourceKey.create(Registries.DIMENSION, packetBuffer.readResourceLocation());
		this.x = packetBuffer.readVarInt();
		this.y = packetBuffer.readVarInt();
		this.zoom = packetBuffer.readDouble();
	}

	@Override
	public void encode(final FriendlyByteBuf packetBuffer) {
		packetBuffer.writeVarInt(atlasID);
		packetBuffer.writeResourceLocation(world.location());
		packetBuffer.writeVarInt(x);
		packetBuffer.writeVarInt(y);
		packetBuffer.writeDouble(zoom);
	}

	@Override
	public boolean handleOnServer(ServerPlayer sender) {
		if (AntiqueAtlas.CONFIG.itemNeeded && !AtlasAPI.getPlayerAtlases(sender).contains(atlasID)) {
			Log.warn("Player %s attempted to put position marker into someone else's Atlas #%d",
					sender.getName(), atlasID);
			return false;
		}

		AntiqueAtlas.tileData.getData(atlasID, sender.level())
				.getWorldData(world).setBrowsingPosition(x, y, zoom);
		return true;
	}
	
	@Override
	public ResourceLocation id() {
		return ID;
	}
}
