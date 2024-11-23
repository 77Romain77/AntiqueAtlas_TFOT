package hunternif.mc.impl.atlas.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import hunternif.mc.impl.atlas.AntiqueAtlasModClient;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;


@Environment(EnvType.CLIENT)
public class KeyHandler {
    public static final KeyMapping ATLAS_KEYMAPPING = new KeyMapping("key.openatlas.desc", InputConstants.Type.KEYSYM, 77, "key.antiqueatlas.category");

    public static void registerBindings() {
        KeyMappingRegistry.register(ATLAS_KEYMAPPING);
    }

    public static void onClientTick(Minecraft client) {
        while (ATLAS_KEYMAPPING.consumeClick()) {
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof GuiAtlas) {
                currentScreen.onClose();
            } else {
                AntiqueAtlasModClient.openAtlasGUI();
            }
        }
    }
}
