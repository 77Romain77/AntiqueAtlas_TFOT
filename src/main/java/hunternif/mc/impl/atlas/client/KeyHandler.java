package hunternif.mc.impl.atlas.client;

import com.mojang.blaze3d.platform.InputConstants;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;


public class KeyHandler {
    public static final KeyMapping ATLAS_KEYMAPPING = new KeyMapping("key.openatlas.desc", InputConstants.Type.KEYSYM, 77, "key.antiqueatlas.category");

    public static void onClientTick(Minecraft client) {
        while (ATLAS_KEYMAPPING.consumeClick()) {
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof GuiAtlas) {
                currentScreen.onClose();
            } else {
                AntiqueAtlasClientSegment.openAtlasGUI();
            }
        }
    }
}
