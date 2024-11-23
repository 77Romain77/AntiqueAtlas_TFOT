package hunternif.mc.impl.atlas.item;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.marker.MarkersData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmptyAtlasItem extends Item {
    public EmptyAtlasItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player,
                                            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide()) {
            world.playSound(player, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1F, 1F);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        int atlasID = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();
        ItemStack atlasStack = AntiqueAtlasItems.getAtlasFromId(atlasID);

        AtlasData atlasData = AntiqueAtlas.tileData.getData(atlasID, world);
        atlasData.getWorldData(player.getCommandSenderWorld().dimension()).setBrowsingPositionTo(player);
        atlasData.setDirty();

        MarkersData markersData = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        markersData.setDirty();

        stack.shrink(1);
        if (stack.isEmpty()) {
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, atlasStack);
        } else {
            if (!player.getInventory().add(atlasStack.copy())) {
                player.drop(atlasStack, true);
            }

            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
    }
}
