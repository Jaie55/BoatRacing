package es.jaie55.boatracing.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Discovers every boat, chest boat and raft material the running server provides, so pickers and
 * commands automatically include new wood types (for example Poplar boats in Minecraft 26.3)
 * without hardcoded lists. Legacy {@code LEGACY_*} materials are excluded.
 */
public final class BoatTypes {

    private static final List<String> PREFERRED_ORDER = List.of(
            "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK", "POPLAR");

    private static List<Material> cached;

    private BoatTypes() {
    }

    /** @return normal boats first, then chest boats, then rafts and chest rafts. */
    public static synchronized List<Material> all() {
        if (cached != null) return cached;
        List<Material> boats = new ArrayList<>();
        List<Material> chestBoats = new ArrayList<>();
        List<Material> rafts = new ArrayList<>();
        List<Material> chestRafts = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!isBoatMaterial(material)) continue;
            String name = material.name();
            if (name.endsWith("_CHEST_RAFT")) chestRafts.add(material);
            else if (name.endsWith("_CHEST_BOAT")) chestBoats.add(material);
            else if (name.endsWith("_RAFT")) rafts.add(material);
            else boats.add(material);
        }
        Comparator<Material> order = Comparator
                .comparingInt((Material material) -> woodRank(material.name()))
                .thenComparing(Material::name);
        boats.sort(order);
        chestBoats.sort(order);
        rafts.sort(order);
        chestRafts.sort(order);
        List<Material> result = new ArrayList<>(boats);
        result.addAll(chestBoats);
        result.addAll(rafts);
        result.addAll(chestRafts);
        cached = List.copyOf(result);
        return cached;
    }

    /** @return lowercase material names ready for commands and tab-completion. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Material material : all()) names.add(material.name().toLowerCase(Locale.ROOT));
        return names;
    }

    public static boolean isBoat(Material material) {
        return isBoatMaterial(material);
    }

    @SuppressWarnings("deprecation")
    private static boolean isBoatMaterial(Material material) {
        if (material == null || !material.isItem() || material.isLegacy()) return false;
        String name = material.name();
        return name.endsWith("_BOAT") || name.endsWith("_RAFT");
    }

    private static int woodRank(String name) {
        for (int i = 0; i < PREFERRED_ORDER.size(); i++) {
            if (name.startsWith(PREFERRED_ORDER.get(i))) return i;
        }
        return PREFERRED_ORDER.size();
    }
}
