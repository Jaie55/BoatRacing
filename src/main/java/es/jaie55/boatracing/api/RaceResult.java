package es.jaie55.boatracing.api;

import java.util.UUID;

/** One entry of a finished race result (used by {@code RaceStopEvent}). */
public record RaceResult(UUID playerId, String playerName, long timeMillis, boolean forfeited) {
}
