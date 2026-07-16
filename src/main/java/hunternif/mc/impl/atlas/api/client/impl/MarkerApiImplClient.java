package hunternif.mc.impl.atlas.api.client.impl;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.api.MarkerAPI;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import hunternif.mc.impl.atlas.marker.Marker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MarkerApiImplClient implements MarkerAPI {
    @Nullable
    @Override
    public Marker putMarker(@NotNull Level world, boolean visibleAhead, int atlasID, ResourceLocation marker, Component label, int x, int z) {
        Marker created = ClientMapManager.getInstance().createMarker(
                world.dimension(), marker, label, x, z, visibleAhead);
        AntiqueAtlasClientSegment.getAtlasGUI().updateBookmarkerList();
        return created;
    }

    @Nullable
    @Override
    public Marker putGlobalMarker(@NotNull Level world, boolean visibleAhead, ResourceLocation marker, Component label, int x, int z) {
        AntiqueAtlas.LOG.warn("Client tried to add a global marker");

        return null;
    }

    @Override
    public void deleteMarker(@NotNull Level world, int atlasID, int markerID) {
        if (ClientMapManager.getInstance().deleteMarker(markerID)) {
            AntiqueAtlasClientSegment.getAtlasGUI().updateBookmarkerList();
        }
    }

    @Override
    public void deleteGlobalMarker(@NotNull Level world, int markerID) {
        AntiqueAtlas.LOG.warn("Client tried to delete a global marker");
    }
}
