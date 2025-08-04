package hunternif.mc.impl.atlas.network.packet.c2s.play;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A request from a client to create a new marker. In order to prevent griefing,
 * the marker has to be local.
 * @author Hunternif
 * @author Haven King
 */
public class PutMarkerC2SPacket extends ServerboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "marker", "put");
	int atlasID, x, z; 
	boolean visibleBeforeDiscovery;
	ResourceLocation markerType;
	Component label;
	
	public PutMarkerC2SPacket(int atlasID, ResourceLocation markerType, int x, int z, boolean visibleBeforeDiscovery, Component label) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.markerType = markerType;
		this.x = x;
		this.z = z;
		this.visibleBeforeDiscovery = visibleBeforeDiscovery;
		this.label = label;
	}

	public PutMarkerC2SPacket(FriendlyByteBuf packetBuffer) {
		super(packetBuffer, AntiqueAtlas.instance.channel);
		this.atlasID = packetBuffer.readVarInt();
		this.markerType = packetBuffer.readResourceLocation();
		this.x = packetBuffer.readVarInt();
		this.z = packetBuffer.readVarInt();
		this.visibleBeforeDiscovery = packetBuffer.readBoolean();
		this.label = Component.Serializer.fromJson(packetBuffer.readUtf());
	}

	@Override
	public void encode(final FriendlyByteBuf packetBuffer) {
		packetBuffer.writeVarInt(atlasID);
		packetBuffer.writeResourceLocation(markerType);
		packetBuffer.writeVarInt(x);
		packetBuffer.writeVarInt(z);
		packetBuffer.writeBoolean(visibleBeforeDiscovery);
		packetBuffer.writeUtf(Component.Serializer.toJson(label));
	}

	@Override
	public boolean handleOnServer(ServerPlayer sender) {
		if (!AtlasAPI.getPlayerAtlases(sender).contains(atlasID)) {
			AntiqueAtlas.LOG.warn(
							"Player {} attempted to put marker into someone else's Atlas #{}}",
							sender.getName(), atlasID);
			return false;
		}

		AtlasAPI.getMarkerAPI().putMarker(sender.level(), visibleBeforeDiscovery, atlasID, markerType, label, x,z);
		return true;
	}
	
	@Override
	public ResourceLocation id() {
		return ID;
	}
}
