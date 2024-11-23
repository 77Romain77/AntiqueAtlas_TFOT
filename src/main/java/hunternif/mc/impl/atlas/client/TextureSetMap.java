package hunternif.mc.impl.atlas.client;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/**
 * Maps texture sets to their names.
 *
 * @author Hunternif
 */
public class TextureSetMap {
    private static final TextureSetMap INSTANCE = new TextureSetMap();

    public static TextureSetMap instance() {
        return INSTANCE;
    }

    private final Map<ResourceLocation, TextureSet> map = new HashMap<>();

    public void register(TextureSet set) {
        map.put(set.name, set);
    }

    public TextureSet getByName(ResourceLocation name) {
        return map.get(name);
    }

    static public boolean isRegistered(ResourceLocation name) {
        return INSTANCE.map.containsKey(name);
    }
}
