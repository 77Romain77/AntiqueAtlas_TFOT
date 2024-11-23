package hunternif.mc.impl.atlas.api.impl;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.api.MarkerAPI;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.c2s.play.DeleteMarkerC2SPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteMarkerS2CPacket;
import hunternif.mc.impl.atlas.network.packet.s2c.play.PutMarkersS2CPacket;
import hunternif.mc.impl.atlas.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class MarkerApiImpl implements MarkerAPI {
    /**
     * Used in place of atlasID to signify that the marker is global.
     */
    private static final int GLOBAL = -1;

    @Nullable
    @Override
    public Marker putMarker(@NotNull Level world, boolean visibleAhead, int atlasID, ResourceLocation marker, Component label, int x, int z) {
        return doPutMarker(world, visibleAhead, atlasID, marker, label, x, z);
    }

    @Nullable
    @Override
    public Marker putGlobalMarker(@NotNull Level world, boolean visibleAhead, ResourceLocation marker, Component label, int x, int z) {
        return doPutMarker(world, visibleAhead, GLOBAL, marker, label, x, z);
    }

    private Marker doPutMarker(Level world, boolean visibleAhead, int atlasID, ResourceLocation markerId, Component label, int x, int z) {
        Marker marker = null;
        if (!world.isClientSide && world.getServer() != null) {
            MarkersData data = atlasID == GLOBAL
                    ? AntiqueAtlas.globalMarkersData.getData()
                    : AntiqueAtlas.markersData.getMarkersData(atlasID, world);

            marker = data.createAndSaveMarker(markerId, world.dimension(), x, z, visibleAhead, label);
            new PutMarkersS2CPacket(atlasID, world.dimension(), Collections.singleton(marker)).send((ServerLevel) world);
        }

        return marker;
    }

    @Override
    public void deleteMarker(@NotNull Level world, int atlasID, int markerID) {
        doDeleteMarker(world, atlasID, markerID);
    }

    @Override
    public void deleteGlobalMarker(@NotNull Level world, int markerID) {
        doDeleteMarker(world, GLOBAL, markerID);
    }

    private void doDeleteMarker(Level world, int atlasID, int markerID) {
        if (world.isClientSide) {
            if (atlasID == GLOBAL) {
                Log.warn("Client tried to delete a global marker!");
            } else {
                new DeleteMarkerC2SPacket(atlasID, markerID).send();
            }
        } else {
            MarkersData data = atlasID == GLOBAL ?
                    AntiqueAtlas.globalMarkersData.getData() :
                    AntiqueAtlas.markersData.getMarkersData(atlasID, world);
            data.removeMarker(markerID);

            new DeleteMarkerS2CPacket(atlasID, markerID).send(((ServerLevel) world).getServer());
        }
    }
}
