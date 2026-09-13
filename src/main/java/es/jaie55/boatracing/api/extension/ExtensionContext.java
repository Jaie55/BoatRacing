package es.jaie55.boatracing.api.extension;

import es.jaie55.boatracing.api.BoatRacingAPI;
import es.jaie55.boatracing.api.HudProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Services BoatRacing provides to a loaded extension. The base plugin owns configuration files,
 * language bundles, storage, scheduling, commands, listeners, HUD lines and placeholders.
 */
public interface ExtensionContext {

    /** @return the extension name from {@code extension.yml} (also the data folder name). */
    String name();

    /** @return the extension version from {@code extension.yml}. */
    String version();

    /** @return the extension data folder ({@code plugins/BoatRacing/extensions/<name>/}). */
    File dataFolder();

    /** @return a logger prefixed with the extension name. */
    Logger logger();

    /** @return the current BoatRacing extension API ({@link es.jaie55.boatracing.api.BoatRacingProvider#get()}). */
    BoatRacingAPI api();

    /** @return the extension config.yml, with bundled defaults merged in. */
    FileConfiguration config();

    /** Reloads the extension config.yml from disk (bundled defaults are re-applied). */
    void reloadConfig();

    /** @return the active BoatRacing language code (config.yml → language). */
    String language();

    /** @return a message from the extension's messages_&lt;language&gt;.yml with English fallback. */
    String message(String key, Object... replacements);

    /** @return a message list from the extension's messages_&lt;language&gt;.yml with English fallback. */
    List<String> messageList(String key);

    /** Reloads the extension language bundles for the active language. */
    void reloadMessages();

    /** @return storage managed by BoatRacing (respects database.mode). */
    ExtensionStorage storage();

    /** @return scheduling helpers backed by the server scheduler (Folia-aware). */
    ExtensionScheduler scheduler();

    /** Registers a subcommand under {@code /boatracing <name> ...}. */
    void registerCommand(ExtensionCommand command);

    /** Registers a subcommand under {@code /boatracing setup <name> ...} (requires {@code boatracing.setup}). */
    void registerSetupCommand(ExtensionCommand command);

    /** @return the track currently selected in setup, or null when none is loaded. */
    String selectedTrackName();

    /** @return this extension's stored value for the selected track. */
    Object selectedTrackData(String key);

    /** Stores a value for the selected track (persisted in the track YAML under {@code extensions.<name>}). */
    void setSelectedTrackData(String key, Object value);

    /** Removes a stored value from the selected track. */
    void removeSelectedTrackData(String key);

    /** @return this extension's stored value for any track (routes to the owning track instance). */
    Object trackData(String trackName, String key);

    /** Stores a value for any track (persisted in {@code tracks/<name>.yml}). */
    void setTrackData(String trackName, String key, Object value);

    /** Removes a stored value from any track. */
    void removeTrackData(String trackName, String key);

    /** Registers a Bukkit listener owned by BoatRacing. */
    void registerListener(Listener listener);

    /** Registers HUD lines appended next to the race sidebar/action bar. */
    void registerHudProvider(HudProvider provider);

    /**
     * Registers a PlaceholderAPI value exposed as {@code %boatracing_<identifier>%}.
     * The resolver may receive a null player (no player context) and should return "" then.
     */
    void registerPlaceholder(String identifier, Function<Player, String> resolver);
}
