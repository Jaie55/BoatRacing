package es.jaie55.boatracing.update;

import es.jaie55.boatracing.util.SchedulerCompat;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private final Plugin plugin;
    private final String releasesApi;
    private final String releasesPage;
    private final HttpClient http;
    private final String currentVersion;

    private volatile String latestVersion = null;
    private volatile String latestUrl = null;
    private volatile int behindCount = 0;
    private volatile boolean checked = false;
    private volatile boolean error = false;
    private volatile boolean errorLoggedOnce = false;

    // Modrinth JSON: array of versions, each with "version_number"
    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Create an UpdateChecker for a Modrinth project slug.
     * @param plugin plugin
     * @param modrinthSlug e.g. "boatracing"
     * @param currentVersion plugin version
     */
    public UpdateChecker(Plugin plugin, String modrinthSlug, String currentVersion) {
        this.plugin = plugin;
    this.releasesApi = "https://api.modrinth.com/v2/project/" + modrinthSlug + "/version";
    // Downloads/announcements should link to Modrinth project page
    this.releasesPage = "https://modrinth.com/plugin/" + modrinthSlug;
        this.currentVersion = currentVersion;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void checkAsync() {
        SchedulerCompat.runAsync(plugin, () -> {
            try {
                checkNow();
            } catch (Exception ex) {
                error = true;
                if (!errorLoggedOnce) {
                    plugin.getLogger().warning("Update check failed: " + ex.getMessage());
                    errorLoggedOnce = true;
                }
            }
        });
    }

    private void checkNow() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(releasesApi))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", plugin.getName() + " UpdateChecker")
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("Modrinth API HTTP " + res.statusCode());
        }
        String body = res.body();

        // Modrinth returns an array (usually newest first). Take the first version_number as latest.
        List<String> versions = new ArrayList<>();
        Matcher m = VERSION_NUMBER_PATTERN.matcher(body);
        while (m.find()) {
            String v = normalizeVersion(m.group(1));
            if (v != null) versions.add(v);
        }
        latestUrl = releasesPage; // point to project page for downloads

        if (versions.isEmpty()) {
            latestVersion = null;
            checked = true;
            return;
        }

        latestVersion = versions.get(0);
        String current = normalizeVersion(currentVersion);
        behindCount = 0;
        if (current != null) {
            for (String v : versions) {
                if (isNewer(v, current)) behindCount++; else break;
            }
            if (latestVersion.equals(current)) behindCount = 0;
        }
        checked = true;
    }

    private String normalizeVersion(String v) {
        if (v == null || v.isEmpty()) return null;
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    private boolean isNewer(String a, String b) {
        if (a == null || b == null) return false;
        int[] na = parseVersion(a);
        int[] nb = parseVersion(b);
        for (int i = 0; i < 3; i++) {
            if (na[i] != nb[i]) return na[i] > nb[i];
        }
        return false;
    }

    private int[] parseVersion(String v) {
        int[] parts = new int[3];
        String[] seg = v.split("\\.");
        for (int i = 0; i < seg.length && i < 3; i++) {
            try { parts[i] = Integer.parseInt(seg[i]); } catch (NumberFormatException ignored) { parts[i] = 0; }
        }
        return parts;
    }

    public boolean isChecked() { return checked; }
    public boolean hasError() { return error; }
    public boolean isOutdated() { return checked && latestVersion != null && behindCount > 0; }
    public String getLatestVersion() { return latestVersion; }
    public String getLatestUrl() { return latestUrl != null ? latestUrl : releasesPage; }
    public int getBehindCount() { return behindCount; }
}
