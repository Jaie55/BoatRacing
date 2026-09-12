package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.util.IconResolver;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Selectable victory effects. "default" and "none" are always available; the rest can be
 * locked for monetization by listing their ids under {@code cosmetics.effects.locked}.
 * The default sound is stored as a namespace key so it stays valid across versions.
 */
public enum VictoryEffectType {

    DEFAULT("default", FireworkEffect.Type.BALL_LARGE, Color.fromRGB(255, 215, 0), Color.fromRGB(255, 69, 0),
            "entity.player.levelup", 3, "FIREWORK_ROCKET", false),
    NONE("none", null, null, null, null, 0, "BARRIER", false),
    GOLD("gold", FireworkEffect.Type.BALL_LARGE, Color.fromRGB(255, 215, 0), Color.fromRGB(255, 140, 0),
            "entity.player.levelup", 3, "GOLD_INGOT", true),
    SILVER("silver", FireworkEffect.Type.BALL, Color.fromRGB(220, 220, 220), Color.fromRGB(160, 160, 160),
            "block.note_block.chime", 2, "IRON_INGOT", true),
    BRONZE("bronze", FireworkEffect.Type.BALL, Color.fromRGB(205, 127, 50), Color.fromRGB(139, 69, 19),
            "block.note_block.bass", 2, "COPPER_INGOT", true),
    RAINBOW("rainbow", FireworkEffect.Type.STAR, Color.fromRGB(255, 0, 0), Color.fromRGB(0, 255, 255),
            "entity.firework_rocket.blast", 3, "FIREWORK_STAR", true),
    HEART("heart", FireworkEffect.Type.BALL_LARGE, Color.fromRGB(255, 105, 180), Color.fromRGB(255, 0, 80),
            "entity.player.levelup", 2, "PINK_DYE", true),
    SOUL("soul", FireworkEffect.Type.BALL, Color.fromRGB(0, 255, 255), Color.fromRGB(0, 100, 120),
            "particle.soul_escape", 2, "SOUL_TORCH", true),
    PARTY("party", FireworkEffect.Type.BURST, Color.fromRGB(255, 170, 0), Color.fromRGB(120, 0, 255),
            "entity.firework_rocket.launch", 3, "CAKE", true);

    private final String id;
    private final FireworkEffect.Type fireworkType;
    private final Color primary;
    private final Color fade;
    private final String soundKey;
    private final int rockets;
    private final String iconName;
    private final boolean lockable;

    VictoryEffectType(String id, FireworkEffect.Type fireworkType, Color primary, Color fade,
                      String soundKey, int rockets, String iconName, boolean lockable) {
        this.id = id;
        this.fireworkType = fireworkType;
        this.primary = primary;
        this.fade = fade;
        this.soundKey = soundKey;
        this.rockets = rockets;
        this.iconName = iconName;
        this.lockable = lockable;
    }

    public String id() { return id; }
    public FireworkEffect.Type fireworkType() { return fireworkType; }
    public Color primary() { return primary; }
    public Color fade() { return fade; }
    public String soundKey() { return soundKey; }
    public int rockets() { return rockets; }
    public boolean lockable() { return lockable; }
    public boolean isNone() { return this == NONE; }
    public boolean isRainbow() { return this == RAINBOW; }

    public Material icon() {
        return IconResolver.resolve(iconName, Material.FIREWORK_ROCKET);
    }

    public String permission() { return "boatracing.cosmetics.effect." + id; }
    public String messageKey() { return "cosmetics.effect." + id; }

    public static VictoryEffectType byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (VictoryEffectType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
