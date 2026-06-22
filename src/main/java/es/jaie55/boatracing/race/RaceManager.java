package es.jaie55.boatracing.race;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.team.Team;
import es.jaie55.boatracing.track.Region;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.PracticeGhostManager;
import es.jaie55.boatracing.util.PracticeStatsManager;
import es.jaie55.boatracing.util.SchedulerCompat;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Criteria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.lang.reflect.Method;
import java.util.*;

public class RaceManager {
    public static class RaceState {
        public int lap = 0;
        public int nextCheckpoint = 0;
        public long progressOrder = Long.MAX_VALUE;
        public boolean finished = false;
        public long finishTime = -1L; // millis
        public long penaltyMillis = 0L;
        public long lastLapSplitMillis = 0L;
        public long lastCheckpointSplitMillis = 0L;
        public boolean wasInFinish = false;
        public boolean wasInPit = false;
        public boolean wasInCheckpoint = false; // for current next checkpoint only
        public boolean forfeited = false;
    }

    public enum BackResult {
        SUCCESS,
        NO_LOCATION,
        EXPIRED
    }

    private final BoatRacingPlugin plugin;
    private final TrackConfig track;
    private final String trackName;
    private String broadcastMode;
    private boolean running = false;
    private boolean registering = false;
    private boolean practiceMode = false;
    private UUID practicePlayerId;
    private int totalLaps;
    private final Map<UUID, RaceState> states = new HashMap<>();
    private long startTime;
    private double pitPenaltySeconds;
    private double falseStartPenaltySeconds;
    private boolean enablePitPenalty;
    private boolean enableFalseStartPenalty;
    private long registrationSeconds;
    private int minPlayersToStart;
    // Lights "all out" delay after all 5 are lit
    private double lightsOutDelaySeconds;
    // Additional random jitter [0..jitter] seconds added to lights out
    private double lightsOutJitterSeconds;
    private final LinkedHashSet<UUID> registered = new LinkedHashSet<>();
    private SchedulerCompat.TaskHandle registrationTask;
    private long registrationSessionId = 0L;
    private final Map<UUID, Long> preStartPenalties = new HashMap<>();
    // Mandatory pitstops per racer
    private int mandatoryPitstops;
    // UI toggles
    private boolean sbShowPos;
    private boolean sbShowLap;
    private boolean sbShowCP;
    private boolean sbShowPit;
    private boolean sbShowName;
    private boolean abShowLap;
    private boolean abShowCP;
    private boolean abShowTime;
    private boolean abShowPit;
    // Optional registration lobby
    private boolean lobbyEnabled;
    private boolean lobbyReturnOnLeave;
    private long backWindowMillis;
    private String lobbyWorld;
    private double lobbyX;
    private double lobbyY;
    private double lobbyZ;
    private float lobbyYaw;
    private float lobbyPitch;
    private final Map<UUID, Location> preLobbyLocations = new HashMap<>();
    private final Map<UUID, Long> preLobbyBackExpiresAt = new HashMap<>();
    private final Map<UUID, SchedulerCompat.TaskHandle> backExpiryTasks = new HashMap<>();
    // Race-spawned boats/rafts to ensure deterministic cleanup on finish/cancel.
    private final Set<UUID> raceVehicleIds = new HashSet<>();
    private final Map<UUID, UUID> raceVehicleByPlayer = new HashMap<>();
    // Participants locked in vehicle during pre-race lights countdown.
    private final Set<UUID> countdownLockedParticipants = new HashSet<>();
    private SchedulerCompat.TaskHandle countdownLightTask;
    private SchedulerCompat.TaskHandle countdownCheckTask;

    // Scoreboard handling
    private SchedulerCompat.TaskHandle scoreboardTask;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Set<UUID> sidebarDisabledPlayers = new HashSet<>();
    private final Set<UUID> overlayPlayers = new HashSet<>();
    private final Map<UUID, Objective> previousSidebarObjectives = new HashMap<>();
    private final Set<UUID> simpleScoreHiddenByBoatRacing = new HashSet<>();
    private static final String SCOREBOARD_OBJECTIVE_NAME = "boatracing";
    private static final int SCOREBOARD_MAX_LINES = 12;
    private static final String[] SCOREBOARD_ENTRIES = new String[]{
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75",
            "\u00A76", "\u00A77", "\u00A78", "\u00A79", "\u00A7a", "\u00A7b"
    };
    // Per-player pitstop count for current lap
    private final Map<UUID, Integer> pitCount = new HashMap<>();
    // Sector leader times per lap: lap -> (checkpointIndex -> first time/ms)
    private final Map<Integer, Map<Integer, Long>> sectorLeaderTimes = new HashMap<>();
    // Leader finish time per lap
    private final Map<Integer, Long> lapLeaderFinishTimes = new HashMap<>();
    private long progressOrderCounter = 0L;

    // Practice ghost (advanced vanilla MVP) state
    private SchedulerCompat.TaskHandle practiceGhostCaptureTask;
    private List<PracticeGhostManager.GhostSample> practiceGhostCapturedSamples = new ArrayList<>();
    private int practiceGhostMaxSamples = 6000;
    private double practiceGhostMinSampleDistanceSq = 0.04D;
    private String practiceGhostCapturedBoatType = Material.OAK_BOAT.name();
    private String practiceGhostCapturedWorld = "";
    private SchedulerCompat.TaskHandle practiceGhostReplayTask;
    private Vehicle practiceGhostBoat;
    private ArmorStand practiceGhostRider;
    private List<PracticeGhostManager.GhostSample> practiceGhostReplaySamples = Collections.emptyList();
    private int practiceGhostReplayIndex = 0;
    private String practiceGhostCollisionTeamName;
    private int practiceGhostHideTickCounter = 0;

    public RaceManager(BoatRacingPlugin plugin, TrackConfig track) {
        this(plugin, track, null);
    }

    public RaceManager(BoatRacingPlugin plugin, TrackConfig track, String trackName) {
        this.plugin = plugin;
        this.track = track;
        String normalized = (trackName == null || trackName.isBlank()) ? null : trackName.trim();
        this.trackName = normalized;
        loadSettings();
    }

    public String getTrackName() {
        if (trackName != null && !trackName.isBlank()) return trackName;
        return Optional.ofNullable(plugin.getTrackLibrary())
                .map(es.jaie55.boatracing.track.TrackLibrary::getCurrent)
                .orElse("unsaved");
    }

    /**
     * Reload all racing settings from global config, applying per-track overrides if present.
     * Called on construction and whenever a track is loaded before a race.
     */
    public void loadSettings() {
        // Race sessions keep their own TrackConfig instance; reload from disk so setup edits
        // made through another TrackConfig instance are visible immediately.
        track.load();
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        this.broadcastMode = track.getRacingString("broadcast-mode", cfg.getString("racing.broadcast-mode", "global")).toLowerCase();
        this.totalLaps = track.getRacingInt("laps", cfg.getInt("racing.laps", 3));
        this.pitPenaltySeconds = track.getRacingDouble("pit-penalty-seconds", cfg.getDouble("racing.pit-penalty-seconds", 5.0));
        this.falseStartPenaltySeconds = track.getRacingDouble("false-start-penalty-seconds", cfg.getDouble("racing.false-start-penalty-seconds", 3.0));
        this.enablePitPenalty = track.getRacingBoolean("enable-pit-penalty", cfg.getBoolean("racing.enable-pit-penalty", true));
        this.enableFalseStartPenalty = track.getRacingBoolean("enable-false-start-penalty", cfg.getBoolean("racing.enable-false-start-penalty", true));
        this.registrationSeconds = track.getRacingLong("registration-seconds", cfg.getLong("racing.registration-seconds", 300L));
        this.minPlayersToStart = Math.max(1, track.getRacingInt("min-players-to-start", cfg.getInt("racing.min-players-to-start", 1)));
        this.lightsOutDelaySeconds = Math.max(0.0, track.getRacingDouble("lights-out-delay-seconds", cfg.getDouble("racing.lights-out-delay-seconds", 1.0)));
        this.lightsOutJitterSeconds = Math.max(0.0, track.getRacingDouble("lights-out-jitter-seconds", cfg.getDouble("racing.lights-out-jitter-seconds", 0.0)));
        this.mandatoryPitstops = Math.max(0, track.getRacingInt("mandatory-pitstops", cfg.getInt("racing.mandatory-pitstops", 0)));
        // UI toggles (defaults true)
        this.sbShowPos = cfg.getBoolean("racing.ui.scoreboard.show-position", true);
        this.sbShowLap = cfg.getBoolean("racing.ui.scoreboard.show-lap", true);
        this.sbShowCP = cfg.getBoolean("racing.ui.scoreboard.show-checkpoints", true);
        this.sbShowPit = cfg.getBoolean("racing.ui.scoreboard.show-pitstops", true);
        this.sbShowName = cfg.getBoolean("racing.ui.scoreboard.show-name", true);
        this.abShowLap = cfg.getBoolean("racing.ui.actionbar.show-lap", true);
        this.abShowCP = cfg.getBoolean("racing.ui.actionbar.show-checkpoints", true);
        this.abShowTime = cfg.getBoolean("racing.ui.actionbar.show-time", true);
        this.abShowPit = cfg.getBoolean("racing.ui.actionbar.show-pitstops", true);
        // Lobby configuration
        ConfigurationSection trackLobby = track.getRacingConfigurationSection("lobby", cfg.getConfigurationSection("racing.lobby"));
        if (trackLobby == null) trackLobby = new org.bukkit.configuration.MemoryConfiguration();
        this.lobbyEnabled = trackLobby.getBoolean("enabled", false);
        this.lobbyReturnOnLeave = trackLobby.getBoolean("return-on-leave", true);
        long configuredBackWindowSeconds = Math.max(1L, trackLobby.getLong("back-window-seconds", 180L));
        this.backWindowMillis = configuredBackWindowSeconds * 1000L;
        this.lobbyWorld = trackLobby.getString("world", "world");
        this.lobbyX = trackLobby.getDouble("x", 0.0);
        this.lobbyY = trackLobby.getDouble("y", 80.0);
        this.lobbyZ = trackLobby.getDouble("z", 0.0);
        this.lobbyYaw = (float) trackLobby.getDouble("yaw", 0.0);
        this.lobbyPitch = (float) trackLobby.getDouble("pitch", 0.0);
    }

    // Allow updating mandatory pitstops at runtime from Setup
    public void setMandatoryPitstops(int n) { this.mandatoryPitstops = Math.max(0, n); }

    public boolean isRunning() { return running; }
    public boolean isRegistering() { return registering; }
    public boolean isCountdownActive() { return !countdownLockedParticipants.isEmpty(); }
    public boolean isPracticeMode() { return practiceMode; }
    public boolean isPracticeActive() { return practiceMode && (running || isCountdownActive()); }
    public int getTotalLaps() { return totalLaps; }
    public void setTotalLaps(int laps) { this.totalLaps = Math.max(1, laps); }
    public TrackConfig getTrack() { return track; }
    public Set<UUID> getRegistered() { return Collections.unmodifiableSet(registered); }
    public Collection<UUID> getParticipants() { return states.keySet(); }
    public boolean isParticipant(UUID playerId) { return playerId != null && states.containsKey(playerId); }
    public boolean shouldPreventVehicleExit(UUID playerId) {
        if (playerId == null) return false;
        RaceState st = states.get(playerId);
        if (running && st != null && !st.finished) return true;
        return countdownLockedParticipants.contains(playerId);
    }
    public int getMandatoryPitstops() { return mandatoryPitstops; }
    public int getMinPlayersToStart() { return minPlayersToStart; }

    // --- Live read-only stats for placeholders ---
    public long getLiveTimeMillis(UUID playerId) {
        RaceState st = states.get(playerId);
        if (st == null) return -1L;
        return timeFor(st);
    }

    public int getLiveLap(UUID playerId) {
        RaceState st = states.get(playerId);
        return st == null ? 0 : st.lap;
    }

    public int getLiveCheckpoint(UUID playerId) {
        RaceState st = states.get(playerId);
        return st == null ? 0 : st.nextCheckpoint;
    }

    public int getLivePitstops(UUID playerId) {
        return pitCount.getOrDefault(playerId, 0);
    }

    public boolean isLiveFinished(UUID playerId) {
        RaceState st = states.get(playerId);
        return st != null && st.finished;
    }

