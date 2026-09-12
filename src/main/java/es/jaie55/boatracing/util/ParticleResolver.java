package es.jaie55.boatracing.util;

import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Version-safe particle resolution.
 *
 * Vanilla particles were renamed across versions (for example {@code VILLAGER_HAPPY} became
 * {@code HAPPY_VILLAGER} in 1.20.5). This resolver tries the configured name and a list of
 * known aliases so cosmetics keep working on every supported version, and reports when a
 * particle does not exist at all (so the cosmetic can be hidden).
 *
 * Some particles also require extra data when spawned (dust, block/item based). Those are
 * marked unsafe so cosmetics using them can be hidden instead of throwing at runtime.
 */
public final class ParticleResolver {

    /** Symmetric alias table: any key maps to the names that may represent the same particle. */
    private static final Map<String, List<String>> ALIASES = buildAliases();

    /** Particles that need extra data (DustOptions, BlockData, ItemStack...) when spawned. */
    private static final Set<String> DATA_REQUIRED = Set.of(
            "DUST", "REDSTONE",
            "BLOCK", "BLOCK_CRACK", "BLOCK_DUST",
            "ITEM", "ITEM_CRACK",
            "FALLING_DUST", "LEGACY_FALLING_DUST",
            "ENTITY_EFFECT", "SPELL_MOB",
            "ENTITY_EFFECT_AMBIENT", "SPELL_MOB_AMBIENT",
            "DUST_COLOR_TRANSITION",
            "TRAIL",
            "LEGACY_BLOCK_CRACK", "LEGACY_BLOCK_DUST"
    );

    private static final Map<String, Optional<Particle>> CACHE = new HashMap<>();

    private ParticleResolver() {
    }

    /** Resolves a particle name (or one of its aliases), or empty when unavailable. */
    public static synchronized Optional<Particle> resolve(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String key = name.trim().toUpperCase(Locale.ROOT);
        Optional<Particle> cached = CACHE.get(key);
        if (cached != null) return cached;

        Optional<Particle> result = Optional.empty();
        List<String> candidates = new ArrayList<>();
        candidates.add(key);
        List<String> aliases = ALIASES.get(key);
        if (aliases != null) candidates.addAll(aliases);

        for (String candidate : candidates) {
            try {
                result = Optional.of(Particle.valueOf(candidate));
                break;
            } catch (IllegalArgumentException ignored) {
                // Try the next alias.
            }
        }
        CACHE.put(key, result);
        return result;
    }

    /** @return true when the particle (or an alias) exists on this server version. */
    public static boolean isAvailable(String name) {
        return resolve(name).isPresent();
    }

    /** @return true when the particle exists and can be spawned without extra data. */
    public static boolean isSafe(String name) {
        Optional<Particle> particle = resolve(name);
        return particle.isPresent() && !DATA_REQUIRED.contains(particle.get().name());
    }

    /** Resolves a particle, or the provided fallback when unavailable/unsafe. */
    public static Particle resolveOr(String name, Particle fallback) {
        Optional<Particle> particle = resolve(name);
        if (particle.isEmpty()) return fallback;
        if (DATA_REQUIRED.contains(particle.get().name())) return fallback;
        return particle.get();
    }

    public static Set<String> dataRequiredNames() {
        return Collections.unmodifiableSet(DATA_REQUIRED);
    }

    private static Map<String, List<String>> buildAliases() {
        Map<String, List<String>> map = new HashMap<>();
        alias(map, "HAPPY_VILLAGER", "VILLAGER_HAPPY");
        alias(map, "ENCHANT", "ENCHANTMENT_TABLE");
        alias(map, "WITCH", "SPELL_WITCH");
        alias(map, "ENCHANTED_HIT", "CRIT_MAGIC");
        alias(map, "DUST", "REDSTONE");
        alias(map, "ENTITY_EFFECT", "SPELL_MOB");
        alias(map, "ENTITY_EFFECT_AMBIENT", "SPELL_MOB_AMBIENT");
        alias(map, "ITEM_SNOWBALL", "SNOW_SHOVEL");
        alias(map, "BLOCK", "BLOCK_CRACK", "BLOCK_DUST");
        alias(map, "ITEM", "ITEM_CRACK");
        return map;
    }

    private static void alias(Map<String, List<String>> map, String... names) {
        for (String name : names) {
            List<String> others = new ArrayList<>();
            for (String other : names) {
                if (!other.equals(name)) others.add(other);
            }
            map.put(name, others);
        }
    }
}
