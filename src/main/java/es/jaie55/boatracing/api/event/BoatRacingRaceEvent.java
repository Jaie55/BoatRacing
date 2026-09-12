package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Base class for BoatRacing extension events. Events are informational (not cancellable) and
 * are always dispatched on the main server thread. Exceptions thrown by listeners are caught
 * and logged by BoatRacing, so a broken extension never breaks a race.
 */
public abstract class BoatRacingRaceEvent extends Event {

    private final RaceSessionView session;
    private final Player player;

    protected BoatRacingRaceEvent(RaceSessionView session, Player player) {
        this.session = session;
        this.player = player;
    }

    public RaceSessionView session() {
        return session;
    }

    /** @return the involved player, or null for session-wide events. */
    public Player player() {
        return player;
    }

    public String trackName() {
        return session != null ? session.trackName() : null;
    }
}
