package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a solo practice run finishes. */
public class PracticeFinishEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long finishTimeMillis;

    public PracticeFinishEvent(RaceSessionView session, Player player, long finishTimeMillis) {
        super(session, player);
        this.finishTimeMillis = finishTimeMillis;
    }

    public long finishTimeMillis() {
        return finishTimeMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
