package es.jaie55.boatracing.api;

/**
 * Public, stable BoatRacing extension API.
 *
 * Only this package (and its subpackages) is a compatibility contract. Internal plugin classes
 * may change between releases without notice. Extensions should depend on the BoatRacing jar
 * with {@code provided} scope and declare {@code depend: [BoatRacing]} in their plugin.yml.
 *
 * Get the instance with {@link BoatRacingProvider#get()} and always check {@link #apiVersion()}
 * against the version you compiled against before using it.
 */
public interface BoatRacingAPI {

    /** Current API version. Bumped only on breaking changes. */
    int API_VERSION = 1;

    /** @return the API version supported by the running BoatRacing build. */
    int apiVersion();

    /** @return the running BoatRacing plugin version. */
    String pluginVersion();

    /** @return true when the extension API is enabled in config.yml ({@code api.enabled}). */
    boolean enabled();

    /** @return every known race session (one per track, active or not). */
    java.util.Collection<RaceSessionView> sessions();

    /** @return the session for a track token, or null when that track has no session yet. */
    RaceSessionView sessionForTrack(String trackName);

    /** @return the session the player is registered/racing in, or null. */
    RaceSessionView sessionForPlayer(java.util.UUID playerId);

    /** @return true when the player is registered or racing in any session. */
    boolean isRacing(java.util.UUID playerId);

    /**
     * Registers a HUD provider so the extension can append its own sidebar lines and action bar
     * suffix while the base plugin owns the race HUD. Call {@link #unregisterHudProvider} on
     * disable.
     */
    void registerHudProvider(org.bukkit.plugin.Plugin owner, HudProvider provider);

    void unregisterHudProvider(HudProvider provider);
}
