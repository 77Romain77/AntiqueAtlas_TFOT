package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.ClientProxy;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Registers only client events, without creating a Forge network channel. */
public final class AntiqueAtlasClientBootstrap {
    private static final AntiqueAtlasClientSegment SEGMENT = new AntiqueAtlasClientSegment();
    private static final ClientProxy CLIENT_PROXY = new ClientProxy();
    private static boolean initialized;

    private AntiqueAtlasClientBootstrap() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(AntiqueAtlasClientBootstrap::clientSetup);
        modBus.addListener(AntiqueAtlasClientBootstrap::registerKeyMappings);
        modBus.addListener(AntiqueAtlasClientBootstrap::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(AntiqueAtlasClientBootstrap::onClientTick);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(SEGMENT::getConfigScreen));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientAtlasItem::registerModelProperty);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyHandler.ATLAS_KEYMAPPING);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        CLIENT_PROXY.registerClientReloadListeners(event::registerReloadListener);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AntiqueAtlasClientSegment.onClientTick();
    }
}
