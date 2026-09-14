package es.jaie55.boatracing.track;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Small helper to render particle wireframes (lines and oriented gates) to a single player.
 * Extracted from SelectionVisualizer so setup previews can reuse the same approach.
 */
public final class ParticleWireframe {

    private ParticleWireframe() {
    }

    /** Resolves a particle by name (with version aliases), falling back to END_ROD. */
    public static Particle resolve(String name) {
        return es.jaie55.boatracing.util.ParticleResolver.resolveOr(name, Particle.END_ROD);
    }

    public static void spawnPoint(Player player, Particle particle, double x, double y, double z) {
        player.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Coloured point using the dust particle when available, END_ROD otherwise. */
    public static void spawnColoredPoint(Player player, double x, double y, double z, Color color) {
        Particle dust = es.jaie55.boatracing.util.ParticleResolver.resolveOr("DUST",
                es.jaie55.boatracing.util.ParticleResolver.resolveOr("REDSTONE", null));
        if (dust != null) {
            try {
                player.spawnParticle(dust, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, 1.2f));
                return;
            } catch (Throwable ignored) {
                // Fall through to the safe particle.
            }
        }
        player.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void drawLine(Player player, Particle particle, Vector a, Vector b, int samples) {
        int steps = Math.max(2, samples);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            spawnPoint(player, particle,
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t);
        }
    }

