package es.jaie55.boatracing.track;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public class Region implements CheckpointShape {
    private final String worldName;
    private final BoundingBox box;

    public Region(String worldName, BoundingBox box) {
        this.worldName = worldName;
        this.box = box;
    }

    public String getWorldName() { return worldName; }
    public BoundingBox getBox() { return box; }

    @Override
    public boolean contains(Location loc) {
        if (loc == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        return box.contains(loc.toVector());
    }

    @Override
    public boolean crossed(Location from, Location to) {
        if (to == null) return false;
        if (!to.getWorld().getName().equals(worldName)) return false;
        if (contains(to)) return true;
        if (from == null || !from.getWorld().getName().equals(worldName)) return false;
        return Geometry.segmentIntersectsBox(from, to, box);
    }

    @Override
    public String worldName() { return worldName; }

    @Override
    public String type() { return "aabb"; }

    @Override
    public String describe() {
        return String.format(java.util.Locale.ROOT, "(%d,%d,%d) -> (%d,%d,%d)",
                (int) Math.floor(box.getMinX()), (int) Math.floor(box.getMinY()), (int) Math.floor(box.getMinZ()),
                (int) Math.floor(box.getMaxX()), (int) Math.floor(box.getMaxY()), (int) Math.floor(box.getMaxZ()));
    }

    public World world() { return Bukkit.getWorld(worldName); }
}
