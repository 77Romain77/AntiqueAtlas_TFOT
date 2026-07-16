package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.client.ClientAtlasItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selects the atlas model for recognised vanilla books in every item GUI. */
@Mixin(net.minecraft.client.renderer.entity.ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void antiqueAtlas$useAtlasModel(ItemStack stack, Level level, LivingEntity entity, int seed,
                                            CallbackInfoReturnable<BakedModel> callback) {
        if (!ClientAtlasItem.isAtlas(stack)) return;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ClientAtlasItem.ATLAS_MODEL);
        if (model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            callback.setReturnValue(model);
        }
    }
}
