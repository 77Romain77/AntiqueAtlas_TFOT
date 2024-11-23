package hunternif.mc.impl.atlas.structure;

import net.minecraft.resources.ResourceLocation;

public class StructurePieceTile {

    private final ResourceLocation tile;
    private final int priority;

    public StructurePieceTile(ResourceLocation tile, int priority) {
        this.tile = tile;
        this.priority = priority;
    }

    public ResourceLocation getTile() {
        return tile;
    }

    public ResourceLocation getTileX() {
        return tile;
    }

    public ResourceLocation getTileZ() {
        return tile;
    }


    public int getPriority() {
        return priority;
    }
}

