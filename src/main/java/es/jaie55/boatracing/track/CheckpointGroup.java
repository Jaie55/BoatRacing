package es.jaie55.boatracing.track;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A checkpoint with one or more alternate gates (pit lanes, bypasses...).
 * Crossing any of the gates (primary or alternate) advances the checkpoint.
 *
 * The group travels with its index in the checkpoint list, so reordering/removing
 * checkpoints never desyncs the alternates from their owner.
 */
public class CheckpointGroup implements CheckpointShape {

    private final CheckpointShape primary;
    private final List<CheckpointShape> alternates = new ArrayList<>();

    public CheckpointGroup(CheckpointShape primary) {
        this(primary, null);
    }

    public CheckpointGroup(CheckpointShape primary, List<CheckpointShape> alternates) {
        if (primary == null) throw new IllegalArgumentException("primary checkpoint must not be null");
        this.primary = primary;
        if (alternates != null) {
            for (CheckpointShape alternate : alternates) {
                if (alternate != null) this.alternates.add(alternate);
            }
        }
    }

    public CheckpointShape getPrimary() { return primary; }

    public List<CheckpointShape> getAlternates() { return Collections.unmodifiableList(alternates); }

    public void addAlternate(CheckpointShape alternate) {
        if (alternate != null) alternates.add(alternate);
    }

    public void clearAlternates() {
        alternates.clear();
    }

    @Override
    public String worldName() {
        return primary.worldName();
    }

    @Override
    public boolean crossed(Location from, Location to) {
        if (primary.crossed(from, to)) return true;
        for (CheckpointShape alternate : alternates) {
            if (alternate.crossed(from, to)) return true;
        }
        return false;
    }

    @Override
    public boolean contains(Location loc) {
        if (primary.contains(loc)) return true;
        for (CheckpointShape alternate : alternates) {
            if (alternate.contains(loc)) return true;
        }
        return false;
    }

    @Override
    public String type() {
        return primary.type();
    }

    @Override
    public String describe() {
        return primary.describe();
    }

    @Override
    public int alternateCount() {
        return alternates.size();
    }
}
