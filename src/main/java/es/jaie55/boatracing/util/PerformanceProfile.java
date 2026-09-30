package es.jaie55.boatracing.util;

import java.util.Locale;

/**
 * Global performance profile. {@code low} doubles periodic intervals (scoreboard, trails, previews)
 * to protect TPS on busy servers, {@code balanced} keeps the configured defaults and {@code high}
 * removes the extra throttling for maximum smoothness on strong hardware.
 */
public enum PerformanceProfile {
    LOW,
    BALANCED,
    HIGH;

    public static PerformanceProfile from(String raw) {
        if (raw == null) return BALANCED;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BALANCED;
        }
    }

    public boolean isLow() {
        return this == LOW;
    }

    /** @return the effective delay/period for a periodic task, scaled for this profile. */
    public int scaleTicks(int baseTicks) {
        int safe = Math.max(1, baseTicks);
        return this == LOW ? safe * 2 : safe;
    }
}
