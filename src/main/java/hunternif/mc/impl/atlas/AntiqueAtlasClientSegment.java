package hunternif.mc.impl.atlas;

import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.OverlayCollector;
import com.stereowalker.unionlib.api.collectors.OverlayCollector.Order;
import com.stereowalker.unionlib.api.keymaps.KeyMappingCollector;
import com.stereowalker.unionlib.client.gui.screens.config.MinecraftModConfigsScreen;
import com.stereowalker.unionlib.insert.ClientInserts;
import com.stereowalker.unionlib.mod.ClientSegment;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.client.KeyHandler;
import hunternif.mc.impl.atlas.client.gui.ExportProgressOverlay;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AntiqueAtlasClientSegment extends ClientSegment {

    private static GuiAtlas guiAtlas;

    public static GuiAtlas getAtlasGUI() {
        if (guiAtlas == null) {
            guiAtlas = new GuiAtlas();
            guiAtlas.setMapScale(AntiqueAtlas.CONFIG.defaultScale);
        }
        return guiAtlas;
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

	@Override
	public ResourceLocation getModIcon() {
		return VersionHelper.toLoc(AntiqueAtlas.ID, "pack.png");
	}
	
	@Override
	public void setupKeymappings(KeyMappingCollector collector) {
		if (!AntiqueAtlas.CONFIG.itemNeeded) {
			collector.addKeyMapping(KeyHandler.ATLAS_KEYMAPPING);
		}
	}

	@Override
	public Screen getConfigScreen(Minecraft mc, Screen previousScreen) {
		return new MinecraftModConfigsScreen(previousScreen, Component.translatable("gui.antiqueatlas.config.title"), AntiqueAtlas.CONFIG);
	}
	
	@Override
	public void registerInserts(InsertCollector collector) {
		collector.addInsert(ClientInserts.CLIENT_TICK_FINISH, ()->{
			if (!AntiqueAtlas.CONFIG.itemNeeded) {
				KeyHandler.onClientTick(Minecraft.getInstance());
			}
		});
	}
	
	@Override
	public void setupGuiOverlays(OverlayCollector collector) {
		collector.register("atlas", Order.END, (gui, renderer, scaledWidth, scaledHeight) -> {
			 ExportProgressOverlay.INSTANCE.draw(renderer.guiGraphics(), scaledWidth, scaledHeight);
		});
	}
}
