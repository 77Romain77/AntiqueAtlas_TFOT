package hunternif.mc.impl.atlas.item;

import java.util.Collection;
import java.util.Objects;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileInfo;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DimensionUpdateS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapBanner;

public class AtlasItem extends Item {

    public AtlasItem(Item.Properties settings) {
        super(settings);
    }
    
    public static int getAtlasID(ItemStack stack) {
    	if (AntiqueAtlasItems.Components.ATLAS_ID_DATA.hasData(stack))
    		return AntiqueAtlasItems.Components.ATLAS_ID_DATA.getData(stack).id();
    	return 0;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(), getAtlasID(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player playerEntity, InteractionHand hand) {
        ItemStack stack = playerEntity.getItemInHand(hand);

        if (world.isClientSide) {
            AntiqueAtlasClientSegment.openAtlasGUI(stack);
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            return super.useOn(context);
        }

        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(BlockTags.BANNERS)) {
            AntiqueAtlasClientSegment.openAtlasGUI(context.getItemInHand());
            MapBanner mapBannerMarker = MapBanner.fromWorld(context.getLevel(), context.getClickedPos());
            AntiqueAtlasClientSegment.getAtlasGUI().openMarkerFinalizer(mapBannerMarker.getName());
            context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);

            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isEquipped) {
        AtlasData data = AntiqueAtlas.tileData.getData(stack, world);
        if (data == null || !(entity instanceof Player)) return;

        int atlasId = getAtlasID(stack);

        // On the first run send the map from the server to the client:
        Player player = (Player) entity;
        if (!world.isClientSide && !data.isSyncedToPlayer(player) && !data.isEmpty()) {
            data.syncToPlayer(atlasId, player);
        }

        // Same thing with the local markers:
        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(stack, world);
        if (!world.isClientSide && !markers.isSyncedOnPlayer(player) && !markers.isEmpty()) {
            markers.syncToPlayer(atlasId, (ServerPlayer) player);
        }

        if (!world.isClientSide) {
            // Updating map around player
            Collection<TileInfo> newTiles = AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(data, player);

            if (!newTiles.isEmpty()) {
                new DimensionUpdateS2CPacket(atlasId, player.getCommandSenderWorld().dimension(), newTiles).send(Objects.requireNonNull(player.getServer()));
            }
        }
    }

}
