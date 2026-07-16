package hunternif.mc.impl.atlas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Source-compatibility shell for obsolete server packets.
 *
 * <p>The client-only edition deliberately registers no channel and never sends
 * these packets. Keeping the shell lets the historical API sources compile
 * without pulling a networking library into the runtime.</p>
 */
public abstract class LegacyServerboundPacket {
    public abstract void encode(FriendlyByteBuf buffer);

    public abstract boolean handleOnServer(ServerPlayer sender);

    public abstract ResourceLocation id();

    public final void send() {
        // Intentionally disabled in the client-only edition.
    }
}
