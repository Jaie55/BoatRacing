package es.jaie55.boatracing.api;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Read-only live view of one track race session. Values are computed on access, so an
 * instance can be cached for the duration of an event handler safely.
 */
public interface RaceSessionView {

    String trackName();

    int totalLaps();

    boolean running();

    boolean registering();

    boolean countdown();

    boolean practice();

    /** @return true when the session was opened in party mode (used by the party add-on). */
    boolean partyEnabled();

    /** @return one of: idle, registering, countdown, running, practice. */
    String status();

    /** @return participant UUIDs (racing or finished). */
    Collection<UUID> participants();

    /** @return registered UUIDs waiting for the race to start. */
    Collection<UUID> registered();

    /** @return the live view for a participant, or null when the player is not part of this race. */
    PlayerRaceView playerView(UUID playerId);

    /** @return online players that should receive race messages (participants + admins). */
    Collection<Player> audience();
}
