package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.util.IconResolver;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Selectable victory sounds. Sounds are stored as namespace keys (e.g. "entity.player.levelup")
 * and played with the string overload, so they stay valid across Minecraft versions.
 * "default" keeps the sound defined by the chosen victory effect and "none" mutes it.
 */
public enum VictorySoundType {

    DEFAULT("default", null, 1.0f, "BELL", false, false),
    NONE("none", null, 1.0f, "BARRIER", false, true),
    LEVEL_UP("level_up", "entity.player.levelup", 1.2f, "EXPERIENCE_BOTTLE", true, true),
    CHIME("chime", "block.note_block.chime", 1.2f, "NOTE_BLOCK", true, true),
    BELL("bell", "block.bell.use", 1.0f, "BELL", true, true),
    PLING("pling", "block.note_block.pling", 1.5f, "GLOWSTONE_DUST", true, true),
    FIREWORK("firework", "entity.firework_rocket.blast", 1.0f, "FIREWORK_ROCKET", true, true),
    DRAGON("dragon", "entity.ender_dragon.growl", 1.0f, "DRAGON_EGG", true, true),
    THUNDER("thunder", "entity.lightning_bolt.thunder", 0.8f, "LIGHTNING_ROD", true, true),
    WITHER("wither", "entity.wither.spawn", 0.8f, "WITHER_SKELETON_SKULL", true, true),
    TOTEM("totem", "item.totem.use", 1.0f, "TOTEM_OF_UNDYING", true, true),
    BEACON("beacon", "block.beacon.activate", 1.1f, "BEACON", true, true),
    PORTAL("portal", "block.portal.travel", 1.0f, "ENDER_PEARL", true, true),
    ANVIL("anvil", "block.anvil.land", 1.0f, "ANVIL", true, true),
    VICTORY("victory", "ui.toast.challenge_complete", 1.2f, "NETHER_STAR", true, true);

    private final String id;
    private final String soundKey;
    private final float pitch;
    private final String iconName;
    private final boolean lockable;
    private final boolean silent;

    VictorySoundType(String id, String soundKey, float pitch, String iconName, boolean lockable, boolean silent) {
        this.id = id;
        this.soundKey = soundKey;
        this.pitch = pitch;
        this.iconName = iconName;
        this.lockable = lockable;
        this.silent = silent;
    }

    public String id() { return id; }
    public String soundKey() { return soundKey; }
    public float pitch() { return pitch; }
    public boolean lockable() { return lockable; }
    public boolean isNone() { return silent; }
    public boolean isDefault() { return this == DEFAULT; }

    public Material icon() {
        return IconResolver.resolve(iconName, Material.BELL);
    }

    public String permission() { return "boatracing.cosmetics.sound." + id; }
    public String messageKey() { return "cosmetics.sound." + id; }

    public static VictorySoundType byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (VictorySoundType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
