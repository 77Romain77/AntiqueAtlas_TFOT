package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.TileDataStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Sent from server to client to remove a custom global tile.
 * @author Hunternif
 * @author Haven King
 */
public class DeleteGlobalTileS2CPacket extends ClientboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "global_tile", "delete");
	
	ResourceKey<Level> world; 
	int chunkX, chunkZ;

	public DeleteGlobalTileS2CPacket(ResourceKey<Level> world, int chunkX, int chunkZ) {
		super(AntiqueAtlas.instance.channel);
		this.world = world;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}
	
	public DeleteGlobalTileS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
		this.chunkX = byteBuf.readVarInt();
		this.chunkZ = byteBuf.readVarInt();
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeResourceLocation(world.location());
		byteBuf.writeVarInt(chunkX);
		byteBuf.writeVarInt(chunkZ);
	}

	@Override
	public boolean runOnClient(Player sender) {
		TileDataStorage data = AntiqueAtlas.globalTileData.getData(world);
		data.removeTile(chunkX, chunkZ);
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
