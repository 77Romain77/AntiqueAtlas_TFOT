package hunternif.mc.impl.atlas.structure;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

public class EndCity {

    public static void registerMarkers() {
        StructureHandler.registerMarker(BuiltinStructures.END_CITY, AntiqueAtlas.id("end_city"), Component.literal(""));
    }

}
