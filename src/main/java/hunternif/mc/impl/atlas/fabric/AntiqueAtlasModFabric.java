package hunternif.mc.impl.atlas.fabric;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.fabricmc.api.ModInitializer;

public class AntiqueAtlasModFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        AntiqueAtlas.init();
    }
}