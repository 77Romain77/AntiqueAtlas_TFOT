package hunternif.mc.impl.atlas.network.packet.s2c.play;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Sends markers set via API from server to client.
 * Only one dimension per packet.
 * The markers in one packet are either all global or all local.
 *
 * @author Hunternif
 * @author Haven King
 */
public class PutMarkersS2CPacket extends ClientboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "marker", "put");

    private static final int GLOBAL = -1;
    int atlasID;
	ResourceKey<Level> world;
	ListMultimap<ResourceLocation, Marker.Precursor> markersByType;

	public PutMarkersS2CPacket(int atlasID, ResourceKey<Level> world, Collection<Marker> markers) {
		super(AntiqueAtlas.instance.channel);
		this.atlasID = atlasID;
		this.world = world;
		markersByType = ArrayListMultimap.create();
		for (Marker marker : markers) {
            markersByType.put(marker.getType(), new Marker.Precursor(marker));
        }
	}
	
	public PutMarkersS2CPacket(FriendlyByteBuf byteBuf) {
		super(byteBuf, AntiqueAtlas.instance.channel);
		this.atlasID = byteBuf.readVarInt();
		this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
        int typesLength = byteBuf.readVarInt();

        this.markersByType = ArrayListMultimap.create();
        for (int i = 0; i < typesLength; ++i) {
            ResourceLocation type = byteBuf.readResourceLocation();
            int markersLength = byteBuf.readVarInt();
            for (int j = 0; j < markersLength; ++j) {
            	this.markersByType.put(type, new Marker.Precursor(byteBuf));
            }
        }
	}

	@Override
	public void encode(final FriendlyByteBuf byteBuf) {
        byteBuf.writeVarInt(atlasID);
        byteBuf.writeResourceLocation(world.location());
        byteBuf.writeVarInt(markersByType.keySet().size());

        for (ResourceLocation type : markersByType.keySet()) {
        	byteBuf.writeResourceLocation(type);
            List<Marker.Precursor> markerList = markersByType.get(type);
            byteBuf.writeVarInt(markerList.size());
            for (Marker.Precursor marker : markerList) {
                new Marker(type, world, marker).write(byteBuf);
            }
        }
	}

	@Override
	public boolean runOnClient(Player sender) {
		MarkersData markersData = atlasID == GLOBAL
                ? AntiqueAtlas.globalMarkersData.getData()
                : AntiqueAtlas.markersData.getMarkersDataCached(atlasID, world);

        for (ResourceLocation type : markersByType.keys()) {
            MarkerType markerType = MarkerType.REGISTRY.get(type);
            for (Marker.Precursor precursor : markersByType.get(type)) {
                markersData.loadMarker(new Marker(MarkerType.REGISTRY.getKey(markerType), world, precursor));
            }
        }

        AntiqueAtlasClientSegment.getAtlasGUI().updateBookmarkerList();
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
