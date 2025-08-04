package hunternif.mc.impl.atlas.network.packet.s2c.play;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.TileDataStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Used to sync custom tiles from server to client.
 * @author Hunternif
 * @author Haven King
 */
public class PutGlobalTileS2CPacket extends ClientboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "global_tile", "put");
	ResourceKey<Level> world;
	List<Map.Entry<ChunkPos, ResourceLocation>> tiles;

	public PutGlobalTileS2CPacket(ResourceKey<Level> world, List<Map.Entry<ChunkPos, ResourceLocation>> tiles) {
		super(AntiqueAtlas.instance.channel);
		this.world = world;
		this.tiles = tiles;
	}
	
	public PutGlobalTileS2CPacket(ResourceKey<Level> world, int chunkX, int chunkZ, ResourceLocation tileId) {
		super(AntiqueAtlas.instance.channel);
		this.world = world;
		this.tiles = new ArrayList<>();
		this.tiles.add(new Map.Entry<ChunkPos, ResourceLocation>() {
			@Override
			public ChunkPos getKey() {
				return new ChunkPos(chunkX, chunkZ);
			}
			@Override
			public ResourceLocation getValue() {
				return tileId;
			}
			@Override
			public ResourceLocation setValue(ResourceLocation arg0) {
				return tileId;
			}
		});
	}
	
	public PutGlobalTileS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
		this.tiles = new ArrayList<>();
		int max = byteBuf.readVarInt();
		for (int i = 0; i < max; ++i) {
			ChunkPos chunk = new ChunkPos(byteBuf.readVarInt(), byteBuf.readVarInt());
			ResourceLocation loc = byteBuf.readResourceLocation();
			this.tiles.add(new Map.Entry<ChunkPos, ResourceLocation>() {
				public ChunkPos getKey() { return chunk; }
				public ResourceLocation getValue() { return loc; }
				public ResourceLocation setValue(ResourceLocation arg0) { return loc; }
			});
		}
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeResourceLocation(world.location());
		byteBuf.writeVarInt(tiles.size());

		for (Map.Entry<ChunkPos, ResourceLocation> entry : tiles) {
			byteBuf.writeVarInt(entry.getKey().x);
			byteBuf.writeVarInt(entry.getKey().z);
			byteBuf.writeResourceLocation(entry.getValue());
		}
	}

	@Override
	public boolean runOnClient(Player sender) {
		TileDataStorage data = AntiqueAtlas.globalTileData.getData(world);
		tiles.forEach(entry -> {
			data.setTile(entry.getKey().x, entry.getKey().z, entry.getValue());
		});
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
