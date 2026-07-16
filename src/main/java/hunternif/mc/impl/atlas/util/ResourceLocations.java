package hunternif.mc.impl.atlas.util;

import net.minecraft.resources.ResourceLocation;

/** Small 1.20.1 resource-location helper replacing the former library utility. */
public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static ResourceLocation parse(String value) {
        return new ResourceLocation(value);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
