package es.jaie55.boatracing.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.Locale;

/**
 * Persistent aggregate stats used by placeholders and holograms.
 */
public class StatsManager {
    private final es.jaie55.boatracing.BoatRacingPlugin plugin;
    private final DocumentStore store;
    private final AsyncDocumentSaver saver;
    private final String documentName = "stats.yml";
    private YamlConfiguration cfg;

    private final Map<UUID, Integer> playerWins = new HashMap<>();
    private final Map<UUID, Integer> teamWins = new HashMap<>();
    private final Map<UUID, Map<Integer, Integer>> playerPositions = new HashMap<>();
    private final Map<UUID, Long> playerBestRace = new HashMap<>();
    private final Map<UUID, Long> playerBestLap = new HashMap<>();
    private final Map<String, Map<UUID, Long>> playerBestRaceByTrack = new HashMap<>();
    private final Map<String, Map<UUID, Long>> playerBestLapByTrack = new HashMap<>();
    private final Map<String, Map<Integer, Map<UUID, Long>>> playerBestRaceByTrackLaps = new HashMap<>();
    private final Map<String, Map<Integer, Map<UUID, Long>>> playerBestLapByTrackLaps = new HashMap<>();

    public StatsManager(es.jaie55.boatracing.BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDocumentStore();
        this.saver = new AsyncDocumentSaver(plugin, this.store, documentName, "stats data");
        reload();
    }

