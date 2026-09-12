package es.jaie55.boatracing.util;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Version-safe material resolution for GUI icons. Materials are stored as names and matched
 * with {@link Material#matchMaterial(String)}, so a material removed or renamed in a future
 * version never throws {@code NoSuchFieldError} at runtime.
 */
public final class IconResolver {

    private static final Map<String, Material> CACHE = new HashMap<>();

    private IconResolver() {
    }

    public static Material resolve(String name, Material fallback) {
        if (name == null || name.isBlank()) return fallback;
        String key = name.trim().toUpperCase(Locale.ROOT);
        Material cached = CACHE.get(key);
        if (cached != null) return cached;
        Material material = null;
        try {
            material = Material.matchMaterial(key);
        } catch (Throwable ignored) {
            // Unknown/unsupported material name on this version.
        }
        if (material == null) material = fallback;
        CACHE.put(key, material);
        return material;
    }
}
