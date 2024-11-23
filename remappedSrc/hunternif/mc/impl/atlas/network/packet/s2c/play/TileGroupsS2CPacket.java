package hunternif.mc.impl.atlas.network.packet.s2c.play;

import dev.architectury.networking.NetworkManager;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileGroup;
import hunternif.mc.impl.atlas.core.WorldData;
import hunternif.mc.impl.atlas.network.packet.s2c.S2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;


/**
 * Syncs tile groups to the client.
 *
 * @author Hunternif
 * @author Haven King
 */
public class TileGroupsS2CPacket extends S2CPacket {
    public static final int TILE_GROUPS_PER_PACKET = 100;
    public static final ResourceLocation ID = AntiqueAtlasMod.id("packet", "s2c", "tile", "groups");

    public TileGroupsS2CPacket(int atlasID, ResourceKey<Level> world, List<TileGroup> tileGroups) {
        this.writeVarInt(atlasID);
        this.writeResourceLocation(world.location());
        this.writeVarInt(tileGroups.size());

        for (TileGroup tileGroup : tileGroups) {
            this.writeNbt(tileGroup.writeToNBT(new CompoundTag()));
        }
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public static void apply(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int atlasID = buf.readVarInt();
        ResourceKey<Level> world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        int length = buf.readVarInt();
        List<TileGroup> tileGroups = new ArrayList<>(length);

        for (int i = 0; i < length; ++i) {
            CompoundTag tag = buf.readNbt();

            if (tag != null) {
                tileGroups.add(TileGroup.fromNBT(tag));
            }
        }


        context.queue(() -> {
            AtlasData atlasData = AntiqueAtlasMod.tileData.getData(atlasID, context.getPlayer().getEntityWorld());
            WorldData worldData = atlasData.getWorldData(world);
            for (TileGroup t : tileGroups) {
                worldData.putTileGroup(t);
            }
        });
    }
}
