package hunternif.mc.impl.atlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlDebug;
import com.stereowalker.unionlib.UnionLib;

@Mixin(GlDebug.class)
public abstract class GlDebugMixin {
	@Inject(method = "printDebugLog", at = @At(value = "HEAD"), cancellable = true)
	private static void noDebug(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
		if (UnionLib.CONFIG.no_gl_debug) {
			ci.cancel();
		}
	}
}
