package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a racer completes a non-final lap. */
public class LapCompleteEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int lap;
    private final long lapMillis;

    public LapCompleteEvent(RaceSessionView session, Player player, int lap, long lapMillis) {
        super(session, player);
        this.lap = lap;
        this.lapMillis = lapMillis;
    }

    /** 1-based lap number just completed. */
    public int lap() {
        return lap;
    }

    /** Lap duration in millis including penalties. */
    public long lapMillis() {
        return lapMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
