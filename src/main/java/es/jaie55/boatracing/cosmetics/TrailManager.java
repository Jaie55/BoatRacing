package es.jaie55.boatracing.cosmetics;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.race.RaceManager;
import es.jaie55.boatracing.track.ParticleWireframe;
import es.jaie55.boatracing.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Renders the equipped particle trails. Players without permission for their saved trail
 * simply get no trail (permissions can be revoked at any time).
 *
 * Context is configurable via {@code cosmetics.trails.show-in}: always | race | practice | race-and-practice.
 */
public class TrailManager {

    private final BoatRacingPlugin plugin;
    private SchedulerCompat.TaskHandle task;

    public TrailManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cosmetics.trails.enabled", true);
    }

    public void startOrReload() {
        stop();
        if (!isEnabled()) {
            plugin.getLogger().fine("Trail rendering disabled by config.");
            return;
        }
        int period = Math.max(2, plugin.getConfig().getInt("cosmetics.trails.period-ticks", 2));
        task = SchedulerCompat.runTimer(plugin, this::tick, period, period);
        plugin.getLogger().fine("Trail rendering started (period=" + period + " ticks).");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!isEnabled()) return;
        if (!plugin.getConfig().getBoolean("cosmetics.enabled", true)) return;
        String context = plugin.getConfig().getString("cosmetics.trails.show-in", "race-and-practice");
        if (plugin.getPlayerPrefsManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String trailId = plugin.getPlayerPrefsManager().getTrail(player.getUniqueId());
            if (trailId == null) continue;

            es.jaie55.boatracing.cosmetics.CosmeticsCatalog catalog = plugin.getCosmeticsCatalog();
            es.jaie55.boatracing.cosmetics.CosmeticsCatalog.TrailDefinition trail =
                    catalog != null ? catalog.trailById(trailId) : null;
            if (trail == null) {
                plugin.getLogger().finer("Unknown, disabled or unsupported trail id '" + trailId + "' for " + player.getName() + "; ignoring.");
                continue;
            }
            if (!catalog.hasAccess(player, es.jaie55.boatracing.cosmetics.CosmeticCategory.TRAIL,
                    trail.id(), trail.permission())) continue;
            if (!trackCosmeticsAllowed(player)) continue;
            if (!contextAllows(context, player)) continue;

            Location loc = player.getVehicle() != null ? player.getVehicle().getLocation() : player.getLocation();
            Particle particle = ParticleWireframe.resolve(trail.particleName());
            int count = catalog.densityCount(player);
            player.getWorld().spawnParticle(
                    particle,
                    loc.getX(), loc.getY() + 0.35, loc.getZ(),
                    count, trail.spread(), trail.spread(), trail.spread(), trail.extra());
        }
    }

    private boolean contextAllows(String context, Player player) {
        if (context == null) return true;
        switch (context.toLowerCase()) {
            case "always" -> {
                return true;
            }
            case "race" -> {
                return isRacing(player);
            }
            case "practice" -> {
                return isPracticing(player);
            }
            case "race-and-practice" -> {
                return isRacing(player) || isPracticing(player);
            }
            default -> {
                return true;
            }
        }
    }

    private boolean isRacing(Player player) {
        for (RaceManager rm : plugin.getAllRaceManagers()) {
            if (rm.isRunning() && !rm.isPracticeMode() && rm.isParticipant(player.getUniqueId())) return true;
        }
        return false;
    }

    /** Honors the per-track {@code racing.cosmetics-enabled} override for active sessions. */
    private boolean trackCosmeticsAllowed(Player player) {
        for (RaceManager rm : plugin.getAllRaceManagers()) {
            if (rm == null || !rm.isParticipant(player.getUniqueId())) continue;
            if (!(rm.isRunning() || rm.isRegistering() || rm.isCountdownActive() || rm.isPracticeActive())) continue;
            if (!rm.getTrack().getRacingBoolean("cosmetics-enabled", true)) return false;
        }
        return true;
    }

    private boolean isPracticing(Player player) {
        for (RaceManager rm : plugin.getAllRaceManagers()) {
            if (rm.isPracticeMode() && rm.isPracticeActive() && rm.isParticipant(player.getUniqueId())) return true;
        }
        return false;
    }
}
