package es.jaie55.boatracing.track;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
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

    private static int samplesForSize(double halfWidth, double halfHeight) {
        double span = Math.max(1.0, halfWidth * 2.0 + halfHeight * 2.0);
        return (int) Math.max(2, Math.min(16, Math.round(span / 2.5)));
    }
}
