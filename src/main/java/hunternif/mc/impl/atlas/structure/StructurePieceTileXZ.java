package hunternif.mc.impl.atlas.structure;

import net.minecraft.resources.ResourceLocation;

public class StructurePieceTileXZ extends StructurePieceTile {
    private final ResourceLocation tileZ;

    public StructurePieceTileXZ(ResourceLocation tileX, ResourceLocation tileZ, int priority) {
        super(tileX, priority);
        this.tileZ = tileZ;
    }

    @Override
    public ResourceLocation getTileZ() {
        return tileZ;
    }
}
