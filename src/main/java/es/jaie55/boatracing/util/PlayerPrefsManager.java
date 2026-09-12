package es.jaie55.boatracing.util;

import es.jaie55.boatracing.BoatRacingPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player cosmetic preferences (trail, title...). Stored in its own document so it is
 * independent from teams (unlike racers.yml, which is wiped when leaving a team).
 *
 * Backward/forward compatible by design:
 * - Missing document => empty preferences, no errors.
 * - Unknown/removed trail or title ids are ignored by the cosmetic managers.
 * - Non-UUID document keys are skipped on load.
 */
public class PlayerPrefsManager {

    private final BoatRacingPlugin plugin;
    private final DocumentStore store;
    private final String documentName = "player-prefs.yml";
    private YamlConfiguration cfg;

    private final Map<UUID, String> trails = new HashMap<>();
    private final Map<UUID, String> titles = new HashMap<>();
    private final Map<UUID, String> effects = new HashMap<>();
    private final Map<UUID, String> victorySounds = new HashMap<>();
    private final Map<UUID, String> checkpointEffects = new HashMap<>();
    private final Map<UUID, String> densities = new HashMap<>();

    public PlayerPrefsManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDocumentStore();
        reload();
    }

    public synchronized void reload() {
        trails.clear();
        titles.clear();
        effects.clear();
        victorySounds.clear();
        checkpointEffects.clear();
        densities.clear();
        cfg = new YamlConfiguration();

        try {
            if (store != null) {
                String raw = store.read(documentName);
                if (raw != null && !raw.isBlank()) {
                    cfg.loadFromString(raw);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player preferences (" + documentName + "): " + e.getMessage());
            plugin.getLogger().warning("If this looks like a bug, run /boatracing debug and attach logs/latest.log to a report.");
        }

        readMap("trails", trails);
        readMap("titles", titles);
        readMap("effects", effects);
        readMap("victory-sounds", victorySounds);
        readMap("checkpoint-effects", checkpointEffects);
        readMap("particle-density", densities);
        plugin.getLogger().fine("Loaded player preferences: " + trails.size() + " trail(s), " + titles.size()
                + " title(s), " + effects.size() + " effect(s), " + victorySounds.size()
                + " victory sound(s), " + checkpointEffects.size() + " checkpoint effect(s), "
                + densities.size() + " density pref(s).");
    }

    public synchronized void save() {
        if (cfg == null) cfg = new YamlConfiguration();
        writeMap("trails", trails);
        writeMap("titles", titles);
        writeMap("effects", effects);
        writeMap("victory-sounds", victorySounds);
        writeMap("checkpoint-effects", checkpointEffects);
        writeMap("particle-density", densities);
        try {
            if (store != null) {
                store.write(documentName, cfg.saveToString());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save player preferences (" + documentName + "): " + e.getMessage());
        }
    }

    public synchronized String getTrail(UUID playerId) {
        return playerId == null ? null : trails.get(playerId);
    }

    public synchronized void setTrail(UUID playerId, String trailId) {
        if (playerId == null) return;
        if (trailId == null || trailId.isBlank()) {
            trails.remove(playerId);
        } else {
            trails.put(playerId, trailId);
        }
        plugin.getLogger().finer("Player preference updated: trail=" + trailId + " for " + playerId);
        save();
    }

    public synchronized String getTitle(UUID playerId) {
        return playerId == null ? null : titles.get(playerId);
    }

    public synchronized String getVictoryEffect(UUID playerId) {
        return playerId == null ? null : effects.get(playerId);
    }

    public synchronized String getCheckpointEffect(UUID playerId) {
        return playerId == null ? null : checkpointEffects.get(playerId);
    }

    public synchronized String getVictorySound(UUID playerId) {
        return playerId == null ? null : victorySounds.get(playerId);
    }

    public synchronized String getParticleDensity(UUID playerId) {
        return playerId == null ? null : densities.get(playerId);
    }

    public synchronized void setParticleDensity(UUID playerId, String densityId) {
        if (playerId == null) return;
        if (densityId == null || densityId.isBlank()) {
            densities.remove(playerId);
        } else {
            densities.put(playerId, densityId);
        }
        plugin.getLogger().finer("Player preference updated: particle-density=" + densityId + " for " + playerId);
        save();
    }

    public synchronized void setVictorySound(UUID playerId, String soundId) {
        if (playerId == null) return;
        if (soundId == null || soundId.isBlank()) {
            victorySounds.remove(playerId);
        } else {
            victorySounds.put(playerId, soundId);
        }
        plugin.getLogger().finer("Player preference updated: victory-sound=" + soundId + " for " + playerId);
        save();
    }

    public synchronized void setCheckpointEffect(UUID playerId, String effectId) {
        if (playerId == null) return;
        if (effectId == null || effectId.isBlank()) {
            checkpointEffects.remove(playerId);
        } else {
            checkpointEffects.put(playerId, effectId);
        }
        plugin.getLogger().finer("Player preference updated: checkpoint-effect=" + effectId + " for " + playerId);
        save();
    }

    public synchronized void setVictoryEffect(UUID playerId, String effectId) {
        if (playerId == null) return;
        if (effectId == null || effectId.isBlank()) {
            effects.remove(playerId);
        } else {
            effects.put(playerId, effectId);
        }
        plugin.getLogger().finer("Player preference updated: effect=" + effectId + " for " + playerId);
        save();
    }

    public synchronized void setTitle(UUID playerId, String titleId) {
        if (playerId == null) return;
        if (titleId == null || titleId.isBlank()) {
            titles.remove(playerId);
        } else {
            titles.put(playerId, titleId);
        }
        plugin.getLogger().finer("Player preference updated: title=" + titleId + " for " + playerId);
        save();
    }

    private void readMap(String path, Map<UUID, String> out) {
        ConfigurationSection section = cfg.getConfigurationSection(path);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String value = section.getString(key);
                if (value != null && !value.isBlank()) out.put(id, value);
            } catch (IllegalArgumentException ignored) {
                // Legacy/corrupt key: skip instead of failing the whole load.
                plugin.getLogger().finer("Skipping invalid player id in " + documentName + "." + path + ": " + key);
            }
        }
    }

    private void writeMap(String path, Map<UUID, String> in) {
        cfg.set(path, null);
        Map<String, String> sorted = new LinkedHashMap<>();
        in.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sorted.put(e.getKey().toString(), e.getValue()));
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            cfg.set(path + "." + e.getKey(), e.getValue());
        }
    }
}
