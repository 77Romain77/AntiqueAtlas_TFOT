package hunternif.mc.impl.atlas;

import hunternif.mc.impl.atlas.client.KeyHandler;
import hunternif.mc.impl.atlas.client.ClientWorldScanner;
import hunternif.mc.impl.atlas.client.gui.GuiClientSettings;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import hunternif.mc.impl.atlas.client.gui.GuiMapProfiles;
import hunternif.mc.impl.atlas.client.storage.ClientMapManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class AntiqueAtlasClientSegment {

    private static GuiAtlas guiAtlas;
    private static final ClientWorldScanner CLIENT_SCANNER = new ClientWorldScanner();

    public static GuiAtlas getAtlasGUI() {
        if (guiAtlas == null) {
            guiAtlas = new GuiAtlas();
            guiAtlas.setMapScale(AntiqueAtlas.CONFIG.defaultScale);
        }
        return guiAtlas;
    }

    public static void resetAtlasGUI() {
        guiAtlas = null;
    }

    public static void resetClientScanner() {
        CLIENT_SCANNER.reset();
    }

    public static ClientWorldScanner.RescanRequest requestRescan() {
        return CLIENT_SCANNER.requestRescan(Minecraft.getInstance());
    }

    public static ClientWorldScanner.RescanStatus getRescanStatus() {
        return CLIENT_SCANNER.getRescanStatus();
    }

    public static void openMapProfiles(GuiAtlas parent) {
        Minecraft.getInstance().setScreen(new GuiMapProfiles(parent));
    }

    public static void openAtlasGUI(ItemStack stack) {
        openAtlasGUI(getAtlasGUI().prepareToOpen(stack));
    }

    public static void openAtlasGUI() {
        openAtlasGUI(getAtlasGUI().prepareToOpen());
    }

    private static void openAtlasGUI(GuiAtlas gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) { // In-game screen
            guiAtlas.updateL18n();
            mc.setScreen(gui);
        }
    }

	public Screen getConfigScreen(Minecraft mc, Screen previousScreen) {
		return new GuiClientSettings(previousScreen);
	}

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientMapManager.getInstance().tick(minecraft);
        CLIENT_SCANNER.tick(minecraft);
        KeyHandler.onClientTick(minecraft);
    }
}
