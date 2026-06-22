package es.jaie55.boatracing.track;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrackConfig {
    private File file;
    private YamlConfiguration cfg;

    public static class StartSlot {
        public final String world;
        public final double x, y, z, yaw, pitch;
        public StartSlot(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
        public Location toLocation() {
            return new Location(Bukkit.getWorld(world), x, y, z, (float) yaw, (float) pitch);
        }
    }

    private final List<StartSlot> starts = new ArrayList<>();
    private Region finish;
    private Region pitlane;
    // Team-specific pit areas by Team UUID string
    private final Map<String, Region> teamPits = new LinkedHashMap<>();
    // Custom start slot (0-based index) per Player UUID string
    private final Map<String, Integer> customStartSlots = new LinkedHashMap<>();
    // Best race times per Player UUID string (millis)
    private final Map<String, Long> bestTimes = new LinkedHashMap<>();
    // Best race times segmented by total laps: laps -> (player UUID -> millis)
    private final Map<Integer, Map<String, Long>> bestTimesByLaps = new LinkedHashMap<>();
    private final List<Region> checkpoints = new ArrayList<>();
    public static class LightPos {
        public final String world; public final int x, y, z;
        public LightPos(String world, int x, int y, int z) { this.world = world; this.x = x; this.y = y; this.z = z; }
        public org.bukkit.block.Block getBlock() { org.bukkit.World w = Bukkit.getWorld(world); return (w!=null) ? w.getBlockAt(x, y, z) : null; }
    }
    private final java.util.List<LightPos> lights = new java.util.ArrayList<>();
    // Per-track racing overrides (optional; fallback to global config.yml values)
    private final Map<String, Object> racingOverrides = new LinkedHashMap<>();

    public TrackConfig(File dataFolder) {
        File tracksDir = new File(dataFolder, "tracks");
        if (!tracksDir.exists()) tracksDir.mkdirs();
        this.file = new File(tracksDir, "unsaved.yml");
        load();
    }

    /**
     * Switch the backing file for this TrackConfig to the provided file under tracks/.
     * Ensures parent directories exist, then loads its contents.
     */
    public void setBackingFile(File newFile) {
        if (newFile == null) return;
        File parent = newFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        this.file = newFile;
        load();
    }

    public List<StartSlot> getStarts() { return Collections.unmodifiableList(starts); }
    public Region getFinish() { return finish; }
    public Region getPitlane() { return pitlane; }
    public Map<String, Region> getTeamPits() { return Collections.unmodifiableMap(teamPits); }
    public Region getTeamPit(java.util.UUID teamId) { return teamId == null ? null : teamPits.get(teamId.toString()); }
    public List<Region> getCheckpoints() { return Collections.unmodifiableList(checkpoints); }
    public Map<String, Integer> getCustomStartSlots() { return Collections.unmodifiableMap(customStartSlots); }
    public Integer getCustomStartSlot(java.util.UUID playerId) { return playerId == null ? null : customStartSlots.get(playerId.toString()); }
    public void setCustomStartSlot(java.util.UUID playerId, int slotIndex0Based) { if (playerId != null) { customStartSlots.put(playerId.toString(), slotIndex0Based); save(); } }
    public void clearCustomStartSlot(java.util.UUID playerId) { if (playerId != null) { customStartSlots.remove(playerId.toString()); save(); } }
    public Map<String, Long> getBestTimes() { return Collections.unmodifiableMap(bestTimes); }
    public Long getBestTime(java.util.UUID playerId) { return playerId == null ? null : bestTimes.get(playerId.toString()); }
    public Map<String, Long> getBestTimesForLaps(int laps) {
        int normalizedLaps = Math.max(1, laps);
        Map<String, Long> lapMap = bestTimesByLaps.get(normalizedLaps);
        return lapMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(lapMap);
    }
    public Long getBestTime(java.util.UUID playerId, int laps) {
        if (playerId == null) return null;
        Map<String, Long> lapMap = bestTimesByLaps.get(Math.max(1, laps));
        return lapMap == null ? null : lapMap.get(playerId.toString());
    }
    public void updateBestTime(java.util.UUID playerId, long millis) {
        if (playerId == null) return;
        String key = playerId.toString();
        Long cur = bestTimes.get(key);
        if (cur == null || millis < cur) { bestTimes.put(key, millis); save(); }
    }
    public void updateBestTime(java.util.UUID playerId, long millis, int laps) {
        if (playerId == null) return;
        int normalizedLaps = Math.max(1, laps);
        String key = playerId.toString();

        boolean changed = false;

        Map<String, Long> lapMap = bestTimesByLaps.computeIfAbsent(normalizedLaps, ignored -> new LinkedHashMap<>());
        Long currentLapBest = lapMap.get(key);
        if (currentLapBest == null || millis < currentLapBest) {
            lapMap.put(key, millis);
            changed = true;
        }

        Long overallBest = bestTimes.get(key);
        if (overallBest == null || millis < overallBest) {
            bestTimes.put(key, millis);
            changed = true;
        }

        if (changed) save();
    }

    public int getConfiguredLaps(int globalDefault) {
        return Math.max(1, getRacingInt("laps", Math.max(1, globalDefault)));
    }

    public void addStart(Location loc) {
        starts.add(new StartSlot(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        save();
    }

    public boolean removeStartAt(int index0Based) {
        if (index0Based < 0 || index0Based >= starts.size()) return false;
        starts.remove(index0Based);

        // Keep custom slot assignments valid after removing one start slot.
        java.util.Iterator<Map.Entry<String, Integer>> it = customStartSlots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            Integer slot = entry.getValue();
            if (slot == null) {
                it.remove();
                continue;
            }
            if (slot == index0Based) {
                it.remove();
            } else if (slot > index0Based) {
                entry.setValue(slot - 1);
            }
        }

        save();
        return true;
    }

    public void setFinish(Region r) { this.finish = r; save(); }
    public void setPitlane(Region r) { this.pitlane = r; save(); }
    public void setTeamPit(java.util.UUID teamId, Region r) { if (teamId != null) { teamPits.put(teamId.toString(), r); save(); } }
    public void clearTeamPits() { teamPits.clear(); save(); }
    public void addCheckpoint(Region r) { this.checkpoints.add(r); save(); }
    public boolean replaceCheckpoint(int index0Based, Region r) {
        if (r == null || index0Based < 0 || index0Based >= checkpoints.size()) return false;
        checkpoints.set(index0Based, r);
        save();
        return true;
    }
    public boolean removeCheckpointAt(int index0Based) {
        if (index0Based < 0 || index0Based >= checkpoints.size()) return false;
        checkpoints.remove(index0Based);
        save();
        return true;
    }
    public boolean moveCheckpoint(int fromIndex0Based, int toIndex0Based) {
        if (fromIndex0Based < 0 || fromIndex0Based >= checkpoints.size()) return false;
        if (toIndex0Based < 0 || toIndex0Based >= checkpoints.size()) return false;
        if (fromIndex0Based == toIndex0Based) return true;
        Region moved = checkpoints.remove(fromIndex0Based);
        checkpoints.add(toIndex0Based, moved);
        save();
        return true;
    }
    public void clearCheckpoints() { this.checkpoints.clear(); save(); }
    public void clearStarts() { this.starts.clear(); save(); }

    /**
     * Reset all track data in-memory and on disk. Used when switching between named tracks.
     */
    public void resetAll() {
        this.starts.clear();
        this.checkpoints.clear();
        this.finish = null;
        this.pitlane = null;
        this.teamPits.clear();
        this.customStartSlots.clear();
        this.racingOverrides.clear();
        // Do not clear bestTimes on reset; they are historical per track
        save();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
    starts.clear(); checkpoints.clear(); lights.clear(); finish = null; pitlane = null; teamPits.clear(); customStartSlots.clear(); bestTimes.clear(); bestTimesByLaps.clear(); racingOverrides.clear();
        if (cfg.contains("starts")) {
            for (Object o : cfg.getList("starts")) {
                if (o instanceof Map<?,?> map) {
                    String world = (String) map.get("world");
                    double x = toD(map.get("x")); double y = toD(map.get("y")); double z = toD(map.get("z"));
                    float yaw = (float) toD(map.get("yaw")); float pitch = (float) toD(map.get("pitch"));
                    starts.add(new StartSlot(world, x, y, z, yaw, pitch));
                }
            }
        }
        if (cfg.contains("finish")) finish = readRegion("finish");
        if (cfg.contains("pitlane")) pitlane = readRegion("pitlane");
        // New: team-specific pits
        if (cfg.isConfigurationSection("teamPits")) {
            ConfigurationSection tp = cfg.getConfigurationSection("teamPits");
            if (tp != null) {
                for (String key : tp.getKeys(false)) {
                    Region r = readRegion("teamPits." + key);
                    if (r != null) teamPits.put(key, r);
                }
            }
        }
        // Custom start slots per player
        if (cfg.isConfigurationSection("customStartSlots")) {
            ConfigurationSection cs = cfg.getConfigurationSection("customStartSlots");
            if (cs != null) {
                for (String key : cs.getKeys(false)) {
                    int idx = cs.getInt(key, -1);
                    if (idx >= 0) customStartSlots.put(key, idx);
                }
            }
        }
        // Best times per player
        if (cfg.isConfigurationSection("bestTimes")) {
            ConfigurationSection bt = cfg.getConfigurationSection("bestTimes");
            if (bt != null) {
                for (String key : bt.getKeys(false)) {
                    long v = bt.getLong(key, -1L);
                    if (v >= 0L) bestTimes.put(key, v);
                }
            }
        }
        // Best times per player grouped by laps (bestTimesByLaps.<laps>.<uuid> = millis)
        if (cfg.isConfigurationSection("bestTimesByLaps")) {
            ConfigurationSection btl = cfg.getConfigurationSection("bestTimesByLaps");
            if (btl != null) {
                for (String lapKey : btl.getKeys(false)) {
                    int laps;
                    try {
                        laps = Integer.parseInt(lapKey);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                    if (laps < 1) continue;

                    ConfigurationSection lapSection = btl.getConfigurationSection(lapKey);
                    if (lapSection == null) continue;

                    Map<String, Long> lapMap = new LinkedHashMap<>();
                    for (String playerKey : lapSection.getKeys(false)) {
                        long v = lapSection.getLong(playerKey, -1L);
                        if (v >= 0L) lapMap.put(playerKey, v);
                    }
                    if (!lapMap.isEmpty()) bestTimesByLaps.put(laps, lapMap);
                }
            }
        }
        if (cfg.contains("checkpoints")) {
            if (cfg.isList("checkpoints")) {
                List<?> list = cfg.getList("checkpoints");
                if (list != null) {
                    for (Object o : list) {
                        if (o instanceof Map<?,?> map) {
                            String world = (String) map.get("world");
                            if (world == null) continue;
                            double minX = toD(map.get("minX"));
                            double minY = toD(map.get("minY"));
                            double minZ = toD(map.get("minZ"));
                            double maxX = toD(map.get("maxX"));
                            double maxY = toD(map.get("maxY"));
                            double maxZ = toD(map.get("maxZ"));
                            org.bukkit.util.BoundingBox b = new org.bukkit.util.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
                            checkpoints.add(new Region(world, b));
                        } else {
                            // Fallback by index if not a map (shouldn't happen with our save format)
                            int idx = checkpoints.size();
                            Region r = readRegion("checkpoints." + idx);
                            if (r != null) checkpoints.add(r);
                        }
                    }
                }
            } else if (cfg.isConfigurationSection("checkpoints")) {
                // Legacy format compatibility: numbered children under a section
                ConfigurationSection sec = cfg.getConfigurationSection("checkpoints");
                if (sec != null) {
                    java.util.List<Integer> keys = new java.util.ArrayList<>();
                    for (String k : sec.getKeys(false)) {
                        try { keys.add(Integer.parseInt(k)); } catch (NumberFormatException ignored) {}
                    }
                    java.util.Collections.sort(keys);
                    for (Integer i : keys) {
                        Region r = readRegion("checkpoints." + i);
                        if (r != null) checkpoints.add(r);
                    }
                }
            }
        }
        if (cfg.contains("lights") && cfg.isList("lights")) {
            java.util.List<?> list = cfg.getList("lights");
            if (list != null) {
                for (Object o : list) {
                    if (o instanceof java.util.Map<?,?> map) {
                        String world = (String) map.get("world");
                        int x = (int) Math.round(toD(map.get("x")));
                        int y = (int) Math.round(toD(map.get("y")));
                        int z = (int) Math.round(toD(map.get("z")));
                        if (world != null) lights.add(new LightPos(world, x, y, z));
                    }
                }
            }
        }
        // Per-track racing overrides
        racingOverrides.clear();
        if (cfg.isConfigurationSection("racing")) {
            ConfigurationSection rs = cfg.getConfigurationSection("racing");
            if (rs != null) {
                for (String key : rs.getKeys(false)) {
                    racingOverrides.put(key, rs.get(key));
                }
            }
        }
    }

    private double toD(Object o) { return o instanceof Number n ? n.doubleValue() : 0.0; }

    private Region readRegion(String path) {
        String world = cfg.getString(path + ".world");
        if (world == null) return null;
        double minX = cfg.getDouble(path + ".minX");
        double minY = cfg.getDouble(path + ".minY");
        double minZ = cfg.getDouble(path + ".minZ");
        double maxX = cfg.getDouble(path + ".maxX");
        double maxY = cfg.getDouble(path + ".maxY");
        double maxZ = cfg.getDouble(path + ".maxZ");
        return new Region(world, new org.bukkit.util.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    private void writeRegion(String path, Region r) {
        cfg.set(path + ".world", r.getWorldName());
        org.bukkit.util.BoundingBox b = r.getBox();
        cfg.set(path + ".minX", b.getMinX()); cfg.set(path + ".minY", b.getMinY()); cfg.set(path + ".minZ", b.getMinZ());
        cfg.set(path + ".maxX", b.getMaxX()); cfg.set(path + ".maxY", b.getMaxY()); cfg.set(path + ".maxZ", b.getMaxZ());
    }

    public void save() {
        if (cfg == null) cfg = new YamlConfiguration();
        List<Map<String,Object>> list = new ArrayList<>();
        for (StartSlot s : starts) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("world", s.world);
            m.put("x", s.x); m.put("y", s.y); m.put("z", s.z);
            m.put("yaw", s.yaw); m.put("pitch", s.pitch);
            list.add(m);
        }
        cfg.set("starts", list);
        if (finish != null) writeRegion("finish", finish);
        if (pitlane != null) writeRegion("pitlane", pitlane);
        // Write team-specific pits
        if (!teamPits.isEmpty()) {
            // Clear first
            cfg.set("teamPits", null);
            for (Map.Entry<String, Region> e : teamPits.entrySet()) {
                writeRegion("teamPits." + e.getKey(), e.getValue());
            }
        } else {
            cfg.set("teamPits", null);
        }
        // Write custom start slots
        if (!customStartSlots.isEmpty()) {
            cfg.set("customStartSlots", null);
            for (Map.Entry<String, Integer> e : customStartSlots.entrySet()) {
                cfg.set("customStartSlots." + e.getKey(), e.getValue());
            }
        } else {
            cfg.set("customStartSlots", null);
        }
        // Write best times
        if (!bestTimes.isEmpty()) {
            cfg.set("bestTimes", null);
            for (Map.Entry<String, Long> e : bestTimes.entrySet()) {
                cfg.set("bestTimes." + e.getKey(), e.getValue());
            }
        } else {
            cfg.set("bestTimes", null);
        }
        // Write best times grouped by laps
        if (!bestTimesByLaps.isEmpty()) {
            cfg.set("bestTimesByLaps", null);
            java.util.List<Integer> lapKeys = new java.util.ArrayList<>(bestTimesByLaps.keySet());
            java.util.Collections.sort(lapKeys);
            for (Integer lapKey : lapKeys) {
                Map<String, Long> lapMap = bestTimesByLaps.get(lapKey);
                if (lapMap == null || lapMap.isEmpty()) continue;
                for (Map.Entry<String, Long> e : lapMap.entrySet()) {
                    cfg.set("bestTimesByLaps." + lapKey + "." + e.getKey(), e.getValue());
                }
            }
        } else {
            cfg.set("bestTimesByLaps", null);
        }
        if (!checkpoints.isEmpty()) {
            java.util.List<java.util.Map<String,Object>> cps = new java.util.ArrayList<>();
            for (Region r : checkpoints) {
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                org.bukkit.util.BoundingBox b = r.getBox();
                m.put("world", r.getWorldName());
                m.put("minX", b.getMinX()); m.put("minY", b.getMinY()); m.put("minZ", b.getMinZ());
                m.put("maxX", b.getMaxX()); m.put("maxY", b.getMaxY()); m.put("maxZ", b.getMaxZ());
                cps.add(m);
            }
            cfg.set("checkpoints", cps);
        } else {
            cfg.set("checkpoints", null);
        }
        if (!lights.isEmpty()) {
            java.util.List<java.util.Map<String,Object>> ls = new java.util.ArrayList<>();
            for (LightPos lp : lights) {
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("world", lp.world);
                m.put("x", lp.x); m.put("y", lp.y); m.put("z", lp.z);
                ls.add(m);
            }
            cfg.set("lights", ls);
        } else {
            cfg.set("lights", null);
        }
        // Per-track racing overrides
        if (!racingOverrides.isEmpty()) {
            cfg.set("racing", null);
            for (Map.Entry<String, Object> e : racingOverrides.entrySet()) {
                cfg.set("racing." + e.getKey(), e.getValue());
            }
        } else {
            cfg.set("racing", null);
        }
        try { cfg.save(file); } catch (IOException ignored) {}
    }

    public boolean isReady() {
        // Minimum required to run a race: at least one start slot and a finish region.
        // Pit area and checkpoints are optional (penalties/checkpoint logic are skipped when absent).
        return finish != null && !starts.isEmpty();
    }

    public java.util.List<String> missingRequirements() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (finish == null) missing.add("finish");
    // Optional: pitlane and checkpoints are not required
        if (starts.isEmpty()) missing.add("start-slot-min");
        return missing;
    }

    // Lights API
    public java.util.List<LightPos> getLights() { return java.util.Collections.unmodifiableList(lights); }
    public void clearLights() { lights.clear(); save(); }
    public boolean removeLightAt(int index0Based) {
        if (index0Based < 0 || index0Based >= lights.size()) return false;
        lights.remove(index0Based);
        save();
        return true;
    }
    public boolean addLight(org.bukkit.block.Block b) {
        if (b == null || b.getType() != org.bukkit.Material.REDSTONE_LAMP) return false;
        if (lights.size() >= 5) return false;
        String w = b.getWorld().getName(); int x = b.getX(), y = b.getY(), z = b.getZ();
        for (LightPos lp : lights) if (lp.world.equals(w) && lp.x==x && lp.y==y && lp.z==z) return false;
        lights.add(new LightPos(w, x, y, z)); save(); return true;
    }
    public boolean hasFiveLights() { return lights.size() == 5; }

    // --- Per-track racing overrides ---
    public boolean hasRacingOverride(String key) { return racingOverrides.containsKey(key); }

    public int getRacingInt(String key, int globalDefault) {
        Object v = racingOverrides.get(key);
        if (v instanceof Number n) return n.intValue();
        return globalDefault;
    }

    public double getRacingDouble(String key, double globalDefault) {
        Object v = racingOverrides.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return globalDefault;
    }

    public boolean getRacingBoolean(String key, boolean globalDefault) {
        Object v = racingOverrides.get(key);
        if (v instanceof Boolean b) return b;
        return globalDefault;
    }

    public long getRacingLong(String key, long globalDefault) {
        Object v = racingOverrides.get(key);
        if (v instanceof Number n) return n.longValue();
        return globalDefault;
    }

    public String getRacingString(String key, String globalDefault) {
        Object v = racingOverrides.get(key);
        if (v instanceof String s) return s;
        return globalDefault;
    }

    public ConfigurationSection getRacingConfigurationSection(String key, ConfigurationSection globalDefault) {
        Object c = racingOverrides.get(key);
        if (c instanceof ConfigurationSection s) return s;
        return globalDefault;
    }

    public void setRacingOverride(String key, Object value) {
        racingOverrides.put(key, value);
        save();
    }

    public void removeRacingOverride(String key) {
        racingOverrides.remove(key);
        save();
    }

    public Map<String, Object> getRacingOverrides() {
        return Collections.unmodifiableMap(racingOverrides);
    }
}
