package hunternif.mc.impl.atlas.client;

import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.ReloadListeners;
import com.stereowalker.unionlib.api.keymaps.KeyMappingCollector;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.ClientProxy;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Registers only client events, without creating a Forge network channel. */
public final class AntiqueAtlasClientBootstrap {
    private static final AntiqueAtlasClientSegment SEGMENT = new AntiqueAtlasClientSegment();
    private static final KeyMappingCollector KEY_MAPPINGS = new KeyMappingCollector();
    private static final ReloadListeners RELOAD_LISTENERS = new ReloadListeners();
    private static final InsertCollector INSERTS = new InsertCollector();
    private static boolean initialized;

    private AntiqueAtlasClientBootstrap() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        SEGMENT.setupKeymappings(KEY_MAPPINGS);
        SEGMENT.registerInserts(INSERTS);
        new ClientProxy().initClient(RELOAD_LISTENERS);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(AntiqueAtlasClientBootstrap::registerKeyMappings);
        modBus.addListener(AntiqueAtlasClientBootstrap::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(AntiqueAtlasClientBootstrap::onClientTick);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(SEGMENT::getConfigScreen));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KEY_MAPPINGS.registerAll(event);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        RELOAD_LISTENERS.listeners().forEach(event::registerReloadListener);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        INSERTS.clientTickFinishHandlers().forEach(Runnable::run);
    }
}
