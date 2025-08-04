package hunternif.mc.impl.atlas.network.packet.s2c.play;

import java.util.ArrayList;
import java.util.List;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileGroup;
import hunternif.mc.impl.atlas.core.WorldData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;


/**
 * Syncs tile groups to the client.
 *
 * @author Hunternif
 * @author Haven King
 */
public class TileGroupsS2CPacket extends ClientboundUnionPacket {
    public static final int TILE_GROUPS_PER_PACKET = 100;
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "tile", "groups");
    int atlasID;
    ResourceKey<Level> world;
    List<TileGroup> tileGroups;

	public TileGroupsS2CPacket(int atlasID, ResourceKey<Level> world, List<TileGroup> tileGroups) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.world = world;
		this.tileGroups = tileGroups;
	}
	
	public TileGroupsS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
        int length = byteBuf.readVarInt();
        this.tileGroups = new ArrayList<>(length);

        for (int i = 0; i < length; ++i) {
            CompoundTag tag = byteBuf.readNbt();

            if (tag != null) {
                tileGroups.add(TileGroup.fromNBT(tag));
            }
        }
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
		byteBuf.writeVarInt(atlasID);
		byteBuf.writeResourceLocation(world.location());
		byteBuf.writeVarInt(tileGroups.size());

        for (TileGroup tileGroup : tileGroups) {
        	byteBuf.writeNbt(tileGroup.writeToNBT(new CompoundTag()));
        }
	}

	@Override
	public boolean runOnClient(Player sender) {
		AtlasData atlasData = AntiqueAtlas.tileData.getData(atlasID, sender.level());
        WorldData worldData = atlasData.getWorldData(world);
        for (TileGroup t : tileGroups) {
            worldData.putTileGroup(t);
        }
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
