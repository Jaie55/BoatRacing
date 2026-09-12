package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.util.IconResolver;
import es.jaie55.boatracing.util.ParticleResolver;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Selectable effects played when the player crosses a checkpoint. Particles are resolved with
 * version aliases and unavailable ones hide the cosmetic; sounds are stored as namespace keys.
 */
public enum CheckpointEffectType {

    NONE("none", null, null, 0, 0.0, "BARRIER", false),
    SPARK("spark", "CRIT", "entity.experience_orb.pickup", 8, 0.25, "FLINT", true),
    FLAME("flame", "FLAME", "block.fire.ambient", 8, 0.20, "BLAZE_POWDER", true),
    HEART("heart", "HEART", "entity.player.levelup", 4, 0.10, "PINK_DYE", true),
    HAPPY("happy", "VILLAGER_HAPPY", "entity.villager.yes", 8, 0.25, "DANDELION", true),
    SOUL("soul", "SOUL", "particle.soul_escape", 8, 0.20, "SOUL_TORCH", true),
    ENCHANT("enchant", "ENCHANTMENT_TABLE", "block.enchantment_table.use", 10, 0.30, "ENCHANTED_BOOK", true),
    NOTE("note", "NOTE", "block.note_block.pling", 5, 0.10, "NOTE_BLOCK", true),
    PORTAL("portal", "PORTAL", "block.portal.travel", 10, 0.30, "ENDER_PEARL", true),
    TOTEM("totem", "TOTEM_OF_UNDYING", "item.totem.use", 6, 0.20, "TOTEM_OF_UNDYING", true),
    SCULK("sculk", "SCULK_SOUL", "block.sculk_sensor.clicking", 6, 0.20, "SCULK", true),
    CHERRY("cherry", "CHERRY_LEAVES", "item.bone_meal.use", 8, 0.25, "PINK_DYE", true);

    private final String id;
    private final String particleName;
    private final String soundKey;
    private final int count;
    private final double spread;
    private final String iconName;
    private final boolean lockable;

    CheckpointEffectType(String id, String particleName, String soundKey, int count, double spread,
                         String iconName, boolean lockable) {
        this.id = id;
        this.particleName = particleName;
        this.soundKey = soundKey;
        this.count = count;
        this.spread = spread;
        this.iconName = iconName;
        this.lockable = lockable;
    }

    public String id() { return id; }
    public String particleName() { return particleName; }
    public String soundKey() { return soundKey; }
    public int count() { return count; }
    public double spread() { return spread; }
    public boolean lockable() { return lockable; }
    public boolean isNone() { return this == NONE; }

    public Material icon() {
        return IconResolver.resolve(iconName, Material.LIGHTNING_ROD);
    }

    /** @return true when this effect can be rendered on the current server version. */
    public boolean available() {
        return this == NONE || ParticleResolver.isSafe(particleName);
    }

    public String permission() { return "boatracing.cosmetics.checkpoint." + id; }
    public String messageKey() { return "cosmetics.checkpoint." + id; }

    public static CheckpointEffectType byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (CheckpointEffectType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
