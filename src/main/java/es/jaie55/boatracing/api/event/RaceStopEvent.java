package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceResult;
import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/** Fired when a race stops (all results computed, before cleanup). */
public class RaceStopEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final List<RaceResult> results;
    private final boolean announced;

    public RaceStopEvent(RaceSessionView session, List<RaceResult> results, boolean announced) {
        super(session, null);
        this.results = results == null ? List.of() : List.copyOf(results);
        this.announced = announced;
    }

    /** Finishers first (time order), then forfeited entries. */
    public List<RaceResult> results() {
        return Collections.unmodifiableList(results);
    }

    public boolean announced() {
        return announced;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
