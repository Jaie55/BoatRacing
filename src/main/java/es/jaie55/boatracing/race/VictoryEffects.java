package es.jaie55.boatracing.race;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.cosmetics.CosmeticCategory;
import es.jaie55.boatracing.cosmetics.VictoryEffectType;
import es.jaie55.boatracing.cosmetics.VictorySoundType;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Random;

/**
 * Screen title, fireworks and sounds for the first finishers of a race.
 * Purely cosmetic and fully controlled by {@code racing.victory-effects.*} and the
 * player's selected victory effect in the cosmetics menu.
 */
public final class VictoryEffects {

    private static final Random RANDOM = new Random();
    private static final Color[] RAINBOW = new Color[]{
            Color.fromRGB(255, 0, 0), Color.fromRGB(255, 165, 0), Color.fromRGB(255, 255, 0),
            Color.fromRGB(0, 255, 0), Color.fromRGB(0, 150, 255), Color.fromRGB(160, 0, 255)
    };

    private VictoryEffects() {
    }

    public static void play(BoatRacingPlugin plugin, Player player, int position, String trackName) {
        play(plugin, player, position, trackName, null);
    }

    public static void play(BoatRacingPlugin plugin, Player player, int position, String trackName, TrackConfig track) {
        if (plugin == null || player == null || !player.isOnline()) return;
        if (!plugin.getConfig().getBoolean("racing.victory-effects.enabled", true)) return;
        int topN = Math.max(1, plugin.getConfig().getInt("racing.victory-effects.top-n", 3));
        if (position < 1 || position > topN) return;

        VictoryEffectType effect = resolveEffect(plugin, player, track);
        if (effect.isNone()) return;

        if (plugin.getConfig().getBoolean("racing.victory-effects.screen-title", true)) {
            String titleKey = position == 1 ? "race.victory.title-first" : "race.victory.title-top";
            String title = plugin.msg().get(titleKey,
                    "pos", String.valueOf(position),
                    "track", trackName == null ? "" : trackName);
            String subtitle = plugin.msg().get("race.victory.subtitle", "track", trackName == null ? "" : trackName);
            player.showTitle(Title.title(
                    Text.c(title),
                    Text.c(subtitle),
                    Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
        }

        if (plugin.getConfig().getBoolean("racing.victory-effects.sounds", true)) {
            playVictorySound(plugin, player, effect, track);
        }

        if (plugin.getConfig().getBoolean("racing.victory-effects.fireworks", true)) {
            spawnFireworks(plugin, player, effect);
        }
        plugin.getLogger().finer("Victory effects played for " + player.getName()
                + " (position " + position + ", effect " + effect.id() + ").");
    }

    /**
     * Plays the player's selected victory sound. "default" uses the sound defined by the
     * chosen victory effect, "none" mutes it, and the rest can be locked for monetization.
     */
    private static void playVictorySound(BoatRacingPlugin plugin, Player player,
                                         VictoryEffectType effect, TrackConfig track) {
        if (!plugin.getConfig().getBoolean("cosmetics.enabled", true)
                || !plugin.getConfig().getBoolean("cosmetics.victory-sounds.enabled", true)
                || (track != null && !track.getRacingBoolean("cosmetics-enabled", true))) {
            if (effect.soundKey() != null) player.playSound(player.getLocation(), effect.soundKey(), 0.9f, 1.2f);
            return;
        }
        String id = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictorySound(player.getUniqueId())
                : null;
        VictorySoundType type = id != null ? VictorySoundType.byId(id) : VictorySoundType.DEFAULT;
        if (type == null) type = VictorySoundType.DEFAULT;
        if (type.lockable()
                && plugin.getCosmeticsCatalog() != null
                && !plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.SOUND, type.id(), type.permission())) {
            type = VictorySoundType.DEFAULT;
        }
        if (type.isNone()) return;
        String toPlay = type.isDefault() ? effect.soundKey() : type.soundKey();
        if (toPlay != null) {
            player.playSound(player.getLocation(), toPlay, 0.9f, type.pitch());
        }
    }

    private static VictoryEffectType resolveEffect(BoatRacingPlugin plugin, Player player, TrackConfig track) {
        if (!plugin.getConfig().getBoolean("cosmetics.enabled", true)) return VictoryEffectType.DEFAULT;
        if (!plugin.getConfig().getBoolean("cosmetics.effects.enabled", true)) return VictoryEffectType.DEFAULT;
        if (track != null && !track.getRacingBoolean("cosmetics-enabled", true)) return VictoryEffectType.DEFAULT;
        String id = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictoryEffect(player.getUniqueId())
                : null;
        VictoryEffectType type = id != null ? VictoryEffectType.byId(id) : VictoryEffectType.DEFAULT;
        if (type == null) type = VictoryEffectType.DEFAULT;
        if (type.lockable()
                && plugin.getCosmeticsCatalog() != null
                && !plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.EFFECT, type.id(), type.permission())) {
            type = VictoryEffectType.DEFAULT;
        }
        return type;
    }

    private static void spawnFireworks(BoatRacingPlugin plugin, Player player, VictoryEffectType effect) {
        if (effect.isNone() || effect.fireworkType() == null) return;
        int density = plugin.getCosmeticsCatalog() != null
                ? plugin.getCosmeticsCatalog().densityCount(player)
                : Math.max(1, effect.rockets());
        int cap = plugin.getCosmeticsCatalog() != null
                ? plugin.getCosmeticsCatalog().maxVictoryRockets()
                : 3;
        int rockets = Math.max(1, Math.min(density, cap));
        for (int i = 0; i < rockets; i++) {
            final Color primary = effect.isRainbow() ? RAINBOW[RANDOM.nextInt(RAINBOW.length)] : effect.primary();
            final Color fade = effect.isRainbow() ? RAINBOW[RANDOM.nextInt(RAINBOW.length)] : effect.fade();
            player.getWorld().spawn(player.getLocation().add(0, 1.0 + i * 0.2, 0), Firework.class, firework -> {
                org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
                FireworkEffect.Builder builder = FireworkEffect.builder()
                        .with(effect.fireworkType())
                        .withColor(primary)
                        .withFade(fade)
                        .withTrail();
                if (effect.isRainbow()) builder.withFlicker();
                meta.addEffect(builder.build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            });
        }
    }
}

