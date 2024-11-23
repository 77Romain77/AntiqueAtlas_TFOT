package hunternif.mc.impl.atlas.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents an icon texture for the atlas gui, such as the arrows and center on player.
 */
@Environment(EnvType.CLIENT)
public class IconTexture extends ATexture {
    public IconTexture(ResourceLocation texture) {
        super(texture);
    }

    @Override
    public int width() {
        return 16;
    }

    @Override
    public int height() {
        return 16;
    }
}