    public int getLivePosition(UUID playerId) {
        RaceState target = states.get(playerId);
        if (target == null) return 0;
        List<Map.Entry<UUID, RaceState>> list = getOrderedRaceEntries();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals(playerId)) return i + 1;
        }
        return 0;
    }

    // --- Race lifecycle ---
    public void startRace(Collection<Player> participants) {
        startRace(participants, false);
    }

    private void startRace(Collection<Player> participants, boolean practice) {
        if (track.getFinish() == null) throw new IllegalStateException("Finish region is not set");
        // Starting a race must always close any registration window/timer first.
        closeRegistrationWindow();
        clearCountdownLock();
        practiceMode = practice;
        practicePlayerId = (practice && participants != null && !participants.isEmpty())
                ? participants.iterator().next().getUniqueId()
                : null;
        running = true;
        startTime = System.currentTimeMillis();
        states.clear();
        sectorLeaderTimes.clear();
        lapLeaderFinishTimes.clear();
        progressOrderCounter = 0L;
        for (Player p : participants) {
            if (trySimpleScoreHide(p)) {
                simpleScoreHiddenByBoatRacing.add(p.getUniqueId());
            }
            RaceState st = new RaceState();
            Long pre = enableFalseStartPenalty ? preStartPenalties.remove(p.getUniqueId()) : null;
            if (pre != null && pre > 0) st.penaltyMillis += pre;
            st.lastLapSplitMillis = 0L;
            st.lastCheckpointSplitMillis = 0L;
            st.progressOrder = nextProgressOrder();
            states.put(p.getUniqueId(), st);
            setupPlayerBoard(p);
            pitCount.put(p.getUniqueId(), 0);
        }
        if (practiceMode) {
            startPracticeGhostSystems(participants);
        } else {
            stopPracticeGhostSystems();
        }
        for (Player p : raceAudience(participants)) {
            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.started", "laps", String.valueOf(totalLaps))));
        }
        startScoreboard();
    }

    public void stopRace(boolean announce) {
        running = false;
        clearCountdownLock();
        if (announce) announceResults();
        cleanupRaceVehicles();
        sendParticipantsToLobbyAfterRace();
        stopScoreboard();
        clearPracticeSessionState();
    }

    public void reset() {
        running = false;
        closeRegistrationWindow();
        clearCountdownLock();
        cleanupRaceVehicles();
        states.clear();
        startTime = 0L;
        registered.clear();
        preStartPenalties.clear();
        cancelAllBackExpiryTasks();
        preLobbyLocations.clear();
        preLobbyBackExpiresAt.clear();
        stopScoreboard();
        clearPracticeSessionState();
    }

    // Called on player movement to update crossing events
    public void tickPlayer(Player p, Location from, Location to) {
        if (!running) return;
        RaceState st = states.get(p.getUniqueId());
        if (st == null || st.finished) return;

        // Only process in the same world as track
        Region finish = track.getFinish();
        if (finish == null || finish.world() == null) return;
        if (!to.getWorld().getName().equals(finish.getWorldName())) return;

        // Checkpoint progression
        if (st.nextCheckpoint < track.getCheckpoints().size()) {
            Region next = track.getCheckpoints().get(st.nextCheckpoint);
            boolean crossed = from != null && segmentIntersectsBox(from, to, next.getBox())
                    && to.getWorld().getName().equals(next.getWorldName());
            if (!crossed) crossed = next.getBox().contains(to.toVector())
                    && to.getWorld().getName().equals(next.getWorldName());
            if (crossed && !st.wasInCheckpoint) {
                st.wasInCheckpoint = true;
                st.nextCheckpoint++;
                st.progressOrder = nextProgressOrder();
                p.sendMessage(color(plugin.pref() + plugin.msg().get("race.checkpoint-reached", "num", String.valueOf(st.nextCheckpoint), "total", String.valueOf(track.getCheckpoints().size()))));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
                int lapNumber = st.lap + 1; // current lap number (1-based)
                int cpIndex = st.nextCheckpoint; // already incremented above
                long nowMs = timeFor(st);
                if (practiceMode) {
                    long sectorMillis = Math.max(0L, nowMs - st.lastCheckpointSplitMillis);
                    st.lastCheckpointSplitMillis = nowMs;
                    PracticeStatsManager stats = plugin.getPracticeStatsManager();
                    if (stats != null) {
                        PracticeStatsManager.PracticeUpdate update = stats.recordSector(p.getUniqueId(), getTrackName(), cpIndex, sectorMillis);
                        sendPracticeSectorFeedback(p, lapNumber, cpIndex, update);
                    }
                } else {
                    Map<Integer, Long> lapMap = sectorLeaderTimes.computeIfAbsent(lapNumber, k -> new HashMap<>());
                    if (!lapMap.containsKey(cpIndex)) {
                        lapMap.put(cpIndex, nowMs);
                    } else {
                        long gap = Math.max(0L, nowMs - lapMap.get(cpIndex));
                        String msg = plugin.msg().get("race.sector-gap", "cp", String.valueOf(cpIndex), "lap", String.valueOf(lapNumber), "player", p.getName(), "gap", formatSeconds(gap));
                        for (Player r : raceAudience(statesToPlayers())) r.sendMessage(color(msg));
                    }
                }
            } else if (!crossed && st.wasInCheckpoint) {
                st.wasInCheckpoint = false;
            }
        }

        // Pit area(s): default and per-team. Count pitstops and apply optional penalty.
        boolean insidePit = false;
        Region defaultPit = track.getPitlane();
        if (defaultPit != null && defaultPit.world() != null) {
            insidePit |= defaultPit.getBox().contains(to.toVector()) && to.getWorld().getName().equals(defaultPit.getWorldName());
        }
        // Team-specific pit
        java.util.UUID teamId = plugin.getTeamManager().getTeamByMember(p.getUniqueId()).map(es.jaie55.boatracing.team.Team::getId).orElse(null);
        Region teamPit = teamId != null ? track.getTeamPit(teamId) : null;
        if (teamPit != null && teamPit.world() != null) {
            insidePit |= teamPit.getBox().contains(to.toVector()) && to.getWorld().getName().equals(teamPit.getWorldName());
        }
        if (defaultPit != null || teamPit != null) {
            boolean wasInPit = st.wasInPit;
            boolean entering = insidePit && !wasInPit;
            boolean exiting = !insidePit && wasInPit;
            if (enablePitPenalty && entering) {
                long add = (long) Math.round(pitPenaltySeconds * 1000.0);
                st.penaltyMillis += add;
                p.sendMessage(color(plugin.pref() + plugin.msg().get("race.pit-penalty", "time", formatSeconds(add))));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            }
            // Update state
            st.wasInPit = insidePit;
            // Count on exit edge
            if (exiting) {
                pitCount.merge(p.getUniqueId(), 1, Integer::sum);
                p.sendMessage(color(plugin.pref() + plugin.msg().get("race.pit-completed", "count", String.valueOf(pitCount.getOrDefault(p.getUniqueId(), 0)))));
            }
        }

        // Finish (or pit as finish) crossing only if all checkpoints collected this lap
        boolean insideFinish = finish.getBox().contains(to.toVector());
        if (from != null && !insideFinish) {
            insideFinish = segmentIntersectsBox(from, to, finish.getBox());
        }
        boolean insideFinishOrPit = insideFinish || insidePit;
        if (insideFinishOrPit && !st.wasInFinish) {
            st.wasInFinish = true;
            boolean pitRequirementMet = true;
            if (mandatoryPitstops > 0) {
                int done = pitCount.getOrDefault(p.getUniqueId(), 0);
                pitRequirementMet = done >= mandatoryPitstops;
                if (!pitRequirementMet && insideFinish) {
                    p.sendMessage(color(plugin.pref() + plugin.msg().get("race.pit-requirement-not-met", "required", String.valueOf(mandatoryPitstops), "done", String.valueOf(done))));
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
                }
            }
            if (st.nextCheckpoint >= track.getCheckpoints().size() && pitRequirementMet) {
                // Lap finish time before mutating state
                int lapCompleted = st.lap + 1;
                long lapFinishMs = (System.currentTimeMillis() - startTime) + st.penaltyMillis;
                long lapDurationMs = Math.max(0L, lapFinishMs - st.lastLapSplitMillis);
                st.lastLapSplitMillis = lapFinishMs;
                st.lastCheckpointSplitMillis = lapFinishMs;
                st.lap++;
                st.nextCheckpoint = 0;
                st.progressOrder = nextProgressOrder();
                st.wasInCheckpoint = false;
                if (plugin.getStatsManager() != null && lapDurationMs > 0) {
                    plugin.getStatsManager().updatePlayerBestLap(p.getUniqueId(), lapDurationMs, getTrackName(), totalLaps);
                }
                PracticeStatsManager.PracticeUpdate lapPracticeUpdate = null;
                if (practiceMode) {
                    PracticeStatsManager practiceStats = plugin.getPracticeStatsManager();
                    if (practiceStats != null && lapDurationMs > 0L) {
                        lapPracticeUpdate = practiceStats.recordLap(p.getUniqueId(), getTrackName(), lapDurationMs);
                    }
                }
                // Do not reset pit counter; mandatory pitstops apply for the whole race
                if (st.lap >= totalLaps) {
                    st.finished = true;
                    long now = System.currentTimeMillis();
                    st.finishTime = (now - startTime) + st.penaltyMillis;
                    try { track.updateBestTime(p.getUniqueId(), st.finishTime, totalLaps); } catch (Exception ignored) { var inst = BoatRacingPlugin.getInstance(); if (inst != null) inst.getLogger().finer("updateBestTime failed: " + ignored.getMessage()); }
                    if (plugin.getStatsManager() != null) {
                        plugin.getStatsManager().updatePlayerBestRace(p.getUniqueId(), st.finishTime, getTrackName(), totalLaps);
                    }
                    if (practiceMode) {
                        PracticeStatsManager practiceStats = plugin.getPracticeStatsManager();
                        PracticeStatsManager.PracticeUpdate runUpdate = null;
                        if (practiceStats != null) {
                            runUpdate = practiceStats.recordRun(p.getUniqueId(), getTrackName(), st.finishTime);
                            sendPracticeRunFeedback(p, runUpdate);
                        } else {
                            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.practice.run-completed", "time", formatSeconds(st.finishTime))));
                        }
                        maybeStorePracticeGhost(p, st.finishTime, runUpdate);
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
                    } else {
                        // Winner reference and gap to winner (same line)
                        Long winnerMs = lapLeaderFinishTimes.get(lapCompleted);
                        if (winnerMs == null) {
                            lapLeaderFinishTimes.put(lapCompleted, lapFinishMs);
                        }
                        String gapSuffix = "";
                        if (winnerMs != null) {
                            long gapF = Math.max(0L, lapFinishMs - winnerMs);
                            gapSuffix = " &7(+" + formatSeconds(gapF) + " to winner)";
                        }
                        // Announce the finisher's total time to all race participants and admins (English)
                        String finMsg = plugin.msg().get("race.player-finished", "player", p.getName(), "time", formatSeconds(st.finishTime), "gap", gapSuffix);
                        for (Player r : raceAudience(statesToPlayers())) r.sendMessage(color(finMsg));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
                    }

                    // Move finished racers to waiting lobby immediately.
                    sendParticipantToLobbyAfterRace(p.getUniqueId(), p);

                    checkAllFinished();
                } else {
                    if (practiceMode) {
                        if (lapPracticeUpdate != null) {
                            sendPracticeLapFeedback(p, lapCompleted, lapPracticeUpdate);
                        } else {
                            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.lap-completed", "lap", String.valueOf(st.lap), "total", String.valueOf(totalLaps))));
                        }
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                    } else {
                        // Lap finish broadcast (always). First finisher sets baseline; others show +gap to leader
                        Long leaderLapMs = lapLeaderFinishTimes.get(lapCompleted);
                        if (leaderLapMs == null) {
                            lapLeaderFinishTimes.put(lapCompleted, lapFinishMs);
                        }
                        String gapLapSuffix = "";
                        if (leaderLapMs != null) {
                            long gap = Math.max(0L, lapFinishMs - leaderLapMs);
                            gapLapSuffix = " &7(+" + formatSeconds(gap) + " to leader)";
                        }
                        String lapMsg = plugin.msg().get("race.lap-finished", "num", String.valueOf(lapCompleted), "player", p.getName(), "gap", gapLapSuffix);
                        for (Player r : raceAudience(statesToPlayers())) r.sendMessage(color(lapMsg));
                        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.lap-completed", "lap", String.valueOf(st.lap), "total", String.valueOf(totalLaps))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                    }
                }
            } else {
                // Tried to finish without checkpoints or missing mandatory pitstops
                if (st.nextCheckpoint < track.getCheckpoints().size()) {
                    int remain = track.getCheckpoints().size() - st.nextCheckpoint;
                    p.sendMessage(color(plugin.pref() + plugin.msg().get("race.checkpoints-incomplete", "remain", String.valueOf(remain))));
                } else if (!pitRequirementMet && !insideFinish) {
                    // If failing pit requirement while using pit lane as finish, show a hint only when not already shown above
                    int done = pitCount.getOrDefault(p.getUniqueId(), 0);
                    p.sendMessage(color(plugin.pref() + plugin.msg().get("race.pit-requirement-not-met", "required", String.valueOf(mandatoryPitstops), "done", String.valueOf(done))));
                }
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            }
        } else if (!insideFinishOrPit && st.wasInFinish) {
            st.wasInFinish = false;
        }
    }

    private void checkAllFinished() {
        for (RaceState s : states.values()) if (!s.finished) return;
        stopRace(true);
    }

    public void announceResults() {
        if (states.isEmpty()) return;
        List<Map.Entry<UUID, RaceState>> list = new ArrayList<>(states.entrySet());
        list.sort(Comparator.comparingLong(e -> timeFor(e.getValue())));

        List<Map.Entry<UUID, RaceState>> finishers = new ArrayList<>();
        List<Map.Entry<UUID, RaceState>> forfeitedEntries = new ArrayList<>();
        for (Map.Entry<UUID, RaceState> e : list) {
            if (e.getValue().forfeited) forfeitedEntries.add(e);
            else finishers.add(e);
        }

        Collection<Player> recipients;
        if (practiceMode) {
            Player runner = practicePlayer();
            if (runner == null || !runner.isOnline()) {
                recipients = statesToPlayers();
            } else {
                recipients = Collections.singletonList(runner);
            }
        } else {
            recipients = new ArrayList<>(Bukkit.getOnlinePlayers());
        }

        // Persist winner stats for placeholders/holograms (only for finishers, not forfeited)
        if (!practiceMode && !finishers.isEmpty() && plugin.getStatsManager() != null) {
            java.util.List<UUID> finishOrder = new java.util.ArrayList<>();
            for (Map.Entry<UUID, RaceState> e : finishers) finishOrder.add(e.getKey());
            plugin.getStatsManager().recordPlayerPositions(finishOrder);

            UUID winner = finishers.get(0).getKey();
            plugin.getStatsManager().addPlayerWin(winner);
            plugin.getTeamManager().getTeamByMember(winner).ifPresent(t -> plugin.getStatsManager().addTeamWin(t.getId()));
        }

        for (Player p : recipients) p.sendMessage(color(plugin.msg().get("race.results.header")));

        int pos = 1;
        for (Map.Entry<UUID, RaceState> e : finishers) {
            UUID id = e.getKey();
            String name = resolveResultName(id);
            RaceState s = e.getValue();
            long t = timeFor(s);

            String penaltyStr = s.penaltyMillis > 0 ? " &7(+" + formatSeconds(s.penaltyMillis) + " penalty)" : "";
            String msgKey = switch (pos) {
                case 1 -> "race.results.first";
                case 2 -> "race.results.second";
                case 3 -> "race.results.third";
                default -> "race.results.other";
            };
            String line = plugin.msg().get(msgKey, "pos", String.valueOf(pos), "player", name, "time", formatSeconds(t), "penalty", penaltyStr);

            pos++;
            for (Player p : recipients) p.sendMessage(color(line));
        }

        for (Map.Entry<UUID, RaceState> e : forfeitedEntries) {
            String name = resolveResultName(e.getKey());
            String line = plugin.msg().get("race.results.forfeited", "player", name);
            for (Player p : recipients) p.sendMessage(color(line));
        }

        // Distribute rewards (only for finishers, not forfeited)
        if (!practiceMode) {
            try {
                es.jaie55.boatracing.reward.RewardManager rm = plugin.getRewardManager();
                if (rm != null && rm.isEnabled(track)) {
                    String trackName = getTrackName();
                    List<Map.Entry<UUID, Long>> results = new ArrayList<>();
                    for (Map.Entry<UUID, RaceState> e : finishers) {
                        results.add(new AbstractMap.SimpleEntry<>(e.getKey(), timeFor(e.getValue())));
                    }
                    rm.giveRewards(results, trackName, totalLaps, track);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to distribute rewards: " + ex.getMessage());
            }
        }
    }

    private String resolveResultName(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return safeRenderName(online);
        }
        String name = safeOfflineName(Bukkit.getOfflinePlayer(id));
        return (name != null && !name.isEmpty()) ? name : id.toString().substring(0, 8);
    }

    private String safeOfflineName(org.bukkit.OfflinePlayer target) {
        if (target == null) return null;
        Player online = target.getPlayer();
        if (online != null && online.getName() != null && !online.getName().isBlank()) {
            return online.getName();
        }
        try {
            java.lang.reflect.Method getPlayerProfile = target.getClass().getMethod("getPlayerProfile");
            Object profile = getPlayerProfile.invoke(target);
            if (profile != null) {
                java.lang.reflect.Method getName = profile.getClass().getMethod("getName");
                Object rawName = getName.invoke(profile);
                if (rawName instanceof String profileName && !profileName.isBlank()) {
                    return profileName;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private long timeFor(RaceState s) {
        if (s.finishTime >= 0) return s.finishTime;
        return (System.currentTimeMillis() - startTime) + s.penaltyMillis;
    }

    private long nextProgressOrder() {
        return ++progressOrderCounter;
    }

    private int compareRaceEntries(Map.Entry<UUID, RaceState> a, Map.Entry<UUID, RaceState> b) {
        RaceState sa = a.getValue();
        RaceState sb = b.getValue();

        if (sa.finished && sb.finished) {
            int byFinish = Long.compare(sa.finishTime, sb.finishTime);
            if (byFinish != 0) return byFinish;
        }
        if (sa.finished != sb.finished) return sa.finished ? -1 : 1;

        if (sa.lap != sb.lap) return Integer.compare(sb.lap, sa.lap);
        if (sa.nextCheckpoint != sb.nextCheckpoint) return Integer.compare(sb.nextCheckpoint, sa.nextCheckpoint);

        int byProgress = Long.compare(sa.progressOrder, sb.progressOrder);
        if (byProgress != 0) return byProgress;

        int byTime = Long.compare(timeFor(sa), timeFor(sb));
        if (byTime != 0) return byTime;

        return a.getKey().compareTo(b.getKey());
    }

    private List<Map.Entry<UUID, RaceState>> getOrderedRaceEntries() {
        List<Map.Entry<UUID, RaceState>> ordered = new ArrayList<>(states.entrySet());
        ordered.sort(this::compareRaceEntries);
        return ordered;
    }

    private static String formatSeconds(long millis) {
        long sec = millis / 1000;
        long ms = millis % 1000;
        long m = sec / 60; long s = sec % 60;
        return String.format("%d:%02d.%03d", m, s, ms);
    }

    private static String formatOptionalPracticeTime(Long millis) {
        return millis != null && millis >= 0L ? formatSeconds(millis) : "--:--.---";
    }

    private String scoreboardText(String key, String fallback) {
        String localized = plugin.msg().get(key);
        if (localized == null || localized.isBlank() || key.equals(localized)) return fallback;
        return localized;
    }

    private String resolvePracticeStatusLine() {
        return scoreboardText("race.scoreboard.practice-label", "PRACTICE");
    }

    private void appendPracticeSidebarTimes(List<Component> lines, UUID viewerId, RaceState viewerState) {
        if (lines == null || viewerId == null || viewerState == null) return;

        long currentRun = (viewerState.finished && viewerState.finishTime >= 0L)
                ? viewerState.finishTime
                : timeFor(viewerState);

        PracticeStatsManager stats = plugin.getPracticeStatsManager();
        String currentTrack = getTrackName();

        Long bestRun = stats != null ? stats.getBestRun(viewerId, currentTrack) : null;
        Long lastRun = stats != null ? stats.getLastRun(viewerId, currentTrack) : null;
        Long bestLap = stats != null ? stats.getBestLap(viewerId, currentTrack) : null;
        Long lastLap = stats != null ? stats.getLastLap(viewerId, currentTrack) : null;

        String currentLabel = scoreboardText("race.scoreboard.practice-current", "Current run");
        String bestRunLabel = scoreboardText("race.scoreboard.practice-best-run", "Best run");
        String lastRunLabel = scoreboardText("race.scoreboard.practice-last-run", "Last run");
        String bestLapLabel = scoreboardText("race.scoreboard.practice-best-lap", "Best lap");
        String lastLapLabel = scoreboardText("race.scoreboard.practice-last-lap", "Last lap");

        lines.add(Component.text(currentLabel + " ", NamedTextColor.YELLOW)
                .append(Component.text(formatSeconds(currentRun), NamedTextColor.WHITE)));

        lines.add(Component.text(" "));

        lines.add(Component.text(bestRunLabel + " ", NamedTextColor.AQUA)
                .append(Component.text(formatOptionalPracticeTime(bestRun), NamedTextColor.WHITE)));
        lines.add(Component.text(lastRunLabel + " ", NamedTextColor.GRAY)
                .append(Component.text(formatOptionalPracticeTime(lastRun), NamedTextColor.WHITE)));

        lines.add(Component.text(" "));

        lines.add(Component.text(bestLapLabel + " ", NamedTextColor.AQUA)
                .append(Component.text(formatOptionalPracticeTime(bestLap), NamedTextColor.WHITE)));
        lines.add(Component.text(lastLapLabel + " ", NamedTextColor.GRAY)
                .append(Component.text(formatOptionalPracticeTime(lastLap), NamedTextColor.WHITE)));

        lines.add(Component.text(" "));
    }

    private void clearPracticeSessionState() {
        stopPracticeGhostSystems();
        practiceMode = false;
        practicePlayerId = null;
    }

    private Player practicePlayer() {
        return practicePlayerId != null ? Bukkit.getPlayer(practicePlayerId) : null;
    }

    private boolean isPracticeGhostEnabled() {
        return plugin.getConfig().getBoolean("practice.ghost.enabled", true);
    }

    private void startPracticeGhostSystems(Collection<Player> participants) {
        stopPracticeGhostSystems();
        if (!isPracticeGhostEnabled()) return;

        boolean onlyPractice = plugin.getConfig().getBoolean("practice.ghost.only-in-practice", true);
        if (onlyPractice && !practiceMode) return;

        Player runner = practicePlayer();
        if (runner == null || !runner.isOnline()) return;

        practiceGhostCapturedSamples = new ArrayList<>();
        practiceGhostMaxSamples = Math.max(200, plugin.getConfig().getInt("practice.ghost.max-samples", 6000));
        double minDistance = Math.max(0.01D, plugin.getConfig().getDouble("practice.ghost.min-distance", 0.20D));
        practiceGhostMinSampleDistanceSq = minDistance * minDistance;
        practiceGhostCapturedWorld = runner.getWorld().getName();
        practiceGhostCapturedBoatType = resolveBoatTypeForPlayer(runner);

        capturePracticeGhostSample(runner, 0L, true);

        int sampleTicks = Math.max(1, plugin.getConfig().getInt("practice.ghost.sample-ticks", 2));
        practiceGhostCaptureTask = SchedulerCompat.runTimer(plugin, () -> capturePracticeGhostTick(runner.getUniqueId()), sampleTicks, sampleTicks);

        startPracticeGhostReplay(runner);
    }

    private void stopPracticeGhostSystems() {
        stopPracticeGhostCapture();
        stopPracticeGhostReplay();
        practiceGhostCapturedSamples = new ArrayList<>();
        practiceGhostCapturedWorld = "";
        practiceGhostCapturedBoatType = Material.OAK_BOAT.name();
    }

    private void stopPracticeGhostCapture() {
        if (practiceGhostCaptureTask != null) {
            try {
                practiceGhostCaptureTask.cancel();
            } catch (Exception ignored) {
            }
            practiceGhostCaptureTask = null;
        }
    }

    private void stopPracticeGhostReplay() {
        if (practiceGhostReplayTask != null) {
            try {
                practiceGhostReplayTask.cancel();
            } catch (Exception ignored) {
            }
            practiceGhostReplayTask = null;
        }

        removeGhostNoCollisionRules();

        if (practiceGhostBoat != null && practiceGhostBoat.isValid()) {
            try {
                practiceGhostBoat.remove();
            } catch (Exception ignored) {
            }
        }
        if (practiceGhostRider != null && practiceGhostRider.isValid()) {
            try {
                practiceGhostRider.remove();
            } catch (Exception ignored) {
            }
        }

        practiceGhostBoat = null;
        practiceGhostRider = null;
        practiceGhostReplaySamples = Collections.emptyList();
        practiceGhostReplayIndex = 0;
        practiceGhostHideTickCounter = 0;
    }

    private void capturePracticeGhostTick(UUID runnerId) {
        if (!running || !practiceMode) return;
        Player runner = Bukkit.getPlayer(runnerId);
        if (runner == null || !runner.isOnline()) return;
        if (!runner.getWorld().getName().equalsIgnoreCase(practiceGhostCapturedWorld)) return;

        long elapsed = Math.max(0L, System.currentTimeMillis() - startTime);
        capturePracticeGhostSample(runner, elapsed, false);
    }

    private void capturePracticeGhostSample(Player runner, long elapsedMs, boolean force) {
        if (runner == null || !runner.isOnline()) return;
        if (practiceGhostCapturedSamples.size() >= practiceGhostMaxSamples) return;

        Location loc = runner.getLocation();
        PracticeGhostManager.GhostSample sample = new PracticeGhostManager.GhostSample(
                Math.max(0L, elapsedMs),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );

        if (!force && !practiceGhostCapturedSamples.isEmpty()) {
            PracticeGhostManager.GhostSample last = practiceGhostCapturedSamples.get(practiceGhostCapturedSamples.size() - 1);
            double dx = sample.getX() - last.getX();
            double dy = sample.getY() - last.getY();
            double dz = sample.getZ() - last.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < practiceGhostMinSampleDistanceSq && (sample.getTimeMs() - last.getTimeMs()) < 100L) {
                return;
            }
        }

        practiceGhostCapturedSamples.add(sample);
    }

    private void maybeStorePracticeGhost(Player runner, long runMillis, PracticeStatsManager.PracticeUpdate runUpdate) {
        if (runner == null || !runner.isOnline()) return;

        capturePracticeGhostSample(runner, Math.max(0L, System.currentTimeMillis() - startTime), true);
        stopPracticeGhostCapture();

        if (!isPracticeGhostEnabled()) return;
        if (runUpdate != null && !runUpdate.isImproved()) return;
        if (practiceGhostCapturedSamples.size() < 2) return;

        PracticeGhostManager ghostManager = plugin.getPracticeGhostManager();
        if (ghostManager == null) return;

        ghostManager.updateBestGhost(
                getTrackName(),
                totalLaps,
                runner.getUniqueId(),
                runner.getName(),
                practiceGhostCapturedWorld,
                practiceGhostCapturedBoatType,
                runMillis,
                new ArrayList<>(practiceGhostCapturedSamples)
        );
    }

    private void startPracticeGhostReplay(Player runner) {
        if (runner == null || !runner.isOnline()) return;

        PracticeGhostManager ghostManager = plugin.getPracticeGhostManager();
        if (ghostManager == null) return;

        PracticeGhostManager.GhostPath path = ghostManager.getBestGhost(getTrackName(), totalLaps);
        if (path == null || path.getSamples().size() < 2) return;

        if (path.getWorldName() != null && !path.getWorldName().isBlank()) {
            if (!runner.getWorld().getName().equalsIgnoreCase(path.getWorldName())) return;
        }

        PracticeGhostManager.GhostSample first = path.getSamples().get(0);
        Location spawn = new Location(
                runner.getWorld(),
                first.getX(),
                first.getY(),
                first.getZ(),
                first.getYaw(),
                first.getPitch()
        );

        String normalizedGhostBoat = normalizeBoatMaterialName(path.getBoatType());
        boolean ghostChestFallback = normalizedGhostBoat.contains("_CHEST_");
        Vehicle ghostBoat;
        ArmorStand ghostRider;
        try {
            ghostBoat = spawnRaceVehicle(runner.getWorld(), spawn, normalizedGhostBoat, ghostChestFallback);
            if (ghostBoat == null) {
                plugin.getLogger().warning("Could not spawn practice ghost vehicle for type " + normalizedGhostBoat + " on track " + getTrackName());
                return;
            }
            ghostRider = runner.getWorld().spawn(spawn, ArmorStand.class);
        } catch (Exception ex) {
            plugin.getLogger().finer("Could not spawn practice ghost entities: " + ex.getMessage());
            return;
        }

        ghostBoat.setGravity(false);
        ghostBoat.setInvulnerable(true);
        ghostBoat.setSilent(true);
        ghostBoat.setPersistent(false);
        setEntityCollidable(ghostBoat, false);

        applyBoatVariantReliably(ghostBoat, normalizedGhostBoat);

        ghostRider.setInvisible(true);
        ghostRider.setMarker(true);
        ghostRider.setGravity(false);
        ghostRider.setInvulnerable(true);
        ghostRider.setSilent(true);
        ghostRider.setPersistent(false);
        setEntityCollidable(ghostRider, false);

        boolean showName = plugin.getConfig().getBoolean("practice.ghost.show-name", true);
        if (showName) {
            ghostRider.setCustomName(path.getOwnerName() == null || path.getOwnerName().isBlank() ? "Ghost" : path.getOwnerName());
            ghostRider.setCustomNameVisible(true);
        }

        ItemStack head = buildGhostHead(path.getOwnerUuid());
        if (head != null && ghostRider.getEquipment() != null) {
            ghostRider.getEquipment().setHelmet(head);
        }

        try {
            ghostBoat.addPassenger(ghostRider);
        } catch (Exception ignored) {
        }

        practiceGhostBoat = ghostBoat;
        practiceGhostRider = ghostRider;
        practiceGhostReplaySamples = path.getSamples();
        practiceGhostReplayIndex = 0;
        practiceGhostHideTickCounter = 0;

        applyGhostNoCollisionRules(ghostBoat, ghostRider);
        ensureGhostHiddenFromOthers(runner, ghostBoat, ghostRider);

        int playbackTicks = Math.max(1, plugin.getConfig().getInt("practice.ghost.playback-ticks", 1));
        practiceGhostReplayTask = SchedulerCompat.runTimer(plugin, () -> tickPracticeGhostReplay(runner.getUniqueId()), playbackTicks, playbackTicks);
    }

    private void tickPracticeGhostReplay(UUID runnerId) {
        if (!running || !practiceMode) {
            stopPracticeGhostReplay();
            return;
        }

        Player runner = Bukkit.getPlayer(runnerId);
        if (runner == null || !runner.isOnline()) {
            stopPracticeGhostReplay();
            return;
        }

        if (practiceGhostBoat == null || !practiceGhostBoat.isValid() || practiceGhostReplaySamples.isEmpty()) {
            stopPracticeGhostReplay();
            return;
        }

        if (!runner.getWorld().equals(practiceGhostBoat.getWorld())) {
            stopPracticeGhostReplay();
            return;
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - startTime);
        long lastTime = practiceGhostReplaySamples.get(practiceGhostReplaySamples.size() - 1).getTimeMs();
        if (elapsed > lastTime) {
            stopPracticeGhostReplay();
            return;
        }

        while (practiceGhostReplayIndex + 1 < practiceGhostReplaySamples.size()
                && practiceGhostReplaySamples.get(practiceGhostReplayIndex + 1).getTimeMs() <= elapsed) {
            practiceGhostReplayIndex++;
        }

        PracticeGhostManager.GhostSample a = practiceGhostReplaySamples.get(practiceGhostReplayIndex);
        PracticeGhostManager.GhostSample b = (practiceGhostReplayIndex + 1 < practiceGhostReplaySamples.size())
                ? practiceGhostReplaySamples.get(practiceGhostReplayIndex + 1)
                : a;

        long dt = Math.max(1L, b.getTimeMs() - a.getTimeMs());
        double t = Math.max(0.0D, Math.min(1.0D, (double) (elapsed - a.getTimeMs()) / (double) dt));

        double x = lerp(a.getX(), b.getX(), t);
        double y = lerp(a.getY(), b.getY(), t);
        double z = lerp(a.getZ(), b.getZ(), t);
        float yaw = lerpAngle(a.getYaw(), b.getYaw(), t);
        float pitch = lerpAngle(a.getPitch(), b.getPitch(), t);

        Location target = new Location(runner.getWorld(), x, y, z, yaw, pitch);
        practiceGhostBoat.teleport(target);

        if (practiceGhostRider != null && practiceGhostRider.isValid()) {
            if (!practiceGhostBoat.getPassengers().contains(practiceGhostRider)) {
                try {
                    practiceGhostBoat.addPassenger(practiceGhostRider);
                } catch (Exception ignored) {
                }
            }
        }

        practiceGhostHideTickCounter++;
        if (practiceGhostHideTickCounter >= 20) {
            practiceGhostHideTickCounter = 0;
            ensureGhostHiddenFromOthers(runner, practiceGhostBoat, practiceGhostRider);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, double t) {
        float delta = b - a;
        while (delta > 180.0F) delta -= 360.0F;
        while (delta < -180.0F) delta += 360.0F;
        return (float) (a + delta * t);
    }

    private void ensureGhostHiddenFromOthers(Player runner, Entity ghostBoat, Entity ghostRider) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) continue;
            if (online.getUniqueId().equals(runner.getUniqueId())) continue;
            try {
                online.hideEntity(plugin, ghostBoat);
                if (ghostRider != null) online.hideEntity(plugin, ghostRider);
            } catch (Exception ignored) {
            }
        }
    }

    private String resolveBoatTypeForPlayer(Player player) {
        if (player == null) return Material.OAK_BOAT.name();
        String selectedBoat = plugin.getTeamManager()
                .getTeamByMember(player.getUniqueId())
            .map((es.jaie55.boatracing.team.Team t) -> t.getBoatType(player.getUniqueId()))
                .orElse(Material.OAK_BOAT.name());
        return normalizeBoatMaterialName(selectedBoat);
    }

    private ItemStack buildGhostHead(UUID ownerUuid) {
        if (ownerUuid == null) return null;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (!(head.getItemMeta() instanceof SkullMeta meta)) return null;

        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
            head.setItemMeta(meta);
            return head;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyGhostNoCollisionRules(Entity ghostBoat, Entity ghostRider) {
        if (ghostBoat == null) return;
        if (Bukkit.getScoreboardManager() == null) return;

        String uuidStr = ghostBoat.getUniqueId().toString().replace("-", "");
        String teamName = "brghost" + uuidStr.substring(0, Math.min(9, uuidStr.length()));
        org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
        if (team == null) {
            try {
                team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(teamName);
            } catch (IllegalArgumentException ex) {
                team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            }
        }
        if (team == null) return;

        try {
            team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        } catch (Exception ignored) {
        }
        try {
            team.setCanSeeFriendlyInvisibles(true);
        } catch (Exception ignored) {
        }

        addEntityToTeam(team, ghostBoat);
        if (ghostRider != null) addEntityToTeam(team, ghostRider);
        practiceGhostCollisionTeamName = teamName;
    }

    private void removeGhostNoCollisionRules() {
        if (practiceGhostCollisionTeamName == null || practiceGhostCollisionTeamName.isBlank()) return;
        if (Bukkit.getScoreboardManager() == null) {
            practiceGhostCollisionTeamName = null;
            return;
        }

        org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(practiceGhostCollisionTeamName);
        if (team == null) {
            practiceGhostCollisionTeamName = null;
            return;
        }

        if (practiceGhostBoat != null) removeEntityFromTeam(team, practiceGhostBoat);
        if (practiceGhostRider != null) removeEntityFromTeam(team, practiceGhostRider);

        try {
            if (team.getEntries().isEmpty()) team.unregister();
        } catch (Exception ignored) {
        }

        practiceGhostCollisionTeamName = null;
    }

    private static void addEntityToTeam(org.bukkit.scoreboard.Team team, Entity entity) {
        if (team == null || entity == null) return;
        try {
            Method addEntity = team.getClass().getMethod("addEntity", Entity.class);
            addEntity.invoke(team, entity);
            return;
        } catch (Exception ignored) {
        }
        team.addEntry(entity.getUniqueId().toString());
    }

    private static void removeEntityFromTeam(org.bukkit.scoreboard.Team team, Entity entity) {
        if (team == null || entity == null) return;
        try {
            Method removeEntity = team.getClass().getMethod("removeEntity", Entity.class);
            removeEntity.invoke(team, entity);
            return;
        } catch (Exception ignored) {
        }
        team.removeEntry(entity.getUniqueId().toString());
    }

    private static void setEntityCollidable(Entity entity, boolean collidable) {
        if (entity == null) return;
        try {
            Method method = entity.getClass().getMethod("setCollidable", boolean.class);
            method.invoke(entity, collidable);
        } catch (Exception ignored) {
        }
    }

    private Collection<Player> raceAudience(Collection<Player> participants) {
        if (!practiceMode) return participantsAndAdmins(participants);

        LinkedHashSet<Player> set = new LinkedHashSet<>();
        if (participants != null) {
            for (Player p : participants) {
                if (p != null && p.isOnline()) set.add(p);
            }
        }
        if (set.isEmpty()) {
            Player owner = practicePlayer();
            if (owner != null && owner.isOnline()) set.add(owner);
        }
        return set;
    }

    private void sendPracticeSectorFeedback(Player player, int lapNumber, int sectionIndex, PracticeStatsManager.PracticeUpdate update) {
        if (player == null || update == null) return;
        if (update.isFirstRecord()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.sector.first",
                    "lap", String.valueOf(lapNumber),
                    "section", String.valueOf(sectionIndex),
                    "time", formatSeconds(update.getValueMillis())
            )));
            return;
        }
        if (update.isImproved()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.sector.new-best",
                    "lap", String.valueOf(lapNumber),
                    "section", String.valueOf(sectionIndex),
                    "time", formatSeconds(update.getValueMillis()),
                    "improve", formatSeconds(update.getImprovementMillis())
            )));
            return;
        }
        player.sendMessage(color(plugin.pref() + plugin.msg().get(
                "race.practice.sector.split",
                "lap", String.valueOf(lapNumber),
                "section", String.valueOf(sectionIndex),
                "time", formatSeconds(update.getValueMillis()),
                "delta", formatSeconds(update.getGapToBestMillis()),
                "best", formatSeconds(update.getBestMillis())
        )));
    }

    private void sendPracticeLapFeedback(Player player, int lapNumber, PracticeStatsManager.PracticeUpdate update) {
        if (player == null || update == null) return;
        if (update.isFirstRecord()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.lap.first",
                    "lap", String.valueOf(lapNumber),
                    "time", formatSeconds(update.getValueMillis())
            )));
            return;
        }
        if (update.isImproved()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.lap.new-best",
                    "lap", String.valueOf(lapNumber),
                    "time", formatSeconds(update.getValueMillis()),
                    "improve", formatSeconds(update.getImprovementMillis())
            )));
            return;
        }
        player.sendMessage(color(plugin.pref() + plugin.msg().get(
                "race.practice.lap.completed",
                "lap", String.valueOf(lapNumber),
                "time", formatSeconds(update.getValueMillis()),
                "delta", formatSeconds(update.getGapToBestMillis()),
                "best", formatSeconds(update.getBestMillis())
        )));
    }

    private void sendPracticeRunFeedback(Player player, PracticeStatsManager.PracticeUpdate update) {
        if (player == null || update == null) return;
        if (update.isFirstRecord()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.run.first",
                    "time", formatSeconds(update.getValueMillis())
            )));
            return;
        }
        if (update.isImproved()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get(
                    "race.practice.run.new-best",
                    "time", formatSeconds(update.getValueMillis()),
                    "improve", formatSeconds(update.getImprovementMillis())
            )));
            return;
        }
        player.sendMessage(color(plugin.pref() + plugin.msg().get(
                "race.practice.run.completed",
                "time", formatSeconds(update.getValueMillis()),
                "delta", formatSeconds(update.getGapToBestMillis()),
                "best", formatSeconds(update.getBestMillis())
        )));
    }

    private static String color(String s) { return es.jaie55.boatracing.util.Text.colorize(s); }

    // --- Registration ---
    public boolean openRegistration(int laps, Long secondsOverride) {
        if (running || registering || isCountdownActive()) return false;
        stopRegistrationTimer();
        setTotalLaps(laps);
        registered.clear();
        registering = true;
        final long sessionId = ++registrationSessionId;
        long seconds = secondsOverride != null ? secondsOverride : registrationSeconds;
        long endAt = System.currentTimeMillis() + (seconds * 1000L);

        String trackName = getTrackName();
        String cmd = "/boatracing race join " + trackName;

        String line = plugin.msg().get("race.registration.announce",
            "track", trackName,
            "laps", String.valueOf(totalLaps),
            "cmd", cmd,
            "label", "boatracing");

        broadcast(color(line));
        broadcast(color(plugin.msg().get("race.registration.closes-in", "seconds", String.valueOf(seconds))));

        registrationTask = SchedulerCompat.runTimer(plugin, new Runnable() {
            long lastAnnounced = seconds;
            @Override public void run() {
                if (!registering || sessionId != registrationSessionId) {
                    return;
                }
                long remain = Math.max(0, (endAt - System.currentTimeMillis()) / 1000L);
                if (remain <= 0) {
                    closeRegistrationWindow();
                    startFromRegistered();
                    return;
                }
                if (shouldAnnounce(remain, lastAnnounced)) {
                    lastAnnounced = remain;
                    broadcast(color(plugin.msg().get("race.registration.countdown", "remain", String.valueOf(remain), "count", String.valueOf(registered.size()))));
                }
            }
        }, 20L, 20L);
        return true;
    }

    private boolean shouldAnnounce(long remain, long last) {
        if (remain >= 60) return (remain % 60) == 0; // every minute
        if (remain == 30 || remain == 20 || remain == 10) return true;
        return remain <= 5; // 5,4,3,2,1
    }

    public boolean join(Player p) {
        if (!registering || running) return false;
        boolean added = registered.add(p.getUniqueId());
        if (added) {
            capturePreLobbyLocation(p);
        }
        teleportToLobbyIfEnabled(p);
        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.joined", "laps", String.valueOf(totalLaps))));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        broadcast(color(plugin.msg().get("race.registration.player-joined", "player", p.getName(), "count", String.valueOf(registered.size()))));
        return true;
    }

    public boolean leave(Player p) {
        if (!registering || running) return false;
        boolean removed = registered.remove(p.getUniqueId());
        if (removed) {
            restoreFromLobbyIfNeeded(p, true);
            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.left")));
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
            broadcast(color(plugin.msg().get("race.registration.player-left", "player", p.getName(), "count", String.valueOf(registered.size()))));
        }
        return removed;
    }

    public void forfeit(Player p) {
        if (!running) return;

        UUID id = p.getUniqueId();
        RaceState st = states.get(id);
        if (st == null) return;
        st.forfeited = true;
        st.finished = true;

        Scoreboard our = scoreboards.get(id);
        Scoreboard prev = previousScoreboards.get(id);
        try {
            if (overlayPlayers.contains(id)) {
                restoreOverlaySidebar(p, our, previousSidebarObjectives.get(id));
            } else {
                Scoreboard current = p.getScoreboard();
                boolean usingOurBoard = (our != null && current == our) || isBoatRacingBoard(current);
                if (usingOurBoard) {
                    restorePreviousScoreboard(p, prev);
                }
            }
            p.sendActionBar(net.kyori.adventure.text.Component.empty());
        } catch (Exception ignored) {
            plugin.getLogger().finer("Failed to restore previous scoreboard for " + p.getName() + ": " + ignored.getMessage());
        }

        scoreboards.remove(id);
        previousScoreboards.remove(id);
        sidebarDisabledPlayers.remove(id);
        overlayPlayers.remove(id);
        previousSidebarObjectives.remove(id);
        trySimpleScoreShow(p);

        if (practiceMode) {
            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.practice.left")));
        } else {
            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.forfeit")));
            for (Player r : raceAudience(statesToPlayers())) {
                if (!r.getUniqueId().equals(id)) {
                    r.sendMessage(color(plugin.msg().get("race.forfeit-other", "player", p.getName())));
                }
            }
        }

        cleanupRaceVehicleForPlayer(id);
        sendParticipantToLobbyAfterRace(id, p);
        if (practiceMode) {
            stopRace(false);
        } else {
            checkAllFinished();
        }
    }

    public boolean leavePractice(Player player) {
        return leavePractice(player, true, true);
    }

    public boolean leavePractice(Player player, boolean returnToLobby, boolean sendMessage) {
        if (player == null) return false;
        if (!practiceMode || !isParticipant(player.getUniqueId())) return false;

        UUID playerId = player.getUniqueId();
        running = false;
        closeRegistrationWindow();
        clearCountdownLock();
        cleanupRaceVehicles();
        states.remove(playerId);

        if (returnToLobby && player.isOnline()) {
            sendParticipantToLobbyAfterRace(playerId, player);
        }

        stopScoreboard();
        clearPracticeSessionState();

        if (sendMessage && player.isOnline()) {
            player.sendMessage(color(plugin.pref() + plugin.msg().get("race.practice.left")));
        }
        return true;
    }

    public boolean forceStart() {
        if (running || isCountdownActive()) return false;
        closeRegistrationWindow();
        return startFromRegistered();
    }

    public boolean startPractice(Player player) {
        if (player == null || !player.isOnline()) return false;
        if (running || registering || isCountdownActive()) return false;

        UUID playerId = player.getUniqueId();
        // Practice skips registration, so we must capture the pre-race location here.
        capturePreLobbyLocation(player);

        List<Player> participants = new ArrayList<>();
        participants.add(player);

        List<Player> placed = placeAtStartsWithBoats(participants);
        if (placed.isEmpty()) {
            preLobbyLocations.remove(playerId);
            preLobbyBackExpiresAt.remove(playerId);
            cancelBackExpiryTask(playerId);
            return false;
        }

        startRaceWithCountdown(placed, true);
        return true;
    }

    public void closeRegistrationWindow() {
        registering = false;
        stopRegistrationTimer();
    }

    private boolean startFromRegistered() {
        if (running || isCountdownActive()) return false;
        List<Player> participants = new ArrayList<>();
        for (UUID id : registered) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) participants.add(p);
        }
        if (participants.isEmpty()) {
            restoreLobbyForRegistered(true);
            cancelAllBackExpiryTasks();
            preLobbyLocations.clear();
            preLobbyBackExpiresAt.clear();
            broadcast(color(plugin.msg().get("race.cancelled-no-participants")));
            return false;
        }
        int required = Math.max(1, minPlayersToStart);
        if (participants.size() < required) {
            restoreLobbyForRegistered(true);
            cancelAllBackExpiryTasks();
            preLobbyLocations.clear();
            preLobbyBackExpiresAt.clear();
            broadcast(color(plugin.msg().get(
                    "race.not-enough-players",
                    "min", String.valueOf(required),
                    "current", String.valueOf(participants.size())
            )));
            return false;
        }
        List<Player> placed = placeAtStartsWithBoats(participants);
        if (placed.isEmpty()) {
            restoreLobbyForRegistered(true);
            cancelAllBackExpiryTasks();
            preLobbyLocations.clear();
            preLobbyBackExpiresAt.clear();
            broadcast(color(plugin.msg().get("race.cancelled-no-slots")));
            return false;
        }
        if (placed.size() < participants.size()) broadcast(color(plugin.msg().get("race.some-not-placed")));
        if (placed.size() < required) {
            cleanupRaceVehicles();
            restoreLobbyForRegistered(true);
            cancelAllBackExpiryTasks();
            preLobbyLocations.clear();
            preLobbyBackExpiresAt.clear();
            broadcast(color(plugin.msg().get(
                    "race.not-enough-players",
                    "min", String.valueOf(required),
                    "current", String.valueOf(placed.size())
            )));
            return false;
        }
        // Keep pre-lobby locations so players can use /boatracing race back after the race.
        startRaceWithCountdown(placed, false);
        return true;
    }

    public boolean cancelRace() {
        if (!running) return false;
        Collection<Player> recips = raceAudience(statesToPlayers());
        running = false;
        closeRegistrationWindow();
        clearCountdownLock();
        cleanupRaceVehicles();
        sendParticipantsToLobbyAfterRace();
        states.clear();
        for (Player p : recips) {
            p.sendMessage(color(plugin.msg().get("race.cancelled-general")));
        }
        stopScoreboard();
        clearPracticeSessionState();
        return true;
    }

    public BackResult returnToSavedLocation(Player p) {
        if (p == null || !p.isOnline()) return BackResult.NO_LOCATION;
        UUID playerId = p.getUniqueId();
        Location prev = preLobbyLocations.get(playerId);
        if (prev == null || prev.getWorld() == null) {
            preLobbyBackExpiresAt.remove(playerId);
            cancelBackExpiryTask(playerId);
            return BackResult.NO_LOCATION;
        }
        Long expiresAt = preLobbyBackExpiresAt.get(playerId);
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
            preLobbyLocations.remove(playerId);
            preLobbyBackExpiresAt.remove(playerId);
            cancelBackExpiryTask(playerId);
            return BackResult.EXPIRED;
        }
        preLobbyLocations.remove(playerId);
        preLobbyBackExpiresAt.remove(playerId);
        cancelBackExpiryTask(playerId);
        if (p.isInsideVehicle()) {
            org.bukkit.entity.Entity vehicle = p.getVehicle();
            p.leaveVehicle();
            if (vehicle != null) vehicle.remove();
        }
        p.teleport(prev);
        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-returned")));
        return BackResult.SUCCESS;
    }

    public boolean cancelRegistration(boolean announce) {
        if (!registering) return false;
        closeRegistrationWindow();
        clearCountdownLock();
        int count = registered.size();
        restoreLobbyForRegistered(true);
        registered.clear();
        cancelAllBackExpiryTasks();
        preLobbyLocations.clear();
        preLobbyBackExpiresAt.clear();
        if (announce) broadcast(color(plugin.msg().get("race.registration.cancelled", "count", String.valueOf(count))));
        return true;
    }

    private void stopRegistrationTimer() {
        registrationSessionId++;
        if (registrationTask != null) {
            try { registrationTask.cancel(); } catch (Exception ignored) { plugin.getLogger().finer("Failed to cancel registration timer: " + ignored.getMessage()); }
            registrationTask = null;
        }
    }

    private Location getLobbyLocation() {
        if (!lobbyEnabled) return null;
        if (lobbyWorld == null || lobbyWorld.isEmpty()) return null;
        org.bukkit.World world = Bukkit.getWorld(lobbyWorld);
        if (world == null) return null;
        return new Location(world, lobbyX, lobbyY, lobbyZ, lobbyYaw, lobbyPitch);
    }

    private void capturePreLobbyLocation(Player p) {
        if (p == null || !p.isOnline()) return;
        preLobbyLocations.put(p.getUniqueId(), p.getLocation().clone());
        preLobbyBackExpiresAt.remove(p.getUniqueId());
        cancelBackExpiryTask(p.getUniqueId());
    }

    private void teleportToLobbyIfEnabled(Player p) {
        if (p == null || !p.isOnline()) return;
        Location lobby = getLobbyLocation();
        if (lobby == null) return;
        if (p.isInsideVehicle()) {
            org.bukkit.entity.Entity vehicle = p.getVehicle();
            p.leaveVehicle();
            if (vehicle != null) vehicle.remove();
        }
        p.teleport(lobby);
        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-teleported")));
    }

    private void sendParticipantsToLobbyAfterRace() {
        for (UUID id : new ArrayList<>(states.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            sendParticipantToLobbyAfterRace(id, p);
        }
    }

    private void sendParticipantToLobbyAfterRace(UUID playerId, Player p) {
        if (playerId == null || p == null || !p.isOnline()) return;

        Location lobby = getLobbyLocation();
        if (lobby == null) return;

        Long existingExpiresAt = preLobbyBackExpiresAt.get(playerId);
        if (existingExpiresAt != null && existingExpiresAt > System.currentTimeMillis()) {
            return;
        }

        cleanupRaceVehicleForPlayer(playerId);
        teleportToLobbyIfEnabled(p);

        Location saved = preLobbyLocations.get(playerId);
        if (saved == null || saved.getWorld() == null) return;

        long expiresAt = System.currentTimeMillis() + backWindowMillis;
        preLobbyBackExpiresAt.put(playerId, expiresAt);
        scheduleBackExpiryNotice(playerId, expiresAt);

        final String backCommand = "/boatracing race back";
        long backWindowSeconds = Math.max(1L, backWindowMillis / 1000L);
        long backWindowMinutes = Math.max(1L, (backWindowSeconds + 59L) / 60L);
        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-waiting-returned")));
        p.sendMessage(color(plugin.pref() + plugin.msg().get(
                "race.registration.lobby-back-window",
                "minutes", String.valueOf(backWindowMinutes),
                "seconds", String.valueOf(backWindowSeconds))));
        p.sendMessage(Text.cmd(plugin.msg().get("race.registration.lobby-back-click"), backCommand));
    }

    private void scheduleBackExpiryNotice(UUID playerId, long expiresAt) {
        cancelBackExpiryTask(playerId);
        long remainingMillis = Math.max(1L, expiresAt - System.currentTimeMillis());
        long delayTicks = Math.max(1L, (remainingMillis + 49L) / 50L);
        SchedulerCompat.TaskHandle handle = SchedulerCompat.runLater(plugin, () -> {
            Long currentExpiresAt = preLobbyBackExpiresAt.get(playerId);
            if (!Objects.equals(currentExpiresAt, expiresAt)) return;
            long now = System.currentTimeMillis();
            if (now < expiresAt) {
                scheduleBackExpiryNotice(playerId, expiresAt);
                return;
            }

            preLobbyBackExpiresAt.remove(playerId);
            preLobbyLocations.remove(playerId);
            backExpiryTasks.remove(playerId);

            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-back-expired")));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            }
        }, delayTicks);
        backExpiryTasks.put(playerId, handle);
    }

    private void cancelBackExpiryTask(UUID playerId) {
        SchedulerCompat.TaskHandle handle = backExpiryTasks.remove(playerId);
        if (handle != null) {
            try { handle.cancel(); } catch (Exception ignored) { plugin.getLogger().finer("Failed to cancel back expiry task: " + ignored.getMessage()); }
        }
    }

    private void cancelAllBackExpiryTasks() {
        for (UUID playerId : new ArrayList<>(backExpiryTasks.keySet())) {
            cancelBackExpiryTask(playerId);
        }
    }

    private void restoreFromLobbyIfNeeded(Player p, boolean onlyWhenLeaving) {
        if (p == null || !p.isOnline()) return;
        if (onlyWhenLeaving && !lobbyReturnOnLeave) return;
        UUID playerId = p.getUniqueId();
        Location prev = preLobbyLocations.remove(playerId);
        preLobbyBackExpiresAt.remove(playerId);
        cancelBackExpiryTask(playerId);
        if (prev != null && prev.getWorld() != null) {
            p.teleport(prev);
            p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-returned")));
        }
    }

    private void restoreLobbyForRegistered(boolean onlyWhenLeaving) {
        if (onlyWhenLeaving && !lobbyReturnOnLeave) return;
        for (UUID id : new ArrayList<>(registered)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                Location prev = preLobbyLocations.remove(id);
                preLobbyBackExpiresAt.remove(id);
                cancelBackExpiryTask(id);
                if (prev != null && prev.getWorld() != null) {
                    p.teleport(prev);
                    p.sendMessage(color(plugin.pref() + plugin.msg().get("race.registration.lobby-returned")));
                }
            }
        }
    }

    public List<Player> placeAtStartsWithBoats(List<Player> participants) {
        List<Player> placed = new ArrayList<>();
        List<TrackConfig.StartSlot> starts = track.getStarts();
        if (starts.isEmpty()) return placed;
        // Defensive: remove any stale race boats before spawning a new grid.
        cleanupRaceVehicles();

        // 1) Build slot assignment map (slotIndex -> player)
        java.util.Map<Integer, Player> slotToPlayer = new java.util.HashMap<>();
        java.util.Set<UUID> unassigned = new java.util.LinkedHashSet<>();
        for (Player p : participants) {
            Integer custom = track.getCustomStartSlot(p.getUniqueId());
            if (custom != null && custom >= 0 && custom < starts.size() && !slotToPlayer.containsKey(custom)) {
                slotToPlayer.put(custom, p);
            } else {
                unassigned.add(p.getUniqueId());
            }
        }
        // 2) Order unassigned by best known time (ascending), players without time go last preserving registration order
        java.util.List<Player> unassignedPlayers = new java.util.ArrayList<>();
        for (UUID id : unassigned) { Player p = Bukkit.getPlayer(id); if (p != null) unassignedPlayers.add(p); }
        unassignedPlayers.sort((a,b) -> {
            Long ta = track.getBestTime(a.getUniqueId(), totalLaps);
            Long tb = track.getBestTime(b.getUniqueId(), totalLaps);
            if (ta == null) ta = track.getBestTime(a.getUniqueId());
            if (tb == null) tb = track.getBestTime(b.getUniqueId());
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1; // a after b
            if (tb == null) return -1; // a before b
            return Long.compare(ta, tb);
        });
        // 3) Fill remaining free slots in natural order
        java.util.List<Integer> freeSlots = new java.util.ArrayList<>();
        for (int i = 0; i < starts.size(); i++) if (!slotToPlayer.containsKey(i)) freeSlots.add(i);
        java.util.Iterator<Player> it = unassignedPlayers.iterator();
        for (int idx : freeSlots) {
            if (!it.hasNext()) break;
            slotToPlayer.put(idx, it.next());
        }

        // 4) Place players into their assigned slots sequentially by slot index
        java.util.List<Integer> finalSlots = new java.util.ArrayList<>(slotToPlayer.keySet());
        java.util.Collections.sort(finalSlots);
        for (int i : finalSlots) {
            if (i < 0 || i >= starts.size()) continue;
            Player p = slotToPlayer.get(i);
            if (p == null) continue;
            var s = starts.get(i);
            org.bukkit.World w = Bukkit.getWorld(s.world);
            if (w == null) continue;
            Location loc = new Location(w, s.x, s.y, s.z, (float) s.yaw, 0.0f);
            if (p.isInsideVehicle()) p.leaveVehicle();
            p.teleport(loc);
            for (org.bukkit.entity.Entity nearby : w.getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                String type = nearby.getType().name();
                if (type.endsWith("BOAT") || type.endsWith("RAFT")) nearby.remove();
            }
            String selectedBoat = plugin.getTeamManager()
                    .getTeamByMember(p.getUniqueId())
                    .map((Team t) -> t.getBoatType(p.getUniqueId()))
                    .orElse(Material.OAK_BOAT.name());
            String normalizedBoat = normalizeBoatMaterialName(selectedBoat);
            boolean chest = normalizedBoat.contains("_CHEST_");
            org.bukkit.entity.Vehicle spawned = spawnRaceVehicle(w, loc, normalizedBoat, chest);
            if (spawned == null) {
                plugin.getLogger().warning("Could not spawn race boat for " + p.getName() + "; skipping start slot #" + (i + 1) + ".");
                continue;
            }
            // Apply selected boat/raft model immediately and with delayed retries.
            // This complements material-specific entity spawn on mixed API versions.
            applyBoatVariantReliably(spawned, normalizedBoat);
            registerRaceVehicle(p.getUniqueId(), spawned);

            if (!spawned.addPassenger(p)) {
                SchedulerCompat.runNow(plugin, () -> {
                    if (spawned.isValid()) spawned.addPassenger(p);
                });
            }
            placed.add(p);
        }
        return placed;
    }

    private org.bukkit.entity.Vehicle spawnRaceVehicle(org.bukkit.World world, Location location, String preferredTypeName, boolean chestFallback) {
        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
        if (preferredTypeName != null && !preferredTypeName.isBlank()) {
            candidates.add(preferredTypeName.toUpperCase(Locale.ROOT));
        }
        candidates.add(chestFallback ? "CHEST_BOAT" : "BOAT");

        for (String candidate : candidates) {
            try {
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(candidate);
                org.bukkit.entity.Entity entity = world.spawnEntity(location, type);
                if (entity instanceof org.bukkit.entity.Vehicle vehicle) {
                    return vehicle;
                }
                plugin.getLogger().finer("Spawned non-vehicle entity for boat candidate " + candidate + ": " + entity.getType().name());
            } catch (IllegalArgumentException ignored) {
                // Candidate not available on this server/API version.
            } catch (Exception ex) {
                plugin.getLogger().finer("Failed to spawn race vehicle candidate " + candidate + ": " + ex.getMessage());
            }
        }
        return null;
    }

    private static String normalizeBoatMaterialName(String raw) {
        if (raw == null || raw.isBlank()) return Material.OAK_BOAT.name();
        String upper = raw.trim().toUpperCase(Locale.ROOT);

        if (upper.equals("BOAT")) return "OAK_BOAT";
        if (upper.equals("CHEST_BOAT")) return "OAK_CHEST_BOAT";
        if (upper.equals("RAFT") || upper.equals("BAMBOO_BOAT")) return "BAMBOO_RAFT";
        if (upper.equals("CHEST_RAFT") || upper.equals("BAMBOO_CHEST_BOAT")) return "BAMBOO_CHEST_RAFT";

        if (upper.endsWith("_BOAT") || upper.endsWith("_CHEST_BOAT") || upper.endsWith("_RAFT") || upper.endsWith("_CHEST_RAFT")) {
            return upper;
        }

        if (upper.startsWith("BAMBOO")) {
            return upper.contains("CHEST") ? "BAMBOO_CHEST_RAFT" : "BAMBOO_RAFT";
        }

        return upper + "_BOAT";
    }

    private void applyBoatVariantReliably(org.bukkit.entity.Vehicle vehicle, String materialName) {
        if (vehicle == null || materialName == null || materialName.isBlank()) return;
        applyBoatVariantUniversal(vehicle, materialName);
        SchedulerCompat.runLater(plugin, () -> {
            if (vehicle.isValid()) applyBoatVariantUniversal(vehicle, materialName);
        }, 1L);
        SchedulerCompat.runLater(plugin, () -> {
            if (vehicle.isValid()) applyBoatVariantUniversal(vehicle, materialName);
        }, 10L);
    }

    private void registerRaceVehicle(UUID playerId, org.bukkit.entity.Vehicle vehicle) {
        if (vehicle == null) return;
        UUID vehicleId = vehicle.getUniqueId();
        raceVehicleIds.add(vehicleId);
        if (playerId != null) raceVehicleByPlayer.put(playerId, vehicleId);
    }

    private void cleanupRaceVehicleForPlayer(UUID playerId) {
        if (playerId == null) return;

        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline() && p.isInsideVehicle()) {
            org.bukkit.entity.Entity currentVehicle = p.getVehicle();
            p.leaveVehicle();
            if (currentVehicle != null) {
                String type = currentVehicle.getType().name();
                if (type.endsWith("BOAT") || type.endsWith("RAFT")) {
                    currentVehicle.remove();
                }
            }
        }

        UUID vehicleId = raceVehicleByPlayer.remove(playerId);
        if (vehicleId == null) return;

        raceVehicleIds.remove(vehicleId);
        org.bukkit.entity.Entity trackedVehicle = Bukkit.getEntity(vehicleId);
        if (trackedVehicle != null && trackedVehicle.isValid()) {
            String type = trackedVehicle.getType().name();
            if (type.endsWith("BOAT") || type.endsWith("RAFT")) {
                trackedVehicle.remove();
            }
        }
    }

    private void cleanupRaceVehicles() {
        if (raceVehicleIds.isEmpty() && raceVehicleByPlayer.isEmpty()) return;

        // First dismount participants still riding tracked race vehicles.
        for (UUID playerId : new ArrayList<>(raceVehicleByPlayer.keySet())) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline() && p.isInsideVehicle()) {
                org.bukkit.entity.Entity vehicle = p.getVehicle();
                p.leaveVehicle();
                if (vehicle != null) {
                    String type = vehicle.getType().name();
                    if (type.endsWith("BOAT") || type.endsWith("RAFT")) {
                        vehicle.remove();
                    }
                }
            }
        }

        // Then remove any remaining tracked race vehicles by entity id.
        for (UUID vehicleId : new ArrayList<>(raceVehicleIds)) {
            org.bukkit.entity.Entity vehicle = Bukkit.getEntity(vehicleId);
            if (vehicle != null && vehicle.isValid()) {
                String type = vehicle.getType().name();
                if (type.endsWith("BOAT") || type.endsWith("RAFT")) {
                    vehicle.remove();
                }
            }
        }

        raceVehicleIds.clear();
        raceVehicleByPlayer.clear();
    }

    // Try to set wood/raft variant on Boat or ChestBoat by reflecting setVariant or setBoatType
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void applyBoatVariantUniversal(Object boatEntity, String materialName) {
        if (boatEntity == null || materialName == null) return;
        String upper = materialName.toUpperCase(Locale.ROOT);
        String base = upper
                .replace("_CHEST_BOAT", "")
                .replace("_BOAT", "")
                .replace("_CHEST_RAFT", "")
                .replace("_RAFT", "");
        // Normalize raft tokens to BAMBOO
        if (base.isEmpty() || base.equals("RAFT") || upper.contains("BAMBOO")) {
            base = "BAMBOO";
        }
        Class<?> cls = boatEntity.getClass();
        // 1) setVariant(Enum)
    try {
            java.lang.reflect.Method m = null;
            for (java.lang.reflect.Method mm : cls.getMethods()) {
                if (mm.getName().equals("setVariant") && mm.getParameterCount() == 1 && mm.getParameterTypes()[0].isEnum()) { m = mm; break; }
            }
            if (m != null) {
                Class<?> enumType = m.getParameterTypes()[0];
                Enum variant = Enum.valueOf((Class) enumType.asSubclass(Enum.class), base);
                m.invoke(boatEntity, variant);
                return;
            }
    } catch (Exception ignored) { var inst = BoatRacingPlugin.getInstance(); if (inst != null) inst.getLogger().finer("setVariant invocation failed: " + ignored.getMessage()); }
        // 2) setBoatType(Enum)
    try {
            java.lang.reflect.Method m = null;
            for (java.lang.reflect.Method mm : cls.getMethods()) {
                if (mm.getName().equals("setBoatType") && mm.getParameterCount() == 1 && mm.getParameterTypes()[0].isEnum()) { m = mm; break; }
            }
            if (m != null) {
                Class<?> enumType = m.getParameterTypes()[0];
                Enum type = null;
                try { type = Enum.valueOf((Class) enumType.asSubclass(Enum.class), base); } catch (IllegalArgumentException ex) { /* map unknown to OAK */ }
                if (type == null) {
                    try { type = Enum.valueOf((Class) enumType.asSubclass(Enum.class), "OAK"); } catch (Exception ignored2) { var inst = BoatRacingPlugin.getInstance(); if (inst != null) inst.getLogger().finer("Failed to map boat enum to OAK: " + ignored2.getMessage()); }
                }
                if (type != null) m.invoke(boatEntity, type);
            }
    } catch (Exception ignored) { var inst = BoatRacingPlugin.getInstance(); if (inst != null) inst.getLogger().finer("setBoatType invocation failed: " + ignored.getMessage()); }
    }

    // --- Countdown with 5 start lights and false-start check ---
    public void startRaceWithCountdown(List<Player> participants) {
        startRaceWithCountdown(participants, false);
    }

    public void startRaceWithCountdown(List<Player> participants, boolean practice) {
        practiceMode = practice;
        practicePlayerId = (practice && participants != null && !participants.isEmpty())
                ? participants.get(0).getUniqueId()
                : null;
        List<TrackConfig.LightPos> ls = track.getLights();
        if (ls.size() != 5) {
            clearCountdownLock();
            startRace(participants, practice);
            return;
        }
        setCountdownLock(participants);

        // Prepare per-player origin and forward vectors
        Map<UUID, Location> origins = new HashMap<>();
        Map<UUID, org.bukkit.util.Vector> forwards = new HashMap<>();
        Set<UUID> penalized = new HashSet<>();
        for (Player p : participants) {
            Location loc = p.getLocation().clone();
            origins.put(p.getUniqueId(), loc);
            double rad = Math.toRadians(loc.getYaw());
            org.bukkit.util.Vector fwd = new org.bukkit.util.Vector(-Math.sin(rad), 0, Math.cos(rad));
            if (fwd.lengthSquared() > 0) fwd.normalize();
            forwards.put(p.getUniqueId(), fwd);
        }

        // Ensure all lamps start unlit
        for (var lp : ls) {
            org.bukkit.block.Block b = lp.getBlock();
            if (b != null && b.getType() == org.bukkit.Material.REDSTONE_LAMP) {
                org.bukkit.block.data.BlockData bd = b.getBlockData();
                if (bd instanceof org.bukkit.block.data.Lightable lightable) {
                    if (lightable.isLit()) { lightable.setLit(false); b.setBlockData(lightable, false); }
                }
            }
        }

        final int[] idx = {0};
        final SchedulerCompat.TaskHandle[] taskRef = new SchedulerCompat.TaskHandle[1];
    final SchedulerCompat.TaskHandle[] checkTaskRef = new SchedulerCompat.TaskHandle[1];

        // False start checker (every 2 ticks) until GO
     if (enableFalseStartPenalty) {
            this.countdownCheckTask = checkTaskRef[0] = SchedulerCompat.runTimer(plugin, () -> {
                if (idx[0] >= ls.size()) return;
                for (Player p : participants) {
                    if (penalized.contains(p.getUniqueId()) || !p.isOnline()) continue;
                    Location origin = origins.get(p.getUniqueId());
                    org.bukkit.util.Vector fwd = forwards.get(p.getUniqueId());
                    if (origin == null || fwd == null) continue;
                    Location now = p.getLocation();
                    if (!now.getWorld().equals(origin.getWorld())) continue;
                    org.bukkit.util.Vector delta = now.toVector().subtract(origin.toVector());
                    double forwardDist = delta.dot(fwd);
                    if (forwardDist > 0.25) {
                        penalized.add(p.getUniqueId());
                        long add = (long) Math.round(falseStartPenaltySeconds * 1000.0);
                        preStartPenalties.merge(p.getUniqueId(), add, Long::sum);
                        p.sendMessage(color(plugin.pref() + plugin.msg().get("race.false-start", "time", formatSeconds(add))));
                        if (!practiceMode) {
                            broadcast(color(plugin.msg().get("race.false-start-broadcast", "player", p.getName(), "time", formatSeconds(add))));
                        }
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
                    }
                }
            }, 1L, 2L);
        }

        // Lights countdown (1 per second)
        this.countdownLightTask = taskRef[0] = SchedulerCompat.runTimer(plugin, () -> {
            if (idx[0] >= ls.size()) {
                if (taskRef[0] != null) taskRef[0].cancel();
                // Keep false-start checker running until actual GO
                double jitter = lightsOutJitterSeconds > 0.0 ? Math.random() * lightsOutJitterSeconds : 0.0;
                long delayTicks = Math.max(0L, Math.round((lightsOutDelaySeconds + jitter) * 20.0));
                SchedulerCompat.runLater(plugin, () -> {
                    // Now turn all lights off and GO
                    for (var lp2 : ls) {
                        org.bukkit.block.Block b2 = lp2.getBlock();
                        if (b2 != null && b2.getType() == org.bukkit.Material.REDSTONE_LAMP) {
                            org.bukkit.block.data.BlockData bd2 = b2.getBlockData();
                            if (bd2 instanceof org.bukkit.block.data.Lightable lightable2) {
                                if (lightable2.isLit()) { lightable2.setLit(false); b2.setBlockData(lightable2, false); }
                            }
                        }
                    }
                    if (checkTaskRef[0] != null) checkTaskRef[0].cancel();
                    clearCountdownLock();
                    Collection<Player> recipsGo = raceAudience(participants);
                    for (Player p : recipsGo) p.sendMessage(color(plugin.msg().get("race.go")));
                    for (Player p : recipsGo) p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    startRace(participants, practice);
                }, delayTicks);
                return;
            }
            var lp = ls.get(idx[0]);
            org.bukkit.block.Block b = lp.getBlock();
            if (b != null && b.getType() == org.bukkit.Material.REDSTONE_LAMP) {
                org.bukkit.block.data.BlockData bd = b.getBlockData();
                if (bd instanceof org.bukkit.block.data.Lightable lightable) {
                    lightable.setLit(true);
                    b.setBlockData(lightable, false);
                }
            }
            int remaining = ls.size() - idx[0];
            Collection<Player> recips = raceAudience(participants);
            for (Player p : recips) p.sendMessage(color(plugin.msg().get("race.starting-countdown", "count", String.valueOf(remaining))));
            for (Player p : recips) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.6f);
            idx[0]++;
        }, 0L, 20L);
    }

    private void setCountdownLock(Collection<Player> participants) {
        countdownLockedParticipants.clear();
        for (Player p : participants) {
            if (p != null) countdownLockedParticipants.add(p.getUniqueId());
        }
    }

    private void clearCountdownLock() {
        if (countdownLightTask != null) { try { countdownLightTask.cancel(); } catch (Exception ignored) {} countdownLightTask = null; }
        if (countdownCheckTask != null) { try { countdownCheckTask.cancel(); } catch (Exception ignored) {} countdownCheckTask = null; }
        countdownLockedParticipants.clear();
    }

    // --- Scoreboard (sidebar) showing Lap, Checkpoints, and Time ---
    private void setupPlayerBoard(Player p) {
        try {
            ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm == null) return;
            UUID pid = p.getUniqueId();
            Scoreboard current = p.getScoreboard();
            Scoreboard main = sm.getMainScoreboard();

            // Hard compatibility mode: if player is already on an external/custom board,
            // do not touch sidebar at all (ActionBar-only during race).
            boolean externalBoard = current != null && current != main && !isBoatRacingBoard(current);
            if (externalBoard) {
                previousScoreboards.putIfAbsent(pid, current);
                sidebarDisabledPlayers.add(pid);
                scoreboards.remove(pid);
                overlayPlayers.remove(pid);
                previousSidebarObjectives.remove(pid);
                return;
            }

            sidebarDisabledPlayers.remove(pid);
            // Preserve the player's active board so external plugins (e.g. SimpleScore) can be restored.
            // Never overwrite with our own board if setup gets called again during the race.
            if (current != null && !isBoatRacingBoard(current)) {
                previousScoreboards.put(pid, current);
            }
            Scoreboard sb = null;
            boolean canOverlay = current != null && current != main && !isBoatRacingBoard(current);
            if (canOverlay && current != null) {
                sb = current;
                overlayPlayers.add(pid);
                previousSidebarObjectives.putIfAbsent(pid, current.getObjective(DisplaySlot.SIDEBAR));
            } else {
                sb = sm.getNewScoreboard();
                if (sb == null) return;
                overlayPlayers.remove(pid);
                previousSidebarObjectives.remove(pid);
            }
            Objective obj = sb.getObjective(SCOREBOARD_OBJECTIVE_NAME); // constant name to play nice with external plugins (e.g., TAB)
            if (obj == null) {
                obj = sb.registerNewObjective(
                    SCOREBOARD_OBJECTIVE_NAME,
                    Criteria.DUMMY,
                    Component.text("BoatRacing", NamedTextColor.GOLD)
                );
            }
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            for (int i = 0; i < SCOREBOARD_ENTRIES.length; i++) {
                String teamName = "br_ln_" + (i+1);
                org.bukkit.scoreboard.Team t = sb.getTeam(teamName); if (t == null) t = sb.registerNewTeam(teamName);
                t.addEntry(SCOREBOARD_ENTRIES[i]);
            }
            if (!canOverlay) {
                p.setScoreboard(sb);
            }
            scoreboards.put(pid, sb);
    } catch (Exception ignored) { BoatRacingPlugin.getInstance().getLogger().finer("setupPlayerBoard failed: " + ignored.getMessage()); }
    }

    private int simpleScoreRefixTickCounter = 39;

    private void startScoreboard() {
        if (scoreboardTask != null) return;
        scoreboardTask = SchedulerCompat.runTimer(plugin, () -> {
            // Periodically re-hide SimpleScore to prevent it from reclaiming the sidebar
            simpleScoreRefixTickCounter++;
            if (simpleScoreRefixTickCounter >= 40) {
                simpleScoreRefixTickCounter = 0;
                for (UUID id : new ArrayList<>(simpleScoreHiddenByBoatRacing)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        trySimpleScoreHide(p);
                    }
                }
            }

            // Build global ordering once per tick
            List<Map.Entry<UUID, RaceState>> ordered = getOrderedRaceEntries();

            int totalCPs = track.getCheckpoints().size();

            // Update each viewer independently (to highlight own name)
            for (UUID viewerId : new ArrayList<>(states.keySet())) {
                Player viewer = Bukkit.getPlayer(viewerId);
                RaceState viewerState = states.get(viewerId);
                if (viewer == null || viewerState == null) continue;
                Scoreboard sb = scoreboards.get(viewerId);
                if (sb == null && !sidebarDisabledPlayers.contains(viewerId)) {
                    setupPlayerBoard(viewer);
                    sb = scoreboards.get(viewerId);
                }

                if (sb != null) {
                    // Build lines for this viewer
                    java.util.List<Component> lines = new java.util.ArrayList<>();
                    if (!practiceMode) {
                        // Spacer line to create space between title and first row
                        lines.add(Component.text(" "));
                    } else {
                        lines.add(Component.text(resolvePracticeStatusLine(), NamedTextColor.AQUA, TextDecoration.BOLD));
                        appendPracticeSidebarTimes(lines, viewerId, viewerState);
                    }

                    int maxRows = 10;
                    int rowCount = Math.min(maxRows, ordered.size());

                    // Build content segments without centering/padding (1.0.8 requirement)
                    String[] nameShown = new String[rowCount];
                    boolean[] isViewerRow = new boolean[rowCount];
                    for (int i = 0; i < rowCount; i++) {
                        Map.Entry<UUID, RaceState> e = ordered.get(i);
                        UUID pid = e.getKey();
                        // Name
                        String baseName;
                        Player online = Bukkit.getPlayer(pid);
                        if (online != null) {
                            baseName = safeRenderName(online);
                        } else {
                            baseName = safeOfflineName(Bukkit.getOfflinePlayer(pid));
                            if (baseName == null) baseName = pid.toString().substring(0, 8);
                        }
                        if (baseName == null) baseName = pid.toString().substring(0, 8);
                        String shown = baseName.length() > 12 ? baseName.substring(0, 12) + "..." : baseName;
                        nameShown[i] = shown;
                        boolean isViewer = pid.equals(viewerId);
                        isViewerRow[i] = isViewer;
                    }

                    for (int i = 0; i < rowCount; i++) {
                        Map.Entry<UUID, RaceState> e = ordered.get(i);
                        RaceState st = e.getValue();
                        String shown = nameShown[i];
                        boolean isViewer = isViewerRow[i];

                        String rankStr = (i + 1) + ". ";
                        NamedTextColor rankColor = switch (i) {
                            case 0 -> NamedTextColor.GOLD;
                            case 1 -> NamedTextColor.GRAY; // silver-ish
                            case 2 -> NamedTextColor.DARK_RED; // bronze-ish alternative
                            default -> NamedTextColor.DARK_GRAY;
                        };
                        // No centering/padding
                        Component leftPad = Component.empty();
                        Component line;
                        if (st.finished) {
                            line = leftPad;
                            if (sbShowPos) line = line.append(Component.text(rankStr, rankColor));
                            // finished: show FIN + time as lap/time segment if desired
                            Component finSeg = Component.text("FIN ", NamedTextColor.GREEN)
                                    .append(Component.text(formatSeconds(st.finishTime), NamedTextColor.WHITE));
                            // If neither lap nor CP are shown, we still show FIN/time always
                            line = line.append(finSeg);
                            if (sbShowName) {
                                line = line.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                                           .append(Component.text(shown, isViewer ? NamedTextColor.GREEN : NamedTextColor.WHITE));
                            }
                        } else {
                            Component lapC = Component.text("L ", NamedTextColor.YELLOW)
                                    .append(Component.text(st.lap + 1, NamedTextColor.WHITE))
                                    .append(Component.text("/", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(totalLaps, NamedTextColor.WHITE));
                            Component cpC = Component.empty();
                            if (totalCPs > 0) {
                                cpC = Component.text(" CP ", NamedTextColor.YELLOW)
                                        .append(Component.text(st.nextCheckpoint, NamedTextColor.WHITE))
                                        .append(Component.text("/", NamedTextColor.DARK_GRAY))
                                        .append(Component.text(totalCPs, NamedTextColor.WHITE));
                            }
                            // Pitstops done/required, shown only if requirement > 0
                            Component pitC = Component.empty();
                            if (mandatoryPitstops > 0) {
                                int done = pitCount.getOrDefault(e.getKey(), 0);
                                pitC = Component.text(" PIT ", NamedTextColor.YELLOW)
                                        .append(Component.text(done, NamedTextColor.WHITE))
                                        .append(Component.text("/", NamedTextColor.DARK_GRAY))
                                        .append(Component.text(mandatoryPitstops, NamedTextColor.WHITE));
                            }
                            line = leftPad;
                            if (sbShowPos) line = line.append(Component.text(rankStr, rankColor));
                            if (sbShowLap) line = line.append(lapC);
                            if (sbShowCP && totalCPs > 0) line = line.append(cpC);
                            if (sbShowPit && mandatoryPitstops > 0) line = line.append(pitC);
                            if (sbShowName) line = line.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                                                    .append(Component.text(shown, isViewer ? NamedTextColor.GREEN : NamedTextColor.WHITE));
                        }
                        lines.add(line);
                    }

                    Objective obj = sb.getObjective(SCOREBOARD_OBJECTIVE_NAME);
                    if (obj == null) continue;
                    int visibleLines = Math.min(lines.size(), SCOREBOARD_MAX_LINES);

                    // Push lines into scoreboard teams and show only used score entries.
                    for (int i = 0; i < SCOREBOARD_MAX_LINES; i++) {
                        String teamName = "br_ln_" + (i+1);
                        org.bukkit.scoreboard.Team t = sb.getTeam(teamName);
                        if (t == null) continue;
                        Component content = i < visibleLines ? lines.get(i) : Component.empty();
                        try { t.prefix(content); } catch (Exception ignored) { plugin.getLogger().finer("Failed to set scoreboard team prefix: " + ignored.getMessage()); }

                        String entry = SCOREBOARD_ENTRIES[i];
                        try {
                            if (i < visibleLines) obj.getScore(entry).setScore(visibleLines - i);
                            else sb.resetScores(entry);
                        } catch (Exception ignored) {
                            plugin.getLogger().finer("Failed to set/reset scoreboard score entry: " + ignored.getMessage());
                        }
                    }
                }

                // Send ActionBar with personal stats (omit CP when none). Update frequently for smooth ms
                int doneCP = Math.min(viewerState.nextCheckpoint, totalCPs);
                Component ab = Component.text("");
                boolean firstSeg = true;
                if (abShowLap) {
                    Component seg = Component.text("Lap ", NamedTextColor.YELLOW)
                            .append(Component.text(viewerState.lap + "/" + totalLaps, NamedTextColor.WHITE));
                    ab = ab.append(seg);
                    firstSeg = false;
                }
                if (abShowCP && totalCPs > 0) {
                    if (!firstSeg) ab = ab.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                    Component seg = Component.text("CP ", NamedTextColor.YELLOW)
                            .append(Component.text(doneCP + "/" + totalCPs, NamedTextColor.WHITE));
                    ab = ab.append(seg);
                    firstSeg = false;
                }
                if (abShowPit && mandatoryPitstops > 0) {
                    if (!firstSeg) ab = ab.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                    int done = pitCount.getOrDefault(viewerId, 0);
                    Component seg = Component.text("Pit ", NamedTextColor.YELLOW)
                            .append(Component.text(done + "/" + mandatoryPitstops, NamedTextColor.WHITE));
                    ab = ab.append(seg);
                    firstSeg = false;
                }
                if (abShowTime) {
                    if (!firstSeg) ab = ab.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                    Component seg = Component.text("Time ", NamedTextColor.YELLOW)
                            .append(Component.text(formatSeconds(timeFor(viewerState)), NamedTextColor.WHITE));
                    ab = ab.append(seg);
                }
                try { viewer.sendActionBar(ab); } catch (Exception ignored) { plugin.getLogger().finer("sendActionBar failed for " + viewer.getName() + ": " + ignored.getMessage()); }
            }
        }, 0L, 2L);
    }

    // Produce a readable player name. Prefer displayName but strip rank prefixes
    // and fall back to the account username. Preserve leading '.' for Bedrock.
    private String safeRenderName(Player online) {
        String account = online.getName();
        String raw;
        try {
            raw = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(online.displayName());
        } catch (Exception t) {
            // If displayName serialization fails, fall back to the account username.
            raw = account;
        }
        if (raw == null || raw.isEmpty()) raw = account;

        // Try to find the account name inside the raw display name (case-insensitive) and use from there
        String rawLower = raw.toLowerCase(java.util.Locale.ROOT);
        String accLower = account != null ? account.toLowerCase(java.util.Locale.ROOT) : "";
        String base;
        int idx = !accLower.isEmpty() ? rawLower.indexOf(accLower) : -1;
        if (idx >= 0) {
            base = raw.substring(idx).trim();
        } else {
            base = stripLeadingWrappers(raw).trim();
        }
    if (base == null || base.isEmpty()) base = account != null ? account : "";
    // Ensure leading '.' is visible for Bedrock (account name starts with '.')
    if (account != null && account.startsWith(".") && (base.isEmpty() || !base.startsWith("."))) {
            base = "." + base;
        }
    return base != null ? base : "";
    }

    // Remove leading bracketed wrappers like [Rank], (Rank), {Rank}, <Rank>, repeated
    private static String stripLeadingWrappers(String s) {
        String out = s;
        boolean changed = true;
        while (changed) {
            changed = false;
            String trimmed = out.trim();
            String next = trimmed.replaceFirst("^(?:\\[[^\\]]*\\]|\\([^)]*\\)|\\{[^}]*\\}|<[^>]*>)+\\s*", "");
            if (!next.equals(trimmed)) { out = next; changed = true; }
        }
        return out;
    }

    private void stopScoreboard() {
        simpleScoreRefixTickCounter = 0;
        if (scoreboardTask != null) {
            try { scoreboardTask.cancel(); } catch (Exception ignored) { plugin.getLogger().finer("Failed to cancel scoreboard task: " + ignored.getMessage()); }
            scoreboardTask = null;
        }
        java.util.Set<UUID> ids = new java.util.LinkedHashSet<>(scoreboards.keySet());
        ids.addAll(previousScoreboards.keySet());
        ids.addAll(sidebarDisabledPlayers);
        ids.addAll(overlayPlayers);
        ids.addAll(previousSidebarObjectives.keySet());
        for (UUID id : ids) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) {
                Scoreboard our = scoreboards.get(id);
                Scoreboard prev = previousScoreboards.get(id);
                try {
                    if (overlayPlayers.contains(id)) {
                        restoreOverlaySidebar(pl, our, previousSidebarObjectives.get(id));
                        continue;
                    }
                    Scoreboard current = pl.getScoreboard();
                    boolean usingOurBoard = (our != null && current == our) || isBoatRacingBoard(current);
                    if (usingOurBoard) {
                        restorePreviousScoreboard(pl, prev);
                    }
                } catch (Exception ignored) {
                    plugin.getLogger().finer("Failed to restore previous scoreboard for " + pl.getName() + ": " + ignored.getMessage());
                }
            }
        }
        scoreboards.clear();
        previousScoreboards.clear();
        sidebarDisabledPlayers.clear();
        overlayPlayers.clear();
        previousSidebarObjectives.clear();
        restoreSimpleScoreForAll();
    }

    private void restoreSimpleScoreForAll() {
        if (simpleScoreHiddenByBoatRacing.isEmpty()) return;
        for (UUID id : new ArrayList<>(simpleScoreHiddenByBoatRacing)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                trySimpleScoreShow(p);
            }
        }
        simpleScoreHiddenByBoatRacing.clear();
    }

    private boolean trySimpleScoreHide(Player player) {
        return trySimpleScoreToggle(player, true);
    }

    private boolean trySimpleScoreShow(Player player) {
        return trySimpleScoreToggle(player, false);
    }

    private boolean trySimpleScoreToggle(Player player, boolean hide) {
        if (player == null || !player.isOnline()) return false;
        if (Bukkit.getPluginManager().getPlugin("SimpleScore") == null) return false;
        try {
            Class<?> managerClass = Class.forName("com.r4g3baby.simplescore.api.Manager");
            Method getInstance = managerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);
            if (manager == null) return false;

            Method getViewer = managerClass.getMethod("getViewer", UUID.class);
            Object viewer = getViewer.invoke(manager, player.getUniqueId());
            if (viewer == null) return false;

            Method getPlatform = managerClass.getMethod("getPlatform");
            Object platform = getPlatform.invoke(manager);
            if (platform == null) return false;

            Method getProvider = platform.getClass().getMethod("getProvider");
            Object provider = getProvider.invoke(platform);
            if (provider == null) return false;

            String methodName = hide ? "hideScoreboard" : "showScoreboard";
            Method toggle = viewer.getClass().getMethod(methodName, provider.getClass());
            Object result = toggle.invoke(viewer, provider);
            if (result instanceof Boolean b) return b;
            return true;
        } catch (Throwable ignored) {
            plugin.getLogger().finer("SimpleScore hook " + (hide ? "hide" : "show") + " failed for " + player.getName() + ": " + ignored.getMessage());
            return false;
        }
    }

    private void restoreOverlaySidebar(Player player, Scoreboard board, Objective previousSidebar) {
        if (player == null || !player.isOnline() || board == null) return;
        try {
            Objective ours = board.getObjective("boatracing");
            if (previousSidebar != null) {
                previousSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else if (ours != null && ours.getDisplaySlot() == DisplaySlot.SIDEBAR) {
                ours.setDisplaySlot(null);
            }
            if (ours != null) {
                try { ours.unregister(); } catch (Exception ignored) { plugin.getLogger().finer("Failed to unregister temporary objective: " + ignored.getMessage()); }
            }
        } catch (Exception ignored) {
            plugin.getLogger().finer("Failed to restore overlay sidebar for " + player.getName() + ": " + ignored.getMessage());
        }
    }

    private void restorePreviousScoreboard(Player player, Scoreboard previous) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard fallback = sm.getMainScoreboard();
        Scoreboard target = previous != null ? previous : fallback;
        if (target != null && isBoatRacingBoard(target)) target = fallback;
        if (target == null) return;

        // First break ownership by returning to the main board, then restore target.
        try { player.setScoreboard(fallback); } catch (Exception ignored) { plugin.getLogger().finer("Scoreboard fallback handoff failed for " + player.getName() + ": " + ignored.getMessage()); }

        restoreScoreboardAttempt(player, target, 1L);
        restoreScoreboardAttempt(player, target, 20L);
        restoreScoreboardAttempt(player, target, 60L);
        restoreScoreboardAttempt(player, target, 120L);
        restoreScoreboardAttempt(player, target, 200L);
    }

    private void restoreScoreboardAttempt(Player player, Scoreboard target, long delayTicks) {
        SchedulerCompat.runLater(plugin, () -> {
            if (player == null || !player.isOnline()) return;
            ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm == null) return;
            Scoreboard current = player.getScoreboard();
            // If another plugin already restored a custom board, do not override it.
            boolean safeToApply = isBoatRacingBoard(current) || current == sm.getMainScoreboard() || current == null;
            if (!safeToApply || current == target) return;
            try {
                player.setScoreboard(target);
            } catch (Exception ignored) {
                plugin.getLogger().finer("Delayed scoreboard restore (" + delayTicks + "t) failed for " + player.getName() + ": " + ignored.getMessage());
            }
        }, Math.max(1L, delayTicks));
    }

    private boolean isBoatRacingBoard(Scoreboard sb) {
        if (sb == null) return false;
        try {
            Objective obj = sb.getObjective("boatracing");
            return obj != null && obj.getDisplaySlot() == DisplaySlot.SIDEBAR;
        } catch (Exception ignored) {
            return false;
        }
    }

    // Number hiding helpers removed by request

    private void broadcast(String msg) {
        boolean globalMode = this.registering || "global".equalsIgnoreCase(this.broadcastMode);
        for (Player p : Bukkit.getOnlinePlayers()) {
            // When broadcast is set to racers and the player is not a racer skip.
            UUID playerId = p.getUniqueId();
            if (!globalMode
                    && !states.containsKey(playerId)
                    && !countdownLockedParticipants.contains(playerId)) {
                continue;
            }
            p.sendMessage(msg);
        }
    }

    // removed unused sendTo helper

    private boolean isAdmin(Player p) {
        return p.hasPermission("boatracing.race.admin") || p.hasPermission("boatracing.setup");
    }

    private Collection<Player> participantsAndAdmins(Collection<Player> participants) {
        LinkedHashSet<Player> set = new LinkedHashSet<>(participants);
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (isAdmin(op)) set.add(op);
        }
        return set;
    }

    // Helper: current participants from states map
    private Collection<Player> statesToPlayers() {
        LinkedHashSet<Player> set = new LinkedHashSet<>();
        for (UUID id : states.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) set.add(p);
        }
        return set;
    }

    private static boolean segmentIntersectsBox(Location from, Location to, org.bukkit.util.BoundingBox box) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < 1e-10) {
            if (from.getX() < box.getMinX() || from.getX() > box.getMaxX()) return false;
        } else {
            double t1 = (box.getMinX() - from.getX()) / dx;
            double t2 = (box.getMaxX() - from.getX()) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dy) < 1e-10) {
            if (from.getY() < box.getMinY() || from.getY() > box.getMaxY()) return false;
        } else {
            double t1 = (box.getMinY() - from.getY()) / dy;
            double t2 = (box.getMaxY() - from.getY()) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dz) < 1e-10) {
            if (from.getZ() < box.getMinZ() || from.getZ() > box.getMaxZ()) return false;
        } else {
            double t1 = (box.getMinZ() - from.getZ()) / dz;
            double t2 = (box.getMaxZ() - from.getZ()) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }
}
