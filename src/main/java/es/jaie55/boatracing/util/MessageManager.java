package es.jaie55.boatracing.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads and serves user-facing messages from the {@code lang/} folder
 * ({@code lang/messages_en.yml}, {@code lang/messages_es.yml}, ...) or any custom bundle placed
 * there. Language is configured in config.yml via the 'language' setting (default: "en").
 * Legacy bundles saved next to config.yml are moved into lang/ automatically.
 */
public final class MessageManager {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private YamlConfiguration englishDefaults;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        migrateLegacyBundles();
        String lang = sanitizeLanguage(plugin.getConfig().getString("language", "en"));

        englishDefaults = loadBundledYaml("lang/messages_en.yml");

        File file = resolveLanguageFile(lang);
        if (file == null && !"en".equalsIgnoreCase(lang)) {
            plugin.getLogger().warning("Language '" + lang + "' was not found. Falling back to 'en'.");
            lang = "en";
            file = resolveLanguageFile(lang);
        }
        if (file == null) {
            plugin.getLogger().severe("Could not load message bundle 'lang/messages_en.yml'. Message keys will be shown as fallback.");
            messages = new YamlConfiguration();
            return;
        }

        String filename = file.getName();
        messages = YamlConfiguration.loadConfiguration(file);

        // Always fallback to English for any missing key in community bundles.
        if (englishDefaults != null) {
            messages.addDefaults(englishDefaults);
        }

        // Merge any new keys added in future updates
        try (InputStream defaults = plugin.getResource("lang/" + filename)) {
            if (defaults != null) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaults, StandardCharsets.UTF_8));
                messages.addDefaults(def);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to merge default messages for '" + filename + "': " + ex.getMessage());
        }

        messages.options().copyDefaults(true);
    }

    /** @return the folder holding every messages_*.yml bundle. */
    public static File languageFolder(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "lang");
    }

    /**
     * Moves language files that older versions saved next to config.yml into the lang folder.
     * Files already present in lang/ are kept as-is and the old copy is renamed to .migrated.
     */
    private void migrateLegacyBundles() {
        File root = plugin.getDataFolder();
        File[] legacy = root.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (legacy == null || legacy.length == 0) return;
        File folder = languageFolder(plugin);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create the lang folder: " + folder.getAbsolutePath());
            return;
        }
        for (File file : legacy) {
            File target = new File(folder, file.getName());
            try {
                if (!target.exists()) {
                    java.nio.file.Files.move(file.toPath(), target.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Moved " + file.getName() + " into the lang folder.");
                } else {
                    java.nio.file.Files.move(file.toPath(),
                            file.toPath().resolveSibling(file.getName() + ".migrated"),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Kept lang/" + file.getName() + " and renamed the old root copy to "
                            + file.getName() + ".migrated.");
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not move " + file.getName() + " into the lang folder: " + ex.getMessage());
            }
        }
    }

    private YamlConfiguration loadBundledYaml(String filename) {
        try (InputStream defaults = plugin.getResource(filename)) {
            if (defaults == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load bundled messages from '" + filename + "': " + ex.getMessage());
            return null;
        }
    }

    private String sanitizeLanguage(String lang) {
        if (lang == null) return "en";
        String normalized = lang.trim();
        if (normalized.isEmpty()) return "en";
        if (!normalized.matches("[A-Za-z0-9_-]+")) {
            plugin.getLogger().warning("Invalid language code '" + normalized + "'. Falling back to 'en'.");
            return "en";
        }
        return normalized;
    }

    private File resolveLanguageFile(String lang) {
        String filename = "messages_" + lang + ".yml";
        File file = new File(languageFolder(plugin), filename);
        if (file.exists()) return file;

        try (InputStream bundled = plugin.getResource("lang/" + filename)) {
            if (bundled == null) return null;
        } catch (Exception ignored) {
            return null;
        }

        plugin.saveResource("lang/" + filename, false);
        return file.exists() ? file : null;
    }

    /**
     * Get a message with optional placeholder pairs.
     * Usage: {@code msg.get("race.track-not-found", "track", trackName)}
     * replaces every {@code {track}} in the template.
     *
     * @param key   dot-path into messages.yml
     * @param pairs alternating placeholder name / value: "ph1", val1, "ph2", val2 …
     * @return the resolved message (still contains &amp; colour codes)
     */
    public String get(String key, Object... pairs) {
        String msg = messages.getString(key);
        if (msg == null && englishDefaults != null) {
            msg = englishDefaults.getString(key);
        }
        if (msg == null) return key; // fallback: show the key itself
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            msg = msg.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return msg;
    }
}