    /** Coloured line using the dust particle when available. */
    public static void drawColoredLine(Player player, Vector a, Vector b, int samples, Color color) {
        int steps = Math.max(2, samples);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            spawnColoredPoint(player,
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t,
                    color);
        }
    }

    /** Draws the four edges of an oriented rectangular gate. */
    public static void drawGate(Player player, Particle particle, PlaneCheckpoint gate) {
        if (player == null || gate == null) return;
        Vector center = gate.getCenter();
        Vector right = gate.getRight().multiply(gate.getHalfWidth());
        Vector up = gate.getUp().multiply(gate.getHalfHeight());

        Vector topLeft = center.clone().add(right).add(up);
        Vector topRight = center.clone().subtract(right).add(up);
        Vector bottomRight = center.clone().subtract(right).subtract(up);
        Vector bottomLeft = center.clone().add(right).subtract(up);

        int samples = samplesForSize(gate.getHalfWidth(), gate.getHalfHeight());
        drawLine(player, particle, topLeft, topRight, samples);
        drawLine(player, particle, topRight, bottomRight, samples);
        drawLine(player, particle, bottomRight, bottomLeft, samples);
        drawLine(player, particle, bottomLeft, topLeft, samples);
    }

    /** Draws an oriented gate in a specific colour (checkpoints are blue, finish yellow). */
    public static void drawGateColored(Player player, PlaneCheckpoint gate, Color color) {
        if (player == null || gate == null || color == null) return;
        Vector center = gate.getCenter();
        Vector right = gate.getRight().multiply(gate.getHalfWidth());
        Vector up = gate.getUp().multiply(gate.getHalfHeight());

        Vector topLeft = center.clone().add(right).add(up);
        Vector topRight = center.clone().subtract(right).add(up);
        Vector bottomRight = center.clone().subtract(right).subtract(up);
        Vector bottomLeft = center.clone().add(right).subtract(up);

        int samples = samplesForSize(gate.getHalfWidth(), gate.getHalfHeight());
        drawColoredLine(player, topLeft, topRight, samples, color);
        drawColoredLine(player, topRight, bottomRight, samples, color);
        drawColoredLine(player, bottomRight, bottomLeft, samples, color);
        drawColoredLine(player, bottomLeft, topLeft, samples, color);
    }

    /** Draws the 12 edges of an axis-aligned box in a specific colour (used for the finish gate). */
    public static void drawBoxColored(Player player, BoundingBox box, Color color, int samplesPerEdge) {
        if (player == null || box == null || color == null) return;
        int samples = Math.max(2, samplesPerEdge);
        Vector min = box.getMin();
        Vector max = box.getMax();
        Vector[] corners = {
                new Vector(min.getX(), min.getY(), min.getZ()),
                new Vector(max.getX(), min.getY(), min.getZ()),
                new Vector(max.getX(), min.getY(), max.getZ()),
                new Vector(min.getX(), min.getY(), max.getZ()),
                new Vector(min.getX(), max.getY(), min.getZ()),
                new Vector(max.getX(), max.getY(), min.getZ()),
                new Vector(max.getX(), max.getY(), max.getZ()),
                new Vector(min.getX(), max.getY(), max.getZ())
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            drawColoredLine(player, corners[edge[0]], corners[edge[1]], samples, color);
        }
    }

    private static int samplesForSize(double halfWidth, double halfHeight) {
        double span = Math.max(1.0, halfWidth * 2.0 + halfHeight * 2.0);
        return (int) Math.max(2, Math.min(16, Math.round(span / 2.5)));
    }

    /** @return the coloured dust particle for this server version (DUST or REDSTONE), or null. */
    public static Particle dustParticle() {
        Particle dust = es.jaie55.boatracing.util.ParticleResolver.resolveOr("DUST", null);
        if (dust == null) dust = es.jaie55.boatracing.util.ParticleResolver.resolveOr("REDSTONE", null);
        return dust;
    }

    /** @return true when the particle is the coloured dust particle (DUST/REDSTONE). */
    public static boolean isDust(Particle particle) {
        if (particle == null) return false;
        String name = particle.name();
        return name.contains("DUST") || name.contains("REDSTONE");
    }

    /**
     * Spawns a single point with the given particle. Dust particles use the provided colour,
     * any other particle is spawned as-is (so gate types can look clearly different).
     */
    public static void spawnEffectPoint(Player player, double x, double y, double z, Particle particle, Color color) {
        if (player == null || particle == null) return;
        if (isDust(particle) && color != null) {
            try {
                player.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, 1.2f));
                return;
            } catch (Throwable ignored) {
                // Fall through to the plain particle.
            }
        }
        player.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void drawEffectLine(Player player, Vector a, Vector b, int samples, Particle particle, Color color) {
        int steps = Math.max(2, samples);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            spawnEffectPoint(player,
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t,
                    particle, color);
        }
    }

    /** Draws an oriented gate using a specific particle (dust particles take the given colour). */
    public static void drawGateEffect(Player player, PlaneCheckpoint gate, Particle particle, Color color) {
        if (player == null || gate == null || particle == null) return;
        Vector center = gate.getCenter();
        Vector right = gate.getRight().multiply(gate.getHalfWidth());
        Vector up = gate.getUp().multiply(gate.getHalfHeight());

        Vector topLeft = center.clone().add(right).add(up);
        Vector topRight = center.clone().subtract(right).add(up);
        Vector bottomRight = center.clone().subtract(right).subtract(up);
        Vector bottomLeft = center.clone().add(right).subtract(up);

        int samples = samplesForSize(gate.getHalfWidth(), gate.getHalfHeight());
        drawEffectLine(player, topLeft, topRight, samples, particle, color);
        drawEffectLine(player, topRight, bottomRight, samples, particle, color);
        drawEffectLine(player, bottomRight, bottomLeft, samples, particle, color);
        drawEffectLine(player, bottomLeft, topLeft, samples, particle, color);
    }

    /** Draws the 12 edges of an axis-aligned box using a specific particle. */
    public static void drawBoxEffect(Player player, BoundingBox box, Particle particle, Color color, int samplesPerEdge) {
        if (player == null || box == null || particle == null) return;
        int samples = Math.max(2, samplesPerEdge);
        Vector min = box.getMin();
        Vector max = box.getMax();
        Vector[] corners = {
                new Vector(min.getX(), min.getY(), min.getZ()),
                new Vector(max.getX(), min.getY(), min.getZ()),
                new Vector(max.getX(), min.getY(), max.getZ()),
                new Vector(min.getX(), min.getY(), max.getZ()),
                new Vector(min.getX(), max.getY(), min.getZ()),
                new Vector(max.getX(), max.getY(), min.getZ()),
                new Vector(max.getX(), max.getY(), max.getZ()),
                new Vector(min.getX(), max.getY(), max.getZ())
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            drawEffectLine(player, corners[edge[0]], corners[edge[1]], samples, particle, color);
        }
    }
}
