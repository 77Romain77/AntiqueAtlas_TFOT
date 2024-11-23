package hunternif.mc.impl.atlas.mixinhooks;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableHooks {
    public static void onTakeItem(Player player, ItemStack map, ItemStack atlas) {
        if (player.getCommandSenderWorld().isClientSide()) {
            return;
        }

        if (map.getItem() == Items.FILLED_MAP) {
            MapItemSavedData mapState = MapItem.getSavedData(map.get(DataComponents.MAP_ID), player.getCommandSenderWorld());
            if (mapState != null) {
                mapState.getDecorations().forEach(icon -> {
                    int i = 1 << mapState.scale;

                    int x = (int) ((int) (icon.x() - 0.5f) / 2f) * i + mapState.centerX;
                    int z = (int) ((int) (icon.y() - 0.5f) / 2f) * i + mapState.centerZ;

                    ResourceLocation type = null;
                    Component label = null;

                    if (icon.type() == MapDecorationTypes.RED_X) {
                        type = AntiqueAtlas.id("red_x_small");
                        label = Component.translatable("gui.antiqueatlas.marker.treasure");
                    } else if (icon.type() == MapDecorationTypes.OCEAN_MONUMENT) {
                        type = AntiqueAtlas.id("monument");
                        label = Component.translatable("gui.antiqueatlas.marker.monument");
                    } else if (icon.type() == MapDecorationTypes.WOODLAND_MANSION) {
                        type = AntiqueAtlas.id("mansion");
                        label = Component.translatable("gui.antiqueatlas.marker.mansion");
                    }

                    if (type != null) {
                        AtlasAPI.getMarkerAPI().putMarker(player.getCommandSenderWorld(), true, AtlasItem.getAtlasID(atlas), type, label, x, z);
                    }
                });
            }
        }
    }
}
