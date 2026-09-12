package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.ParticleWireframe;
import es.jaie55.boatracing.track.TrackConfig;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Plays the player's selected checkpoint effect (particles + sound) when they cross a gate.
 * Respects the master cosmetics switch, the per-track override and locked/monetized effects.
 */
public final class CosmeticFx {

    private CosmeticFx() {
    }

    public static void playCheckpoint(Player player, TrackConfig track) {
        if (player == null || !player.isOnline()) return;
        BoatRacingPlugin plugin = BoatRacingPlugin.getInstance();
        if (plugin == null) return;
        if (!plugin.getConfig().getBoolean("cosmetics.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("cosmetics.checkpoints.enabled", true)) return;
        if (track != null && !track.getRacingBoolean("cosmetics-enabled", true)) return;
        if (plugin.getPlayerPrefsManager() == null) return;

        String id = plugin.getPlayerPrefsManager().getCheckpointEffect(player.getUniqueId());
        CheckpointEffectType effect = id != null ? CheckpointEffectType.byId(id) : CheckpointEffectType.NONE;
        if (effect == null || effect.isNone()) return;
        if (plugin.getCosmeticsCatalog() != null
                && !plugin.getCosmeticsCatalog().isAvailable(CosmeticCategory.CHECKPOINT, effect.id())) return;

        boolean allowed = plugin.getCosmeticsCatalog() != null
                && plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.CHECKPOINT, effect.id(), effect.permission());
        if (!allowed) return;

        Particle particle = ParticleWireframe.resolve(effect.particleName());
        int count = plugin.getCosmeticsCatalog() != null
                ? plugin.getCosmeticsCatalog().densityCount(player)
                : Math.max(1, effect.count());
        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1.0, 0),
                count,
                effect.spread(), effect.spread(), effect.spread(),
                0.0);
        if (effect.soundKey() != null) {
            player.playSound(player.getLocation(), effect.soundKey(), 0.8f, 1.2f);
        }
        plugin.getLogger().finer("Checkpoint effect " + effect.id() + " played for " + player.getName() + ".");
    }
}
