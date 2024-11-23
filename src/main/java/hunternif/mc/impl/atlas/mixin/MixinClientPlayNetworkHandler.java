package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.mixinhooks.NewServerConnectionCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Shadow
    private Minecraft client;

    @Inject(at = @At("RETURN"), method = "onGameJoin")
    public void afterGameJoin(ClientboundLoginPacket packet, CallbackInfo info) {
//        NewServerConnectionCallback.EVENT.invoker().onNewConnection(!client.hasSingleplayerServer());
    }
}