package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Puts biome tile into one atlas.
 * @author Hunternif
 * @author Haven King
 */
public class PutTileS2CPacket extends ClientboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "tile", "put");

	int atlasID;
    ResourceKey<Level> world;
    int x, z;
    ResourceLocation tile;

	public PutTileS2CPacket(int atlasID, ResourceKey<Level> world, int x, int z, ResourceLocation tile) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.world = world;
		this.x = x;
		this.z = z;
		this.tile = tile;
	}
	
	public PutTileS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
		this.x = byteBuf.readVarInt();
		this.z = byteBuf.readVarInt();
		this.tile = byteBuf.readResourceLocation();
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeInt(atlasID);
		byteBuf.writeResourceLocation(world.location());
		byteBuf.writeVarInt(x);
		byteBuf.writeVarInt(z);
		byteBuf.writeResourceLocation(tile);
	}

	@Override
	public boolean runOnClient(Player sender) {
		AtlasData data = AntiqueAtlas.tileData.getData(atlasID, sender.level());
		data.setTile(world, x, z, tile);
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
