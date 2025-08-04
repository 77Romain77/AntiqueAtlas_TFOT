package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "setLevel", at=@At("TAIL"))
    void AntiqueAtlas_joinWorld(ClientLevel world, CallbackInfo info)
    {
    	if (world != null)
    		ClientProxy.assignCustomBiomeTextures(world);
    }
}
