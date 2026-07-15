package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.config.ClientConfigStorage;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/** Persistent client-side visibility filter for marker types. */
public final class MarkerVisibility {
    private MarkerVisibility() {
    }

    public static boolean isVisible(ResourceLocation type) {
        return type == null || !AntiqueAtlas.CONFIG.hiddenMarkerTypes.contains(type.toString());
    }

    public static void setVisible(ResourceLocation type, boolean visible) {
        if (type == null) return;
        String id = type.toString();
        boolean changed = visible
                ? AntiqueAtlas.CONFIG.hiddenMarkerTypes.removeIf(id::equals)
                : addHidden(id);
        if (changed) ClientConfigStorage.save(AntiqueAtlas.CONFIG);
    }

    public static void showAll() {
        if (AntiqueAtlas.CONFIG.hiddenMarkerTypes.isEmpty()) return;
        AntiqueAtlas.CONFIG.hiddenMarkerTypes.clear();
        ClientConfigStorage.save(AntiqueAtlas.CONFIG);
    }

    public static void hideAll(Collection<ResourceLocation> types) {
        boolean changed = false;
        for (ResourceLocation type : types) {
            if (type != null) changed |= addHidden(type.toString());
        }
        if (changed) ClientConfigStorage.save(AntiqueAtlas.CONFIG);
    }

    private static boolean addHidden(String id) {
        if (AntiqueAtlas.CONFIG.hiddenMarkerTypes.contains(id)) return false;
        AntiqueAtlas.CONFIG.hiddenMarkerTypes.add(id);
        return true;
    }
}
