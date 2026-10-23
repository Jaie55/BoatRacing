package es.jaie55.boatracing.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent practice-only stats per track and player.
 * Stored in practice-stats.yml to keep race stats separated from competitive data.
 */
public class PracticeStatsManager {
    public static final class PlayerTrackStatsView {
        private final Long bestLapMillis;
        private final Long lastLapMillis;
        private final Long bestRunMillis;
        private final Long lastRunMillis;
        private final Map<Integer, Long> bestSectorMillis;
        private final Map<Integer, Long> lastSectorMillis;

        private PlayerTrackStatsView(
                Long bestLapMillis,
                Long lastLapMillis,
                Long bestRunMillis,
                Long lastRunMillis,
                Map<Integer, Long> bestSectorMillis,
                Map<Integer, Long> lastSectorMillis
        ) {
            this.bestLapMillis = bestLapMillis;
            this.lastLapMillis = lastLapMillis;
            this.bestRunMillis = bestRunMillis;
            this.lastRunMillis = lastRunMillis;
            this.bestSectorMillis = java.util.Collections.unmodifiableMap(bestSectorMillis);
            this.lastSectorMillis = java.util.Collections.unmodifiableMap(lastSectorMillis);
        }

        public Long getBestLapMillis() { return bestLapMillis; }
        public Long getLastLapMillis() { return lastLapMillis; }
        public Long getBestRunMillis() { return bestRunMillis; }
        public Long getLastRunMillis() { return lastRunMillis; }
        public Map<Integer, Long> getBestSectorMillis() { return bestSectorMillis; }
        public Map<Integer, Long> getLastSectorMillis() { return lastSectorMillis; }
    }

    public static final class PracticeUpdate {
        private final long valueMillis;
        private final Long previousBestMillis;
        private final long bestMillis;

        private PracticeUpdate(long valueMillis, Long previousBestMillis, long bestMillis) {
            this.valueMillis = valueMillis;
            this.previousBestMillis = previousBestMillis;
            this.bestMillis = bestMillis;
        }

        public long getValueMillis() {
            return valueMillis;
        }

        public Long getPreviousBestMillis() {
            return previousBestMillis;
        }

        public long getBestMillis() {
            return bestMillis;
        }

        public boolean isFirstRecord() {
            return previousBestMillis == null;
        }

        public boolean isImproved() {
            return previousBestMillis == null || valueMillis < previousBestMillis;
        }

        public long getImprovementMillis() {
            if (previousBestMillis == null) return 0L;
            return Math.max(0L, previousBestMillis - valueMillis);
        }

        public long getGapToBestMillis() {
            return Math.max(0L, valueMillis - bestMillis);
        }
    }

    private static final class PlayerPracticeStats {
        private Long bestLapMillis;
        private Long lastLapMillis;
        private Long bestRunMillis;
        private Long lastRunMillis;
        private final Map<Integer, Long> bestSectorMillis = new HashMap<>();
        private final Map<Integer, Long> lastSectorMillis = new HashMap<>();
    }

    private final es.jaie55.boatracing.BoatRacingPlugin plugin;
    private final DocumentStore store;
    private final AsyncDocumentSaver saver;
    private final String documentName = "practice-stats.yml";
    private YamlConfiguration cfg;

    // trackToken -> (playerId -> stats)
    private final Map<String, Map<UUID, PlayerPracticeStats>> data = new HashMap<>();

    public PracticeStatsManager(es.jaie55.boatracing.BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDocumentStore();
        this.saver = new AsyncDocumentSaver(plugin, this.store, documentName, "practice stats data");
        reload();
    }

