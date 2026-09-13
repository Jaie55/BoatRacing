package es.jaie55.boatracing.api.internal;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.PlayerRaceView;
import es.jaie55.boatracing.api.RaceSessionView;
import es.jaie55.boatracing.race.RaceManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Live {@link RaceSessionView} backed by a {@link RaceManager}. */
public final class RaceSessionViewImpl implements RaceSessionView {

    private final BoatRacingPlugin plugin;
    private final RaceManager manager;

    public RaceSessionViewImpl(BoatRacingPlugin plugin, RaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public String trackName() {
        return manager.getTrackName();
    }

    @Override
    public int totalLaps() {
        return manager.getTotalLaps();
    }

    @Override
    public boolean running() {
        return manager.isRunning();
    }

    @Override
    public boolean registering() {
        return manager.isRegistering();
    }

    @Override
    public boolean countdown() {
        return manager.isCountdownActive();
    }

    @Override
    public boolean practice() {
        return manager.isPracticeActive();
    }

    @Override
    public boolean partyEnabled() {
        return manager.isPartyMode();
    }

    @Override
    public String status() {
        if (manager.isPracticeActive()) return "practice";
        if (manager.isCountdownActive()) return "countdown";
        if (manager.isRunning()) return "running";
        if (manager.isRegistering()) return "registering";
        return "idle";
    }

    @Override
    public Collection<UUID> participants() {
        return List.copyOf(manager.getParticipants());
    }

    @Override
    public Collection<UUID> registered() {
        return List.copyOf(manager.getRegistered());
    }

    @Override
    public PlayerRaceView playerView(UUID playerId) {
        if (playerId == null || !manager.isParticipant(playerId)) return null;
        return new PlayerRaceViewImpl(manager, playerId);
    }

    @Override
    public Collection<Player> audience() {
        Collection<Player> audience = manager.apiAudience();
        return audience == null ? new ArrayList<>() : new ArrayList<>(audience);
    }
}
