package hunternif.mc.impl.atlas.mixinhooks;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface NewPlayerConnectionCallback {
    Event<NewPlayerConnectionCallback> EVENT = EventFactory.createLoop();

    void onNewConnection(ServerPlayer player);
}
