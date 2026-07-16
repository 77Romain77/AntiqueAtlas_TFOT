package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.core.watcher.DeathWatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles the vanilla death notification sent by any unmodded server. */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListenerDeath {
    // TAIL runs only after vanilla has moved packet handling onto the client
    // thread. Injecting at HEAD would also execute once on the network thread.
    @Inject(method = "handlePlayerCombatKill", at = @At("TAIL"))
    private void antiqueAtlas$onLocalPlayerDeath(ClientboundPlayerCombatKillPacket packet,
                                                  CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && packet.getPlayerId() == minecraft.player.getId()) {
            DeathWatcher.onPlayerDeath(minecraft.player);
        }
    }
}
