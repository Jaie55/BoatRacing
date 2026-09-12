package es.jaie55.boatracing.track;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Arrays;

/**
 * Renders a lightweight particle wireframe for the current wand selection.
 */
public final class SelectionVisualizer {
    private final BoatRacingPlugin plugin;
    private SchedulerCompat.TaskHandle task;

    public SelectionVisualizer(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public void startOrReload() {
        stop();

        if (!plugin.getConfig().getBoolean("setup.selection-visualizer.enabled", true)) return;

        long periodTicks = Math.max(1L, plugin.getConfig().getLong("setup.selection-visualizer.period-ticks", 10L));
        task = SchedulerCompat.runTimer(plugin, this::renderTick, periodTicks, periodTicks);
    }

    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
            task = null;
        }
    }

    private void renderTick() {
        if (!plugin.getConfig().getBoolean("setup.selection-visualizer.enabled", true)) return;

        final boolean requireWand = plugin.getConfig().getBoolean("setup.selection-visualizer.show-only-with-wand", true);
        final double spacing = Math.max(0.4D, plugin.getConfig().getDouble("setup.selection-visualizer.spacing", 1.0D));
        final double viewDistance = Math.max(0.0D, plugin.getConfig().getDouble("setup.selection-visualizer.view-distance", 96.0D));
        final double viewDistanceSq = viewDistance * viewDistance;
        final int maxParticlesPerPlayer = Math.max(20, plugin.getConfig().getInt("setup.selection-visualizer.max-particles-per-player", 250));
        final Particle particle = resolveParticle(plugin.getConfig().getString("setup.selection-visualizer.particle", "END_ROD"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (requireWand && !isHoldingWand(player)) continue;

            SelectionManager.Sel selection = SelectionManager.get(player);
            if (selection == null || selection.world == null) continue;
            if (!player.getWorld().getName().equalsIgnoreCase(selection.world)) continue;

            final double px = player.getLocation().getX();
            final double py = player.getLocation().getY();
            final double pz = player.getLocation().getZ();
            int emitted = 0;

            boolean hasPos1 = !(selection.x1 == 0 && selection.y1 == 0 && selection.z1 == 0);
            boolean hasPos2 = !(selection.x2 == 0 && selection.y2 == 0 && selection.z2 == 0);

            if (hasPos1 && emitted < maxParticlesPerPlayer) {
                emitted += spawnPoint(player, particle,
                        selection.x1 + 0.5D,
                        selection.y1 + 0.5D,
                        selection.z1 + 0.5D);
            }
            if (hasPos2 && emitted < maxParticlesPerPlayer) {
                emitted += spawnPoint(player, particle,
                        selection.x2 + 0.5D,
                        selection.y2 + 0.5D,
                        selection.z2 + 0.5D);
            }

            BoundingBox box = SelectionManager.toBox(selection);
            if (box == null || emitted >= maxParticlesPerPlayer) continue;

            // Cull by distance to the whole selection box instead of individual points.
            // This avoids half-cut wireframes when only part of the box is past the radius.
            if (viewDistance > 0.0D && distanceSquaredToBox(px, py, pz, box) > viewDistanceSq) continue;

            drawWireframe(player, particle, box, spacing, maxParticlesPerPlayer - emitted);
        }
    }

    private static Particle resolveParticle(String raw) {
        return es.jaie55.boatracing.util.ParticleResolver.resolveOr(raw, Particle.END_ROD);
    }

    private static boolean isHoldingWand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return SelectionManager.isWand(main) || SelectionManager.isWand(off);
    }

    private static int spawnPoint(
            Player player,
            Particle particle,
            double x,
            double y,
            double z
    ) {
        player.spawnParticle(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        return 1;
    }

    private static void drawWireframe(
            Player player,
            Particle particle,
            BoundingBox box,
            double spacing,
            int budget
    ) {
        if (budget <= 0) return;

        Vector p000 = new Vector(box.getMinX(), box.getMinY(), box.getMinZ());
        Vector p100 = new Vector(box.getMaxX(), box.getMinY(), box.getMinZ());
        Vector p010 = new Vector(box.getMinX(), box.getMaxY(), box.getMinZ());
        Vector p110 = new Vector(box.getMaxX(), box.getMaxY(), box.getMinZ());
        Vector p001 = new Vector(box.getMinX(), box.getMinY(), box.getMaxZ());
        Vector p101 = new Vector(box.getMaxX(), box.getMinY(), box.getMaxZ());
        Vector p011 = new Vector(box.getMinX(), box.getMaxY(), box.getMaxZ());
        Vector p111 = new Vector(box.getMaxX(), box.getMaxY(), box.getMaxZ());

        Vector[][] edges = new Vector[][] {
                {p000, p100}, {p100, p110}, {p110, p010}, {p010, p000},
                {p001, p101}, {p101, p111}, {p111, p011}, {p011, p001},
                {p000, p001}, {p100, p101}, {p110, p111}, {p010, p011}
        };

        int[] desiredSamples = new int[edges.length];
        int totalDesired = 0;
        for (int i = 0; i < edges.length; i++) {
            double length = edges[i][0].distance(edges[i][1]);
            int samples = Math.max(2, (int) Math.ceil(length / spacing) + 1);
            desiredSamples[i] = samples;
            totalDesired += samples;
        }

        int[] allocated = allocateSamplesPerEdge(desiredSamples, budget);
        for (int i = 0; i < edges.length; i++) {
            drawLine(player, particle, edges[i][0], edges[i][1], allocated[i]);
        }
    }

    private static void drawLine(
            Player player,
            Particle particle,
            Vector a,
            Vector b,
            int samples
    ) {
        if (samples <= 0) return;

        if (samples == 1) {
            player.spawnParticle(particle, a.getX(), a.getY(), a.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            return;
        }

        for (int i = 0; i < samples; i++) {
            double t = (double) i / (double) (samples - 1);
            double x = lerp(a.getX(), b.getX(), t);
            double y = lerp(a.getY(), b.getY(), t);
            double z = lerp(a.getZ(), b.getZ(), t);
            player.spawnParticle(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static int[] allocateSamplesPerEdge(int[] desired, int budget) {
        int edgeCount = desired.length;
        if (edgeCount == 0 || budget <= 0) return new int[0];

        int[] out = new int[edgeCount];

        // Base one sample per edge so every edge remains represented.
        Arrays.fill(out, 1);
        int used = edgeCount;
        if (used >= budget) return out;

        int[] need = new int[edgeCount];
        int totalNeed = 0;
        for (int i = 0; i < edgeCount; i++) {
            need[i] = Math.max(0, desired[i] - 1);
            totalNeed += need[i];
        }
        if (totalNeed <= 0) return out;

        int remaining = budget - used;
        double[] remainder = new double[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            if (need[i] == 0) continue;
            double exact = ((double) remaining * (double) need[i]) / (double) totalNeed;
            int add = Math.min(need[i], (int) Math.floor(exact));
            out[i] += add;
            remainder[i] = exact - add;
            used += add;
        }

        while (used < budget) {
            int best = -1;
            double bestScore = -1.0D;
            for (int i = 0; i < edgeCount; i++) {
                if (out[i] >= desired[i]) continue;
                double score = remainder[i];
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            if (best < 0) break;
            out[best]++;
            remainder[best] = 0.0D;
            used++;
        }

        return out;
    }

    private static double distanceSquaredToBox(double x, double y, double z, BoundingBox box) {
        double dx = 0.0D;
        if (x < box.getMinX()) dx = box.getMinX() - x;
        else if (x > box.getMaxX()) dx = x - box.getMaxX();

        double dy = 0.0D;
        if (y < box.getMinY()) dy = box.getMinY() - y;
        else if (y > box.getMaxY()) dy = y - box.getMaxY();

        double dz = 0.0D;
        if (z < box.getMinZ()) dz = box.getMinZ() - z;
        else if (z > box.getMaxZ()) dz = z - box.getMaxZ();

        return dx * dx + dy * dy + dz * dz;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
