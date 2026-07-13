package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    void antiqueAtlas$clearClientCaches(Screen screen, CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        boolean isRemote = !minecraft.hasSingleplayerServer();

        AntiqueAtlas.tileData.onClientConnectedToServer(isRemote);
        AntiqueAtlas.markersData.onClientConnectedToServer(isRemote);
        AntiqueAtlas.globalMarkersData.onClientConnectedToServer(isRemote);
        AntiqueAtlas.globalTileData.onClientConnectedToServer(isRemote);
        AntiqueAtlasClientSegment.resetAtlasGUI();
    }
}
