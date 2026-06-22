package es.jaie55.boatracing;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
// No TabExecutor needed: JavaPlugin already handles CommandExecutor and TabCompleter when overriding methods
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import es.jaie55.boatracing.team.TeamManager;
import es.jaie55.boatracing.ui.TeamGUI;
import es.jaie55.boatracing.util.Text;
import es.jaie55.boatracing.util.SchedulerCompat;
import es.jaie55.boatracing.update.UpdateChecker;
import es.jaie55.boatracing.update.UpdateNotifier;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.track.TrackLibrary;
import es.jaie55.boatracing.track.Region;
import es.jaie55.boatracing.track.SelectionUtils;
import es.jaie55.boatracing.race.RaceManager;
import es.jaie55.boatracing.reward.RewardManager;
import es.jaie55.boatracing.setup.SetupWizard;
import es.jaie55.boatracing.util.MessageManager;
import es.jaie55.boatracing.util.DocumentStore;
import es.jaie55.boatracing.util.DocumentStoreFactory;
import es.jaie55.boatracing.util.PracticeGhostManager;
import es.jaie55.boatracing.util.PracticeStatsManager;
import es.jaie55.boatracing.util.StatsManager;
import es.jaie55.boatracing.placeholder.BoatRacingPlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class BoatRacingPlugin extends JavaPlugin {
    private static BoatRacingPlugin instance;
    private TeamManager teamManager;
    private TeamGUI teamGUI;
    private es.jaie55.boatracing.ui.AdminGUI adminGUI;
    private es.jaie55.boatracing.ui.AdminRaceGUI adminRaceGUI;
    private String prefix;
    private UpdateChecker updateChecker;
    private TrackConfig trackConfig;
    private TrackLibrary trackLibrary;
    private RaceManager raceManager;
    private final java.util.Map<String, RaceManager> raceSessions = new java.util.LinkedHashMap<>();
    private boolean mapVoteOpen = false;
    private final java.util.Map<String, Integer> mapVoteCounts = new java.util.LinkedHashMap<>();
    private final java.util.Map<java.util.UUID, String> mapVotesByPlayer = new java.util.HashMap<>();
    private SchedulerCompat.TaskHandle mapVoteTask;
    private String mapVoteCommandLabel = "boatracing";
    private RewardManager rewardManager;
    private SetupWizard setupWizard;
    private MessageManager messageManager;
    private DocumentStore documentStore;
    private StatsManager statsManager;
    private PracticeStatsManager practiceStatsManager;
    private PracticeGhostManager practiceGhostManager;
    private BoatRacingPlaceholderExpansion placeholderExpansion;
    private es.jaie55.boatracing.track.SelectionVisualizer selectionVisualizer;
    private es.jaie55.boatracing.ui.AdminTracksGUI tracksGUI;
    private es.jaie55.boatracing.ui.VoteGUI voteGUI;
    // Last latest-version announced in console due to 5-minute silent checks (to avoid duplicate prints)
    private volatile String lastConsoleAnnouncedVersion = null;
    // Track pending disband confirmations per player
    private final java.util.Set<java.util.UUID> pendingDisband = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, java.util.UUID> pendingTransfer = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, java.util.UUID> pendingKick = new java.util.HashMap<>();
        private static final java.util.List<String> BUNDLED_LANGUAGE_CODES = java.util.Arrays.asList(
            "en", "es", "es_419", "fr", "pt_BR", "pt_PT", "de", "it", "pl", "tr", "ja", "ko", "sv", "zh_TW", "zh_CN", "ru"
        );

    public static BoatRacingPlugin getInstance() { return instance; }
    public TeamManager getTeamManager() { return teamManager; }
    public String pref() { return prefix; }
    public es.jaie55.boatracing.ui.AdminGUI getAdminGUI() { return adminGUI; }
    public es.jaie55.boatracing.ui.AdminRaceGUI getAdminRaceGUI() { return adminRaceGUI; }
    public es.jaie55.boatracing.ui.TeamGUI getTeamGUI() { return teamGUI; }
    public RaceManager getRaceManager() { return raceManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public TrackConfig getTrackConfig() { return trackConfig; }
    public TrackLibrary getTrackLibrary() { return trackLibrary; }
    public es.jaie55.boatracing.ui.AdminTracksGUI getTracksGUI() { return tracksGUI; }
    public es.jaie55.boatracing.ui.VoteGUI getVoteGUI() { return voteGUI; }
    public MessageManager msg() { return messageManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public PracticeStatsManager getPracticeStatsManager() { return practiceStatsManager; }
    public PracticeGhostManager getPracticeGhostManager() { return practiceGhostManager; }
    public DocumentStore getDocumentStore() { return documentStore; }
    public java.util.Collection<RaceManager> getAllRaceManagers() {
        java.util.List<RaceManager> out = new java.util.ArrayList<>();
        out.addAll(raceSessions.values());
        if (raceManager != null && !out.contains(raceManager)) out.add(raceManager);
        return out;
    }

    private RaceManager findRaceSessionByKey(String trackKey) {
        for (java.util.Map.Entry<String, RaceManager> e : raceSessions.entrySet()) {
            if (e.getKey().equalsIgnoreCase(trackKey)) return e.getValue();
        }
        return null;
    }

    public RaceManager getRaceManagerByTrack(String trackName) {
        if (trackName == null || trackName.isBlank()) return null;
        String key = normalizeTrackKey(trackName);
        if (key.equalsIgnoreCase("unsaved")) return raceManager;
        return findRaceSessionByKey(key);
    }

    public RaceManager getOrCreateRaceManagerByTrack(String trackName) {
        if (trackName == null || trackName.isBlank()) return raceManager;
        return getOrCreateRaceSession(trackName);
    }

    public boolean isTrackSessionBusy(String trackName) {
        RaceManager rm = getRaceManagerByTrack(trackName);
        return rm != null && (rm.isRunning() || rm.isRegistering() || rm.isCountdownActive());
    }

    public void discardInactiveRaceSession(String trackName) {
        if (trackName == null || trackName.isBlank()) return;
        String key = normalizeTrackKey(trackName);
        if (key.equalsIgnoreCase("unsaved")) return;

        java.util.Iterator<java.util.Map.Entry<String, RaceManager>> it = raceSessions.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, RaceManager> e = it.next();
            if (!e.getKey().equalsIgnoreCase(key)) continue;
            RaceManager rm = e.getValue();
            if (rm == null || (!rm.isRunning() && !rm.isRegistering() && !rm.isCountdownActive())) {
                it.remove();
            }
            break;
        }
    }

    public RaceManager getRaceManagerForPlayer(java.util.UUID playerId) {
        if (playerId == null) return null;
        for (RaceManager rm : getAllRaceManagers()) {
            if (rm == null) continue;
            if (rm.isParticipant(playerId)) return rm;
            if (rm.isRegistering() && rm.getRegistered().contains(playerId)) return rm;
        }
        return null;
    }

    /** Whether the player can manage races on the currently loaded track (admin perms OR player-start flag). */
    public boolean canManageRace(Player p) {
        return canManageRace(p, trackConfig);
    }

    public boolean canManageRace(Player p, TrackConfig raceTrack) {
        if (p.hasPermission("boatracing.race.admin") || p.hasPermission("boatracing.setup")) return true;
        boolean global = getConfig().getBoolean("player-actions.allow-player-race-start", false);
        return raceTrack != null && raceTrack.getRacingBoolean("allow-player-start", global);
    }

    private boolean canOpenMapVote(Player p) {
        return p.hasPermission("boatracing.race.voteopen")
                || p.hasPermission("boatracing.race.admin")
                || p.hasPermission("boatracing.setup");
    }

    private String localizeTrackRequirement(String requirementKey) {
        if (requirementKey == null || requirementKey.isBlank()) return "";
        String fullKey = "race.requirements." + requirementKey;
        String translated = msg().get(fullKey);
        return fullKey.equals(translated) ? requirementKey : translated;
    }

    public String formatTrackRequirements(java.util.List<String> requirementKeys) {
        if (requirementKeys == null || requirementKeys.isEmpty()) return msg().get("general.none");
        java.util.List<String> localized = new java.util.ArrayList<>();
        for (String requirementKey : requirementKeys) {
            String value = localizeTrackRequirement(requirementKey);
            if (!value.isBlank()) localized.add(value);
        }
        if (localized.isEmpty()) return msg().get("general.none");
        return String.join(", ", localized);
    }

    private static String normalizePracticeTrackToken(String token) {
        if (token == null || token.isBlank()) return "unsaved";
        return token.trim().replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private String statsUnsavedTrackLabel() {
        String key = "stats.track-unsaved-label";
        String localized = msg().get(key);
        return key.equals(localized) ? msg().get("general.unsaved") : localized;
    }

    private String resolvePracticeTrackDisplayName(String token) {
        if (token == null || token.isBlank()) return msg().get("general.none");
        if ("unsaved".equalsIgnoreCase(token)) return statsUnsavedTrackLabel();
        if (trackLibrary != null) {
            for (String trackName : trackLibrary.list()) {
                if (normalizePracticeTrackToken(trackName).equalsIgnoreCase(token)) return trackName;
            }
        }
        return token.replace('_', ' ');
    }

    private static String formatMillis(long millis) {
        long sec = millis / 1000L;
        long ms = millis % 1000L;
        long m = sec / 60L;
        long s = sec % 60L;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%03d", m, s, ms);
    }

    private String formatOptionalMillis(Long millis) {
        if (millis == null || millis < 0L) return msg().get("general.none");
        return formatMillis(millis);
    }

    private java.util.Map<Integer, Long> sanitizePracticeSectors(java.util.Map<Integer, Long> sectors) {
        java.util.Map<Integer, Long> out = new java.util.TreeMap<>();
        if (sectors == null || sectors.isEmpty()) return out;

        for (java.util.Map.Entry<Integer, Long> entry : sectors.entrySet()) {
            Integer section = entry.getKey();
            Long millis = entry.getValue();
            if (section == null || section <= 0 || millis == null || millis <= 0L) continue;
            out.put(section, millis);
        }
        return out;
    }

    private boolean hasDisplayablePracticeSectors(java.util.Map<Integer, Long> sectors) {
        return !sanitizePracticeSectors(sectors).isEmpty();
    }

    private String formatPracticeSectors(java.util.Map<Integer, Long> sectors) {
        java.util.Map<Integer, Long> cleaned = sanitizePracticeSectors(sectors);
        if (cleaned.isEmpty()) return msg().get("stats.sectors-none");

        java.util.List<String> parts = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, Long> entry : cleaned.entrySet()) {
            parts.add("S" + entry.getKey() + "=" + formatMillis(entry.getValue()));
        }
        return String.join(", ", parts);
    }

    private String formatPositionLabel(int position) {
        return switch (position) {
            case 1 -> msg().get("stats.position.first");
            case 2 -> msg().get("stats.position.second");
            case 3 -> msg().get("stats.position.third");
            default -> msg().get("stats.position.nth", "n", String.valueOf(position));
        };
    }

    // Avoid OfflinePlayer#getName in tab/lookup paths because it may force playerdata
    // conversion on newer Paper versions and log errors for malformed legacy data.
    private String safeOfflineName(org.bukkit.OfflinePlayer target) {
        if (target == null) return null;
        org.bukkit.entity.Player online = target.getPlayer();
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
            // Keep compatibility across API variants without hard dependency on profile APIs.
        }
        return null;
    }

    private String displayName(org.bukkit.OfflinePlayer target) {
        if (target == null) return msg().get("general.none");
        String name = safeOfflineName(target);
        if (name != null && !name.isBlank()) return name;
        java.util.UUID id = target.getUniqueId();
        return id != null ? id.toString() : msg().get("general.none");
    }

    private void sendStatsReport(Player viewer, org.bukkit.OfflinePlayer target) {
        if (viewer == null || target == null) return;
        java.util.UUID targetId = target.getUniqueId();
        if (targetId == null) return;

        if (statsManager == null) statsManager = new StatsManager(this);
        if (practiceStatsManager == null) practiceStatsManager = new PracticeStatsManager(this);

        String targetName = displayName(target);
        boolean self = viewer.getUniqueId().equals(targetId);
        viewer.sendMessage(Text.colorize(prefix + msg().get(self ? "stats.header-self" : "stats.header-other", "player", targetName)));
        viewer.sendMessage(Text.colorize(msg().get("stats.line-player", "player", targetName)));

        java.util.Optional<es.jaie55.boatracing.team.Team> teamOpt = teamManager.getTeamByMember(targetId);
        String teamName = teamOpt.map(es.jaie55.boatracing.team.Team::getName).orElse(msg().get("general.none"));

        viewer.sendMessage(Text.colorize(msg().get("stats.line-team", "team", teamName)));
        viewer.sendMessage(Text.colorize(msg().get("stats.line-best-race", "time", formatOptionalMillis(statsManager.getPlayerBestRace(targetId)))));
        viewer.sendMessage(Text.colorize(msg().get("stats.line-best-lap", "time", formatOptionalMillis(statsManager.getPlayerBestLap(targetId)))));

        viewer.sendMessage(Text.colorize(msg().get("stats.positions-header")));
        java.util.Map<Integer, Integer> positions = statsManager.getPlayerPositions(targetId);
        if (positions.isEmpty()) {
            viewer.sendMessage(Text.colorize(msg().get("stats.positions-none")));
        } else {
            for (java.util.Map.Entry<Integer, Integer> entry : positions.entrySet()) {
                viewer.sendMessage(Text.colorize(msg().get(
                        "stats.position-line",
                        "position", formatPositionLabel(entry.getKey()),
                        "count", String.valueOf(entry.getValue())
                )));
            }
        }

        viewer.sendMessage(Text.colorize(msg().get("stats.practice-header")));
        java.util.Map<String, PracticeStatsManager.PlayerTrackStatsView> practiceByTrack = practiceStatsManager.getAllTrackStats(targetId);
        if (practiceByTrack.isEmpty()) {
            viewer.sendMessage(Text.colorize(msg().get("stats.practice-none")));
            return;
        }

        for (java.util.Map.Entry<String, PracticeStatsManager.PlayerTrackStatsView> entry : practiceByTrack.entrySet()) {
            String trackName = resolvePracticeTrackDisplayName(entry.getKey());
            PracticeStatsManager.PlayerTrackStatsView stats = entry.getValue();
            viewer.sendMessage(Text.colorize(msg().get(
                    "stats.practice-track",
                    "track", trackName,
                    "best_run", formatOptionalMillis(stats.getBestRunMillis()),
                    "last_run", formatOptionalMillis(stats.getLastRunMillis()),
                    "best_lap", formatOptionalMillis(stats.getBestLapMillis()),
                    "last_lap", formatOptionalMillis(stats.getLastLapMillis())
            )));

            if (hasDisplayablePracticeSectors(stats.getBestSectorMillis())) {
                viewer.sendMessage(Text.colorize(msg().get("stats.practice-sectors-best", "sectors", formatPracticeSectors(stats.getBestSectorMillis()))));
            }
        }
    }

    private boolean isActiveTrackRequest(String name) {
        if (name == null) return false;
        String current = trackLibrary != null ? trackLibrary.getCurrent() : null;
        if (current == null) return name.equalsIgnoreCase("unsaved");
        return current.equalsIgnoreCase(name);
    }

    private boolean ensureRaceTrackLoaded(String name) {
        if (isActiveTrackRequest(name)) return true;
        return trackLibrary != null && trackLibrary.exists(name) && trackLibrary.select(name);
    }

    private static String normalizeTrackKey(String name) {
        if (name == null || name.isBlank()) return "unsaved";
        return name.trim();
    }

    private static String normalizeLanguageCode(String code) {
        if (code == null) return "en";
        String normalized = code.trim();
        return normalized.isEmpty() ? "en" : normalized;
    }

    private static boolean isValidLanguageCode(String code) {
        return code != null && code.matches("[A-Za-z0-9_-]+");
    }

    private java.util.Set<String> getAvailableLanguageCodes() {
        java.util.LinkedHashSet<String> codes = new java.util.LinkedHashSet<>(BUNDLED_LANGUAGE_CODES);
        java.io.File[] localBundles = getDataFolder().listFiles((dir, name) -> name != null && name.startsWith("messages_") && name.endsWith(".yml"));
        if (localBundles != null) {
            for (java.io.File file : localBundles) {
                String name = file.getName();
                String code = name.substring("messages_".length(), name.length() - ".yml".length());
                if (isValidLanguageCode(code)) codes.add(code);
            }
        }
        return codes;
    }

    private boolean hasLanguageBundle(String code) {
        if (!isValidLanguageCode(code)) return false;
        String filename = "messages_" + code + ".yml";
        java.io.File customBundle = new java.io.File(getDataFolder(), filename);
        if (customBundle.exists() && customBundle.isFile()) return true;
        try (InputStream bundled = getResource(filename)) {
            return bundled != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveCanonicalLanguageCode(String code) {
        String normalized = normalizeLanguageCode(code);
        for (String available : getAvailableLanguageCodes()) {
            if (available.equalsIgnoreCase(normalized)) return available;
        }
        return normalized;
    }

    private boolean trackExistsForRace(String name) {
        String key = normalizeTrackKey(name);
        if (key.equalsIgnoreCase("unsaved")) return true;
        return trackLibrary != null && trackLibrary.exists(key);
    }

    private java.io.File trackFileForName(String trackName) {
        String key = normalizeTrackKey(trackName);
        return new java.io.File(new java.io.File(getDataFolder(), "tracks"), key + ".yml");
    }

    private RaceManager getOrCreateRaceSession(String trackName) {
        String key = normalizeTrackKey(trackName);
        if (key.equalsIgnoreCase("unsaved")) return raceManager;

        RaceManager existing = findRaceSessionByKey(key);
        if (existing != null) return existing;

        if (!trackExistsForRace(key)) return null;
        TrackConfig cfg = new TrackConfig(getDataFolder());
        cfg.setBackingFile(trackFileForName(key));
        RaceManager created = new RaceManager(this, cfg, key);
        raceSessions.put(key, created);
        return created;
    }

    private boolean isPlayerBusyInAnyOtherRace(java.util.UUID playerId, RaceManager target) {
        if (playerId == null) return false;
        for (RaceManager rm : getAllRaceManagers()) {
            if (rm == null || rm == target) continue;
            if ((rm.isRegistering() && rm.getRegistered().contains(playerId)) || (rm.isRunning() && rm.getParticipants().contains(playerId))) {
                return true;
            }
        }
        return false;
    }

    private void tickPlayerAcrossRaceSessions(Player player, org.bukkit.Location from, org.bukkit.Location to) {
        if (player == null || to == null) return;
        java.util.UUID playerId = player.getUniqueId();
        for (RaceManager rm : getAllRaceManagers()) {
            if (rm == null || !rm.isRunning()) continue;
            if (!rm.isParticipant(playerId)) continue;
            rm.tickPlayer(player, from, to);
            return;
        }
    }

    private boolean shouldPreventVehicleExitAcrossRaceSessions(java.util.UUID playerId) {
        if (playerId == null) return false;
        for (RaceManager rm : getAllRaceManagers()) {
            if (rm != null && rm.shouldPreventVehicleExit(playerId)) return true;
        }
        return false;
    }

    private boolean openVotedTrackRegistration(String trackName) {
        if (!trackExistsForRace(trackName)) return false;
        RaceManager rm = getOrCreateRaceSession(trackName);
        if (rm == null || rm.getTrack() == null || !rm.getTrack().isReady()) return false;

        rm.loadSettings();
        int laps = rm.getTotalLaps();
        return rm.openRegistration(laps, null);
    }

    private void sendMapVoteNextStepToPrivileged(String winner) {
        String line = Text.colorize(pref() + msg().get("race.vote.next-step", "winner", winner, "label", mapVoteCommandLabel));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canOpenMapVote(online)) {
                online.sendMessage(line);
            }
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    private void closeMapVote(boolean announceResult) {
        if (!mapVoteOpen) return;
        mapVoteOpen = false;
        if (mapVoteTask != null) {
            try { mapVoteTask.cancel(); } catch (Exception ignored) { getLogger().finer("Failed to cancel map vote task: " + ignored.getMessage()); }
            mapVoteTask = null;
        }

        if (!announceResult) {
            mapVoteCounts.clear();
            mapVotesByPlayer.clear();
            mapVoteCommandLabel = "boatracing";
            return;
        }

        if (mapVoteCounts.isEmpty()) {
            Bukkit.broadcastMessage(Text.colorize(pref() + msg().get("race.vote.ended-no-options")));
            mapVoteCounts.clear();
            mapVotesByPlayer.clear();
            mapVoteCommandLabel = "boatracing";
            return;
        }

        int max = Integer.MIN_VALUE;
        java.util.List<String> winners = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : mapVoteCounts.entrySet()) {
            int v = e.getValue();
            if (v > max) {
                max = v;
                winners.clear();
                winners.add(e.getKey());
            } else if (v == max) {
                winners.add(e.getKey());
            }
        }

        String winner = winners.isEmpty() ? null : winners.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(winners.size()));
        if (winner == null) {
            Bukkit.broadcastMessage(Text.colorize(pref() + msg().get("race.vote.ended-no-winner")));
        } else {
            if (trackLibrary != null && trackExistsForRace(winner)) {
                try { trackLibrary.select(winner); } catch (Exception ignored) { getLogger().finer("Could not select voted track: " + ignored.getMessage()); }
            }
            Bukkit.broadcastMessage(Text.colorize(pref() + msg().get("race.vote.ended-winner", "winner", winner, "votes", String.valueOf(max))));
            if (!openVotedTrackRegistration(winner)) {
                sendMapVoteNextStepToPrivileged(winner);
            }
        }

        mapVoteCounts.clear();
        mapVotesByPlayer.clear();
        mapVoteCommandLabel = "boatracing";
    }

    public boolean isMapVoteOpen() {
        return mapVoteOpen;
    }

    public java.util.Map<String, Integer> getMapVoteCountsSnapshot() {
        return new java.util.LinkedHashMap<>(mapVoteCounts);
    }

    public boolean startMapVote(Player initiator, java.util.Collection<String> requestedOptions, long seconds, String label) {
        java.util.LinkedHashSet<String> options = new java.util.LinkedHashSet<>();
        for (String raw : requestedOptions) {
            String opt = normalizeTrackKey(raw);
            if (trackExistsForRace(opt)) options.add(opt);
        }

        if (options.size() < 2) {
            initiator.sendMessage(Text.colorize(prefix + msg().get("race.vote.need-two-tracks")));
            return false;
        }

        long safeSeconds = Math.max(10L, seconds);
        String safeLabel = (label == null || label.isBlank()) ? "boatracing" : label;

        closeMapVote(false);
        mapVoteOpen = true;
        mapVoteCommandLabel = safeLabel;
        mapVoteCounts.clear();
        for (String opt : options) mapVoteCounts.put(opt, 0);
        mapVotesByPlayer.clear();

        Bukkit.broadcastMessage(Text.colorize(prefix + msg().get("race.vote.started", "seconds", String.valueOf(safeSeconds))));
        String voteUiCommand = "/" + safeLabel + " race voteui";
        net.kyori.adventure.text.Component voteUiClickable = Text.cmd(msg().get("race.vote.started-click"), voteUiCommand);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(voteUiClickable);
        }
        Bukkit.broadcastMessage(Text.colorize(prefix + msg().get("race.vote.started-instructions", "label", safeLabel)));
        Bukkit.broadcastMessage(Text.colorize(prefix + msg().get("race.vote.started-options", "options", String.join("&7, &f", options))));

        mapVoteTask = SchedulerCompat.runLater(this, () -> closeMapVote(true), Math.max(20L, safeSeconds * 20L));
        return true;
    }

    public boolean submitMapVote(Player player, String desiredTrack) {
        if (!mapVoteOpen) {
            player.sendMessage(Text.colorize(prefix + msg().get("race.vote.no-active")));
            return false;
        }

        String desired = normalizeTrackKey(desiredTrack);
        String selected = null;
        for (String opt : mapVoteCounts.keySet()) {
            if (opt.equalsIgnoreCase(desired)) {
                selected = opt;
                break;
            }
        }
        if (selected == null) {
            player.sendMessage(Text.colorize(prefix + msg().get("race.vote.invalid-option")));
            return false;
        }

        String previous = mapVotesByPlayer.put(player.getUniqueId(), selected);
        if (previous != null && mapVoteCounts.containsKey(previous)) {
            mapVoteCounts.put(previous, Math.max(0, mapVoteCounts.getOrDefault(previous, 0) - 1));
        }
        mapVoteCounts.put(selected, mapVoteCounts.getOrDefault(selected, 0) + 1);
        player.sendMessage(Text.colorize(prefix + msg().get("race.vote.registered", "track", selected)));
        return true;
    }

    public boolean closeMapVoteFromAdmin(Player player) {
        if (!mapVoteOpen) {
            player.sendMessage(Text.colorize(prefix + msg().get("race.vote.no-active")));
            return false;
        }
        closeMapVote(true);
        return true;
    }

    public void sendMapVoteStatus(Player player) {
        if (!mapVoteOpen) {
            player.sendMessage(Text.colorize(prefix + msg().get("race.vote.no-active")));
            return;
        }
        player.sendMessage(Text.colorize(prefix + msg().get("race.vote.status-header")));
        for (java.util.Map.Entry<String, Integer> e : mapVoteCounts.entrySet()) {
            player.sendMessage(Text.colorize(msg().get("race.vote.status-line", "track", e.getKey(), "count", String.valueOf(e.getValue()))));
        }
    }
    

    private boolean canUsePracticeCommand(org.bukkit.command.CommandSender sender) {
        return sender != null && sender.hasPermission("boatracing.race.practice");
    }

    private void handlePracticeExitOnDisconnect(Player player) {
        if (player == null) return;
        RaceManager rm = getRaceManagerForPlayer(player.getUniqueId());
        if (rm == null || !rm.isPracticeMode() || !rm.isParticipant(player.getUniqueId())) return;
        rm.leavePractice(player, false, false);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // Ensure new default keys are merged into existing config.yml on updates
        try {
            mergeConfigDefaults();
        } catch (Exception t) {
            // Log the problem but continue startup; specific exception types can be handled
            // if needed in the future.
            getLogger().warning("Failed to merge default config values: " + t.getMessage());
        }
        this.prefix = Text.colorize(getConfig().getString("prefix", "&6[BoatRacing] "));
        this.messageManager = new MessageManager(this);
        this.documentStore = DocumentStoreFactory.create(this);
        this.teamManager = new TeamManager(this);
        this.statsManager = new StatsManager(this);
        this.practiceStatsManager = new PracticeStatsManager(this);
        this.practiceGhostManager = new PracticeGhostManager(this);
        this.teamGUI = new TeamGUI(this);
    this.adminGUI = new es.jaie55.boatracing.ui.AdminGUI(this);
    this.adminRaceGUI = new es.jaie55.boatracing.ui.AdminRaceGUI(this);
    this.trackConfig = new TrackConfig(getDataFolder());
    this.trackLibrary = new TrackLibrary(this, getDataFolder(), trackConfig);
    this.raceManager = new RaceManager(this, trackConfig);
    this.rewardManager = new RewardManager(this);
    this.setupWizard = new SetupWizard(this);
    this.tracksGUI = new es.jaie55.boatracing.ui.AdminTracksGUI(this, trackLibrary);
    this.voteGUI = new es.jaie55.boatracing.ui.VoteGUI(this);
    Bukkit.getPluginManager().registerEvents(teamGUI, this);
    Bukkit.getPluginManager().registerEvents(adminGUI, this);
    Bukkit.getPluginManager().registerEvents(tracksGUI, this);
    Bukkit.getPluginManager().registerEvents(adminRaceGUI, this);
    Bukkit.getPluginManager().registerEvents(voteGUI, this);
    
    es.jaie55.boatracing.track.SelectionManager.init(this);
    Bukkit.getPluginManager().registerEvents(new es.jaie55.boatracing.track.WandListener(this), this);
    this.selectionVisualizer = new es.jaie55.boatracing.track.SelectionVisualizer(this);
    this.selectionVisualizer.startOrReload();
    // Movement listener for race tracking (skip if player hasn't changed block — massive perf gain)
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (e.getTo() == null) return;
                org.bukkit.Location from = e.getFrom();
                org.bukkit.Location to = e.getTo();
                if (from.getBlockX() == to.getBlockX()
                        && from.getBlockY() == to.getBlockY()
                        && from.getBlockZ() == to.getBlockZ()) return;
                tickPlayerAcrossRaceSessions(e.getPlayer(), e.getFrom(), to);
            }

            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                handlePracticeExitOnDisconnect(e.getPlayer());
            }

            @org.bukkit.event.EventHandler
            public void onKick(org.bukkit.event.player.PlayerKickEvent e) {
                handlePracticeExitOnDisconnect(e.getPlayer());
            }

            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
            public void onVehicleExit(org.bukkit.event.vehicle.VehicleExitEvent e) {
                if (!(e.getExited() instanceof Player p)) return;
                if (!shouldPreventVehicleExitAcrossRaceSessions(p.getUniqueId())) return;

                String vehicleType = e.getVehicle().getType().name();
                if (!vehicleType.endsWith("BOAT") && !vehicleType.endsWith("RAFT")) return;

                e.setCancelled(true);
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.9f);
            }
        }, this);
    
    try {
            boolean metricsEnabled = getConfig().getBoolean("bstats.enabled", true);
            if (metricsEnabled) {
                final int pluginId = 26881; // fixed bStats plugin id
                new org.bstats.bukkit.Metrics(this, pluginId);
                getLogger().info("Starting Metrics. Opt-out using the global bStats config.");
            }
        } catch (Exception t) {
            getLogger().warning("Failed to initialize bStats metrics: " + t.getMessage());
        }

    // Updates
    if (getConfig().getBoolean("updates.enabled", true)) {
            String currentVersion = getDescription().getVersion();
            updateChecker = new UpdateChecker(this, "boatracing", currentVersion);
            updateChecker.checkAsync();
            // Post-result console notice (delayed)
            SchedulerCompat.runLater(this, () -> {
                if (updateChecker.isChecked() && updateChecker.isOutdated()) {
                    int behind = updateChecker.getBehindCount();
                    String current = currentVersion;
                    String latest = updateChecker.getLatestVersion() != null ? updateChecker.getLatestVersion() : "latest";
                    if (getConfig().getBoolean("updates.console-warn", true)) {
                        Bukkit.getLogger().warning("[" + getName() + "] An update is available. You are " + behind + " version(s) out of date.");
                        Bukkit.getLogger().warning("[" + getName() + "] You are running " + current + ", the latest version is " + latest + ".");
                        Bukkit.getLogger().warning("[" + getName() + "] Update at " + updateChecker.getLatestUrl());
                        // Record which latest was announced
                        lastConsoleAnnouncedVersion = updateChecker.getLatestVersion();
                    }
                }
            }, 20L * 5); // ~5s after enable
            // In-game notifications for admins
            if (getConfig().getBoolean("updates.notify-admins", true)) {
                Bukkit.getPluginManager().registerEvents(new UpdateNotifier(this, updateChecker, prefix), this);
            }
            // Periodic silent update checks every 5 minutes. If a NEW update is first detected here,
            // print a console WARN immediately (single time per version); hourly reminders handle repetition.
            long period = 20L * 60L * 5L; // 5 minutes
            SchedulerCompat.runAsyncTimer(this, () -> {
                    try {
                        if (!getConfig().getBoolean("updates.enabled", true)) return;
                        updateChecker.checkAsync();
                        // Evaluate result shortly after on the main thread to avoid race conditions
                        SchedulerCompat.runLater(this, () -> {
                            if (!getConfig().getBoolean("updates.enabled", true)) return;
                            if (!getConfig().getBoolean("updates.console-warn", true)) return;
                            if (updateChecker.isChecked() && updateChecker.isOutdated()) {
                                String latest = updateChecker.getLatestVersion();
                                if (latest != null && (lastConsoleAnnouncedVersion == null || !latest.equals(lastConsoleAnnouncedVersion))) {
                                    int behind = updateChecker.getBehindCount();
                                    String current = getDescription().getVersion();
                                    Bukkit.getLogger().warning("[" + getName() + "] An update is available. You are " + behind + " version(s) out of date.");
                                    Bukkit.getLogger().warning("[" + getName() + "] You are running " + current + ", the latest version is " + latest + ".");
                                    Bukkit.getLogger().warning("[" + getName() + "] Update at " + updateChecker.getLatestUrl());
                                    lastConsoleAnnouncedVersion = latest; // avoid duplicate console prints for the same version here
                                }
                            }
                        }, 20L * 8L);
                    } catch (Exception ignored) { getLogger().finer("Periodic update check failed: " + ignored.getMessage()); }
            }, period, period);
            // Console reminder every hour:
            // - Warn immediately if we already know we're outdated
            // - Trigger a fresh async check and then warn once when the result arrives (with retries to cover latency)
            // Hourly console reminder aligned to the top of each local hour (00:00, 01:00, 02:00, ...)
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
            java.time.ZonedDateTime nextHour = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            long delayTicks = Math.max(1L, java.time.Duration.between(now, nextHour).toMillis() / 50L);
            long hourly = 20L * 60L * 60L; // 1 hour
            SchedulerCompat.runTimer(this, () -> {
                if (!getConfig().getBoolean("updates.enabled", true)) return;
                if (!getConfig().getBoolean("updates.console-warn", true)) return;
                try { updateChecker.checkAsync(); } catch (Exception ignored) { getLogger().finer("Update check failed: " + ignored.getMessage()); }
                SchedulerCompat.runLater(this, () -> {
                    if (!getConfig().getBoolean("updates.enabled", true)) return;
                    if (!getConfig().getBoolean("updates.console-warn", true)) return;
                    if (updateChecker.isChecked() && updateChecker.isOutdated()) {
                        int behind = updateChecker.getBehindCount();
                        String current = getDescription().getVersion();
                        String latest = updateChecker.getLatestVersion() != null ? updateChecker.getLatestVersion() : "latest";
                        Bukkit.getLogger().warning("[" + getName() + "] An update is available. You are " + behind + " version(s) out of date.");
                        Bukkit.getLogger().warning("[" + getName() + "] You are running " + current + ", the latest version is " + latest + ".");
                        Bukkit.getLogger().warning("[" + getName() + "] Update at " + updateChecker.getLatestUrl());
                    }
                }, 20L * 10L);
            }, delayTicks, hourly);
        }

    if (getCommand("boatracing") != null) {
            getCommand("boatracing").setExecutor(this);
            getCommand("boatracing").setTabCompleter(this);
        }

    // Optional PlaceholderAPI integration for holograms/scoreboards
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                this.placeholderExpansion = new BoatRacingPlaceholderExpansion(this);
                this.placeholderExpansion.register();
                getLogger().info("PlaceholderAPI detected: BoatRacing placeholders registered.");
            } catch (Exception ex) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + ex.getMessage());
            }
        }

    getLogger().info("BoatRacing enabled");
    }

    // Merge default config.yml values into the existing config without overwriting user changes
    private void mergeConfigDefaults() {
        InputStream is = getResource("config.yml");
        if (is == null) return;
        YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
        FileConfiguration cfg = getConfig();
        cfg.addDefaults(def);
        cfg.options().copyDefaults(true);
        saveConfig();
    }

    // Resolve an OfflinePlayer without remote lookups: prefer online, then cache, or UUID literal
    private org.bukkit.OfflinePlayer resolveOffline(String token) {
        if (token == null || token.isEmpty()) return null;
        // 1) Exact online match
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(token);
        if (online != null) return online;
        // 2) Try UUID literal
        try {
            java.util.UUID uid = java.util.UUID.fromString(token);
            return Bukkit.getOfflinePlayer(uid);
        } catch (IllegalArgumentException ignored) {}
        // 3) Try offline cache entries by name (case-insensitive)
        for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String name = safeOfflineName(op);
            if (name != null && name.equalsIgnoreCase(token)) return op;
        }
        // Not found locally
        return null;
    }

    @Override
    public void onDisable() {
        if (teamManager != null) teamManager.save();
        if (statsManager != null) statsManager.save();
        if (practiceStatsManager != null) practiceStatsManager.save();
        if (practiceGhostManager != null) practiceGhostManager.save();
        if (selectionVisualizer != null) selectionVisualizer.stop();
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        if (documentStore != null) { try { documentStore.close(); } catch (Exception ignored) { getLogger().finer("Failed to close persistent storage: " + ignored.getMessage()); } }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check for a targeted player (last argument).
        Player other = null;
        if (sender.hasPermission("boatracing.admin") && args.length != 0) {
            String a = args[args.length - 1];
            if (a.startsWith("-p:")) {
                String target = a.substring(3);
                other = org.bukkit.Bukkit.getPlayerExact(target);
                // If the player was not found (or is offline)
                if (other==null) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("stats.player-not-found", "player", target)));
                    return true;
                }
                // Remove the last argument from the array.
                args = java.util.Arrays.copyOf(args, args.length - 1);
            }
        }

        // Allow other senders if there's a targeted player.
        if (other==null && !(sender instanceof Player)) {
            sender.sendMessage(Text.colorize(prefix + msg().get("general.players-only")));
            return true;
        }

        // Set the player to either the sender or target.
        Player p = (other==null) ? (Player) sender : other;

        if (command.getName().equalsIgnoreCase("boatracing")) {
            if (args.length == 0) {
                p.sendMessage(Text.colorize(prefix + msg().get("race.usage.main", "label", label)));
                return true;
            }
            // /boatracing version
            if (args[0].equalsIgnoreCase("version")) {
                if (!sender.hasPermission("boatracing.version")) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    return true;
                }
                String current = getDescription().getVersion();
                java.util.List<String> authors = getDescription().getAuthors();
                p.sendMessage(Text.colorize(prefix + msg().get("plugin.version", "name", getName(), "version", current)));
                if (!authors.isEmpty()) {
                    p.sendMessage(Text.colorize(prefix + msg().get("plugin.authors", "authors", String.join(", ", authors))));
                }
                

                boolean updatesEnabled = getConfig().getBoolean("updates.enabled", true);
                if (!updatesEnabled) {
                    p.sendMessage(Text.colorize(prefix + msg().get("update.disabled")));
                    return true;
                }

                // Ensure we have a checker and run one if needed
                if (updateChecker == null) {
                    updateChecker = new UpdateChecker(this, "boatracing", current);
                }
                if (!updateChecker.isChecked()) {
                    p.sendMessage(Text.colorize(prefix + msg().get("update.checking")));
                    updateChecker.checkAsync();
                    // Poll a couple of times to deliver result to the user shortly after
                    SchedulerCompat.runLater(this, () -> sendUpdateStatus(p), 40L);
                    SchedulerCompat.runLater(this, () -> sendUpdateStatus(p), 100L);
                    return true;
                }
                // Already have a result
                sendUpdateStatus(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("boatracing.reload")) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    return true;
                }
                // Persist current state, reload config and data
                if (teamManager != null) teamManager.save();
                reloadConfig();
                // After reload, also merge any new defaults into config.yml
                try { mergeConfigDefaults(); } catch (Exception ignored) { getLogger().finer("mergeConfigDefaults() during reload failed: " + ignored.getMessage()); }
                this.prefix = Text.colorize(getConfig().getString("prefix", "&6[BoatRacing] "));
                this.messageManager.reload();
                // Recreate team manager to re-read data and settings
                this.teamManager = new TeamManager(this);
                if (this.statsManager == null) this.statsManager = new StatsManager(this);
                else this.statsManager.reload();
                if (this.practiceStatsManager == null) this.practiceStatsManager = new PracticeStatsManager(this);
                else this.practiceStatsManager.reload();
                if (this.practiceGhostManager == null) this.practiceGhostManager = new PracticeGhostManager(this);
                else this.practiceGhostManager.reload();
                if (this.selectionVisualizer == null) this.selectionVisualizer = new es.jaie55.boatracing.track.SelectionVisualizer(this);
                this.selectionVisualizer.startOrReload();
                p.sendMessage(Text.colorize(prefix + msg().get("plugin.reloaded")));
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                return true;
            }
            // /boatracing stats [player]
            if (args[0].equalsIgnoreCase("stats")) {
                if (!sender.hasPermission("boatracing.stats")) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    return true;
                }
                if (args.length > 2) {
                    p.sendMessage(Text.colorize(prefix + msg().get("stats.usage", "label", label)));
                    return true;
                }

                if (args.length == 2 && !sender.hasPermission("boatracing.stats.others")) {
                    boolean selfAlias = (p.getName() != null && args[1].equalsIgnoreCase(p.getName()))
                            || args[1].equalsIgnoreCase(p.getUniqueId().toString());
                    if (!selfAlias) {
                        sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        return true;
                    }
                }

                org.bukkit.OfflinePlayer target = p;
                if (args.length == 2 && sender.hasPermission("boatracing.stats.others")) {
                    target = resolveOffline(args[1]);
                    if (target == null || target.getUniqueId() == null) {
                        sender.sendMessage(Text.colorize(prefix + msg().get("stats.player-not-found", "player", args[1])));
                        if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        return true;
                    }
                }

                sendStatsReport(p, target);
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                return true;
            }
            // /boatracing race
            if (args[0].equalsIgnoreCase("race")) {
                if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
                    p.sendMessage(Text.colorize(prefix + msg().get("race.help.header")));
                    p.sendMessage(Text.colorize(msg().get("race.help.join", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("race.help.leave", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("race.help.status", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("race.help.vote", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("race.help.voteui", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("race.help.votestatus", "label", label)));
                    if (sender.hasPermission("boatracing.race.back")) {
                        sender.sendMessage(Text.colorize(msg().get("race.help.back", "label", label)));
                    }
                    if (sender.hasPermission("boatracing.race.practice")) {
                        sender.sendMessage(Text.colorize(msg().get("race.help.practice", "label", label)));
                    }
                    if (sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup")
                            || getConfig().getBoolean("player-actions.allow-player-race-start", false)) {
                        sender.sendMessage(Text.colorize(msg().get("race.help.admin", "label", label)));
                    }
                    if (canOpenMapVote(p)) {
                        sender.sendMessage(Text.colorize(msg().get("race.help.voteopen", "label", label)));
                    }
                    if (sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup")) {
                        sender.sendMessage(Text.colorize(msg().get("race.help.voteclose", "label", label)));
                    }
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "open" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.open", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (!canManageRace(p, rm.getTrack())) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (!rm.getTrack().isReady()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-ready", "requirements", formatTrackRequirements(rm.getTrack().missingRequirements()))));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        rm.loadSettings();
                        int laps = rm.getTotalLaps();
                        boolean ok = rm.openRegistration(laps, null);
                        if (!ok) p.sendMessage(Text.colorize(prefix + msg().get("race.cannot-open-registration")));
                        return true;
                    }
                    case "join" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.join", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (!rm.getTrack().isReady()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-ready", "requirements", formatTrackRequirements(rm.getTrack().missingRequirements()))));
                            return true;
                        }
                        // Must be in a team
                        if (teamManager.getTeamByMember(p.getUniqueId()).isEmpty()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.must-be-in-team")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (isPlayerBusyInAnyOtherRace(p.getUniqueId(), rm)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-in-other-race")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (!rm.join(p)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.registration.not-open")));
                        }
                        return true;
                    }
                    case "leave" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.leave", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        boolean removed = rm.leave(p);
                        if (!removed) {
                            if (!rm.isRegistering()) {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.registration.not-open")));
                            } else {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.registration.not-registered")));
                            }
                        }
                        return true;
                    }
                    case "forfeit" -> {
                        if (!sender.hasPermission("boatracing.race.forfeit")) {
                            sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        RaceManager rm = getRaceManagerForPlayer(p.getUniqueId());
                        
                        if (rm != null && rm.isRunning()) {
                            rm.forfeit(p);
                        } else {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.registration.not-registered")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        }
                        return true;
                    }
                    case "back" -> {
                        if (!sender.hasPermission("boatracing.race.back")) {
                            sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        for (RaceManager rm : getAllRaceManagers()) {
                            if (rm.isRegistering() && rm.getRegistered().contains(p.getUniqueId())) {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.registration.back-while-registered", "label", label)));
                                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                                return true;
                            }
                            if (rm.isRunning() && rm.getParticipants().contains(p.getUniqueId())) {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.registration.back-while-racing")));
                                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                                return true;
                            }
                        }

                        RaceManager.BackResult bestResult = RaceManager.BackResult.NO_LOCATION;
                        for (RaceManager rm : getAllRaceManagers()) {
                            RaceManager.BackResult result = rm.returnToSavedLocation(p);
                            if (result == RaceManager.BackResult.SUCCESS) {
                                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.1f);
                                return true;
                            }
                            if (result == RaceManager.BackResult.EXPIRED) bestResult = RaceManager.BackResult.EXPIRED;
                        }

                        String key = bestResult == RaceManager.BackResult.EXPIRED
                                ? "race.registration.lobby-back-expired"
                                : "race.registration.lobby-no-previous";
                        p.sendMessage(Text.colorize(prefix + msg().get(key)));
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        return true;
                    }
                    case "force" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.force", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (!canManageRace(p, rm.getTrack())) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        rm.loadSettings();
                        java.util.List<org.bukkit.entity.Player> forceParticipants = new java.util.ArrayList<>();
                        java.util.Set<java.util.UUID> forceRegs = new java.util.LinkedHashSet<>(rm.getRegistered());
                        for (java.util.UUID id : forceRegs) {
                            org.bukkit.entity.Player rp = Bukkit.getPlayer(id);
                            if (rp != null && rp.isOnline()) forceParticipants.add(rp);
                        }
                        if (forceParticipants.isEmpty()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.no-participants", "label", label)));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        int forceMinPlayers = rm.getMinPlayersToStart();
                        if (forceParticipants.size() < forceMinPlayers) {
                            p.sendMessage(Text.colorize(prefix + msg().get(
                                    "race.not-enough-players",
                                    "min", String.valueOf(forceMinPlayers),
                                    "current", String.valueOf(forceParticipants.size())
                            )));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (!rm.forceStart()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-running")));
                        }
                        return true;
                    }
                    case "start" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.start", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (!canManageRace(p, rm.getTrack())) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (!rm.getTrack().isReady()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-ready", "requirements", formatTrackRequirements(rm.getTrack().missingRequirements()))));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (rm.isRunning() || rm.isCountdownActive()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-running")));
                            return true;
                        }
                        rm.loadSettings();
                        // Build participants: strictly registered participants only
                        java.util.List<org.bukkit.entity.Player> participants = new java.util.ArrayList<>();
                        java.util.Set<java.util.UUID> regs = new java.util.LinkedHashSet<>(rm.getRegistered());
                        for (java.util.UUID id : regs) {
                            org.bukkit.entity.Player rp = Bukkit.getPlayer(id);
                            if (rp != null && rp.isOnline()) participants.add(rp);
                        }
                        if (participants.isEmpty()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.no-participants", "label", label)));
                            return true;
                        }
                        int minPlayers = rm.getMinPlayersToStart();
                        if (participants.size() < minPlayers) {
                            p.sendMessage(Text.colorize(prefix + msg().get(
                                    "race.not-enough-players",
                                    "min", String.valueOf(minPlayers),
                                    "current", String.valueOf(participants.size())
                            )));
                            return true;
                        }
                        if (!rm.forceStart()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-running")));
                        }
                        return true;
                    }
                    case "practice" -> {
                        // One permission gates both starting and leaving practice.
                        if (!canUsePracticeCommand(p)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.practice", "label", label))); return true; }

                        boolean leavePractice = args[2].equalsIgnoreCase("leave") || args[2].equalsIgnoreCase("exit");
                        String tname = leavePractice
                                ? (args.length >= 4 ? args[3] : "")
                                : args[2];
                        if (tname.isBlank()) {
                            p.sendMessage(Text.colorize(prefix + msg().get(
                                    leavePractice ? "race.usage.practice-leave" : "race.usage.practice",
                                    "label", label)));
                            return true;
                        }
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (leavePractice) {
                            if (!rm.leavePractice(p)) {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.practice.not-found", "track", tname)));
                            }
                            return true;
                        }
                        if (!rm.getTrack().isReady()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-ready", "requirements", formatTrackRequirements(rm.getTrack().missingRequirements()))));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        if (rm.isRunning() || rm.isCountdownActive()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-running")));
                            return true;
                        }
                        if (rm.isRegistering()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.cannot-open-registration")));
                            return true;
                        }
                        if (isPlayerBusyInAnyOtherRace(p.getUniqueId(), rm)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.already-in-other-race")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        rm.loadSettings();
                        if (!rm.startPractice(p)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.no-start-slots")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        }
                        return true;
                    }
                    case "stop" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.stop", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        if (!canManageRace(p, rm.getTrack())) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        boolean any = false;
                        if (rm.isRegistering()) {
                            any |= rm.cancelRegistration(true);
                        }
                        if (rm.isRunning()) {
                            any |= rm.cancelRace();
                        }
                        if (!any) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.nothing-to-stop")));
                        }
                        return true;
                    }
                    case "status" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("race.usage.status", "label", label))); return true; }
                        String tname = args[2];
                        if (!trackExistsForRace(tname)) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-not-found", "track", tname))); return true; }
                        RaceManager rm = getOrCreateRaceSession(tname);
                        if (rm == null) { p.sendMessage(Text.colorize(prefix + msg().get("race.track-load-failed", "track", tname))); return true; }
                        TrackConfig raceTrack = rm.getTrack();
                        String cur = rm.getTrackName();
                        boolean running = rm.isRunning();
                        boolean registering = rm.isRegistering();
                        int regs = rm.getRegistered().size();
                        int laps = rm.getTotalLaps();
                        int participants = running ? rm.getParticipants().size() : 0;
                        int starts = raceTrack.getStarts().size();
                        int lights = raceTrack.getLights().size();
                        int cps = raceTrack.getCheckpoints().size();
                        boolean hasFinish = raceTrack.getFinish() != null;
                        boolean hasPit = raceTrack.getPitlane() != null;
                        boolean ready = raceTrack.isReady();
                        java.util.List<String> missing = ready ? java.util.Collections.emptyList() : raceTrack.missingRequirements();

                        p.sendMessage(Text.colorize(prefix + msg().get("race.status.header")));
                        p.sendMessage(Text.colorize(msg().get("race.status.track", "track", cur)));
                        p.sendMessage(Text.colorize(running ? msg().get("race.status.running", "count", participants) : msg().get("race.status.not-running")));
                        p.sendMessage(Text.colorize(registering ? msg().get("race.status.registration-open", "count", regs) : msg().get("race.status.registration-closed")));
                        p.sendMessage(Text.colorize(msg().get("race.status.laps", "count", laps)));
                        p.sendMessage(Text.colorize(msg().get("race.status.starts-lights", "starts", starts, "lights", lights, "finish", msg().get(hasFinish ? "general.yes" : "general.no"), "pit", msg().get(hasPit ? "general.yes" : "general.no"))));
                        p.sendMessage(Text.colorize(msg().get("race.status.checkpoints", "count", cps)));
                        p.sendMessage(Text.colorize(msg().get("race.status.mandatory-pitstops", "count", rm.getMandatoryPitstops())));
                        if (ready) {
                            p.sendMessage(Text.colorize(msg().get("race.status.track-ready")));
                        } else {
                            p.sendMessage(Text.colorize(msg().get("race.track-not-ready", "requirements", formatTrackRequirements(missing))));
                        }
                        return true;
                    }
                    case "voteopen" -> {
                        if (!canOpenMapVote(p)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        int endIndex = args.length;
                        long seconds = 30L;
                        if (args.length >= 3) {
                            String maybeSeconds = args[args.length - 1];
                            if (maybeSeconds.matches("\\d+")) {
                                seconds = Math.max(10L, Long.parseLong(maybeSeconds));
                                endIndex = args.length - 1;
                            }
                        }

                        java.util.LinkedHashSet<String> options = new java.util.LinkedHashSet<>();
                        boolean useAllTracks = (args.length == 2) || (endIndex <= 2);
                        for (int i = 2; i < endIndex; i++) {
                            String raw = args[i];
                            if (raw.equalsIgnoreCase("all") || raw.equals("*")) {
                                useAllTracks = true;
                                continue;
                            }
                            String opt = normalizeTrackKey(raw);
                            if (trackExistsForRace(opt)) options.add(opt);
                        }

                        if (useAllTracks && trackLibrary != null) {
                            for (String trackName : trackLibrary.list()) {
                                String opt = normalizeTrackKey(trackName);
                                if (trackExistsForRace(opt)) options.add(opt);
                            }
                        }

                        startMapVote(p, options, seconds, label);
                        return true;
                    }
                    case "vote" -> {
                        if (args.length < 3) {
                            if (!isMapVoteOpen()) {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.vote.no-active")));
                                return true;
                            }
                            if (voteGUI != null) {
                                voteGUI.openPlayerVote(p);
                            } else {
                                p.sendMessage(Text.colorize(prefix + msg().get("race.usage.vote", "label", label)));
                            }
                            return true;
                        }
                        submitMapVote(p, args[2]);
                        return true;
                    }
                    case "votestatus" -> {
                        sendMapVoteStatus(p);
                        return true;
                    }
                    case "voteclose" -> {
                        if (!(sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup"))) {
                            sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                            return true;
                        }
                        closeMapVoteFromAdmin(p);
                        return true;
                    }
                    case "voteui" -> {
                        if (!isMapVoteOpen()) {
                            p.sendMessage(Text.colorize(prefix + msg().get("race.vote.no-active")));
                            return true;
                        }
                        if (voteGUI != null) voteGUI.openPlayerVote(p);
                        return true;
                    }
                    default -> { p.sendMessage(Text.colorize(prefix + msg().get("race.unknown-subcommand", "label", label))); return true; }
                }
            }
            // /boatracing setup
            if (args[0].equalsIgnoreCase("setup")) {
                if (!sender.hasPermission("boatracing.setup")) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    return true;
                }
                if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
                    p.sendMessage(Text.colorize(prefix + msg().get("setup.usage.main")));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-addstart", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearstarts", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-removestart", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setfinish", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearfinish", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setpit", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearpit", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-addcheckpoint", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-addlight", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-removelight", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearlights", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setlaps", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setpitstops", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setlobby", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-setpos", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearpos", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-clearcheckpoints", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-show", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-selinfo", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-wand", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("setup.usage.cmd-wizard", "label", label)));
                    return true;
                }
                String sub = args[1].toLowerCase();
                switch (sub) {
                    case "wand" -> {
                        es.jaie55.boatracing.track.SelectionManager.giveWand(p);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.wand-ready")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.2f);
                        return true;
                    }
                    case "wizard" -> {
                        // Guided setup assistant with simple sub-actions
                        if (args.length >= 3) {
                            String action = args[2].toLowerCase();
                            switch (action) {
                                case "finish" -> setupWizard.finish(p);
                                case "back" -> setupWizard.back(p);
                                case "status" -> setupWizard.status(p);
                                case "cancel" -> setupWizard.cancel(p);
                                case "skip" -> setupWizard.skip(p); // only works on optional steps
                                case "next" -> setupWizard.next(p);
                                default -> setupWizard.start(p);
                            }
                        } else if (setupWizard.isActive(p)) {
                            setupWizard.status(p);
                        } else {
                            setupWizard.start(p);
                        }
                        return true;
                    }
                    case "addstart" -> {
                        org.bukkit.Location loc = p.getLocation();
                        trackConfig.addStart(loc);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.start-added", "x", loc.getBlockX(), "y", loc.getBlockY(), "z", loc.getBlockZ(), "world", loc.getWorld().getName())));
                        p.sendMessage(Text.colorize(msg().get("setup.start-added-tip")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "clearstarts" -> {
                        trackConfig.clearStarts();
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.starts-cleared")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "removestart" -> {
                        if (args.length < 3 || !args[2].matches("\\d+")) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.error.removestart", "label", label)));
                            return true;
                        }
                        int oneBased = Integer.parseInt(args[2]);
                        int maxStarts = trackConfig.getStarts().size();
                        if (oneBased < 1 || oneBased > maxStarts) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.removestart-invalid", "max", maxStarts)));
                            return true;
                        }
                        TrackConfig.StartSlot removed = trackConfig.getStarts().get(oneBased - 1);
                        if (!trackConfig.removeStartAt(oneBased - 1)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.removestart-invalid", "max", maxStarts)));
                            return true;
                        }
                        p.sendMessage(Text.colorize(prefix + msg().get(
                                "setup.start-removed",
                                "slot", oneBased,
                                "x", (int) Math.floor(removed.x),
                                "y", (int) Math.floor(removed.y),
                                "z", (int) Math.floor(removed.z),
                                "world", removed.world
                        )));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "setfinish" -> {
                        var sel = SelectionUtils.getSelectionDetailed(p);
                        if (sel == null) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.no-selection")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        Region r = new Region(sel.worldName, sel.box);
                        trackConfig.setFinish(r);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.finish-set", "box", fmtBox(sel.box))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "clearfinish" -> {
                        trackConfig.setFinish(null);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.finish-cleared")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "setpit" -> {
                        var sel = SelectionUtils.getSelectionDetailed(p);
                        if (sel == null) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.no-selection")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        Region r = new Region(sel.worldName, sel.box);
                        if (args.length >= 3) {
                            // Join the rest of tokens to support names with spaces; allow quoted names
                            String raw = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).trim();
                            String teamName = raw;
                            if ((teamName.startsWith("\"") && teamName.endsWith("\"")) || (teamName.startsWith("'") && teamName.endsWith("'"))) {
                                teamName = teamName.substring(1, teamName.length()-1);
                            }
                            var ot = teamManager.findByName(teamName);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("setup.team-not-found"))); return true; }
                            trackConfig.setTeamPit(ot.get().getId(), r);
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.pit-set-team", "team", ot.get().getName(), "box", fmtBox(sel.box))));
                        } else {
                            trackConfig.setPitlane(r);
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.pit-set-default", "box", fmtBox(sel.box))));
                        }
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "clearpit" -> {
                        trackConfig.setPitlane(null);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.pit-cleared")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "addcheckpoint" -> {
                        var sel = SelectionUtils.getSelectionDetailed(p);
                        if (sel == null) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.no-selection")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        Region r = new Region(sel.worldName, sel.box);
                        trackConfig.addCheckpoint(r);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.checkpoint-added", "num", trackConfig.getCheckpoints().size(), "box", fmtBox(sel.box))));
                        p.sendMessage(Text.colorize(msg().get("setup.checkpoint-added-tip")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "addlight" -> {
                        org.bukkit.block.Block target = p.getTargetBlockExact(6);
                        if (target == null) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.light-look")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        boolean ok = trackConfig.addLight(target);
                        if (!ok) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.light-fail")));
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            return true;
                        }
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.light-added", "x", target.getX(), "y", target.getY(), "z", target.getZ())));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "removelight" -> {
                        if (args.length < 3 || !args[2].matches("\\d+")) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.error.removelight", "label", label)));
                            return true;
                        }
                        int oneBased = Integer.parseInt(args[2]);
                        int maxLights = trackConfig.getLights().size();
                        if (oneBased < 1 || oneBased > maxLights) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.removelight-invalid", "max", maxLights)));
                            return true;
                        }
                        TrackConfig.LightPos removed = trackConfig.getLights().get(oneBased - 1);
                        if (!trackConfig.removeLightAt(oneBased - 1)) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.removelight-invalid", "max", maxLights)));
                            return true;
                        }
                        p.sendMessage(Text.colorize(prefix + msg().get(
                                "setup.light-removed",
                                "slot", oneBased,
                                "x", removed.x,
                                "y", removed.y,
                                "z", removed.z,
                                "world", removed.world
                        )));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "clearlights" -> {
                        trackConfig.clearLights();
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.lights-cleared")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "setlaps" -> {
                        if (args.length < 3 || !args[2].matches("\\d+")) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.error.setlaps", "label", label)));
                            return true;
                        }
                        int laps = Math.max(1, Integer.parseInt(args[2]));
                        String tlName = trackLibrary != null ? trackLibrary.getCurrent() : null;
                        if (tlName == null || tlName.isBlank()) {
                            raceManager.setTotalLaps(laps);
                        } else {
                            RaceManager activeSession = findRaceSessionByKey(normalizeTrackKey(tlName));
                            if (activeSession != null) activeSession.setTotalLaps(laps);
                        }
                        trackConfig.setRacingOverride("laps", laps);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.laps-set", "laps", laps, "track_info", (tlName != null ? msg().get("setup.track-info", "track", tlName) : ""))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "setpitstops" -> {
                        if (args.length < 3 || !args[2].matches("\\d+")) {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.error.setpitstops", "label", label)));
                            return true;
                        }
                        int req = Math.max(0, Integer.parseInt(args[2]));
                        String tlNamePit = trackLibrary != null ? trackLibrary.getCurrent() : null;
                        if (tlNamePit == null || tlNamePit.isBlank()) {
                            raceManager.setMandatoryPitstops(req);
                        } else {
                            RaceManager activeSession = findRaceSessionByKey(normalizeTrackKey(tlNamePit));
                            if (activeSession != null) activeSession.setMandatoryPitstops(req);
                        }
                        trackConfig.setRacingOverride("mandatory-pitstops", req);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.pitstops-set", "count", req, "track_info", (tlNamePit != null ? msg().get("setup.track-info", "track", tlNamePit) : ""))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "setlobby" -> {
                        org.bukkit.Location loc = p.getLocation();
                        String worldName = (loc.getWorld() != null ? loc.getWorld().getName() : "world");
                        FileConfiguration cfg = getConfig();
                        cfg.set("racing.lobby.enabled", true);
                        cfg.set("racing.lobby.world", worldName);
                        cfg.set("racing.lobby.x", loc.getX());
                        cfg.set("racing.lobby.y", loc.getY());
                        cfg.set("racing.lobby.z", loc.getZ());
                        cfg.set("racing.lobby.yaw", loc.getYaw());
                        cfg.set("racing.lobby.pitch", loc.getPitch());
                        saveConfig();
                        raceManager.loadSettings();
                        p.sendMessage(Text.colorize(prefix + msg().get(
                                "setup.lobby-set",
                                "x", loc.getBlockX(),
                                "y", loc.getBlockY(),
                                "z", loc.getBlockZ(),
                                "world", worldName
                        )));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                        return true;
                    }
                    case "setpos" -> {
                        if (args.length < 4) { p.sendMessage(Text.colorize(prefix + msg().get("setup.error.setpos", "label", label))); return true; }
                        org.bukkit.OfflinePlayer off = resolveOffline(args[2]);
                        if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("setup.player-not-found"))); return true; }
                        String slotArg = args[3];
                        if (slotArg.equalsIgnoreCase("auto")) {
                            trackConfig.clearCustomStartSlot(off.getUniqueId());
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.setpos-removed", "player", displayName(off))));
                        } else if (slotArg.matches("\\d+")) {
                            int oneBased = Integer.parseInt(slotArg);
                            if (oneBased < 1 || oneBased > trackConfig.getStarts().size()) { p.sendMessage(Text.colorize(prefix + msg().get("setup.setpos-invalid", "max", trackConfig.getStarts().size()))); return true; }
                            trackConfig.setCustomStartSlot(off.getUniqueId(), oneBased - 1);
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.setpos-set", "player", displayName(off), "slot", oneBased)));
                        } else {
                            p.sendMessage(Text.colorize(prefix + msg().get("setup.error.setpos", "label", label)));
                        }
                        return true;
                    }
                    case "clearpos" -> {
                        if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("setup.error.clearpos", "label", label))); return true; }
                        org.bukkit.OfflinePlayer off = resolveOffline(args[2]);
                        if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("setup.player-not-found"))); return true; }
                        trackConfig.clearCustomStartSlot(off.getUniqueId());
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.setpos-removed", "player", displayName(off))));
                        return true;
                    }
                    case "clearcheckpoints" -> {
                        trackConfig.clearCheckpoints();
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.checkpoints-cleared")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        if (setupWizard != null) setupWizard.afterAction(p);
                    }
                    case "show" -> {
                        int starts = trackConfig.getStarts().size();
                        int lights = trackConfig.getLights().size();
                        int cps = trackConfig.getCheckpoints().size();
                        boolean hasFinish = trackConfig.getFinish() != null;
                        boolean hasPit = trackConfig.getPitlane() != null;
                        int teamPitCount = trackConfig.getTeamPits().size();
                        int customStarts = trackConfig.getCustomStartSlots().size();
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.show.header")));
                        String tname = (getTrackLibrary() != null && getTrackLibrary().getCurrent() != null) ? getTrackLibrary().getCurrent() : msg().get("general.unsaved");
                        p.sendMessage(Text.colorize(msg().get("setup.show.track", "track", tname)));
                        p.sendMessage(Text.colorize(msg().get("setup.show.starts", "count", starts)));
                        p.sendMessage(Text.colorize(msg().get("setup.show.lights", "count", lights)));
                        p.sendMessage(Text.colorize(msg().get("setup.show.finish", "status", msg().get(hasFinish ? "general.yes" : "general.no"))));
                        p.sendMessage(Text.colorize(msg().get("setup.show.pit-default", "status", msg().get(hasPit ? "general.yes" : "general.no"))));
                        p.sendMessage(Text.colorize(msg().get("setup.show.pit-teams", "info", (teamPitCount > 0 ? msg().get("setup.show.info-configured", "count", teamPitCount) : msg().get("general.none")))));
                        p.sendMessage(Text.colorize(msg().get("setup.show.custom-starts", "info", (customStarts > 0 ? msg().get("setup.show.info-players", "count", customStarts) : msg().get("general.none")))));
                        p.sendMessage(Text.colorize(msg().get("setup.show.checkpoints", "count", cps)));
                        p.sendMessage(Text.colorize(msg().get("setup.show.pitstops", "count", raceManager.getMandatoryPitstops())));
                        // Show per-track racing overrides if any
                        java.util.Map<String, Object> overrides = trackConfig.getRacingOverrides();
                        if (!overrides.isEmpty()) {
                            p.sendMessage(Text.colorize(msg().get("setup.show.overrides-header")));
                            for (java.util.Map.Entry<String, Object> oe : overrides.entrySet()) {
                                p.sendMessage(Text.colorize(msg().get("setup.show.override-entry", "key", oe.getKey(), "value", oe.getValue())));
                            }
                        }
                    }
                    case "selinfo" -> {
                        java.util.List<String> dump = SelectionUtils.debugSelection(p);
                        p.sendMessage(Text.colorize(prefix + msg().get("setup.selection-info")));
                        for (String line : dump) p.sendMessage(Text.colorize(msg().get("setup.show.selection-entry", "line", line)));
                    }
                    default -> p.sendMessage(Text.colorize(prefix + msg().get("setup.error.unknown-subcommand", "label", label)));
                }
                return true;
            }
            // /boatracing admin
            if (args[0].equalsIgnoreCase("admin")) {
                boolean isLanguageSubcommand = args.length >= 2 && args[1].equalsIgnoreCase("language");
                boolean canUseAdmin = sender.hasPermission("boatracing.admin");
                boolean canManageLanguage = sender.hasPermission("boatracing.admin.language");
                if (!canUseAdmin && !(isLanguageSubcommand && canManageLanguage)) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    return true;
                }
                if (args.length == 1) {
                    if (!canUseAdmin) {
                        sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }
                    // Open Admin GUI by default
                    adminGUI.openMain(p);
                    return true;
                }
                if (args[1].equalsIgnoreCase("language")) {
                    if (!(canUseAdmin || canManageLanguage)) {
                        sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }

                    String currentLang = resolveCanonicalLanguageCode(getConfig().getString("language", "en"));
                    java.util.Set<String> availableLanguages = getAvailableLanguageCodes();

                    if (args.length < 3) {
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.language", "label", label)));
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-current", "lang", currentLang)));
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-available", "langs", String.join(", ", availableLanguages))));
                        return true;
                    }

                    String requestedRaw = normalizeLanguageCode(args[2]);
                    if (!isValidLanguageCode(requestedRaw)) {
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-invalid-code", "code", requestedRaw)));
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.language", "label", label)));
                        return true;
                    }

                    String requestedLang = resolveCanonicalLanguageCode(requestedRaw);
                    if (!hasLanguageBundle(requestedLang)) {
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-not-found", "code", requestedRaw)));
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-available", "langs", String.join(", ", availableLanguages))));
                        return true;
                    }

                    if (requestedLang.equalsIgnoreCase(currentLang)) {
                        p.sendMessage(Text.colorize(prefix + msg().get("admin.language-already-set", "lang", requestedLang)));
                        return true;
                    }

                    getConfig().set("language", requestedLang);
                    saveConfig();
                    this.messageManager.reload();
                    p.sendMessage(Text.colorize(prefix + msg().get("admin.language-updated", "old", currentLang, "new", requestedLang)));
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                    return true;
                }
                if (args[1].equalsIgnoreCase("help")) {
                    if (!canUseAdmin) {
                        p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }
                    p.sendMessage(Text.colorize(prefix + msg().get("admin.help.header")));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-create", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-delete", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-rename", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-color", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-add", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.team-remove", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.player-setteam", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.player-setnumber", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.player-setboat", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.language", "label", label)));
                    p.sendMessage(Text.colorize(msg().get("admin.help.tracks", "label", label)));
                    return true;
                }
                if (args[1].equalsIgnoreCase("tracks")) {
                    if (!canUseAdmin) {
                        p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }
                    if (!sender.hasPermission("boatracing.setup")) { sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission"))); return true; }
                    tracksGUI.open(p);
                    return true;
                }
                // admin team ...
                if (args[1].equalsIgnoreCase("team")) {
                    if (!canUseAdmin) {
                        p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }
                    if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-main", "label", label))); return true; }
                    String op = args[2].toLowerCase();
                    switch (op) {
                        case "create" -> {
                            if (args.length < 4) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-create", "label", label))); return true; }
                            String name = args[3];
                            org.bukkit.DyeColor color = org.bukkit.DyeColor.WHITE;
                            if (args.length >= 5) {
                                try { color = org.bukkit.DyeColor.valueOf(args[4].toUpperCase()); } catch (Exception ex) { p.sendMessage(Text.colorize(prefix + msg().get("admin.invalid-color"))); return true; }
                            }
                            java.util.UUID firstMemberId = p.getUniqueId();
                            if (args.length >= 6) {
                                org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(args[5]);
                                if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                                firstMemberId = off.getUniqueId();
                            }
                            if (teamManager.findByName(name).isPresent()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-name-exists"))); return true; }
                            teamManager.createTeam(firstMemberId, name, color);
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.team-created-colored", "name", name, "color", color.name())));
                            return true;
                        }
                        case "delete" -> {
                            if (args.length < 4) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-delete", "label", label))); return true; }
                            String name = args[3];
                            var ot = teamManager.findByName(name);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                            // Notify members before deletion
                            java.util.List<java.util.UUID> members = new java.util.ArrayList<>(ot.get().getMembers());
                            teamManager.removeTeam(ot.get());
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.team-deleted")));
                            for (java.util.UUID m : members) {
                                org.bukkit.OfflinePlayer memOp = Bukkit.getOfflinePlayer(m);
                                if (memOp.isOnline() && memOp.getPlayer() != null) {
                                    memOp.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.team-deleted-notify")));
                                    memOp.getPlayer().playSound(memOp.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.6f, 0.9f);
                                }
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "rename" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-rename", "label", label))); return true; }
                            var ot = teamManager.findByName(args[3]);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                            String newName = args[4];
                            if (teamManager.findByName(newName).isPresent()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-name-exists"))); return true; }
                            ot.get().setName(newName);
                            teamManager.save();
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.team-renamed", "name", newName)));
                            for (java.util.UUID m : ot.get().getMembers()) {
                                org.bukkit.OfflinePlayer memOp = Bukkit.getOfflinePlayer(m);
                                if (memOp.isOnline() && memOp.getPlayer() != null) {
                                    memOp.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.team-renamed-notify", "name", newName)));
                                    memOp.getPlayer().playSound(memOp.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                                }
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "color" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-color", "label", label))); return true; }
                            var ot = teamManager.findByName(args[3]);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                            org.bukkit.DyeColor color;
                            try { color = org.bukkit.DyeColor.valueOf(args[4].toUpperCase()); } catch (Exception ex) { p.sendMessage(Text.colorize(prefix + msg().get("admin.invalid-color"))); return true; }
                            ot.get().setColor(color);
                            teamManager.save();
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.team-color-set", "color", color.name())));
                            for (java.util.UUID m : ot.get().getMembers()) {
                                org.bukkit.OfflinePlayer memOp = Bukkit.getOfflinePlayer(m);
                                if (memOp.isOnline() && memOp.getPlayer() != null) {
                                    memOp.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.team-color-changed-notify", "color", color.name())));
                                    memOp.getPlayer().playSound(memOp.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                                }
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "add" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-add", "label", label))); return true; }
                            var ot = teamManager.findByName(args[3]);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(args[4]);
                            if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                            // remove from previous team if any
                            teamManager.getTeamByMember(off.getUniqueId()).ifPresent(prev -> { prev.removeMember(off.getUniqueId()); });
                            boolean ok = teamManager.addMember(ot.get(), off.getUniqueId());
                            if (!ok) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-full", "max", teamManager.getMaxMembers()))); return true; }
                            teamManager.save();
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.member-added", "player", off.getName())));
                            if (off.isOnline() && off.getPlayer() != null) {
                                off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.member-added-notify", "team", ot.get().getName())));
                                off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "remove" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.team-remove", "label", label))); return true; }
                            var ot = teamManager.findByName(args[3]);
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(args[4]);
                            if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                            boolean ok = teamManager.removeMember(ot.get(), off.getUniqueId());
                            if (!ok) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-member"))); return true; }
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.player-removed")));
                            teamManager.save();
                            if (off.isOnline() && off.getPlayer() != null) {
                                off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.player-removed-notify", "team", ot.get().getName())));
                                off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        default -> { p.sendMessage(Text.colorize(prefix + msg().get("admin.unknown-team-op"))); return true; }
                    }
                }
                // admin player ...
                if (args[1].equalsIgnoreCase("player")) {
                    if (!canUseAdmin) {
                        p.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                        return true;
                    }
                    if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.player-main", "label", label))); return true; }
                    String op = args[2].toLowerCase();
                    switch (op) {
                        case "setteam" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.player-setteam", "label", label))); return true; }
                            org.bukkit.OfflinePlayer off = resolveOffline(args[3]);
                            if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                            String teamName = args[4];
                            teamManager.getTeamByMember(off.getUniqueId()).ifPresent(prev -> prev.removeMember(off.getUniqueId()));
                            if (!teamName.equalsIgnoreCase("none")) {
                                var ot = teamManager.findByName(teamName);
                                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-not-found"))); return true; }
                                if (!teamManager.addMember(ot.get(), off.getUniqueId())) { p.sendMessage(Text.colorize(prefix + msg().get("admin.team-full", "max", teamManager.getMaxMembers()))); return true; }
                            }
                            teamManager.save();
                            if (teamName.equalsIgnoreCase("none")) {
                                p.sendMessage(Text.colorize(prefix + msg().get("admin.player-unset-team")));
                            } else {
                                p.sendMessage(Text.colorize(prefix + msg().get("admin.player-assigned", "team", teamName)));
                            }
                            if (off.isOnline() && off.getPlayer() != null) {
                                if (teamName.equalsIgnoreCase("none")) {
                                    off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.player-unset-team-notify")));
                                    off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                                } else {
                                    off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.player-assigned-notify", "team", teamName)));
                                    off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                                }
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "setnumber" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.player-setnumber", "label", label))); return true; }
                            org.bukkit.OfflinePlayer off = resolveOffline(args[3]);
                            if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                            int num;
                            try { num = Integer.parseInt(args[4]); } catch (Exception ex) { p.sendMessage(Text.colorize(prefix + msg().get("admin.invalid-number"))); return true; }
                            if (num < 1 || num > 99) { p.sendMessage(Text.colorize(prefix + msg().get("admin.number-out-of-range"))); return true; }
                            var ot = teamManager.getTeamByMember(off.getUniqueId());
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-no-team"))); return true; }
                            // Optional: global uniqueness check could go here
                            ot.get().setRacerNumber(off.getUniqueId(), num);
                            teamManager.save();
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.racer-number-set", "player", off.getName(), "num", num)));
                            if (off.isOnline() && off.getPlayer() != null) {
                                off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.racer-number-changed-notify", "num", num)));
                                off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        case "setboat" -> {
                            if (args.length < 5) { p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.player-setboat", "label", label))); return true; }
                            org.bukkit.OfflinePlayer off = resolveOffline(args[3]);
                            if (off == null || off.getUniqueId() == null) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-not-found"))); return true; }
                            String type = args[4].toUpperCase();
                            // Validate against allowed boats: boats, chest boats, and raft names
                            java.util.Set<String> allowed = new java.util.LinkedHashSet<>();
                            for (org.bukkit.Material m : org.bukkit.Material.values()) {
                                String n = m.name();
                                if (n.endsWith("_BOAT") || n.endsWith("_CHEST_BOAT")) allowed.add(n);
                            }
                            // Also accept RAFT/CHEST_RAFT tokens even if not present as Materials
                            allowed.add("RAFT");
                            allowed.add("CHEST_RAFT");
                            if (!allowed.contains(type)) { p.sendMessage(Text.colorize(prefix + msg().get("admin.invalid-boat"))); return true; }
                            var ot = teamManager.getTeamByMember(off.getUniqueId());
                            if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("admin.player-no-team"))); return true; }
                            ot.get().setBoatType(off.getUniqueId(), type);
                            teamManager.save();
                            p.sendMessage(Text.colorize(prefix + msg().get("admin.boat-changed", "boat", type)));
                            if (off.isOnline() && off.getPlayer() != null) {
                                off.getPlayer().sendMessage(Text.colorize(prefix + msg().get("admin.boat-changed-notify", "boat", type)));
                                off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                            }
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                            return true;
                        }
                        default -> { p.sendMessage(Text.colorize(prefix + msg().get("admin.unknown-player-op"))); return true; }
                    }
                }
                p.sendMessage(Text.colorize(prefix + msg().get("admin.usage.admin-help", "label", label)));
                return true;
            }
            if (!args[0].equalsIgnoreCase("teams")) {
                p.sendMessage(Text.colorize(prefix + msg().get("race.usage.main", "label", label)));
                return true;
            }
            // /boatracing teams
            if (args.length == 1) {
                if (!sender.hasPermission("boatracing.teams")) {
                    sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                    if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    return true;
                }
                teamGUI.openMain(p);
                return true;
            }
            // /boatracing teams create <name>
            if (!sender.hasPermission("boatracing.teams")) {
                sender.sendMessage(Text.colorize(prefix + msg().get("general.no-permission")));
                if (sender == p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
                boolean allowCreate = getConfig().getBoolean("player-actions.allow-team-create", true);
                if (!allowCreate) { p.sendMessage(Text.colorize(prefix + msg().get("team.create-restricted"))); return true; }
                if (teamManager.getTeamByMember(p.getUniqueId()).isPresent()) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.already-in-team")));
                    return true;
                }
                if (args.length < 3) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.create-usage")));
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                String err = es.jaie55.boatracing.ui.TeamGUI.validateNameMessage(name);
                if (err != null) {
                    p.sendMessage(Text.colorize(prefix + msg().get(err)));
                    return true;
                }
                boolean exists = teamManager.getTeams().stream().anyMatch(t -> t.getName().equalsIgnoreCase(name));
                if (exists) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.name-exists")));
                    return true;
                }
                teamManager.createTeam(p, name, org.bukkit.DyeColor.WHITE);
                return true;
            }
            // /boatracing teams rename <new name>
            if (args.length >= 2 && args[1].equalsIgnoreCase("rename")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                es.jaie55.boatracing.team.Team t = ot.get();
                boolean allowRename = getConfig().getBoolean("player-actions.allow-team-rename", false);
                if (!allowRename && !sender.hasPermission("boatracing.admin")) { sender.sendMessage(Text.colorize(prefix + msg().get("team.rename-restricted"))); return true; }
                if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("team.rename-usage"))); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                String err = es.jaie55.boatracing.ui.TeamGUI.validateNameMessage(name);
                if (err != null) { p.sendMessage(Text.colorize(prefix + msg().get(err))); return true; }
                boolean exists = teamManager.getTeams().stream().anyMatch(tt -> tt != t && tt.getName().equalsIgnoreCase(name));
                if (exists) { p.sendMessage(Text.colorize(prefix + msg().get("team.name-exists"))); return true; }
                t.setName(name); teamManager.save();
                p.sendMessage(Text.colorize(prefix + msg().get("team.renamed", "name", name)));
                return true;
            }
            // /boatracing teams color <dyeColor>
            if (args.length >= 2 && args[1].equalsIgnoreCase("color")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                es.jaie55.boatracing.team.Team t = ot.get();
                boolean allowColor = getConfig().getBoolean("player-actions.allow-team-color", false);
                if (!allowColor && !sender.hasPermission("boatracing.admin")) { sender.sendMessage(Text.colorize(prefix + msg().get("team.color-restricted"))); return true; }
                if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("team.color-usage"))); return true; }
                try {
                    org.bukkit.DyeColor dc = org.bukkit.DyeColor.valueOf(args[2].toUpperCase());
                    t.setColor(dc); teamManager.save();
                    p.sendMessage(Text.colorize(prefix + msg().get("team.color-set", "color", dc.name())));
                } catch (IllegalArgumentException ex) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.invalid-color")));
                }
                return true;
            }
            // /boatracing teams join <team name>
            if (args.length >= 2 && args[1].equalsIgnoreCase("join")) {
                if (teamManager.getTeamByMember(p.getUniqueId()).isPresent()) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.already-in-team")));
                    return true;
                }
                if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("team.join-usage"))); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                es.jaie55.boatracing.team.Team target = teamManager.getTeams().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                if (target == null) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-found"))); return true; }
                if (target.getMembers().size() >= teamManager.getMaxMembers()) { p.sendMessage(Text.colorize(prefix + msg().get("team.full"))); return true; }
                target.addMember(p.getUniqueId()); teamManager.save();
                p.sendMessage(Text.colorize(prefix + msg().get("team.joined", "team", target.getName())));
                for (java.util.UUID m : target.getMembers()) {
                    if (m.equals(p.getUniqueId())) continue;
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                    if (op.isOnline() && op.getPlayer() != null) {
                        op.getPlayer().sendMessage(Text.colorize(prefix + msg().get("team.member-joined", "player", p.getName())));
                    }
                }
                return true;
            }
            // /boatracing teams leave (with confirm if team would be empty)
            if (args.length >= 2 && args[1].equalsIgnoreCase("leave")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                es.jaie55.boatracing.team.Team t = ot.get();
                if (t.getMembers().size() <= 1) {
                    pendingDisband.add(p.getUniqueId());
                    p.sendMessage(Text.colorize(prefix + msg().get("team.last-member-warning")));
                    p.sendMessage(Text.colorize(prefix + msg().get("team.confirm-prompt", "label", label)));
                    return true;
                }
                t.removeMember(p.getUniqueId());
                teamManager.save();
                p.sendMessage(Text.colorize(prefix + msg().get("team.left")));
                for (java.util.UUID m : t.getMembers()) {
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                    if (op.isOnline() && op.getPlayer() != null) {
                        op.getPlayer().sendMessage(Text.colorize(prefix + msg().get("team.member-left", "player", p.getName())));
                    }
                }
                return true;
            }
            // /boatracing teams kick <playerName> (with confirm)
            if (args.length >= 2 && args[1].equalsIgnoreCase("kick")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                p.sendMessage(Text.colorize(prefix + msg().get("team.kick-admin-only", "label", label)));
                return true;
            }
            // /boatracing teams transfer <playerName>
            if (args.length >= 2 && args[1].equalsIgnoreCase("transfer")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                p.sendMessage(Text.colorize(prefix + msg().get("team.transfer-removed")));
                return true;
            }
            // /boatracing teams boat <type>
            if (args.length >= 2 && args[1].equalsIgnoreCase("boat")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("team.boat-usage"))); return true; }
                boolean allowBoat = getConfig().getBoolean("player-actions.allow-set-boat", true);
                if (!allowBoat) { p.sendMessage(Text.colorize(prefix + msg().get("team.boat-restricted"))); return true; }
                String type = args[2].toUpperCase();
                // Accept RAFT tokens directly, otherwise require a valid BOAT material
                if (type.equals("RAFT") || type.equals("CHEST_RAFT")) {
                    ot.get().setBoatType(p.getUniqueId(), type);
                    teamManager.save();
                    p.sendMessage(Text.colorize(prefix + msg().get("team.boat-set", "boat", type.toLowerCase())));
                } else {
                    try {
                        org.bukkit.Material m = org.bukkit.Material.valueOf(type);
                        if (!m.name().endsWith("BOAT")) throw new IllegalArgumentException();
                        ot.get().setBoatType(p.getUniqueId(), m.name());
                        teamManager.save();
                        p.sendMessage(Text.colorize(prefix + msg().get("team.boat-set", "boat", type.toLowerCase())));
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage(Text.colorize(prefix + msg().get("team.invalid-boat")));
                    }
                }
                return true;
            }
            // /boatracing teams number <1-99>
            if (args.length >= 2 && args[1].equalsIgnoreCase("number")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                if (args.length < 3) { p.sendMessage(Text.colorize(prefix + msg().get("team.number-usage"))); return true; }
                boolean allowNumber = getConfig().getBoolean("player-actions.allow-set-number", true);
                if (!allowNumber) { p.sendMessage(Text.colorize(prefix + msg().get("team.number-restricted"))); return true; }
                String s = args[2];
                if (!s.matches("\\d+")) { p.sendMessage(Text.colorize(prefix + msg().get("admin.invalid-number"))); return true; }
                int n = Integer.parseInt(s);
                if (n < 1 || n > 99) { p.sendMessage(Text.colorize(prefix + msg().get("admin.number-out-of-range"))); return true; }
                ot.get().setRacerNumber(p.getUniqueId(), n); teamManager.save();
                p.sendMessage(Text.colorize(prefix + msg().get("team.number-set", "number", String.valueOf(n))));
                return true;
            }
            // /boatracing teams disband y /boatracing teams confirm
            if (args.length >= 2 && args[1].equalsIgnoreCase("disband")) {
                java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                p.sendMessage(Text.colorize(prefix + msg().get("team.disband-admin-only", "label", label)));
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                // Confirm pending dangerous actions (last-member leave -> disband)
                if (pendingDisband.remove(p.getUniqueId())) {
                    java.util.Optional<es.jaie55.boatracing.team.Team> ot = teamManager.getTeamByMember(p.getUniqueId());
                    if (ot.isEmpty()) { p.sendMessage(Text.colorize(prefix + msg().get("team.not-in-team"))); return true; }
                    es.jaie55.boatracing.team.Team t = ot.get();
                    // Proceed: remove member (self) and delete team since no members left
                    t.removeMember(p.getUniqueId());
                    teamManager.deleteTeam(t);
                    p.sendMessage(Text.colorize(prefix + msg().get("team.left-and-deleted")));
                    return true;
                }
                p.sendMessage(Text.colorize(prefix + msg().get("team.nothing-to-confirm")));
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
                boolean any = false;
                if (pendingDisband.remove(p.getUniqueId())) { any = true; }
                if (pendingTransfer.remove(p.getUniqueId()) != null) { any = true; }
                if (pendingKick.remove(p.getUniqueId()) != null) { any = true; }
                if (!any) {
                    p.sendMessage(Text.colorize(prefix + msg().get("team.nothing-to-cancel")));
                    return true;
                }
                        p.sendMessage(Text.colorize(prefix + msg().get("team.cancelled")));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
                return true;
            }
            // default fallback
            p.sendMessage(Text.colorize(prefix + msg().get("team.unknown-subcommand")));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("boatracing")) {
            // Root suggestions (handle no-arg and first arg prefix)
            if (args.length == 0 || (args.length == 1 && (args[0] == null || args[0].isEmpty()))) {
                java.util.List<String> root = new java.util.ArrayList<>();
                if (sender.hasPermission("boatracing.teams")) root.add("teams");
                // Expose 'race' root to all users for join/leave/status discoverability
                root.add("race");
                if (sender.hasPermission("boatracing.stats")) root.add("stats");
                if (sender.hasPermission("boatracing.setup")) root.add("setup");
                    if (sender.hasPermission("boatracing.admin") || sender.hasPermission("boatracing.admin.language")) root.add("admin");
                if (sender.hasPermission("boatracing.reload")) root.add("reload");
                if (sender.hasPermission("boatracing.version")) root.add("version");
                return root;
            }
            if (args.length == 1) {
                String pref = args[0].toLowerCase();
                java.util.List<String> root = new java.util.ArrayList<>();
                if (sender.hasPermission("boatracing.teams")) root.add("teams");
                root.add("race");
                if (sender.hasPermission("boatracing.stats")) root.add("stats");
                if (sender.hasPermission("boatracing.setup")) root.add("setup");
                    if (sender.hasPermission("boatracing.admin") || sender.hasPermission("boatracing.admin.language")) root.add("admin");
                if (sender.hasPermission("boatracing.reload")) root.add("reload");
                if (sender.hasPermission("boatracing.version")) root.add("version");
                return root.stream().filter(s -> s.startsWith(pref)).toList();
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("stats")) {
                if (!sender.hasPermission("boatracing.stats")) return java.util.Collections.emptyList();
                if (args.length == 2) {
                    if (!sender.hasPermission("boatracing.stats.others")) return java.util.Collections.emptyList();
                    String pref = args[1] == null ? "" : args[1].toLowerCase();
                    java.util.Set<String> names = new java.util.LinkedHashSet<>();
                    for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (online.getName() != null && online.getName().toLowerCase().startsWith(pref)) names.add(online.getName());
                    }
                    for (org.bukkit.OfflinePlayer offline : org.bukkit.Bukkit.getOfflinePlayers()) {
                        String name = safeOfflineName(offline);
                        if (name != null && name.toLowerCase().startsWith(pref)) names.add(name);
                    }
                    return new java.util.ArrayList<>(names);
                }
                return java.util.Collections.emptyList();
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
                boolean canUseAdmin = sender.hasPermission("boatracing.admin");
                boolean canManageLanguage = sender.hasPermission("boatracing.admin.language");
                if (!canUseAdmin && !canManageLanguage) return java.util.Collections.emptyList();
                if (args.length == 2) {
                    String pref = args[1] == null ? "" : args[1].toLowerCase();
                    java.util.List<String> subs = new java.util.ArrayList<>();
                    if (canUseAdmin) {
                        subs.add("help");
                        subs.add("team");
                        subs.add("player");
                        subs.add("tracks");
                    }
                    subs.add("language");
                    return subs.stream().filter(s -> s.startsWith(pref)).toList();
                }
                if (args[1].equalsIgnoreCase("language")) {
                    if (!(canUseAdmin || canManageLanguage)) return java.util.Collections.emptyList();
                    if (args.length == 3) {
                        String pref = args[2] == null ? "" : args[2].toLowerCase();
                        java.util.List<String> codes = new java.util.ArrayList<>(getAvailableLanguageCodes());
                        return codes.stream().filter(code -> code.toLowerCase().startsWith(pref)).toList();
                    }
                    return java.util.Collections.emptyList();
                }
                if (!canUseAdmin) return java.util.Collections.emptyList();
                if (args.length == 3 && args[1].equalsIgnoreCase("team")) return java.util.Arrays.asList("create","delete","rename","color","add","remove");
                if (args.length == 3 && args[1].equalsIgnoreCase("player")) return java.util.Arrays.asList("setteam","setnumber","setboat");
                if (args.length == 5 && args[2].equalsIgnoreCase("setboat")) {
                    return java.util.Arrays.asList(
                        "oak_boat","spruce_boat","birch_boat","jungle_boat","acacia_boat","dark_oak_boat","mangrove_boat","cherry_boat","pale_oak_boat",
                        "oak_chest_boat","spruce_chest_boat","birch_chest_boat","jungle_chest_boat","acacia_chest_boat","dark_oak_chest_boat","mangrove_chest_boat","cherry_chest_boat","pale_oak_chest_boat",
                        "bamboo_raft","bamboo_chest_raft"
                    );
                }
                return java.util.Collections.emptyList();
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("race")) {
                // Admin subcommands guarded; expose join/leave/status to everyone
                if (args.length == 2) {
                    java.util.List<String> subs = new java.util.ArrayList<>();
                    subs.add("help");
                    subs.add("join"); subs.add("leave"); subs.add("forfeit"); subs.add("status");
                    subs.add("vote"); subs.add("voteui"); subs.add("votestatus");
                    if (sender.hasPermission("boatracing.race.back")) subs.add("back");
                    if (sender.hasPermission("boatracing.race.practice")) subs.add("practice");
                    if (sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup")
                            || getConfig().getBoolean("player-actions.allow-player-race-start", false)) {
                        subs.add("open"); subs.add("start"); subs.add("force"); subs.add("stop");
                    }
                    if (sender.hasPermission("boatracing.race.voteopen") || sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup")) {
                        subs.add("voteopen");
                    }
                    if (sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup")) {
                        subs.add("voteclose");
                    }
                    String pref = args[1] == null ? "" : args[1].toLowerCase();
                    return subs.stream().filter(s -> s.startsWith(pref)).toList();
                }
                // For subcommands that take <track>, suggest track names from library
                String raceSub = args[1].toLowerCase();
                if (raceSub.equals("practice") && !canUsePracticeCommand(sender)) {
                    return java.util.Collections.emptyList();
                }
                if (args.length == 3 && raceSub.equals("practice")) {
                    java.util.List<String> options = new java.util.ArrayList<>();
                    options.add("leave");
                    if (trackLibrary != null) {
                        for (String n : trackLibrary.list()) if (n.toLowerCase().startsWith(args[2].toLowerCase())) options.add(n);
                    }
                    if ("unsaved".startsWith(args[2].toLowerCase())) options.add("unsaved");
                    return options;
                }
                if (args.length == 4 && raceSub.equals("practice") && (args[2].equalsIgnoreCase("leave") || args[2].equalsIgnoreCase("exit"))) {
                    String prefix = args[3] == null ? "" : args[3].toLowerCase();
                    java.util.List<String> names = new java.util.ArrayList<>();
                    if (trackLibrary != null) {
                        for (String n : trackLibrary.list()) if (n.toLowerCase().startsWith(prefix)) names.add(n);
                    }
                    if ("unsaved".startsWith(prefix)) names.add("unsaved");
                    return names;
                }
                if (args.length == 3 && java.util.Arrays.asList("open","join","leave","force","start","stop","status","vote").contains(raceSub)) {
                    String prefix = args[2] == null ? "" : args[2].toLowerCase();
                    java.util.List<String> names = new java.util.ArrayList<>();
                    if (trackLibrary != null) {
                        for (String n : trackLibrary.list()) if (n.toLowerCase().startsWith(prefix)) names.add(n);
                    }
                    if ("unsaved".startsWith(prefix)) names.add("unsaved");
                    return names;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("voteopen")) {
                    if (!(sender.hasPermission("boatracing.race.voteopen") || sender.hasPermission("boatracing.race.admin") || sender.hasPermission("boatracing.setup"))) {
                        return java.util.Collections.emptyList();
                    }
                    // voteopen [all|<track1> <track2> ...] [seconds]
                    if (args.length >= 4 && args[args.length - 1].matches("\\d+")) {
                        return java.util.Collections.emptyList();
                    }
                    String prefix = args[args.length - 1] == null ? "" : args[args.length - 1].toLowerCase();
                    java.util.List<String> names = new java.util.ArrayList<>();
                    if ("all".startsWith(prefix)) names.add("all");
                    if ("*".startsWith(prefix)) names.add("*");
                    if (trackLibrary != null) {
                        for (String n : trackLibrary.list()) if (n.toLowerCase().startsWith(prefix)) names.add(n);
                    }
                    if ("unsaved".startsWith(prefix)) names.add("unsaved");
                    if ("30".startsWith(prefix)) names.add("30");
                    return names;
                }
                return java.util.Collections.emptyList();
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("setup")) {
                if (!sender.hasPermission("boatracing.setup")) return Collections.emptyList();
                if (args.length == 2) return Arrays.asList("help","addstart","clearstarts","removestart","setfinish","clearfinish","setpit","clearpit","addcheckpoint","clearcheckpoints","addlight","removelight","clearlights","setlaps","setpitstops","setlobby","setpos","clearpos","show","selinfo","wand","wizard");
                if (args.length >= 3 && args[1].equalsIgnoreCase("setpit")) {
                    // Build current partial input (join tokens from index 2)
                    String partial = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).toLowerCase();
                    boolean startedQuote = args[2] != null && (args[2].startsWith("\"") || args[2].startsWith("'"));
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (es.jaie55.boatracing.team.Team t : teamManager.getTeams()) {
                        String name = t.getName();
                        if (name == null) continue;
                        String quoted = '"' + name + '"';
                        String cand = startedQuote ? quoted : name;
                        if (cand.toLowerCase().startsWith(partial)) names.add(cand);
                    }
                    return names;
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("removestart")) {
                    String pref = args[2] == null ? "" : args[2].toLowerCase();
                    java.util.List<String> opts = new java.util.ArrayList<>();
                    int max = trackConfig.getStarts().size();
                    for (int i = 1; i <= Math.min(max, 30); i++) opts.add(String.valueOf(i));
                    return opts.stream().filter(s -> s.startsWith(pref)).toList();
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("removelight")) {
                    String pref = args[2] == null ? "" : args[2].toLowerCase();
                    java.util.List<String> opts = new java.util.ArrayList<>();
                    int max = trackConfig.getLights().size();
                    for (int i = 1; i <= Math.min(max, 5); i++) opts.add(String.valueOf(i));
                    return opts.stream().filter(s -> s.startsWith(pref)).toList();
                }
                if (args.length >= 3 && (args[1].equalsIgnoreCase("setpos") || args[1].equalsIgnoreCase("clearpos"))) {
                    // Suggest player names (online + known offline)
                    String prefName = args[2] == null ? "" : args[2].toLowerCase();
                    java.util.Set<String> names = new java.util.LinkedHashSet<>();
                    for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (op.getName() != null && op.getName().toLowerCase().startsWith(prefName)) names.add(op.getName());
                    }
                    for (org.bukkit.OfflinePlayer op : org.bukkit.Bukkit.getOfflinePlayers()) {
                        String name = safeOfflineName(op);
                        if (name != null && name.toLowerCase().startsWith(prefName)) names.add(name);
                    }
                    if (args.length == 3) return new java.util.ArrayList<>(names);
                    if (args.length == 4 && args[1].equalsIgnoreCase("setpos")) {
                        // Suggest slot numbers and keyword 'auto'
                        java.util.List<String> opts = new java.util.ArrayList<>();
                        opts.add("auto");
                        int max = trackConfig.getStarts().size();
                        for (int i = 1; i <= Math.min(max, 20); i++) opts.add(String.valueOf(i));
                        String pref = args[3] == null ? "" : args[3].toLowerCase();
                        return opts.stream().filter(s -> s.startsWith(pref)).toList();
                    }
                    return java.util.Collections.emptyList();
                }
                // Do not expose wizard subcommands in tab-completion; single entrypoint UX
                return Collections.emptyList();
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("teams")) {
                if (!sender.hasPermission("boatracing.teams")) return Collections.emptyList();
                if (args.length == 2) return Arrays.asList("create", "rename", "color", "join", "leave", "boat", "number", "confirm", "cancel");
                // Autocomplete team names for 'join'
                if (args.length >= 3 && args[1].equalsIgnoreCase("join")) {
                    String prefix = args[2].toLowerCase();
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (es.jaie55.boatracing.team.Team t : teamManager.getTeams()) {
                        String name = t.getName();
                        if (name != null && name.toLowerCase().startsWith(prefix)) names.add(name);
                    }
                    return names;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("create")) return Collections.emptyList();
                if (args.length >= 3 && args[1].equalsIgnoreCase("color")) return java.util.Arrays.stream(org.bukkit.DyeColor.values()).map(Enum::name).map(String::toLowerCase).toList();
                if (args.length >= 3 && args[1].equalsIgnoreCase("boat")) return Arrays.asList(
                    // Normal boats first
                    "oak_boat","spruce_boat","birch_boat","jungle_boat","acacia_boat","dark_oak_boat","mangrove_boat","cherry_boat","pale_oak_boat",
                    // Then chest-boat variants
                    "oak_chest_boat","spruce_chest_boat","birch_chest_boat","jungle_chest_boat","acacia_chest_boat","dark_oak_chest_boat","mangrove_chest_boat","cherry_chest_boat","pale_oak_chest_boat",
                    // Rafts (bamboo)
                    "bamboo_raft","bamboo_chest_raft"
                );
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private void sendUpdateStatus(Player p) {
        if (updateChecker == null) return;
                        String current = getDescription().getVersion();
        if (!updateChecker.isChecked()) return;
        if (updateChecker.hasError()) {
            p.sendMessage(Text.colorize(prefix + msg().get("update.failed")));
            p.sendMessage(Text.colorize(prefix + msg().get("update.releases", "url", updateChecker.getLatestUrl())));
            return;
        }
        if (updateChecker.isOutdated()) {
            int behind = updateChecker.getBehindCount();
            String latest = updateChecker.getLatestVersion() != null ? updateChecker.getLatestVersion() : "latest";
            p.sendMessage(Text.colorize(prefix + msg().get("update.available", "name", getName(), "behind", String.valueOf(behind))));
            p.sendMessage(Text.colorize(prefix + msg().get("update.current-vs-latest", "current", current, "latest", latest)));
            p.sendMessage(Text.colorize(prefix + msg().get("update.link", "url", updateChecker.getLatestUrl())));
        } else {
            String latest = updateChecker.getLatestVersion() != null ? updateChecker.getLatestVersion() : current;
            p.sendMessage(Text.colorize(prefix + msg().get("update.up-to-date", "version", current)));
            p.sendMessage(Text.colorize(prefix + msg().get("update.info", "latest", latest, "url", updateChecker.getLatestUrl())));
        }
    }

    private static String fmtBox(org.bukkit.util.BoundingBox b) {
        return String.format("min(%d,%d,%d) max(%d,%d,%d)",
                (int) Math.floor(b.getMinX()), (int) Math.floor(b.getMinY()), (int) Math.floor(b.getMinZ()),
                (int) Math.floor(b.getMaxX()), (int) Math.floor(b.getMaxY()), (int) Math.floor(b.getMaxZ()));
    }
}
