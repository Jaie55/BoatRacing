package es.jaie55.boatracing.api.extension;

/**
 * Entry point of a BoatRacing extension.
 *
 * Extensions are <b>not</b> Bukkit plugins: BoatRacing loads their jar from
 * {@code plugins/BoatRacing/extensions/}, reads {@code extension.yml} from inside the jar and calls
 * {@link #onEnable(ExtensionContext)}. Everything user-facing (commands, config, language files,
 * HUD, storage, scheduling) is managed by BoatRacing through the {@link ExtensionContext}.
 *
 * The extension jar may be loaded with a different version of the base plugin; always check
 * {@code extension.yml → api-version} and {@link es.jaie55.boatracing.api.BoatRacingAPI#apiVersion()}.
 */
public interface BoatRacingExtension {

    /** Called when the extension is loaded. Throw to abort loading (BoatRacing logs the error). */
    void onEnable(ExtensionContext context) throws Exception;

    /** Called when BoatRacing disables the extension or the server stops. */
    default void onDisable() {
    }

    /** Called after {@code /boatracing reload} or an extension reload, config/messages are already refreshed. */
    default void onReload() {
    }
}
