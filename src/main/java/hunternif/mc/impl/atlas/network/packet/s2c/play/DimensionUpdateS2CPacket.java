package hunternif.mc.impl.atlas.network.packet.s2c.play;

import java.util.ArrayList;
import java.util.Collection;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class DimensionUpdateS2CPacket extends ClientboundUnionPacket {
	public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "dimension", "update");
	
	int atlasID;
	ResourceKey<Level> world;
	Collection<TileInfo> tiles;

	public DimensionUpdateS2CPacket(int atlasID, ResourceKey<Level> world, Collection<TileInfo> tiles) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.world = world;
		this.tiles = tiles;
	}
	
	public DimensionUpdateS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
		int tileCount = byteBuf.readVarInt();
		this.tiles = new ArrayList<>();
		for (int i = 0; i < tileCount; ++i) {
			tiles.add(new TileInfo(
					byteBuf.readVarInt(),
					byteBuf.readVarInt(),
					byteBuf.readResourceLocation())
			);
		}
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeVarInt(atlasID);
		byteBuf.writeResourceLocation(world.location());
		byteBuf.writeVarInt(tiles.size());

		for (TileInfo tile : tiles) {
			byteBuf.writeVarInt(tile.x);
			byteBuf.writeVarInt(tile.z);
			byteBuf.writeResourceLocation(tile.id);
		}
	}

	@Override
	public boolean runOnClient(Player sender) {
		AtlasData data = AntiqueAtlas.tileData.getData(atlasID, sender.level());

		for (TileInfo info : tiles) {
			data.getWorldData(world).setTile(info.x, info.z, info.id);
		}
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
