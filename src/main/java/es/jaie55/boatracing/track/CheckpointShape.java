package es.jaie55.boatracing.track;

import org.bukkit.Location;

/**
 * A checkpoint gate of any geometry. Implementations must be safe to call from the
 * race tick loop: no allocations beyond what is strictly needed and no Bukkit calls
 * that require a specific thread.
 */
public interface CheckpointShape {

    /** World this gate belongs to. */
    String worldName();

    /**
     * @return true when the movement segment {@code from -> to} passes through this gate.
     *         Implementations must accept {@code from == null} (e.g. first movement tick)
     *         and fall back to a containment test.
     */
    boolean crossed(Location from, Location to);

    /** @return true when the given location is inside (or touching) this gate. */
    boolean contains(Location loc);

    /** Stable identifier used for serialization: "aabb" or "plane". */
    String type();

    /** Short human readable description used in messages and GUIs. */
    String describe();

    /** Number of alternate gates attached to this checkpoint (0 for simple gates). */
    default int alternateCount() {
        return 0;
    }
}
