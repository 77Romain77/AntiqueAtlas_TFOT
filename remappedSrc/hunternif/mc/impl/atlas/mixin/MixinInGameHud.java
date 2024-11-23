package hunternif.mc.impl.atlas.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import hunternif.mc.impl.atlas.client.gui.ExportProgressOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
@Environment(EnvType.CLIENT)
public class MixinInGameHud {
    @Shadow
    private int scaledWidth;
    @Shadow
    private int scaledHeight;

    @Inject(at = @At("TAIL"), method = "render")
    public void draw(PoseStack matrix, float partial, CallbackInfo info) {
        ExportProgressOverlay.INSTANCE.draw(matrix, scaledWidth, scaledHeight);
    }
}
