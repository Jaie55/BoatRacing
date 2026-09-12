package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.cosmetics.CosmeticCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Win-based titles. Thresholds live in {@code cosmetics.titles.thresholds} (id -> wins).
 * Players can equip any unlocked title; when none is equipped the highest unlocked one is used.
 *
 * Backward/forward compatible: unknown ids in config or saved preferences fall back to the
 * default thresholds / highest unlocked title instead of failing.
 */
public class TitleManager {

    private static final Map<String, Integer> DEFAULT_THRESHOLDS = defaultThresholds();

    private final BoatRacingPlugin plugin;

    public TitleManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cosmetics.titles.enabled", true);
    }

    /** Ordered title ids from lowest to highest requirement. */
    public List<String> orderedTitleIds() {
        Map<String, Integer> thresholds = thresholds();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(thresholds.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) ids.add(e.getKey());
        return ids;
    }

    public int threshold(String titleId) {
        if (titleId == null) return Integer.MAX_VALUE;
        return thresholds().getOrDefault(titleId, Integer.MAX_VALUE);
    }

    public boolean isUnlocked(UUID playerId, String titleId) {
        if (playerId == null || titleId == null) return false;
        if (plugin.getStatsManager() == null) return false;
        int wins = plugin.getStatsManager().getPlayerWins(playerId);
        return wins >= threshold(titleId);
    }

    /** Highest title the player has unlocked for the given wins. */
    public String titleForWins(int wins) {
        Map<String, Integer> thresholds = thresholds();
        String best = null;
        int bestRequirement = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> e : thresholds.entrySet()) {
            if (wins >= e.getValue() && e.getValue() > bestRequirement) {
                bestRequirement = e.getValue();
                best = e.getKey();
            }
        }
        if (best == null) {
            // No threshold met (e.g. all thresholds > 0): use the lowest one for display purposes.
            return orderedTitleIds().stream().findFirst().orElse(null);
        }
        return best;
    }

    /** Equipped title if unlocked, otherwise the automatic title for the player's wins. */
    public String resolvedTitle(UUID playerId) {
        if (!isEnabled() || playerId == null) return null;
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getTitle(playerId)
                : null;
        if (equipped != null && threshold(equipped) != Integer.MAX_VALUE) {
            boolean unlocked = isUnlocked(playerId, equipped);
            if (!unlocked) {
                org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(playerId);
                unlocked = online != null && plugin.getCosmeticsCatalog() != null
                        && plugin.getCosmeticsCatalog().hasAccess(online, CosmeticCategory.TITLE, equipped,
                                "boatracing.cosmetics.title." + equipped);
            }
            if (unlocked) return equipped;
        }
        int wins = plugin.getStatsManager() != null ? plugin.getStatsManager().getPlayerWins(playerId) : 0;
        String title = titleForWins(wins);
        plugin.getLogger().finer("Resolved title for " + playerId + ": " + title + " (wins=" + wins + ")");
        return title;
    }

    /** Localized display name for a title id. Falls back to the raw id when no message exists. */
    public String displayName(String titleId) {
        if (titleId == null) return "";
        return plugin.msg().get("cosmetics.title." + titleId, "id", titleId);
    }

    private Map<String, Integer> thresholds() {
        Map<String, Integer> out = new LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("cosmetics.titles.thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object raw = section.get(key);
                if (raw instanceof Number n) out.put(key, Math.max(0, n.intValue()));
            }
        }
        if (out.isEmpty()) out.putAll(DEFAULT_THRESHOLDS);
        return out;
    }

    private static Map<String, Integer> defaultThresholds() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("rookie", 0);
        map.put("pro", 5);
        map.put("elite", 25);
        map.put("legend", 100);
        return map;
    }
}
