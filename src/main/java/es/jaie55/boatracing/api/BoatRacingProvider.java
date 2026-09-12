package es.jaie55.boatracing.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Static access point for {@link BoatRacingAPI}.
 *
 * Extensions should call {@link #get()} (or {@link #optional()}) and check the API version.
 */
public final class BoatRacingProvider {

    private BoatRacingProvider() {
    }

    /** @return the API, or null when BoatRacing is missing or the API is disabled. */
    public static BoatRacingAPI get() {
        RegisteredServiceProvider<BoatRacingAPI> registration =
                Bukkit.getServicesManager().getRegistration(BoatRacingAPI.class);
        return registration != null ? registration.getProvider() : null;
    }

    public static Optional<BoatRacingAPI> optional() {
        return Optional.ofNullable(get());
    }

    /** @return true when the running API is compatible with the given compiled version. */
    public static boolean isCompatible(int compiledApiVersion) {
        BoatRacingAPI api = get();
        return api != null && api.enabled() && api.apiVersion() >= compiledApiVersion;
    }
}
