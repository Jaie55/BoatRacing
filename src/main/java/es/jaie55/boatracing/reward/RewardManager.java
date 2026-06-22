package es.jaie55.boatracing.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.Text;

/**
 * Manages race rewards configured in config.yml under racing.rewards.
 * Supports per-position commands, messages, and broadcasts with placeholders.
 */
public class RewardManager {
    private final ConfigurationSection rewardSection;

    public RewardManager(BoatRacingPlugin plugin) {
        this.rewardSection = plugin.getConfig().getConfigurationSection("racing.rewards");
    }

    public boolean isEnabled() {
        return this.rewardSection != null && this.rewardSection.getBoolean("enabled", false);
    }

    public boolean isEnabled(TrackConfig track) {
        if (track == null) return isEnabled();
        // Try to get the reward section from the track, else fall back to the default.
        ConfigurationSection trackSection = track.getRacingConfigurationSection("rewards", this.rewardSection);
        return trackSection != null && trackSection.getBoolean("enabled", false);
    }

    /**
     * Distribute rewards to all finishers in order.
     *
     * @param results  sorted list of (UUID, finishTime) pairs, position 1 first
     * @param trackName name of the track
     * @param totalLaps total laps in the race
     */
    public void giveRewards(List<Map.Entry<UUID, Long>> results, String trackName, int totalLaps, TrackConfig track) {
        ConfigurationSection targetSection = (track == null) ? this.rewardSection : track.getRacingConfigurationSection("rewards", this.rewardSection);
        if (targetSection == null || !targetSection.getBoolean("enabled", false)) return;
        ConfigurationSection posSection = targetSection.getConfigurationSection("positions");
        if (posSection == null) return;
        ConfigurationSection defaultReward = posSection.getConfigurationSection("default");

        int position = 1;
        for (Map.Entry<UUID, Long> entry : results) {
            UUID playerId = entry.getKey();
            long finishTimeMs = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) { position++; continue; }

            // Find the config section for this position, fall back to "default"
            ConfigurationSection reward = posSection.getConfigurationSection(String.valueOf(position));
            if (reward == null) {
                reward = defaultReward;
            }
            if (reward == null) { position++; continue; }

            String playerName = player.getName();
            String timeFormatted = formatTime(finishTimeMs);

            // Execute console commands
            List<String> commands = resolveRewardList(reward, defaultReward, "commands");
            if (commands.isEmpty()) {
                // Compatibility with older/simplified configs that use singular key.
                commands = resolveRewardList(reward, defaultReward, "command");
            }
            for (String cmd : commands) {
                String resolved = applyPlaceholders(cmd, playerName, position, timeFormatted, trackName, totalLaps);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }

            // Send personal messages
            List<String> messages = resolveRewardList(reward, defaultReward, "messages");
            for (String msg : messages) {
                String resolved = applyPlaceholders(msg, playerName, position, timeFormatted, trackName, totalLaps);
                player.sendMessage(Text.colorize(resolved));
            }

            // Broadcast messages to all online players
            List<String> broadcasts = resolveRewardList(reward, defaultReward, "broadcast");
            for (String bc : broadcasts) {
                String resolved = applyPlaceholders(bc, playerName, position, timeFormatted, trackName, totalLaps);
                String colorized = Text.colorize(resolved);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(colorized);
                }
            }

            position++;
        }
    }

    private String applyPlaceholders(String template, String playerName, int position, String time, String track, int laps) {
        return template
                .replace("{player}", playerName)
                .replace("{position}", String.valueOf(position))
                .replace("{time}", time)
                .replace("{track}", track)
                .replace("{laps}", String.valueOf(laps));
    }

    private List<String> resolveRewardList(ConfigurationSection reward, ConfigurationSection fallback, String key) {
        List<String> direct = readListOrSingle(reward, key);
        if (!direct.isEmpty()) return direct;
        if (reward != null && reward.contains(key, false)) return direct;
        if (fallback == null || fallback == reward) return direct;
        return readListOrSingle(fallback, key);
    }

    private List<String> readListOrSingle(ConfigurationSection section, String key) {
        if (section == null || key == null || key.isEmpty()) return java.util.Collections.emptyList();
        Object raw = section.get(key);
        if (raw == null) return java.util.Collections.emptyList();

        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry == null) continue;
                String value = String.valueOf(entry).trim();
                if (!value.isEmpty()) out.add(value);
            }
            return out;
        }

        if (raw instanceof String str) {
            String value = str.trim();
            if (!value.isEmpty()) out.add(value);
        }
        return out;
    }

    private static String formatTime(long millis) {
        long sec = millis / 1000;
        long ms = millis % 1000;
        long m = sec / 60;
        long s = sec % 60;
        return String.format("%d:%02d.%03d", m, s, ms);
    }
}
