package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Used to sync atlas data from server to client.
 *
 * @author Hunternif
 * @author Haven King
 */
public class MapDataS2CPacket extends ClientboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "map", "data");

    int atlasID;
    CompoundTag data;

	public MapDataS2CPacket(int atlasID, CompoundTag data) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.data = data;
	}
	
	public MapDataS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.data = byteBuf.readNbt();
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeVarInt(atlasID);
		byteBuf.writeNbt(data);
	}

	@Override
	public boolean runOnClient(Player sender) {
		AtlasData atlasData = AntiqueAtlas.tileData.getData(atlasID, sender.level());
        atlasData.updateFromNbt(data);

        if (AntiqueAtlas.CONFIG.doSaveBrowsingPos && Minecraft.getInstance().screen instanceof GuiAtlas atlas) {
        	atlas.loadSavedBrowsingPosition();
        }
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
