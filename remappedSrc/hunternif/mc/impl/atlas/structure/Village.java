package hunternif.mc.impl.atlas.structure;

import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.levelgen.feature.StructureFeature;

public class Village {
    public static void registerMarkers() {
        if (AntiqueAtlasMod.CONFIG.autoVillageMarkers) {
            StructureHandler.registerMarker(StructureFeature.VILLAGE, AntiqueAtlasMod.id("village"), new TranslatableComponent("gui.antiqueatlas.marker.village"));
        }
    }
}
