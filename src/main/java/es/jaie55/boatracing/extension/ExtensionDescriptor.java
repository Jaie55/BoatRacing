package es.jaie55.boatracing.extension;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code extension.yml} descriptor found inside an extension jar. */
public record ExtensionDescriptor(String name, String version, String main, int apiVersion, String description,
                                  List<PermissionEntry> permissions) {

    /** A permission declared by the extension and registered by BoatRacing at load time. */
    public record PermissionEntry(String name, String description, PermissionDefault defaultLevel) {
    }

    public ExtensionDescriptor {
        permissions = permissions == null ? List.of() : Collections.unmodifiableList(permissions);
    }

    public static ExtensionDescriptor read(InputStream in) throws IOException {
        if (in == null) throw new IOException("missing extension.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        String name = yml.getString("name");
        String main = yml.getString("main");
        String version = yml.getString("version", "unknown");
        int apiVersion = yml.getInt("api-version", 1);
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9._ -]{1,63}") || name.contains("..")) {
            throw new IOException("invalid or missing 'name' (letters/digits first; letters, digits, dot, underscore, dash or space; no '..')");
        }
        if (main == null || main.isBlank()) {
            throw new IOException("missing 'main' class");
        }
        List<PermissionEntry> permissions = new ArrayList<>();
        ConfigurationSection permissionSection = yml.getConfigurationSection("permissions");
        if (permissionSection != null) {
            for (String key : permissionSection.getKeys(false)) {
                ConfigurationSection node = permissionSection.getConfigurationSection(key);
                String permissionName = node != null ? node.getString("name", key) : key;
                if (permissionName == null || permissionName.isBlank()) continue;
                String permissionDescription = node != null ? node.getString("description", "") : "";
                PermissionDefault defaultLevel = PermissionDefault.OP;
                if (node != null) {
                    String raw = node.getString("default", "op");
                    try {
                        PermissionDefault parsed = PermissionDefault.getByName(raw);
                        if (parsed != null) defaultLevel = parsed;
                    } catch (Throwable ignored) {
                        defaultLevel = PermissionDefault.OP;
                    }
                }
                permissions.add(new PermissionEntry(permissionName, permissionDescription, defaultLevel));
            }
        }
        return new ExtensionDescriptor(name, version, main, apiVersion, yml.getString("description", ""), permissions);
    }
}
