package hunternif.mc.impl.atlas.client;

import com.mojang.blaze3d.platform.InputConstants;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import hunternif.mc.impl.atlas.client.gui.GuiMapProfiles;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


public class KeyHandler {
    public static final KeyMapping ATLAS_KEYMAPPING = new KeyMapping("key.openatlas.desc", InputConstants.Type.KEYSYM, 77, "key.antiqueatlas.category");

    public static void onClientTick(Minecraft client) {
        while (ATLAS_KEYMAPPING.consumeClick()) {
            Screen currentScreen = client.screen;
            if (currentScreen instanceof GuiAtlas || currentScreen instanceof GuiMapProfiles) {
                currentScreen.onClose();
            } else if (currentScreen == null && client.player != null) {
                if (!ClientMapManager.getInstance().isReady()) {
                    continue;
                }
                if (Screen.hasShiftDown()) {
                    AntiqueAtlasClientSegment.openMapProfiles(null);
                    continue;
                }
                if (AntiqueAtlas.CONFIG.itemNeeded) {
                    ItemStack atlas = ClientAtlasItem.find(client.player);
                    if (atlas.isEmpty()) {
                        client.player.displayClientMessage(
                                Component.translatable("message.antiqueatlas.atlas_required"), true);
                        continue;
                    }
                    AntiqueAtlasClientSegment.openAtlasGUI(atlas);
                } else {
                    AntiqueAtlasClientSegment.openAtlasGUI();
                }
            }
        }
    }
}
