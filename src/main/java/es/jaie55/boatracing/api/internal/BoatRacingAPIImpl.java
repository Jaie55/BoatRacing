package es.jaie55.boatracing.api.internal;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.BoatRacingAPI;
import es.jaie55.boatracing.api.RaceSessionView;
import es.jaie55.boatracing.race.RaceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Default {@link BoatRacingAPI} implementation registered through Bukkit's ServicesManager. */
public final class BoatRacingAPIImpl implements BoatRacingAPI {

    private final BoatRacingPlugin plugin;

    public BoatRacingAPIImpl(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int apiVersion() {
        return API_VERSION;
    }

    @Override
    public String pluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean enabled() {
        return plugin.isApiEnabled();
    }

    @Override
    public Collection<RaceSessionView> sessions() {
        Collection<RaceManager> managers = plugin.getAllRaceManagers();
        List<RaceSessionView> views = new ArrayList<>(managers.size());
        for (RaceManager manager : managers) {
            if (manager != null) views.add(new RaceSessionViewImpl(plugin, manager));
        }
        return views;
    }

    @Override
    public RaceSessionView sessionForTrack(String trackName) {
        RaceManager manager = plugin.getRaceManagerByTrack(trackName);
        return manager == null ? null : new RaceSessionViewImpl(plugin, manager);
    }

    @Override
    public RaceSessionView sessionForPlayer(UUID playerId) {
        RaceManager manager = plugin.getRaceManagerForPlayer(playerId);
        return manager == null ? null : new RaceSessionViewImpl(plugin, manager);
    }

    @Override
    public boolean isRacing(UUID playerId) {
        return plugin.getRaceManagerForPlayer(playerId) != null;
    }

    @Override
    public void registerHudProvider(org.bukkit.plugin.Plugin owner, es.jaie55.boatracing.api.HudProvider provider) {
        if (provider == null) return;
        if (!plugin.getHudProviders().contains(provider)) {
            plugin.getHudProviders().add(provider);
        }
    }

    @Override
    public void unregisterHudProvider(es.jaie55.boatracing.api.HudProvider provider) {
        if (provider != null) plugin.getHudProviders().remove(provider);
    }
}
