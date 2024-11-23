package hunternif.mc.impl.atlas.network.packet.s2c.play;

import dev.architectury.networking.NetworkManager;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.core.TileDataStorage;
import hunternif.mc.impl.atlas.network.packet.s2c.S2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Map;

/**
 * Used to sync custom tiles from server to client.
 * @author Hunternif
 * @author Haven King
 */
public class PutGlobalTileS2CPacket extends S2CPacket {
	public static final ResourceLocation ID = AntiqueAtlasMod.id("packet", "s2c", "global_tile", "put");

	public PutGlobalTileS2CPacket(ResourceKey<Level> world, List<Map.Entry<ChunkPos, ResourceLocation>> tiles) {
		this.writeResourceLocation(world.location());
		this.writeVarInt(tiles.size());

		for (Map.Entry<ChunkPos, ResourceLocation> entry : tiles) {
			this.writeVarInt(entry.getKey().x);
			this.writeVarInt(entry.getKey().z);
			this.writeResourceLocation(entry.getValue());
		}
	}

	public PutGlobalTileS2CPacket(ResourceKey<Level> world, int chunkX, int chunkZ, ResourceLocation tileId) {
		this.writeResourceLocation(world.location());
		this.writeVarInt(1);
		this.writeVarInt(chunkX);
		this.writeVarInt(chunkZ);
		this.writeResourceLocation(tileId);
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public static void apply(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
		ResourceKey<Level> world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		int tileCount = buf.readVarInt();

		TileDataStorage data = AntiqueAtlasMod.globalTileData.getData(world);
		for (int i = 0; i < tileCount; ++i) {
			data.setTile(buf.readVarInt(), buf.readVarInt(), buf.readResourceLocation());
		}
	}
}
