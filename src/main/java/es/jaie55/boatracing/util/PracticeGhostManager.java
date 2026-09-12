package es.jaie55.boatracing.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores practice ghosts per track and lap count.
 * Each track+laps scope keeps only the best run ghost.
 */
public final class PracticeGhostManager {
    public static final class GhostSample {
        private final long timeMs;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public GhostSample(long timeMs, double x, double y, double z, float yaw, float pitch) {
            this.timeMs = Math.max(0L, timeMs);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public long getTimeMs() { return timeMs; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }

    public static final class GhostPath {
        private final UUID ownerUuid;
        private final String ownerName;
        private final String worldName;
        private final String boatType;
        private final long bestRunMillis;
        private final List<GhostSample> samples;
        private final String source;

        private GhostPath(
                UUID ownerUuid,
                String ownerName,
                String worldName,
                String boatType,
                long bestRunMillis,
                List<GhostSample> samples,
                String source
        ) {
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.worldName = worldName;
            this.boatType = boatType;
            this.bestRunMillis = bestRunMillis;
            this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
            this.source = source == null || source.isBlank() ? "practice" : source;
        }

        public UUID getOwnerUuid() { return ownerUuid; }
        public String getOwnerName() { return ownerName; }
        public String getWorldName() { return worldName; }
        public String getBoatType() { return boatType; }
        public long getBestRunMillis() { return bestRunMillis; }
        public List<GhostSample> getSamples() { return samples; }
        /** "practice" or "race". */
        public String getSource() { return source; }
    }

    private final es.jaie55.boatracing.BoatRacingPlugin plugin;
    private final DocumentStore store;
    private final String documentName = "practice-ghosts.yml";
    private YamlConfiguration cfg;

    private final Map<String, Map<Integer, GhostPath>> bestGhosts = new HashMap<>();

    public PracticeGhostManager(es.jaie55.boatracing.BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDocumentStore();
        reload();
    }

    public synchronized void reload() {
        cfg = new YamlConfiguration();
        bestGhosts.clear();

        try {
            if (store != null) {
                String raw = store.read(documentName);
                if (raw != null && !raw.isBlank()) {
                    cfg.loadFromString(raw);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load practice ghost data: " + e.getMessage());
        }

        ConfigurationSection tracks = cfg.getConfigurationSection("tracks");
        if (tracks == null) return;

        for (String trackTokenRaw : tracks.getKeys(false)) {
            String trackToken = normalizeTrackToken(trackTokenRaw);
            ConfigurationSection lapsSection = tracks.getConfigurationSection(trackTokenRaw + ".laps");
            if (lapsSection == null) continue;

            Map<Integer, GhostPath> perLaps = new HashMap<>();
            for (String lapKey : lapsSection.getKeys(false)) {
                int laps;
                try {
                    laps = Integer.parseInt(lapKey);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (laps < 1) continue;

                ConfigurationSection entry = lapsSection.getConfigurationSection(lapKey);
                if (entry == null) continue;

                long bestRunMillis = entry.getLong("bestRunMillis", -1L);
                if (bestRunMillis < 0L) continue;

                String ownerName = entry.getString("ownerName", "ghost");
                String worldName = entry.getString("world", "");
                String boatType = entry.getString("boatType", "OAK_BOAT");
                String source = entry.getString("source", "practice");
                UUID ownerUuid = parseUuid(entry.getString("ownerUuid", null));

                List<GhostSample> samples = sanitizeSamples(readSamples(entry.getList("samples")));
                if (samples.isEmpty()) continue;

                perLaps.put(laps, new GhostPath(ownerUuid, ownerName, worldName, boatType, bestRunMillis, samples, source));
            }

            if (!perLaps.isEmpty()) bestGhosts.put(trackToken, perLaps);
        }
    }

    public synchronized void save() {
        if (cfg == null) cfg = new YamlConfiguration();

        cfg.set("tracks", null);
        for (Map.Entry<String, Map<Integer, GhostPath>> trackEntry : bestGhosts.entrySet()) {
            String trackToken = trackEntry.getKey();
            Map<Integer, GhostPath> perLaps = trackEntry.getValue();
            if (trackToken == null || trackToken.isBlank() || perLaps == null || perLaps.isEmpty()) continue;

            for (Map.Entry<Integer, GhostPath> lapEntry : perLaps.entrySet()) {
                Integer laps = lapEntry.getKey();
                GhostPath path = lapEntry.getValue();
                if (laps == null || laps < 1 || path == null || path.getSamples().isEmpty()) continue;

                String base = "tracks." + trackToken + ".laps." + laps;
                cfg.set(base + ".bestRunMillis", path.getBestRunMillis());
                cfg.set(base + ".ownerUuid", path.getOwnerUuid() != null ? path.getOwnerUuid().toString() : null);
                cfg.set(base + ".ownerName", path.getOwnerName());
                cfg.set(base + ".world", path.getWorldName());
                cfg.set(base + ".boatType", path.getBoatType());
                cfg.set(base + ".source", path.getSource());
                cfg.set(base + ".samples", writeSamples(path.getSamples()));
            }
        }

        try {
            if (store != null) {
                store.write(documentName, cfg.saveToString());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save practice ghost data: " + e.getMessage());
        }
    }

    public synchronized GhostPath getBestGhost(String trackToken, int laps) {
        Map<Integer, GhostPath> perLaps = bestGhosts.get(normalizeTrackToken(trackToken));
        if (perLaps == null) return null;
        GhostPath path = perLaps.get(Math.max(1, laps));
        if (path == null) return null;
        return copyPath(path);
    }

    public synchronized boolean updateBestGhost(
            String trackToken,
            int laps,
            UUID ownerUuid,
            String ownerName,
            String worldName,
            String boatType,
            long runMillis,
            List<GhostSample> samples
    ) {
        return updateBestGhost(trackToken, laps, ownerUuid, ownerName, worldName, boatType, runMillis, samples, "practice");
    }

    public synchronized boolean updateBestGhost(
            String trackToken,
            int laps,
            UUID ownerUuid,
            String ownerName,
            String worldName,
            String boatType,
            long runMillis,
            List<GhostSample> samples,
            String source
    ) {
        if (runMillis < 0L || samples == null || samples.isEmpty()) return false;

        int normalizedLaps = Math.max(1, laps);
        String normalizedTrack = normalizeTrackToken(trackToken);
        List<GhostSample> sanitized = sanitizeSamples(samples);
        if (sanitized.isEmpty()) return false;

        Map<Integer, GhostPath> perLaps = bestGhosts.computeIfAbsent(normalizedTrack, ignored -> new HashMap<>());
        GhostPath current = perLaps.get(normalizedLaps);
        if (current != null && runMillis >= current.getBestRunMillis()) {
            return false;
        }

        GhostPath updated = new GhostPath(
                ownerUuid,
                ownerName == null || ownerName.isBlank() ? "ghost" : ownerName,
                worldName == null ? "" : worldName,
                boatType == null || boatType.isBlank() ? "OAK_BOAT" : boatType,
                runMillis,
                sanitized,
                source
        );
        perLaps.put(normalizedLaps, updated);
        save();
        return true;
    }

    private static GhostPath copyPath(GhostPath path) {
        return new GhostPath(
                path.getOwnerUuid(),
                path.getOwnerName(),
                path.getWorldName(),
                path.getBoatType(),
                path.getBestRunMillis(),
                path.getSamples(),
                path.getSource()
        );
    }

    private static String normalizeTrackToken(String token) {
        if (token == null || token.isBlank()) return "unsaved";
        return token.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<GhostSample> readSamples(List<?> rawSamples) {
        if (rawSamples == null || rawSamples.isEmpty()) return Collections.emptyList();

        List<GhostSample> out = new ArrayList<>();
        for (Object entry : rawSamples) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            Long time = asLong(map.get("t"));
            Double x = asDouble(map.get("x"));
            Double y = asDouble(map.get("y"));
            Double z = asDouble(map.get("z"));
            Float yaw = asFloat(map.get("yaw"));
            Float pitch = asFloat(map.get("pitch"));

            if (time == null || x == null || y == null || z == null || yaw == null || pitch == null) continue;
            out.add(new GhostSample(time, x, y, z, yaw, pitch));
        }
        return out;
    }

    private static List<Map<String, Object>> writeSamples(List<GhostSample> samples) {
        if (samples == null || samples.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> out = new ArrayList<>(samples.size());
        for (GhostSample sample : samples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("t", sample.getTimeMs());
            row.put("x", sample.getX());
            row.put("y", sample.getY());
            row.put("z", sample.getZ());
            row.put("yaw", sample.getYaw());
            row.put("pitch", sample.getPitch());
            out.add(row);
        }
        return out;
    }

    private static List<GhostSample> sanitizeSamples(List<GhostSample> samples) {
        if (samples == null || samples.isEmpty()) return Collections.emptyList();

        List<GhostSample> out = new ArrayList<>(samples.size());
        long lastTime = -1L;
        for (GhostSample sample : samples) {
            if (sample == null) continue;
            if (!Double.isFinite(sample.getX()) || !Double.isFinite(sample.getY()) || !Double.isFinite(sample.getZ())) continue;
            if (!Float.isFinite(sample.getYaw()) || !Float.isFinite(sample.getPitch())) continue;

            long time = Math.max(0L, sample.getTimeMs());
            if (time < lastTime) continue;
            if (time == lastTime && !out.isEmpty()) continue;

            out.add(new GhostSample(time, sample.getX(), sample.getY(), sample.getZ(), sample.getYaw(), sample.getPitch()));
            lastTime = time;
        }
        return out;
    }

    private static Long asLong(Object raw) {
        if (!(raw instanceof Number number)) return null;
        return number.longValue();
    }

    private static Double asDouble(Object raw) {
        if (!(raw instanceof Number number)) return null;
        return number.doubleValue();
    }

    private static Float asFloat(Object raw) {
        if (!(raw instanceof Number number)) return null;
        return number.floatValue();
    }

}
