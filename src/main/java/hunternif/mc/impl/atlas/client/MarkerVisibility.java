package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/** Marker visibility filter stored independently in the active map profile. */
public final class MarkerVisibility {
    private MarkerVisibility() {
    }

    public static boolean isVisible(ResourceLocation type) {
        return ClientMapManager.getInstance().isMarkerTypeVisible(type);
    }

    public static void setVisible(ResourceLocation type, boolean visible) {
        ClientMapManager.getInstance().setMarkerTypeVisible(type, visible);
    }

    public static void showAll() {
        ClientMapManager.getInstance().showAllMarkerTypes();
    }

    public static void hideAll(Collection<ResourceLocation> types) {
        ClientMapManager.getInstance().hideMarkerTypes(types);
    }
}
