package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.event.HandlerList;

/** Fired when registration opens for a track. */
public class RaceOpenEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public RaceOpenEvent(RaceSessionView session) {
        super(session, null);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
