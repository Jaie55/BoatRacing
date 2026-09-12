package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.BoatRacingPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Merges built-in cosmetics with the admin-configured ones, filters out cosmetics whose
 * particle does not exist on the current server version, and exposes density/shop helpers.
 */
public final class CosmeticsCatalog {

    public record TrailDefinition(
            String id,
            String particleName,
            Material icon,
            int count,
            double spread,
            double extra,
            String permission
    ) {
        public String messageKey() {
            return "cosmetics.trail." + id;
        }
    }

    private final BoatRacingPlugin plugin;
    private List<TrailDefinition> trails = new ArrayList<>();
    private final Set<String> knownTrailIds = new LinkedHashSet<>();
    private final Set<String> hiddenTrailIds = new LinkedHashSet<>();
    private final Set<String> hiddenCheckpointIds = new LinkedHashSet<>();

    public CosmeticsCatalog(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        rebuild();
    }

    /** Recomputes the catalog (call on reload so config changes and version availability refresh). */
    public void rebuild() {
        Map<String, TrailDefinition> all = new LinkedHashMap<>();
        List<String> disabled = plugin.getConfig().getStringList("cosmetics.trails.disabled");
        for (TrailType type : TrailType.values()) {
            if (disabled.contains(type.id)) continue;
            all.put(type.id, new TrailDefinition(
                    type.id, type.particleName, type.icon(), type.count, type.spread, type.extra, type.permission()));
        }

        ConfigurationSection custom = plugin.getConfig().getConfigurationSection("cosmetics.trails.custom");
        if (custom != null) {
            for (String id : custom.getKeys(false)) {
                if (id == null || id.isBlank()) continue;
                ConfigurationSection section = custom.getConfigurationSection(id);
                if (section == null) continue;
                if (!section.getBoolean("enabled", true)) {
                    all.remove(id);
                    continue;
                }
                String particle = section.getString("particle", "END_ROD");
                String iconName = section.getString("material", "GUNPOWDER");
                all.put(id, new TrailDefinition(
                        id,
                        particle,
                        es.jaie55.boatracing.util.IconResolver.resolve(iconName, Material.GUNPOWDER),
                        Math.max(1, section.getInt("count", 2)),
                        section.getDouble("spread", 0.05),
                        section.getDouble("extra", 0.0),
                        section.getString("permission", "boatracing.cosmetics.trail." + id)));
            }
        }

        knownTrailIds.clear();
        knownTrailIds.addAll(all.keySet());

        boolean hideUnsupported = !"fallback".equalsIgnoreCase(
                plugin.getConfig().getString("cosmetics.unsupported", "hide"));

        List<TrailDefinition> visible = new ArrayList<>();
        hiddenTrailIds.clear();
        for (TrailDefinition trail : all.values()) {
            boolean safe = es.jaie55.boatracing.util.ParticleResolver.isSafe(trail.particleName());
            if (safe || !hideUnsupported) {
                visible.add(trail);
            } else {
                hiddenTrailIds.add(trail.id());
            }
        }
        this.trails = visible;

        hiddenCheckpointIds.clear();
        for (CheckpointEffectType effect : CheckpointEffectType.values()) {
            if (!effect.available() && hideUnsupported) hiddenCheckpointIds.add(effect.id());
        }

        int hidden = hiddenTrailIds.size() + hiddenCheckpointIds.size();
        if (hidden > 0) {
            plugin.getLogger().info(hidden + " cosmetic(s) hidden: their particle does not exist on this server version.");
            plugin.getLogger().fine("Hidden trails: " + hiddenTrailIds + " | hidden checkpoint effects: " + hiddenCheckpointIds);
        }
    }

    /** Visible trails (unsupported ones are filtered out). */
    public List<TrailDefinition> trails() {
        return new ArrayList<>(trails);
    }

    public TrailDefinition trailById(String id) {
        if (id == null || id.isBlank()) return null;
        for (TrailDefinition trail : trails) {
            if (trail.id().equalsIgnoreCase(id)) return trail;
        }
        return null;
    }

    /** All known trail ids, including hidden ones (used to validate admin commands). */
    public Set<String> knownTrailIds() {
        return new LinkedHashSet<>(knownTrailIds);
    }

    public boolean isHidden(CosmeticCategory category, String id) {
        if (category == null || id == null) return false;
        return switch (category) {
            case TRAIL -> hiddenTrailIds.contains(id);
            case CHECKPOINT -> hiddenCheckpointIds.contains(id);
            default -> false;
        };
    }

