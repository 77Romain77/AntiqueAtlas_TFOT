package hunternif.mc.impl.atlas.structure;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

public class Village {
    public static void registerMarkers() {
        if (AntiqueAtlas.CONFIG.autoVillageMarkers) {
        	StructureHandler.registerMarker(BuiltinStructures.VILLAGE_DESERT, AntiqueAtlas.id("village"), Component.literal("gui.antiqueatlas.marker.village"));
        	StructureHandler.registerMarker(BuiltinStructures.VILLAGE_PLAINS, AntiqueAtlas.id("village"), Component.literal("gui.antiqueatlas.marker.village"));
            StructureHandler.registerMarker(BuiltinStructures.VILLAGE_SAVANNA, AntiqueAtlas.id("village"), Component.literal("gui.antiqueatlas.marker.village"));
            StructureHandler.registerMarker(BuiltinStructures.VILLAGE_SNOWY, AntiqueAtlas.id("village"), Component.literal("gui.antiqueatlas.marker.village"));
            StructureHandler.registerMarker(BuiltinStructures.VILLAGE_TAIGA, AntiqueAtlas.id("village"), Component.literal("gui.antiqueatlas.marker.village"));
        }
    }
}
