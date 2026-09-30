package es.jaie55.boatracing.util;

import es.jaie55.boatracing.BoatRacingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists where racers were before a race so that a crash/restart mid-race can send them back.
 */
public final class CrashRecoveryStore {

    private final BoatRacingPlugin plugin;
    private final Map<UUID, Location> pending = new LinkedHashMap<>();

    public CrashRecoveryStore(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void persist(Map<UUID, Location> homes, String trackName) {
        pending.clear();
        if (homes != null) {
            for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue().getWorld() != null) {
                    pending.put(entry.getKey(), entry.getValue().clone());
                }
            }
        }
        save(trackName);
    }

    /** Returns true when the player has a location pending from an interrupted race. */
    public boolean hasPending(UUID playerId) {
        return playerId != null && pending.containsKey(playerId);
    }

    /** Removes and returns the saved location for the player, persisting the removal. */
    public Location take(UUID playerId) {
        Location location = pending.remove(playerId);
        if (location != null) save(null);
        return location;
    }

    public void clear() {
        if (pending.isEmpty()) return;
        pending.clear();
        save(null);
    }

    private void save(String trackName) {
        DocumentStore store = plugin.getDocumentStore();
        if (store == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        if (trackName != null) yaml.set("track", trackName);
        for (Map.Entry<UUID, Location> entry : pending.entrySet()) {
            Location location = entry.getValue();
            String base = "players." + entry.getKey();
            yaml.set(base + ".world", location.getWorld().getName());
            yaml.set(base + ".x", location.getX());
            yaml.set(base + ".y", location.getY());
            yaml.set(base + ".z", location.getZ());
            yaml.set(base + ".yaw", location.getYaw());
            yaml.set(base + ".pitch", location.getPitch());
        }
        try {
            store.write("race-recovery.yml", yaml.saveToString());
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to persist race recovery data: " + exception.getMessage());
        }
    }

    private void load() {
        DocumentStore store = plugin.getDocumentStore();
        if (store == null) return;
        try {
            String content = store.read("race-recovery.yml");
            if (content == null || content.isBlank()) return;
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(content);
            ConfigurationSection section = yaml.getConfigurationSection("players");
            if (section == null) return;
            for (String key : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    World world = Bukkit.getWorld(section.getString(key + ".world", ""));
                    if (world == null) continue;
                    Location location = new Location(world,
                            section.getDouble(key + ".x"),
                            section.getDouble(key + ".y"),
                            section.getDouble(key + ".z"),
                            (float) section.getDouble(key + ".yaw"),
                            (float) section.getDouble(key + ".pitch"));
                    pending.put(id, location);
                } catch (Exception ignored) {
                }
            }
            if (!pending.isEmpty()) {
                plugin.getLogger().warning("Interrupted race detected for " + pending.size()
                        + " player(s); they will be returned to their previous location when they join.");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to read race recovery data: " + exception.getMessage());
        }
    }
}
