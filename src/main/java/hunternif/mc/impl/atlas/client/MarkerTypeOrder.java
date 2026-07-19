package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Applies the client-configured marker order consistently across the atlas UI. */
public final class MarkerTypeOrder {
    private static final Map<String, Integer> CONFIGURED_RANKS = createConfiguredRanks();
    private static final Comparator<ResourceLocation> ID_COMPARATOR = Comparator
            .comparingInt(MarkerTypeOrder::rank)
            .thenComparing(ResourceLocation::toString);
    private static final Comparator<MarkerType> TYPE_COMPARATOR = Comparator.comparing(
            MarkerType.REGISTRY::getKey,
            Comparator.nullsLast(ID_COMPARATOR));

    private MarkerTypeOrder() {
    }

    public static Comparator<ResourceLocation> idComparator() {
        return ID_COMPARATOR;
    }

    public static List<MarkerType> sortedNonTechnicalTypes() {
        List<MarkerType> types = new ArrayList<>();
        for (MarkerType type : MarkerType.REGISTRY) {
            if (!type.isTechnical() && MarkerType.REGISTRY.getKey(type) != null) {
                types.add(type);
            }
        }
        types.sort(TYPE_COMPARATOR);
        return types;
    }

    private static int rank(ResourceLocation id) {
        return CONFIGURED_RANKS.getOrDefault(id.toString(), Integer.MAX_VALUE);
    }

    private static Map<String, Integer> createConfiguredRanks() {
        Map<String, Integer> ranks = new HashMap<>();
        List<String> configuredOrder = AntiqueAtlas.CONFIG.markerTypeOrder;
        for (int index = 0; index < configuredOrder.size(); index++) {
            ranks.putIfAbsent(configuredOrder.get(index), index);
        }
        return ranks;
    }
}
