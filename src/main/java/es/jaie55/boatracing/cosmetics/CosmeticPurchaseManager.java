package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.util.DocumentStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent cosmetic unlocks used by the shop and the admin unlock commands.
 *
 * Storage per player is nested so YAML dots never split keys:
 * <pre>
 *   grants:
 *     &lt;uuid&gt;:
 *       trail:
 *         flame: 1730000000000   # expiresAt, 0 = permanent
 *         "*": 0                 # whole category
 *       "*":
 *         "*": 0                 # everything (includes future cosmetics)
 * </pre>
 * Lookup order: specific id, then category wildcard, then global wildcard.
 */
public class CosmeticPurchaseManager {

    public record GrantEntry(String categoryId, String cosmeticId, long expiresAt) {
        public boolean permanent() {
            return expiresAt <= 0L;
        }
    }

    private static final String ALL = "*";
    private static final String KEY_SEPARATOR = "|";

    private final BoatRacingPlugin plugin;
    private final DocumentStore store;
    private final String documentName = "cosmetic-unlocks.yml";
    private YamlConfiguration cfg;

    private final Map<UUID, Map<String, Long>> grants = new LinkedHashMap<>();

    public CosmeticPurchaseManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDocumentStore();
        reload();
    }

    public synchronized void reload() {
        grants.clear();
        cfg = new YamlConfiguration();
        try {
            if (store != null) {
                String raw = store.read(documentName);
                if (raw != null && !raw.isBlank()) cfg.loadFromString(raw);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load cosmetic unlocks (" + documentName + "): " + e.getMessage());
        }

        ConfigurationSection root = cfg.getConfigurationSection("grants");
        if (root != null) {
            for (String uuidKey : root.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(uuidKey);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().finer("Skipping invalid player id in " + documentName + ": " + uuidKey);
                    continue;
                }
                ConfigurationSection playerSection = root.getConfigurationSection(uuidKey);
                if (playerSection == null) continue;
                Map<String, Long> playerGrants = new LinkedHashMap<>();
                for (String categoryKey : playerSection.getKeys(false)) {
                    ConfigurationSection categorySection = playerSection.getConfigurationSection(categoryKey);
                    if (categorySection == null) continue;
                    for (String idKey : categorySection.getKeys(false)) {
                        long expiresAt = Math.max(0L, categorySection.getLong(idKey, 0L));
                        playerGrants.put(categoryKey.toLowerCase(java.util.Locale.ROOT)
                                + KEY_SEPARATOR + idKey.toLowerCase(java.util.Locale.ROOT), expiresAt);
                    }
                }
                if (!playerGrants.isEmpty()) grants.put(playerId, playerGrants);
            }
        }
        plugin.getLogger().fine("Loaded cosmetic unlocks for " + grants.size() + " player(s).");
    }

    public synchronized void save() {
        if (cfg == null) cfg = new YamlConfiguration();
        cfg.set("grants", null);
        for (Map.Entry<UUID, Map<String, Long>> playerEntry : grants.entrySet()) {
            if (playerEntry.getValue() == null || playerEntry.getValue().isEmpty()) continue;
            for (Map.Entry<String, Long> grant : playerEntry.getValue().entrySet()) {
                String[] parts = splitKey(grant.getKey());
                if (parts == null) continue;
                cfg.set("grants." + playerEntry.getKey() + "." + parts[0] + "." + parts[1], grant.getValue());
            }
        }
        try {
            if (store != null) store.write(documentName, cfg.saveToString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save cosmetic unlocks (" + documentName + "): " + e.getMessage());
        }
    }

    public synchronized boolean hasAccess(UUID playerId, CosmeticCategory category, String cosmeticId) {
        if (playerId == null || category == null) return false;
        Map<String, Long> playerGrants = grants.get(playerId);
        if (playerGrants == null || playerGrants.isEmpty()) return false;
        long now = System.currentTimeMillis();
        if (isValid(playerGrants.get(key(category.id(), cosmeticId)), now)) return true;
        if (isValid(playerGrants.get(key(category.id(), ALL)), now)) return true;
        return isValid(playerGrants.get(key(ALL, ALL)), now);
    }

    /**
     * @return expiry of the grant covering this cosmetic (0 = permanent, -1 = no grant).
     */
    public synchronized long expiresAt(UUID playerId, CosmeticCategory category, String cosmeticId) {
        if (playerId == null || category == null) return -1L;
        Map<String, Long> playerGrants = grants.get(playerId);
        if (playerGrants == null || playerGrants.isEmpty()) return -1L;
        long now = System.currentTimeMillis();
        Long value = playerGrants.get(key(category.id(), cosmeticId));
        if (isValid(value, now)) return value == null ? -1L : value;
        value = playerGrants.get(key(category.id(), ALL));
        if (isValid(value, now)) return value == null ? -1L : value;
        value = playerGrants.get(key(ALL, ALL));
        if (isValid(value, now)) return value == null ? -1L : value;
        return -1L;
    }

    public synchronized void grant(UUID playerId, CosmeticCategory category, String cosmeticId, long expiresAt) {
        if (playerId == null || category == null || cosmeticId == null || cosmeticId.isBlank()) return;
        put(playerId, key(category.id(), cosmeticId), expiresAt);
    }

    public synchronized void grantCategory(UUID playerId, CosmeticCategory category, long expiresAt) {
        if (playerId == null || category == null) return;
        put(playerId, key(category.id(), ALL), expiresAt);
    }

    public synchronized void grantAll(UUID playerId, long expiresAt) {
        if (playerId == null) return;
        put(playerId, key(ALL, ALL), expiresAt);
    }

    public synchronized boolean revoke(UUID playerId, CosmeticCategory category, String cosmeticId) {
        if (playerId == null || category == null) return false;
        Map<String, Long> playerGrants = grants.get(playerId);
        if (playerGrants == null) return false;
        boolean removed = playerGrants.remove(key(category.id(), cosmeticId)) != null;
        cleanupIfEmpty(playerId, playerGrants);
        if (removed) save();
        return removed;
    }

    public synchronized int revokeCategory(UUID playerId, CosmeticCategory category) {
        if (playerId == null || category == null) return 0;
        Map<String, Long> playerGrants = grants.get(playerId);
        if (playerGrants == null) return 0;
        String prefix = category.id().toLowerCase(java.util.Locale.ROOT) + KEY_SEPARATOR;
        int removed = 0;
        java.util.Iterator<Map.Entry<String, Long>> it = playerGrants.entrySet().iterator();
        while (it.hasNext()) {
            String grantKey = it.next().getKey();
            if (grantKey.startsWith(prefix)) {
                it.remove();
                removed++;
            }
        }
        cleanupIfEmpty(playerId, playerGrants);
        if (removed > 0) save();
        return removed;
    }

    public synchronized int revokeAll(UUID playerId) {
        if (playerId == null) return 0;
        Map<String, Long> removed = grants.remove(playerId);
        if (removed == null || removed.isEmpty()) return 0;
        save();
        return removed.size();
    }

    public synchronized List<GrantEntry> list(UUID playerId) {
        Map<String, Long> playerGrants = playerId == null ? null : grants.get(playerId);
        if (playerGrants == null || playerGrants.isEmpty()) return Collections.emptyList();
        List<GrantEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> entry : playerGrants.entrySet()) {
            String[] parts = splitKey(entry.getKey());
            if (parts == null) continue;
            entries.add(new GrantEntry(parts[0], parts[1], entry.getValue()));
        }
        entries.sort((a, b) -> {
            int c = a.categoryId().compareToIgnoreCase(b.categoryId());
            if (c != 0) return c;
            return a.cosmeticId().compareToIgnoreCase(b.cosmeticId());
        });
        return entries;
    }

    /** Removes expired grants; returns true when something changed. */
    public synchronized boolean cleanupExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        java.util.Iterator<Map.Entry<UUID, Map<String, Long>>> playerIt = grants.entrySet().iterator();
        while (playerIt.hasNext()) {
            Map<String, Long> playerGrants = playerIt.next().getValue();
            java.util.Iterator<Map.Entry<String, Long>> grantIt = playerGrants.entrySet().iterator();
            while (grantIt.hasNext()) {
                long expiresAt = grantIt.next().getValue();
                if (expiresAt > 0L && expiresAt <= now) {
                    grantIt.remove();
                    changed = true;
                }
            }
            if (playerGrants.isEmpty()) playerIt.remove();
        }
        if (changed) save();
        return changed;
    }

    private void put(UUID playerId, String grantKey, long expiresAt) {
        grants.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>())
                .put(grantKey.toLowerCase(java.util.Locale.ROOT), Math.max(0L, expiresAt));
        save();
    }

    private static String key(String categoryId, String cosmeticId) {
        return categoryId.toLowerCase(java.util.Locale.ROOT) + KEY_SEPARATOR
                + cosmeticId.toLowerCase(java.util.Locale.ROOT);
    }

    private static String[] splitKey(String grantKey) {
        if (grantKey == null) return null;
        int index = grantKey.indexOf(KEY_SEPARATOR);
        if (index <= 0) return null;
        return new String[]{grantKey.substring(0, index), grantKey.substring(index + 1)};
    }

    private void cleanupIfEmpty(UUID playerId, Map<String, Long> playerGrants) {
        if (playerGrants.isEmpty()) grants.remove(playerId);
    }

    private static boolean isValid(Long expiresAt, long now) {
        if (expiresAt == null) return false;
        return expiresAt <= 0L || expiresAt > now;
    }
}
