package es.jaie55.boatracing.util;

import es.jaie55.boatracing.BoatRacingPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Lightweight Discord webhook sender for race events.
 * The configuration is read on every call so {@code /boatracing reload} applies immediately.
 */
public class DiscordWebhook {

    private static final int EMBED_COLOR = 12306129;

    private final BoatRacingPlugin plugin;
    private final HttpClient http;

    public DiscordWebhook(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false) && webhookUrl() != null;
    }

    public void sendRaceStart(String track, int laps, Collection<String> racers) {
        if (!isEnabled() || !eventEnabled("race-start")) return;
        int count = racers == null ? 0 : racers.size();
        String names = racers == null || racers.isEmpty() ? "-" : String.join(", ", racers);
        plugin.getLogger().fine("Discord webhook: race-start on '" + track + "' (" + count + " racer(s)).");
        send(embed("Race started | " + track, "**Laps:** " + laps + "\n**Racers (" + count + "):** " + names));
    }

    public void sendRaceResults(String track, int laps, List<String> lines) {
        if (!isEnabled() || !eventEnabled("race-results")) return;
        plugin.getLogger().fine("Discord webhook: race-results on '" + track + "'.");
        String body = "**Laps:** " + laps + "\n" + (lines == null || lines.isEmpty() ? "-" : String.join("\n", lines));
        send(embed("Race results | " + track, body));
    }

    public void sendRecord(String track, String player, long millis, int laps) {
        if (!isEnabled() || !eventEnabled("record-broken")) return;
        plugin.getLogger().fine("Discord webhook: record-broken on '" + track + "' by " + player + ".");
        send(embed("Track record | " + track,
                "**" + player + "** set a new record: **" + formatSeconds(millis) + "** (" + laps + " laps)"));
    }

    private boolean eventEnabled(String key) {
        return plugin.getConfig().getBoolean("discord.events." + key, true);
    }

    private String webhookUrl() {
        String url = plugin.getConfig().getString("discord.webhook-url", "");
        if (url == null) return null;
        url = url.trim();
        if (url.isEmpty() || !url.startsWith("https://")) return null;
        return url;
    }

    private String embed(String title, String description) {
        String username = plugin.getConfig().getString("discord.username", "BoatRacing");
        String avatar = plugin.getConfig().getString("discord.avatar-url", "");
        StringBuilder json = new StringBuilder();
        json.append('{');
        if (username != null && !username.isBlank()) {
            json.append("\"username\":\"").append(escape(username)).append("\",");
        }
        if (avatar != null && !avatar.isBlank()) {
            json.append("\"avatar_url\":\"").append(escape(avatar)).append("\",");
        }
        json.append("\"embeds\":[{\"title\":\"").append(escape(title))
                .append("\",\"description\":\"").append(escape(description))
                .append("\",\"color\":").append(EMBED_COLOR).append("}]}");
        return json.toString();
    }

    private void send(String payload) {
        String url = webhookUrl();
        if (url == null) return;
        SchedulerCompat.runAsyncNow(plugin, () -> post(url, payload));
    }

    private void post(String url, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                plugin.getLogger().warning("Discord webhook returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to send Discord webhook: " + ex.getMessage()
                    + " (check discord.webhook-url; run /boatracing debug when reporting this)");
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String formatSeconds(long millis) {
        long total = Math.max(0L, millis);
        long minutes = total / 60000L;
        long seconds = (total % 60000L) / 1000L;
        long ms = total % 1000L;
        if (minutes > 0) return String.format(java.util.Locale.ROOT, "%d:%02d.%03d", minutes, seconds, ms);
        return String.format(java.util.Locale.ROOT, "%d.%03d", seconds, ms);
    }
}
