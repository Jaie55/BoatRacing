package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Fired when a race starts (after the countdown, when timing begins). */
public class RaceStartEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final List<UUID> participants;

    public RaceStartEvent(RaceSessionView session, List<UUID> participants) {
        super(session, null);
        this.participants = participants == null ? List.of() : List.copyOf(participants);
    }

    public List<UUID> participants() {
        return Collections.unmodifiableList(participants);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
