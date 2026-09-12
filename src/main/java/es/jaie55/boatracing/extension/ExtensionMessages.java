package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.BoatRacingPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Language-aware messages for an extension. Files live in the extension folder
 * ({@code messages_<language>.yml}) and fall back to the ones bundled inside the extension jar,
 * mirroring the base plugin. The parent classloader is never used, so base plugin files can never
 * shadow the extension's own bundles.
 */
public final class ExtensionMessages {

    private final BoatRacingPlugin plugin;
    private final File dataFolder;
    private final File jarFile;
    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration defaults = new YamlConfiguration();

    ExtensionMessages(BoatRacingPlugin plugin, File dataFolder, File jarFile) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
        this.jarFile = jarFile;
        reload();
    }

    public void reload() {
        defaults = load("messages_en.yml");
        String language = plugin.getConfig().getString("language", "en");
        if (language == null || !language.matches("[A-Za-z0-9_-]+")) language = "en";
        messages = load("messages_" + language + ".yml");
        if (messages.getKeys(false).isEmpty() && !"en".equalsIgnoreCase(language)) {
            messages = load("messages_en.yml");
        }
        messages.setDefaults(defaults);
    }

    private YamlConfiguration load(String name) {
        File file = new File(new File(dataFolder, "lang"), name);
        if (file.exists()) return YamlConfiguration.loadConfiguration(file);
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("lang/" + name);
            if (entry == null) return new YamlConfiguration();
            try (InputStream in = jar.getInputStream(entry)) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            return new YamlConfiguration();
        }
    }

    public String get(String key, Object... replacements) {
        String raw = messages.getString(key);
        if (raw == null) raw = defaults.getString(key);
        if (raw == null) return key;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return raw;
    }

    public List<String> getList(String key) {
        List<String> raw = messages.getStringList(key);
        if (raw.isEmpty()) raw = defaults.getStringList(key);
        return raw;
    }
}
