package hunternif.mc.impl.atlas.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Lightweight standalone client config with no network hooks. */
public final class ClientConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("antiqueatlas-client.json");

    private ClientConfigStorage() {
    }

    public static AntiqueAtlasConfig load() {
        AntiqueAtlasConfig config = new AntiqueAtlasConfig();
        if (Files.isRegularFile(FILE)) {
            try {
                AntiqueAtlasConfig loaded = GSON.fromJson(
                        Files.readString(FILE, StandardCharsets.UTF_8), AntiqueAtlasConfig.class);
                if (loaded != null) config = loaded;
            } catch (Exception exception) {
                AntiqueAtlas.LOG.error("Could not read client config {}", FILE, exception);
            }
        }
        validate(config);
        save(config);
        return config;
    }

    public static void save(AntiqueAtlasConfig config) {
        validate(config);
        try {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            AntiqueAtlas.LOG.error("Could not save client config {}", FILE, exception);
        }
    }

    private static void validate(AntiqueAtlasConfig config) {
        config.scanRadius = clamp(config.scanRadius, 0, 32);
        config.clientScanBudget = clamp(config.clientScanBudget, 1, 64);
        config.markerLimit = Math.max(0, config.markerLimit);
        config.maxMaps = Math.max(1, config.maxMaps);
        config.tileSize = clamp(config.tileSize, 1, 10);
        config.markerSize = Math.max(0, config.markerSize);
        config.playerIconWidth = Math.max(0, config.playerIconWidth);
        config.playerIconHeight = Math.max(0, config.playerIconHeight);
        config.minScale = Math.max(1.0 / 1024.0, config.minScale);
        config.maxScale = Math.max(config.minScale, config.maxScale);
        config.defaultScale = Math.max(config.minScale, Math.min(config.maxScale, config.defaultScale));

        if (config.markerTypeOrder == null) {
            config.markerTypeOrder = new ArrayList<>(AntiqueAtlasConfig.DEFAULT_MARKER_TYPE_ORDER);
        } else {
            Set<String> normalizedOrder = new LinkedHashSet<>();
            for (String configuredId : config.markerTypeOrder) {
                if (configuredId == null) continue;
                ResourceLocation id = ResourceLocation.tryParse(configuredId.trim());
                if (id != null) normalizedOrder.add(id.toString());
            }
            config.markerTypeOrder = new ArrayList<>(normalizedOrder);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
