package es.jaie55.boatracing.api.event;

import es.jaie55.boatracing.api.RaceSessionView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a racer crosses a checkpoint. */
public class CheckpointReachedEvent extends BoatRacingRaceEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int checkpoint;
    private final int totalCheckpoints;
    private final int lap;

    public CheckpointReachedEvent(RaceSessionView session, Player player,
                                  int checkpoint, int totalCheckpoints, int lap) {
        super(session, player);
        this.checkpoint = checkpoint;
        this.totalCheckpoints = totalCheckpoints;
        this.lap = lap;
    }

    /** 1-based checkpoint number just reached. */
    public int checkpoint() {
        return checkpoint;
    }

    public int totalCheckpoints() {
        return totalCheckpoints;
    }

    /** 1-based lap in which the checkpoint was crossed. */
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
