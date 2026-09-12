package es.jaie55.boatracing.api;

import java.util.UUID;

/** Read-only live view of a single racer within a {@link RaceSessionView}. */
public interface PlayerRaceView {

    UUID playerId();

    String playerName();

    int lap();

    int totalLaps();

    int checkpoint();

    int totalCheckpoints();

    /** @return 1-based live position, or -1 when unavailable. */
    int position();

    /** @return elapsed race time in millis including penalties, or the final time when finished. */
    long elapsedMillis();

    boolean finished();

    boolean forfeited();

    /** @return finish time in millis, or -1 when the racer has not finished. */
    long finishTimeMillis();
}
