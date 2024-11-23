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
import net.minecraft.world.level.Level;

/**
 * Sent from server to client to remove a custom global tile.
 * @author Hunternif
 * @author Haven King
 */
public class DeleteGlobalTileS2CPacket extends S2CPacket {
	public static final ResourceLocation ID = AntiqueAtlasMod.id("packet", "c2s", "global_tile", "delete");

	public DeleteGlobalTileS2CPacket(ResourceKey<Level> world, int chunkX, int chunkZ) {
		this.writeResourceLocation(world.location());
		this.writeVarInt(chunkX);
		this.writeVarInt(chunkZ);
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public static void apply(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
		ResourceKey<Level> world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		int chunkX = buf.readVarInt();
		int chunkZ = buf.readVarInt();

		context.queue(() -> {
			TileDataStorage data = AntiqueAtlasMod.globalTileData.getData(world);
			data.removeTile(chunkX, chunkZ);
		});
	}
}
