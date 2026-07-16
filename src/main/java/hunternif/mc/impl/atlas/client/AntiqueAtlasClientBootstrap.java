package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
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
        modBus.addListener(AntiqueAtlasClientBootstrap::registerModels);
        modBus.addListener(AntiqueAtlasClientBootstrap::registerKeyMappings);
        modBus.addListener(AntiqueAtlasClientBootstrap::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(AntiqueAtlasClientBootstrap::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(AntiqueAtlasClientBootstrap::onRightClickItem);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(SEGMENT::getConfigScreen));
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(ClientAtlasItem.ATLAS_MODEL);
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

    /** Opens a recognised atlas after block/entity interactions had a chance to consume right-click. */
    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;

        ItemStack stack = event.getItemStack();
        if (!ClientAtlasItem.isAtlas(stack)) return;

        if (KeyHandler.toggleAtlas(Minecraft.getInstance(), stack)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
