package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired after a player leaves a race registration. */
public class RaceLeaveEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public RaceLeaveEvent(RaceSessionView session, Player player) {
        super(session, player);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
