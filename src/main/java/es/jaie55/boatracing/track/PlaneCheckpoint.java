package es.jaie55.boatracing.track;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Oriented rectangular gate (F1 style). Crossing is detected by the sign change of the
 * signed distance to the gate plane, and the hit point must fall inside the rectangle
 * defined by {@code halfWidth}/{@code halfHeight} on the {@code right}/{@code up} axes.
 *
 * This allows gates on diagonal or curved track sections where axis-aligned boxes would
 * give false positives or require oversized regions.
 */
public class PlaneCheckpoint implements CheckpointShape {

    /** Half thickness used only by {@link #contains(Location)}. */
    private static final double PLANE_THICKNESS = 0.45;

    private final String worldName;
    private final Vector center;
    private final Vector normal;
    private final Vector right;
    private final Vector up;
    private final double halfWidth;
    private final double halfHeight;

    public PlaneCheckpoint(String worldName, Vector center, Vector normal, Vector right, Vector up,
                           double halfWidth, double halfHeight) {
        this.worldName = worldName == null ? "world" : worldName;
        this.center = center == null ? new Vector() : center.clone();

        Vector n = normal == null ? new Vector(0, 0, 1) : normal.clone();
        if (n.lengthSquared() < 1e-8) n = new Vector(0, 0, 1);
        n.normalize();

        Vector r = right == null ? new Vector() : right.clone();
        Vector u = up == null ? new Vector() : up.clone();

        // Orthonormalize right against normal.
        r.subtract(n.clone().multiply(r.dot(n)));
        if (r.lengthSquared() < 1e-8) {
            r = new Vector(-n.getZ(), 0, n.getX());
            if (r.lengthSquared() < 1e-8) r = new Vector(1, 0, 0);
        }
        r.normalize();

        // Orthonormalize up against normal and right.
        u.subtract(n.clone().multiply(u.dot(n)));
        u.subtract(r.clone().multiply(u.dot(r)));
        if (u.lengthSquared() < 1e-8) {
            u = n.clone().crossProduct(r);
            if (u.lengthSquared() < 1e-8) u = new Vector(0, 1, 0);
        }
        u.normalize();

        this.normal = n;
        this.right = r;
        this.up = u;
        this.halfWidth = Math.max(0.1, Math.abs(halfWidth));
        this.halfHeight = Math.max(0.1, Math.abs(halfHeight));
    }

    /**
     * Builds a vertical gate facing the given horizontal direction (uses yaw-like heading).
     * Direction must not be zero on the XZ plane.
     */
    public static PlaneCheckpoint facing(String worldName, Vector center, Vector direction,
                                         double halfWidth, double halfHeight) {
        Vector dir = direction == null ? new Vector(0, 0, 1) : direction.clone();
        dir.setY(0);
        if (dir.lengthSquared() < 1e-8) dir = new Vector(0, 0, 1);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        Vector up = new Vector(0, 1, 0);
        return new PlaneCheckpoint(worldName, center, dir, right, up, halfWidth, halfHeight);
    }

    @Override
    public String worldName() { return worldName; }

    public Vector getCenter() { return center.clone(); }
    public Vector getNormal() { return normal.clone(); }
    public Vector getRight() { return right.clone(); }
    public Vector getUp() { return up.clone(); }
    public double getHalfWidth() { return halfWidth; }
    public double getHalfHeight() { return halfHeight; }

    @Override
    public boolean crossed(Location from, Location to) {
        if (to == null) return false;
        if (!to.getWorld().getName().equals(worldName)) return false;
        if (from == null || !from.getWorld().getName().equals(worldName)) return contains(to);

        double d0 = from.toVector().subtract(center).dot(normal);
        double d1 = to.toVector().subtract(center).dot(normal);
        if (Math.abs(d0 - d1) < 1e-9) return false; // movement parallel to the gate plane

        double t = d0 / (d0 - d1);
        if (t < 0.0 || t > 1.0) return false;

        Vector hit = from.toVector().add(to.toVector().subtract(from.toVector()).multiply(t));
        Vector local = hit.subtract(center);
        return Math.abs(local.dot(right)) <= halfWidth + 1e-6
                && Math.abs(local.dot(up)) <= halfHeight + 1e-6;
    }

    @Override
    public boolean contains(Location loc) {
        if (loc == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        Vector local = loc.toVector().subtract(center);
        if (Math.abs(local.dot(normal)) > PLANE_THICKNESS) return false;
        return Math.abs(local.dot(right)) <= halfWidth + 1e-6
                && Math.abs(local.dot(up)) <= halfHeight + 1e-6;
    }

    @Override
    public String type() { return "plane"; }

    @Override
    public String describe() {
        return String.format(Locale.ROOT, "plane %.1fx%.1f @ %.1f,%.1f,%.1f",
                halfWidth * 2.0, halfHeight * 2.0, center.getX(), center.getY(), center.getZ());
    }
}
