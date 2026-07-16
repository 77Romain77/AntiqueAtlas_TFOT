package hunternif.mc.impl.atlas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Source-compatibility shell for obsolete client packets; no channel is registered. */
public abstract class LegacyClientboundPacket {
    public abstract void encode(FriendlyByteBuf buffer);

    public abstract boolean runOnClient(Player sender);

    public abstract ResourceLocation id();

    public final void send(ServerPlayer player) {
        // Intentionally disabled in the client-only edition.
    }

    public final void send(ServerLevel level) {
        // Intentionally disabled in the client-only edition.
    }
}
