package es.jaie55.boatracing.track;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

/**
 * Shared geometry helpers for checkpoint crossing detection.
 */
public final class Geometry {

    private Geometry() {
    }

    /**
     * Segment / axis-aligned bounding box intersection using the slab method.
     * Kept identical to the original RaceManager implementation to preserve behavior.
     */
    public static boolean segmentIntersectsBox(Location from, Location to, BoundingBox box) {
        if (from == null || to == null || box == null) return false;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < 1e-10) {
            if (from.getX() < box.getMinX() || from.getX() > box.getMaxX()) return false;
        } else {
            double t1 = (box.getMinX() - from.getX()) / dx;
            double t2 = (box.getMaxX() - from.getX()) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dy) < 1e-10) {
            if (from.getY() < box.getMinY() || from.getY() > box.getMaxY()) return false;
        } else {
            double t1 = (box.getMinY() - from.getY()) / dy;
            double t2 = (box.getMaxY() - from.getY()) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dz) < 1e-10) {
            if (from.getZ() < box.getMinZ() || from.getZ() > box.getMaxZ()) return false;
        } else {
            double t1 = (box.getMinZ() - from.getZ()) / dz;
            double t2 = (box.getMaxZ() - from.getZ()) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }
}
