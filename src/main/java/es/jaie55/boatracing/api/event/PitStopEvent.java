package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a racer exits the pit area and the pit stop is counted. */
public class PitStopEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int totalPitStops;
    private final int lap;

    public PitStopEvent(RaceSessionView session, Player player, int totalPitStops, int lap) {
        super(session, player);
        this.totalPitStops = totalPitStops;
        this.lap = lap;
    }

    /** Total pit stops completed by the racer in this race. */
    public int totalPitStops() {
        return totalPitStops;
    }

    public int lap() {
        return lap;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