    public synchronized void reload() {
        cfg = new YamlConfiguration();
        data.clear();

        try {
            if (store != null) {
                String raw = store.read(documentName);
                if (raw != null && !raw.isBlank()) {
                    cfg.loadFromString(raw);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load practice stats data: " + e.getMessage());
        }

        ConfigurationSection tracks = cfg.getConfigurationSection("tracks");
        if (tracks == null) return;

        for (String trackToken : tracks.getKeys(false)) {
            ConfigurationSection playersSec = tracks.getConfigurationSection(trackToken + ".players");
            if (playersSec == null) continue;

            Map<UUID, PlayerPracticeStats> perTrack = new HashMap<>();
            for (String uuidKey : playersSec.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(uuidKey);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().finer("Ignoring invalid UUID in practice-stats.yml: " + uuidKey);
                    continue;
                }

                ConfigurationSection ps = playersSec.getConfigurationSection(uuidKey);
                if (ps == null) continue;

                PlayerPracticeStats stats = new PlayerPracticeStats();
                if (ps.contains("bestLapMillis")) stats.bestLapMillis = readNonNegative(ps.get("bestLapMillis"));
                if (ps.contains("lastLapMillis")) stats.lastLapMillis = readNonNegative(ps.get("lastLapMillis"));
                if (ps.contains("bestRunMillis")) stats.bestRunMillis = readNonNegative(ps.get("bestRunMillis"));
                if (ps.contains("lastRunMillis")) stats.lastRunMillis = readNonNegative(ps.get("lastRunMillis"));

                ConfigurationSection bestSectors = ps.getConfigurationSection("bestSectorMillis");
                if (bestSectors != null) {
                    readSectorMap(bestSectors, stats.bestSectorMillis);
                }

                ConfigurationSection lastSectors = ps.getConfigurationSection("lastSectorMillis");
                if (lastSectors != null) {
                    readSectorMap(lastSectors, stats.lastSectorMillis);
                }

                perTrack.put(playerId, stats);
            }

            data.put(normalizeTrackToken(trackToken), perTrack);
        }
    }

    public synchronized void save() {
        saver.write(serialize());
    }

    private synchronized String serialize() {
        if (cfg == null) cfg = new YamlConfiguration();

        cfg.set("tracks", null);
        for (Map.Entry<String, Map<UUID, PlayerPracticeStats>> trackEntry : data.entrySet()) {
            String trackToken = trackEntry.getKey();
            for (Map.Entry<UUID, PlayerPracticeStats> playerEntry : trackEntry.getValue().entrySet()) {
                UUID playerId = playerEntry.getKey();
                PlayerPracticeStats stats = playerEntry.getValue();
                String base = "tracks." + trackToken + ".players." + playerId;

                cfg.set(base + ".bestLapMillis", stats.bestLapMillis);
                cfg.set(base + ".lastLapMillis", stats.lastLapMillis);
                cfg.set(base + ".bestRunMillis", stats.bestRunMillis);
                cfg.set(base + ".lastRunMillis", stats.lastRunMillis);

                cfg.set(base + ".bestSectorMillis", null);
                for (Map.Entry<Integer, Long> sec : stats.bestSectorMillis.entrySet()) {
                    cfg.set(base + ".bestSectorMillis." + sec.getKey(), sec.getValue());
                }

                cfg.set(base + ".lastSectorMillis", null);
                for (Map.Entry<Integer, Long> sec : stats.lastSectorMillis.entrySet()) {
                    cfg.set(base + ".lastSectorMillis." + sec.getKey(), sec.getValue());
                }
            }
        }

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

    public synchronized PracticeUpdate recordLap(UUID playerId, String trackToken, long lapMillis) {
        if (playerId == null || lapMillis < 0L) {
            return new PracticeUpdate(Math.max(0L, lapMillis), null, Math.max(0L, lapMillis));
        }
        PlayerPracticeStats stats = ensurePlayerStats(playerId, trackToken);
        Long previousBest = stats.bestLapMillis;
        stats.lastLapMillis = lapMillis;
        if (previousBest == null || lapMillis < previousBest) {
            stats.bestLapMillis = lapMillis;
        }
        long best = stats.bestLapMillis != null ? stats.bestLapMillis : lapMillis;
        save();
        return new PracticeUpdate(lapMillis, previousBest, best);
    }

    public synchronized PracticeUpdate recordRun(UUID playerId, String trackToken, long runMillis) {
        if (playerId == null || runMillis < 0L) {
            return new PracticeUpdate(Math.max(0L, runMillis), null, Math.max(0L, runMillis));
        }
        PlayerPracticeStats stats = ensurePlayerStats(playerId, trackToken);
        Long previousBest = stats.bestRunMillis;
        stats.lastRunMillis = runMillis;
        if (previousBest == null || runMillis < previousBest) {
            stats.bestRunMillis = runMillis;
        }
        long best = stats.bestRunMillis != null ? stats.bestRunMillis : runMillis;
        save();
        return new PracticeUpdate(runMillis, previousBest, best);
    }

    public synchronized PracticeUpdate recordSector(UUID playerId, String trackToken, int sectionIndex, long sectorMillis) {
        if (playerId == null || sectionIndex <= 0 || sectorMillis < 0L) {
            return new PracticeUpdate(Math.max(0L, sectorMillis), null, Math.max(0L, sectorMillis));
        }
        PlayerPracticeStats stats = ensurePlayerStats(playerId, trackToken);
        Long previousBest = stats.bestSectorMillis.get(sectionIndex);
        stats.lastSectorMillis.put(sectionIndex, sectorMillis);
        if (previousBest == null || sectorMillis < previousBest) {
            stats.bestSectorMillis.put(sectionIndex, sectorMillis);
        }
        Long bestObj = stats.bestSectorMillis.get(sectionIndex);
        long best = bestObj != null ? bestObj : sectorMillis;
        save();
        return new PracticeUpdate(sectorMillis, previousBest, best);
    }

    public synchronized Long getBestLap(UUID playerId, String trackToken) {
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.bestLapMillis : null;
    }

    public synchronized Long getLastLap(UUID playerId, String trackToken) {
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.lastLapMillis : null;
    }

    public synchronized Long getBestRun(UUID playerId, String trackToken) {
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.bestRunMillis : null;
    }

    public synchronized Long getLastRun(UUID playerId, String trackToken) {
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.lastRunMillis : null;
    }

    public synchronized Long getBestSector(UUID playerId, String trackToken, int sectionIndex) {
        if (sectionIndex <= 0) return null;
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.bestSectorMillis.get(sectionIndex) : null;
    }

    public synchronized Long getLastSector(UUID playerId, String trackToken, int sectionIndex) {
        if (sectionIndex <= 0) return null;
        PlayerPracticeStats stats = getPlayerStats(playerId, trackToken);
        return stats != null ? stats.lastSectorMillis.get(sectionIndex) : null;
    }

    public synchronized Map<String, PlayerTrackStatsView> getAllTrackStats(UUID playerId) {
        if (playerId == null) return java.util.Collections.emptyMap();

        Map<String, PlayerTrackStatsView> out = new java.util.LinkedHashMap<>();
        java.util.TreeSet<String> sortedTracks = new java.util.TreeSet<>(data.keySet());
        for (String trackToken : sortedTracks) {
            Map<UUID, PlayerPracticeStats> perTrack = data.get(trackToken);
            if (perTrack == null) continue;

            PlayerPracticeStats stats = perTrack.get(playerId);
            if (stats == null) continue;

            out.put(trackToken, new PlayerTrackStatsView(
                    stats.bestLapMillis,
                    stats.lastLapMillis,
                    stats.bestRunMillis,
                    stats.lastRunMillis,
                    new java.util.LinkedHashMap<>(new java.util.TreeMap<>(stats.bestSectorMillis)),
                    new java.util.LinkedHashMap<>(new java.util.TreeMap<>(stats.lastSectorMillis))
            ));
        }
        return out;
    }

    private PlayerPracticeStats ensurePlayerStats(UUID playerId, String trackToken) {
        String normalizedTrack = normalizeTrackToken(trackToken);
        Map<UUID, PlayerPracticeStats> perTrack = data.computeIfAbsent(normalizedTrack, k -> new HashMap<>());
        return perTrack.computeIfAbsent(playerId, k -> new PlayerPracticeStats());
    }

    private PlayerPracticeStats getPlayerStats(UUID playerId, String trackToken) {
        if (playerId == null) return null;
        Map<UUID, PlayerPracticeStats> perTrack = data.get(normalizeTrackToken(trackToken));
        if (perTrack == null) return null;
        return perTrack.get(playerId);
    }

    private static String normalizeTrackToken(String token) {
        if (token == null || token.isBlank()) return "unsaved";
        return token.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private static Long readNonNegative(Object raw) {
        if (!(raw instanceof Number n)) return null;
        long value = n.longValue();
        return value >= 0L ? value : null;
    }

    private static void readSectorMap(ConfigurationSection sec, Map<Integer, Long> out) {
        for (String key : sec.getKeys(false)) {
            int idx;
            try {
                idx = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (idx <= 0) continue;
            long value = sec.getLong(key, -1L);
            if (value >= 0L) out.put(idx, value);
        }
    }
}