    /** @return true when the cosmetic can be shown/rendered under the current unsupported mode. */
    public boolean isAvailable(CosmeticCategory category, String id) {
        return !isHidden(category, id);
    }

    // ------------------------------------------------------------------
    // Density
    // ------------------------------------------------------------------

    public int densityCount(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("cosmetics.density");
        int low = section != null ? Math.max(1, section.getInt("low", 1)) : 1;
        int normal = section != null ? Math.max(1, section.getInt("normal", 2)) : 2;
        int high = section != null ? Math.max(1, section.getInt("high", 4)) : 4;
        if (section == null || !section.getBoolean("enabled", true)) return normal;

        String selected = player != null && plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getParticleDensity(player.getUniqueId())
                : null;
        String level = selected != null ? selected : (section.getString("default", "normal"));
        return switch (level == null ? "normal" : level.toLowerCase(Locale.ROOT)) {
            case "low" -> low;
            case "high" -> high;
            default -> normal;
        };
    }

    public int maxVictoryRockets() {
        return Math.max(1, plugin.getConfig().getInt("cosmetics.density.max-victory-rockets", 3));
    }

    // ------------------------------------------------------------------
    // Shop / access
    // ------------------------------------------------------------------

    public boolean shopEnabled() {
        return plugin.getConfig().getBoolean("cosmetics.shop.enabled", true);
    }

    public boolean gatedByDefault() {
        return plugin.getConfig().getBoolean("cosmetics.shop.gated-by-default", true);
    }

    public boolean requireVault() {
        return plugin.getConfig().getBoolean("cosmetics.shop.require-vault", true);
    }

    public String currencyName() {
        return plugin.getConfig().getString("cosmetics.shop.currency-name", "coins");
    }

    /** @return price, 0 when free, or -1 when the cosmetic is not for sale. */
    public int price(CosmeticCategory category, String id) {
        if (category == null || id == null) return -1;
        String path = "cosmetics.shop.prices." + category.id() + "." + id.toLowerCase(Locale.ROOT);
        if (plugin.getConfig().isSet(path)) {
            return Math.max(0, plugin.getConfig().getInt(path));
        }
        return plugin.getConfig().getInt("cosmetics.shop.default-price", -1);
    }

    public boolean isFree(CosmeticCategory category, String id) {
        return price(category, id) == 0;
    }

    public boolean hasWildcard(Player player) {
        return player != null && player.hasPermission(UNLOCK_ALL_PERMISSION);
    }

    /**
     * Unified access check: permission, unlock-all wildcard, free price, legacy locked lists
     * (when gated-by-default is off) or a purchase.
     */
    public boolean hasAccess(Player player, CosmeticCategory category, String id, String permission) {
        if (player == null || category == null || id == null) return false;
        if (permission != null && (player.hasPermission(permission) || hasWildcard(player))) return true;
        if (isFree(category, id)) return true;
        if (!gatedByDefault() && !legacyLocked(category, id)) return true;
        return plugin.getPurchaseManager() != null
                && plugin.getPurchaseManager().hasAccess(player.getUniqueId(), category, id);
    }

    /**
     * Offline-safe access check used by placeholders: online players get the full check
     * (permissions + purchases + free), offline players only free prices and purchases.
     */
    public boolean hasAccess(org.bukkit.OfflinePlayer player, CosmeticCategory category, String id, String permission) {
        if (player == null || category == null || id == null) return false;
        if (player instanceof Player online) return hasAccess(online, category, id, permission);
        if (isFree(category, id)) return true;
        return plugin.getPurchaseManager() != null
                && plugin.getPurchaseManager().hasAccess(player.getUniqueId(), category, id);
    }

    private boolean legacyLocked(CosmeticCategory category, String id) {
        String path = switch (category) {
            case EFFECT -> "cosmetics.effects.locked";
            case SOUND -> "cosmetics.victory-sounds.locked";
            case CHECKPOINT -> "cosmetics.checkpoints.locked";
            default -> null;
        };
        return path != null && plugin.getConfig().getStringList(path).contains(id);
    }

    /** Wildcard permission that unlocks every cosmetic (trails, titles, effects, sounds, checkpoints). */
    public static final String UNLOCK_ALL_PERMISSION = "boatracing.cosmetics.unlock.all";
}
