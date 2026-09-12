package es.jaie55.boatracing.util;

import java.util.Locale;

/**
 * Duration helpers shared by the cosmetic shop and the admin unlock commands.
 * Accepted formats: {@code 30s}, {@code 15m}, {@code 12h}, {@code 7d}, {@code 0}/{@code permanent}.
 */
public final class TimeFormat {

    private TimeFormat() {
    }

    /** @return duration in seconds, 0 for permanent, or -1 when the format is invalid. */
    public static long parseDurationSeconds(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("0") || value.equals("perm") || value.equals("permanent")) return 0L;
        char unit = value.charAt(value.length() - 1);
        String number = value.substring(0, value.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(number);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
        if (amount < 0) return -1L;
        return switch (unit) {
            case 's' -> amount;
            case 'm' -> amount * 60L;
            case 'h' -> amount * 3600L;
            case 'd' -> amount * 86400L;
            default -> -1L;
        };
    }

    public static long expiresAtFromSeconds(long seconds) {
        if (seconds <= 0L) return 0L;
        return System.currentTimeMillis() + seconds * 1000L;
    }

    /** @return human readable remaining time, "permanent" when permanent, "-" when unknown. */
    public static String formatRemaining(long expiresAt) {
        if (expiresAt <= 0L) return "permanent";
        long remaining = Math.max(0L, expiresAt - System.currentTimeMillis());
        long seconds = remaining / 1000L;
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }
}
