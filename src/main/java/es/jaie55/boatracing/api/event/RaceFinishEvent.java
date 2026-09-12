package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a racer crosses the finish line of a competitive race. */
public class RaceFinishEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int position;
    private final long finishTimeMillis;
    private final long penaltyMillis;

    public RaceFinishEvent(RaceSessionView session, Player player,
                           int position, long finishTimeMillis, long penaltyMillis) {
        super(session, player);
        this.position = position;
        this.finishTimeMillis = finishTimeMillis;
        this.penaltyMillis = penaltyMillis;
    }

    /** 1-based finishing position at the moment of crossing (not the final classification). */
    public int position() {
        return position;
    }

    public long finishTimeMillis() {
        return finishTimeMillis;
    }

    public long penaltyMillis() {
        return penaltyMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
