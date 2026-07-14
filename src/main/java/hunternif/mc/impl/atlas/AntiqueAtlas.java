package hunternif.mc.impl.atlas;

import com.stereowalker.unionlib.util.VersionHelper;
import hunternif.mc.impl.atlas.client.AntiqueAtlasClientBootstrap;
import hunternif.mc.impl.atlas.config.ClientConfigStorage;
import hunternif.mc.impl.atlas.core.AtlasIdData;
import hunternif.mc.impl.atlas.core.GlobalTileDataHandler;
import hunternif.mc.impl.atlas.core.TileDataHandler;
import hunternif.mc.impl.atlas.core.scaning.WorldScanner;
import hunternif.mc.impl.atlas.marker.GlobalMarkersDataHandler;
import hunternif.mc.impl.atlas.marker.MarkersDataHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-only Forge entry point.
 *
 * <p>The upstream UnionLib {@code MinecraftMod} base class always creates a
 * mandatory SimpleChannel. This fork deliberately does not extend it, so a
 * client can join servers that do not have Antique Atlas installed.</p>
 */
@Mod(AntiqueAtlas.ID)
public class AntiqueAtlas {
    public static final String ID = "antiqueatlas";
    public static final String NAME = "Antique Atlas";
    public static final Logger LOG = LogManager.getLogger(NAME);

    public static AntiqueAtlas instance;

    /**
     * Kept only for binary/source compatibility with dormant legacy packet
     * classes. It is never initialized and no network channel is registered.
     */
    @Deprecated
    public final SimpleChannel channel = null;

    // Legacy server helpers remain available to old API classes, but no server
    // event ever invokes them in the client-only build.
    public static final WorldScanner worldScanner = new WorldScanner();
    public static final TileDataHandler tileData = new TileDataHandler();
    public static final MarkersDataHandler markersData = new MarkersDataHandler();
    public static final GlobalTileDataHandler globalTileData = new GlobalTileDataHandler();
    public static final GlobalMarkersDataHandler globalMarkersData = new GlobalMarkersDataHandler();
    public static final AntiqueAtlasConfig CONFIG = ClientConfigStorage.load();

    public AntiqueAtlas() {
        instance = this;
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AntiqueAtlasClientBootstrap::init);
    }

    public static ResourceLocation id(String... path) {
        return path[0].contains(":")
                ? VersionHelper.toLoc(String.join(".", path))
                : VersionHelper.toLoc(ID, String.join(".", path));
    }

    /** Legacy server API retained so old, unused item classes still compile. */
    public static AtlasIdData getAtlasIdData(Level world) {
        if (world.isClientSide()) {
            LOG.warn("Tried to access legacy server-only atlas id data from the client");
            return null;
        }
        return ((ServerLevel) world).getDataStorage().computeIfAbsent(
                AtlasIdData::fromNbt, AtlasIdData::new, "antiqueatlas_global_atlas_data");
    }
}
