package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a racer forfeits an active race. */
public class RaceForfeitEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int lap;
    private final int checkpoint;

    public RaceForfeitEvent(RaceSessionView session, Player player, int lap, int checkpoint) {
        super(session, player);
        this.lap = lap;
        this.checkpoint = checkpoint;
    }

    public int lap() {
        return lap;
    }

    public int checkpoint() {
        return checkpoint;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
