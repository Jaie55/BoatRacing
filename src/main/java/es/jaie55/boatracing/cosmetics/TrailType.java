package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.util.IconResolver;
import es.jaie55.boatracing.util.ParticleResolver;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Built-in particle trails. Particle names are resolved at runtime with version aliases and a
 * safe fallback, and icons are stored as material names so a material removed in a future
 * version never breaks the plugin.
 */
public enum TrailType {

    SMOKE("smoke", "SMOKE_NORMAL", "GUNPOWDER", 2, 0.03, 0.01),
    FLAME("flame", "FLAME", "BLAZE_POWDER", 2, 0.02, 0.01),
    SOUL("soul", "SOUL_FIRE_FLAME", "SOUL_TORCH", 2, 0.02, 0.01),
    CLOUD("cloud", "CLOUD", "FEATHER", 2, 0.08, 0.01),
    SPARK("spark", "CRIT", "FLINT", 2, 0.05, 0.05),
    HEART("heart", "HEART", "RED_DYE", 1, 0.02, 0.0),
    HAPPY("happy", "VILLAGER_HAPPY", "DANDELION", 2, 0.10, 0.0),
    WITCH("witch", "SPELL_WITCH", "GLASS_BOTTLE", 2, 0.10, 0.0),
    END_ROD("end_rod", "END_ROD", "END_ROD", 1, 0.0, 0.01),
    TOTEM("totem", "TOTEM_OF_UNDYING", "TOTEM_OF_UNDYING", 1, 0.0, 0.02),
    DRIP("drip", "DRIPPING_WATER", "WATER_BUCKET", 2, 0.06, 0.0),
    ENCHANT("enchant", "ENCHANTMENT_TABLE", "ENCHANTED_BOOK", 3, 0.2, 0.0),
    BUBBLE("bubble", "BUBBLE_POP", "PRISMARINE_SHARD", 3, 0.08, 0.01),
    SNOW("snow", "SNOWFLAKE", "SNOWBALL", 2, 0.10, 0.0),
    LAVA("lava", "LAVA", "LAVA_BUCKET", 1, 0.02, 0.0),
    NOTE("note", "NOTE", "NOTE_BLOCK", 1, 0.0, 0.0),
    PORTAL("portal", "PORTAL", "ENDER_PEARL", 3, 0.15, 0.0),
    ENCHANTED("enchanted", "ENCHANTED_HIT", "PURPLE_DYE", 2, 0.05, 0.0),
    DAMAGE("damage", "DAMAGE_INDICATOR", "IRON_SWORD", 1, 0.02, 0.0),
    SPORE("spore", "SPORE_BLOSSOM_AIR", "SPORE_BLOSSOM", 2, 0.10, 0.0),
    SCULK("sculk", "SCULK_SOUL", "SCULK", 1, 0.05, 0.0),
    CHERRY("cherry", "CHERRY_LEAVES", "PINK_DYE", 3, 0.15, 0.0),
    DRIP_LAVA("drip_lava", "DRIPPING_LAVA", "POINTED_DRIPSTONE", 2, 0.05, 0.0),
    INK("ink", "SQUID_INK", "INK_SAC", 2, 0.08, 0.0);

    public final String id;
    public final String particleName;
    public final String iconName;
    public final int count;
    public final double spread;
    public final double extra;

    TrailType(String id, String particleName, String iconName, int count, double spread, double extra) {
        this.id = id;
        this.particleName = particleName;
        this.iconName = iconName;
        this.count = count;
        this.spread = spread;
        this.extra = extra;
    }

    public Material icon() {
        return IconResolver.resolve(iconName, Material.GUNPOWDER);
    }

    /** @return true when this trail can be rendered on the current server version. */
    public boolean available() {
        return ParticleResolver.isSafe(particleName);
    }

    public String permission() {
        return "boatracing.cosmetics.trail." + id;
    }

    public String messageKey() {
        return "cosmetics.trail." + id;
    }

    public static TrailType byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (TrailType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