    public synchronized void reload() {
        cfg = new YamlConfiguration();
        playerWins.clear();
        teamWins.clear();
        playerPositions.clear();
        playerBestRace.clear();
        playerBestLap.clear();
        playerBestRaceByTrack.clear();
        playerBestLapByTrack.clear();
        playerBestRaceByTrackLaps.clear();
        playerBestLapByTrackLaps.clear();

        try {
            if (store != null) {
                String raw = store.read(documentName);
                if (raw != null && !raw.isBlank()) {
                    cfg.loadFromString(raw);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load stats data: " + e.getMessage());
        }

        readIntMap("playerWins", playerWins);
        readIntMap("teamWins", teamWins);
        readPositionMap("playerPositions", playerPositions);
        readLongMap("playerBestRace", playerBestRace);
        readLongMap("playerBestLap", playerBestLap);
        readTrackLongMap("playerBestRaceByTrack", playerBestRaceByTrack);
        readTrackLongMap("playerBestLapByTrack", playerBestLapByTrack);
        readTrackLapLongMap("playerBestRaceByTrackLaps", playerBestRaceByTrackLaps);
        readTrackLapLongMap("playerBestLapByTrackLaps", playerBestLapByTrackLaps);

        // Legacy compatibility: if no explicit position map exists yet,
        // bootstrap 1st-place counts from historical player wins.
        if (playerPositions.isEmpty() && !playerWins.isEmpty()) {
            for (Map.Entry<UUID, Integer> e : playerWins.entrySet()) {
                int wins = e.getValue() == null ? 0 : e.getValue();
                if (wins <= 0) continue;
                Map<Integer, Integer> per = new HashMap<>();
                per.put(1, wins);
                playerPositions.put(e.getKey(), per);
            }
            save();
        }
    }

    public synchronized void save() {
        saver.write(serialize());
    }

    private synchronized String serialize() {
        if (cfg == null) cfg = new YamlConfiguration();
        writeIntMap("playerWins", playerWins);
        writeIntMap("teamWins", teamWins);
        writePositionMap("playerPositions", playerPositions);
        writeLongMap("playerBestRace", playerBestRace);
        writeLongMap("playerBestLap", playerBestLap);
        writeTrackLongMap("playerBestRaceByTrack", playerBestRaceByTrack);
        writeTrackLongMap("playerBestLapByTrack", playerBestLapByTrack);
        writeTrackLapLongMap("playerBestRaceByTrackLaps", playerBestRaceByTrackLaps);
        writeTrackLapLongMap("playerBestLapByTrackLaps", playerBestLapByTrackLaps);
        return cfg.saveToString();
    }

    public boolean isDirty() {
        return saver.isDirty();
    }

    public void setSyncSaves(boolean sync) {
        saver.setSync(sync);
    }

    public void closeSaver() {
        saver.close();
    }

    public int getPlayerWins(UUID playerId) {
        return playerWins.getOrDefault(playerId, 0);
    }

    public int getTeamWins(UUID teamId) {
        return teamWins.getOrDefault(teamId, 0);
    }

    public Map<Integer, Integer> getPlayerPositions(UUID playerId) {
        if (playerId == null) return Collections.emptyMap();

        Map<Integer, Integer> per = playerPositions.get(playerId);
        TreeMap<Integer, Integer> out = new TreeMap<>();
        if (per != null) {
            for (Map.Entry<Integer, Integer> e : per.entrySet()) {
                Integer pos = e.getKey();
                Integer count = e.getValue();
                if (pos == null || count == null || pos < 1 || count < 1) continue;
                out.put(pos, count);
            }
        }

        if (!out.isEmpty()) return Collections.unmodifiableMap(out);

        // Extra legacy fallback when position map is still empty for this player.
        int wins = playerWins.getOrDefault(playerId, 0);
        if (wins > 0) {
            out.put(1, wins);
            return Collections.unmodifiableMap(out);
        }
        return Collections.emptyMap();
    }

    public void recordPlayerPositions(List<UUID> orderedPlayersByFinish) {
        if (orderedPlayersByFinish == null || orderedPlayersByFinish.isEmpty()) return;

        boolean changed = false;
        int position = 1;
        for (UUID playerId : orderedPlayersByFinish) {
            if (playerId != null) {
                Map<Integer, Integer> per = playerPositions.computeIfAbsent(playerId, k -> new HashMap<>());
                per.merge(position, 1, Integer::sum);
                changed = true;
            }
            position++;
        }

        if (changed) save();
    }

    public Long getPlayerBestRace(UUID playerId) {
        return playerBestRace.get(playerId);
    }

    public Long getPlayerBestRace(UUID playerId, String trackToken) {
        if (playerId == null || trackToken == null || trackToken.isBlank()) return null;
        Map<UUID, Long> perTrack = playerBestRaceByTrack.get(normalizeTrackToken(trackToken));
        if (perTrack == null) return null;
        return perTrack.get(playerId);
    }

    public Long getPlayerBestRace(UUID playerId, String trackToken, int laps) {
        if (playerId == null || trackToken == null || trackToken.isBlank()) return null;
        Map<Integer, Map<UUID, Long>> perTrack = playerBestRaceByTrackLaps.get(normalizeTrackToken(trackToken));
        if (perTrack == null) return null;
        Map<UUID, Long> perTrackLap = perTrack.get(Math.max(1, laps));
        if (perTrackLap == null) return null;
        return perTrackLap.get(playerId);
    }

    public Long getPlayerBestLap(UUID playerId) {
        return playerBestLap.get(playerId);
    }

    public Long getPlayerBestLap(UUID playerId, String trackToken) {
        if (playerId == null || trackToken == null || trackToken.isBlank()) return null;
        Map<UUID, Long> perTrack = playerBestLapByTrack.get(normalizeTrackToken(trackToken));
        if (perTrack == null) return null;
        return perTrack.get(playerId);
    }

    public Long getPlayerBestLap(UUID playerId, String trackToken, int laps) {
        if (playerId == null || trackToken == null || trackToken.isBlank()) return null;
        Map<Integer, Map<UUID, Long>> perTrack = playerBestLapByTrackLaps.get(normalizeTrackToken(trackToken));
        if (perTrack == null) return null;
        Map<UUID, Long> perTrackLap = perTrack.get(Math.max(1, laps));
        if (perTrackLap == null) return null;
        return perTrackLap.get(playerId);
    }

    public void addPlayerWin(UUID playerId) {
        if (playerId == null) return;
        playerWins.merge(playerId, 1, Integer::sum);
        save();
    }

    public void addTeamWin(UUID teamId) {
        if (teamId == null) return;
        teamWins.merge(teamId, 1, Integer::sum);
        save();
    }

    public void updatePlayerBestRace(UUID playerId, long millis) {
        if (playerId == null || millis < 0) return;
        Long cur = playerBestRace.get(playerId);
        if (cur == null || millis < cur) {
            playerBestRace.put(playerId, millis);
            save();
        }
    }

    public void updatePlayerBestRace(UUID playerId, long millis, String trackToken, int laps) {
        if (playerId == null || millis < 0) return;

        boolean changed = false;
        Long curGlobal = playerBestRace.get(playerId);
        if (curGlobal == null || millis < curGlobal) {
            playerBestRace.put(playerId, millis);
            changed = true;
        }

        if (trackToken != null && !trackToken.isBlank()) {
            String normalizedTrack = normalizeTrackToken(trackToken);

            Map<UUID, Long> perTrack = playerBestRaceByTrack.computeIfAbsent(normalizedTrack, ignored -> new HashMap<>());
            Long curTrack = perTrack.get(playerId);
            if (curTrack == null || millis < curTrack) {
                perTrack.put(playerId, millis);
                changed = true;
            }

            int normalizedLaps = Math.max(1, laps);
            Map<Integer, Map<UUID, Long>> perTrackLaps = playerBestRaceByTrackLaps.computeIfAbsent(normalizedTrack, ignored -> new HashMap<>());
            Map<UUID, Long> perTrackLap = perTrackLaps.computeIfAbsent(normalizedLaps, ignored -> new HashMap<>());
            Long curTrackLap = perTrackLap.get(playerId);
            if (curTrackLap == null || millis < curTrackLap) {
                perTrackLap.put(playerId, millis);
                changed = true;
            }
        }

        if (changed) save();
    }

    public void updatePlayerBestLap(UUID playerId, long millis) {
        if (playerId == null || millis < 0) return;
        Long cur = playerBestLap.get(playerId);
        if (cur == null || millis < cur) {
            playerBestLap.put(playerId, millis);
            save();
        }
    }

    public void updatePlayerBestLap(UUID playerId, long millis, String trackToken, int laps) {
        if (playerId == null || millis < 0) return;

        boolean changed = false;
        Long curGlobal = playerBestLap.get(playerId);
        if (curGlobal == null || millis < curGlobal) {
            playerBestLap.put(playerId, millis);
            changed = true;
        }

        if (trackToken != null && !trackToken.isBlank()) {
            String normalizedTrack = normalizeTrackToken(trackToken);

            Map<UUID, Long> perTrack = playerBestLapByTrack.computeIfAbsent(normalizedTrack, ignored -> new HashMap<>());
            Long curTrack = perTrack.get(playerId);
            if (curTrack == null || millis < curTrack) {
                perTrack.put(playerId, millis);
                changed = true;
            }

            int normalizedLaps = Math.max(1, laps);
            Map<Integer, Map<UUID, Long>> perTrackLaps = playerBestLapByTrackLaps.computeIfAbsent(normalizedTrack, ignored -> new HashMap<>());
            Map<UUID, Long> perTrackLap = perTrackLaps.computeIfAbsent(normalizedLaps, ignored -> new HashMap<>());
            Long curTrackLap = perTrackLap.get(playerId);
            if (curTrackLap == null || millis < curTrackLap) {
                perTrackLap.put(playerId, millis);
                changed = true;
            }
        }

        if (changed) save();
    }

    public Optional<Map.Entry<UUID, Integer>> topPlayerByWins() {
        return playerWins.entrySet().stream().max(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Integer>> topTeamByWins() {
        return teamWins.entrySet().stream().max(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestRace() {
        return playerBestRace.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestRace(String trackToken) {
        if (trackToken == null || trackToken.isBlank()) return Optional.empty();
        Map<UUID, Long> perTrack = playerBestRaceByTrack.get(normalizeTrackToken(trackToken));
        if (perTrack == null || perTrack.isEmpty()) return Optional.empty();
        return perTrack.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestRace(String trackToken, int laps) {
        if (trackToken == null || trackToken.isBlank()) return Optional.empty();
        Map<Integer, Map<UUID, Long>> perTrack = playerBestRaceByTrackLaps.get(normalizeTrackToken(trackToken));
        if (perTrack == null || perTrack.isEmpty()) return Optional.empty();
        Map<UUID, Long> perTrackLap = perTrack.get(Math.max(1, laps));
        if (perTrackLap == null || perTrackLap.isEmpty()) return Optional.empty();
        return perTrackLap.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestLap() {
        return playerBestLap.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestLap(String trackToken) {
        if (trackToken == null || trackToken.isBlank()) return Optional.empty();
        Map<UUID, Long> perTrack = playerBestLapByTrack.get(normalizeTrackToken(trackToken));
        if (perTrack == null || perTrack.isEmpty()) return Optional.empty();
        return perTrack.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    public Optional<Map.Entry<UUID, Long>> topPlayerByBestLap(String trackToken, int laps) {
        if (trackToken == null || trackToken.isBlank()) return Optional.empty();
        Map<Integer, Map<UUID, Long>> perTrack = playerBestLapByTrackLaps.get(normalizeTrackToken(trackToken));
        if (perTrack == null || perTrack.isEmpty()) return Optional.empty();
        Map<UUID, Long> perTrackLap = perTrack.get(Math.max(1, laps));
        if (perTrackLap == null || perTrackLap.isEmpty()) return Optional.empty();
        return perTrackLap.entrySet().stream().min(Map.Entry.comparingByValue());
    }

    private void readIntMap(String path, Map<UUID, Integer> out) {
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                out.put(id, sec.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().finer("Ignoring invalid UUID in stats.yml at " + path + "." + key);
            }
        }
    }

    private void readLongMap(String path, Map<UUID, Long> out) {
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                out.put(id, sec.getLong(key, -1L));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().finer("Ignoring invalid UUID in stats.yml at " + path + "." + key);
            }
        }
    }

    private void readTrackLongMap(String path, Map<String, Map<UUID, Long>> out) {
        ConfigurationSection root = cfg.getConfigurationSection(path);
        if (root == null) return;

        for (String trackKey : root.getKeys(false)) {
            ConfigurationSection trackSec = root.getConfigurationSection(trackKey);
            if (trackSec == null) continue;

            Map<UUID, Long> perTrack = new HashMap<>();
            for (String uuidKey : trackSec.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidKey);
                    long value = trackSec.getLong(uuidKey, -1L);
                    if (value >= 0L) perTrack.put(id, value);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().finer("Ignoring invalid UUID in stats.yml at " + path + "." + trackKey + "." + uuidKey);
                }
            }

            if (!perTrack.isEmpty()) out.put(normalizeTrackToken(trackKey), perTrack);
        }
    }

    private void readTrackLapLongMap(String path, Map<String, Map<Integer, Map<UUID, Long>>> out) {
        ConfigurationSection root = cfg.getConfigurationSection(path);
        if (root == null) return;

        for (String trackKey : root.getKeys(false)) {
            ConfigurationSection trackSec = root.getConfigurationSection(trackKey);
            if (trackSec == null) continue;

            Map<Integer, Map<UUID, Long>> perTrack = new HashMap<>();
            for (String lapKey : trackSec.getKeys(false)) {
                int laps;
                try {
                    laps = Integer.parseInt(lapKey);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (laps < 1) continue;

                ConfigurationSection lapSec = trackSec.getConfigurationSection(lapKey);
                if (lapSec == null) continue;

                Map<UUID, Long> perLap = new HashMap<>();
                for (String uuidKey : lapSec.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(uuidKey);
                        long value = lapSec.getLong(uuidKey, -1L);
                        if (value >= 0L) perLap.put(id, value);
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().finer("Ignoring invalid UUID in stats.yml at " + path + "." + trackKey + "." + lapKey + "." + uuidKey);
                    }
                }

                if (!perLap.isEmpty()) perTrack.put(laps, perLap);
            }

            if (!perTrack.isEmpty()) out.put(normalizeTrackToken(trackKey), perTrack);
        }
    }

    private void readPositionMap(String path, Map<UUID, Map<Integer, Integer>> out) {
        ConfigurationSection root = cfg.getConfigurationSection(path);
        if (root == null) return;

        for (String playerKey : root.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().finer("Ignoring invalid UUID in stats.yml at " + path + "." + playerKey);
                continue;
            }

            ConfigurationSection perSec = root.getConfigurationSection(playerKey);
            if (perSec == null) continue;

            Map<Integer, Integer> per = new HashMap<>();
            for (String posKey : perSec.getKeys(false)) {
                int pos;
                try {
                    pos = Integer.parseInt(posKey);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (pos < 1) continue;
                int count = perSec.getInt(posKey, 0);
                if (count > 0) per.put(pos, count);
            }
            if (!per.isEmpty()) out.put(playerId, per);
        }
    }

    private void writeIntMap(String path, Map<UUID, Integer> in) {
        cfg.set(path, null);
        for (Map.Entry<UUID, Integer> e : in.entrySet()) {
            cfg.set(path + "." + e.getKey(), e.getValue());
        }
    }

    private void writeLongMap(String path, Map<UUID, Long> in) {
        cfg.set(path, null);
        for (Map.Entry<UUID, Long> e : in.entrySet()) {
            cfg.set(path + "." + e.getKey(), e.getValue());
        }
    }

    private void writeTrackLongMap(String path, Map<String, Map<UUID, Long>> in) {
        cfg.set(path, null);
        for (Map.Entry<String, Map<UUID, Long>> trackEntry : in.entrySet()) {
            String trackToken = trackEntry.getKey();
            Map<UUID, Long> perTrack = trackEntry.getValue();
            if (trackToken == null || trackToken.isBlank() || perTrack == null || perTrack.isEmpty()) continue;

            for (Map.Entry<UUID, Long> playerEntry : perTrack.entrySet()) {
                UUID playerId = playerEntry.getKey();
                Long millis = playerEntry.getValue();
                if (playerId == null || millis == null || millis < 0L) continue;
                cfg.set(path + "." + trackToken + "." + playerId, millis);
            }
        }
    }

    private void writeTrackLapLongMap(String path, Map<String, Map<Integer, Map<UUID, Long>>> in) {
        cfg.set(path, null);
        for (Map.Entry<String, Map<Integer, Map<UUID, Long>>> trackEntry : in.entrySet()) {
            String trackToken = trackEntry.getKey();
            Map<Integer, Map<UUID, Long>> perTrack = trackEntry.getValue();
            if (trackToken == null || trackToken.isBlank() || perTrack == null || perTrack.isEmpty()) continue;

            for (Map.Entry<Integer, Map<UUID, Long>> lapEntry : perTrack.entrySet()) {
                Integer laps = lapEntry.getKey();
                Map<UUID, Long> perLap = lapEntry.getValue();
                if (laps == null || laps < 1 || perLap == null || perLap.isEmpty()) continue;

                for (Map.Entry<UUID, Long> playerEntry : perLap.entrySet()) {
                    UUID playerId = playerEntry.getKey();
                    Long millis = playerEntry.getValue();
                    if (playerId == null || millis == null || millis < 0L) continue;
                    cfg.set(path + "." + trackToken + "." + laps + "." + playerId, millis);
                }
            }
        }
    }

    private static String normalizeTrackToken(String token) {
        if (token == null || token.isBlank()) return "unsaved";
        return token.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private void writePositionMap(String path, Map<UUID, Map<Integer, Integer>> in) {
        cfg.set(path, null);
        for (Map.Entry<UUID, Map<Integer, Integer>> playerEntry : in.entrySet()) {
            UUID playerId = playerEntry.getKey();
            Map<Integer, Integer> per = playerEntry.getValue();
            if (playerId == null || per == null || per.isEmpty()) continue;

            for (Map.Entry<Integer, Integer> posEntry : per.entrySet()) {
                Integer pos = posEntry.getKey();
                Integer count = posEntry.getValue();
                if (pos == null || count == null || pos < 1 || count < 1) continue;
                cfg.set(path + "." + playerId + "." + pos, count);
            }
        }
    }
}
