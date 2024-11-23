package hunternif.mc.impl.atlas.network.packet.s2c.play;

import dev.architectury.networking.NetworkManager;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.network.packet.s2c.S2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Puts biome tile into one atlas.
 * @author Hunternif
 * @author Haven King
 */
public class PutTileS2CPacket extends S2CPacket {
	public static final ResourceLocation ID = AntiqueAtlasMod.id("packet", "s2c", "tile", "put");

	public PutTileS2CPacket(int atlasID, ResourceKey<Level> world, int x, int z, ResourceLocation tile) {
		this.writeInt(atlasID);
		this.writeResourceLocation(world.location());
		this.writeVarInt(x);
		this.writeVarInt(z);
		this.writeResourceLocation(tile);
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public static void apply(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
		int atlasID = buf.readVarInt();
		ResourceKey<Level> world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		int x = buf.readVarInt();
		int z = buf.readVarInt();
		ResourceLocation tile = buf.readResourceLocation();

		context.queue(() -> {
			AtlasData data = AntiqueAtlasMod.tileData.getData(atlasID, context.getPlayer().getEntityWorld());
			data.setTile(world, x, z, tile);
		});
	}
}
