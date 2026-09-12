package es.jaie55.boatracing.api.internal;

import es.jaie55.boatracing.api.PlayerRaceView;
import es.jaie55.boatracing.race.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Live {@link PlayerRaceView} backed by a {@link RaceManager}. */
public final class PlayerRaceViewImpl implements PlayerRaceView {

    private final RaceManager manager;
    private final UUID playerId;

    public PlayerRaceViewImpl(RaceManager manager, UUID playerId) {
        this.manager = manager;
        this.playerId = playerId;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    @Override
    public String playerName() {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.getName() != null) return online.getName();
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null ? name : playerId.toString();
    }

    @Override
    public int lap() {
        return manager.getLiveLap(playerId);
    }

    @Override
    public int totalLaps() {
        return manager.getTotalLaps();
    }

    @Override
    public int checkpoint() {
        return manager.getLiveCheckpoint(playerId);
    }

    @Override
    public int totalCheckpoints() {
        return manager.getTrack().getCheckpoints().size();
    }

    @Override
    public int position() {
        return manager.getLivePosition(playerId);
    }

    @Override
    public long elapsedMillis() {
        return manager.getLiveTimeMillis(playerId);
    }

    @Override
    public boolean finished() {
        return manager.isLiveFinished(playerId);
    }

    @Override
    public boolean forfeited() {
        return manager.isLiveForfeited(playerId);
    }

    @Override
    public long finishTimeMillis() {
        return manager.getLiveFinishTimeMillis(playerId);
    }
}
