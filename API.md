# BoatRacing Extension API

Stable contract for building extensions on top of BoatRacing (for example, a paid
party-style abilities addon). There are two supported models:

- **Separate plugin**: a normal Bukkit plugin that depends on BoatRacing and talks to the API
  through `BoatRacingProvider`.
- **Base-managed extension**: a jar in `plugins/BoatRacing/extensions/` that BoatRacing loads
  itself (not a Bukkit plugin). BoatRacing owns its config, language files, storage, scheduler,
  commands, HUD lines and placeholders.

In both cases extensions are never bundled into the BoatRacing jar, and BoatRacing keeps working
exactly the same with or without them.

## Compatibility contract

- Only the `es.jaie55.boatracing.api` package (and subpackages: `.event`, `.extension`; `.internal`
  is private) is a compatibility contract. Any other class is internal and may change without notice.
- `BoatRacingAPI.API_VERSION` is bumped only on breaking changes.
- Events are **not cancellable** and are always dispatched on the main server thread.
- Listener exceptions are caught and logged by BoatRacing (`Level.FINE`), so a broken extension
  can never break a race.

## Setup

### Option A: separate plugin

`plugin.yml` of your plugin:

```yaml
name: MyBoatRacingPlugin
main: com.example.party.PartyPlugin
api-version: '1.19'
depend: [BoatRacing]
softdepend: [Vault, PlaceholderAPI]
```

### Option B: base-managed extension

`extension.yml` inside your jar:

```yaml
name: BoatRacing-PartyExtension
main: com.example.party.PartyExtension
version: '26.3'
api-version: 1
description: Party items and abilities for BoatRacing.
authors: [You]
```

Rules:

- `name` is also the folder created under `plugins/BoatRacing/extensions/`.
- `version` should match the BoatRacing plugin version you built against; BoatRacing logs a warning
  (but still loads) when it does not.
- `api-version` must be `<=` `BoatRacingAPI.API_VERSION`; newer jars are skipped with a warning.
- `main` must implement `es.jaie55.boatracing.api.extension.BoatRacingExtension` and have a public
  no-argument constructor.
- Bundled `config.yml` and `lang/messages_*.yml` are copied to the extension folder on first load.
- Bundled resources are read from the extension jar itself (the base plugin files can never shadow
  them), and a config key that also exists in the base `config.yml` is treated as base-owned and
  removed from the extension config on load.

Compile against the BoatRacing build with `provided` scope (no need to publish anything; a
private repo can install the jar to the local Maven repository with `mvn install`).

## Getting the API

```java
BoatRacingAPI api = BoatRacingProvider.get();
if (api == null || !api.enabled()) {
    getLogger().warning("BoatRacingAPI not available; disabling extension.");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
if (api.apiVersion() < BoatRacingAPI.API_VERSION) {
    getLogger().warning("BoatRacing is too old for this extension (need API " + BoatRacingAPI.API_VERSION + ").");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

## Base-managed extensions

A base-managed extension implements `BoatRacingExtension` and receives an `ExtensionContext`:

```java
public final class PartyExtension implements BoatRacingExtension {

    private ExtensionContext context;
    private PartyPoints points;

    @Override
    public void onEnable(ExtensionContext context) {
        this.context = context;
        this.points = new PartyPoints(context);

        context.registerListener(new PartyListener(this));
        context.registerCommand(new PartyCommand(this));
        context.registerHudProvider(viewer -> List.of("&dPoints: &f" + points.get(viewer.getUniqueId())));
        context.registerPlaceholder("party_points", player ->
                player == null ? "" : String.valueOf(points.get(player.getUniqueId())));
        context.scheduler().runTimer(this::tick, 20L, 20L);
    }

    @Override
    public void onDisable() {
        points.save();
    }

    @Override
    public void onReload() {
        points.reload();
    }
}
```

`ExtensionContext` services:

| Method | Purpose |
|---|---|
| `name()`, `version()`, `dataFolder()`, `logger()` | Identity and per-extension folder/log. |
| `api()` | The regular `BoatRacingAPI` (events, sessions, HUD). |
| `config()`, `reloadConfig()` | `config.yml` from the extension folder, bundled defaults merged. |
| `language()`, `message(key, ...)`, `messageList(key)`, `reloadMessages()` | `lang/messages_<language>.yml` with English fallback. |
| `storage()`, `read/write(document)` | BoatRacing-managed persistence (respects `database.mode`). |
| `scheduler()` | Folia-aware `runNow/runLater/runTimer/runAsync/runAsyncTimer`. |
| `registerCommand(ExtensionCommand)` | Subcommand under `/boatracing <name> ...` with permissions and tab completion. |
| `registerSetupCommand(ExtensionCommand)` | Subcommand under `/boatracing setup <name> ...` (listed in `setup help`, tab-completed, requires `boatracing.setup`). |
| `selectedTrackName()` | Name of the track currently selected in setup, or null. |
| `selectedTrackData(key)` / `setSelectedTrackData(key, value)` / `removeSelectedTrackData(key)` | Per-track data for the selected track. |
| `trackData(track, key)` / `setTrackData(...)` / `removeTrackData(...)` | Per-track data for any named track. |
| `registerListener(Listener)` | Bukkit listener owned/registered by BoatRacing. |
| `registerHudProvider(HudProvider)` | Sidebar/action bar additions. |
| `registerPlaceholder(String, Function<Player, String>)` | `%boatracing_<identifier>%` value. |

`ExtensionCommand`:

```java
public final class PartyCommand implements ExtensionCommand {

    @Override
    public String name() {
        return "party"; // /boatracing party ...
    }

    @Override
    public String permission() {
        return "boatracing.party.use";
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    @Override
    public String usage() {
        return "&cUsage: /{label} party help"; // {label} is replaced by the base command label
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // args are the arguments after 'party'
        return true;
    }
}
```

Storage example:

```java
YamlConfiguration cfg = new YamlConfiguration();
cfg.set("points", 12);
context.storage().write("party-stats.yml", cfg.saveToString());

String content = context.storage().read("party-stats.yml"); // null when missing
```

Per-track data example (saved inside `tracks/<track>.yml` under `extensions.<your extension>`,
so it travels with the track file and is never touched by the base plugin):

```java
context.setSelectedTrackData("boxes", List.of(Map.of("world", "world", "x", 12.5, "y", 64.0, "z", -3.5)));
Object raw = context.selectedTrackData("boxes");

// Any named track (works even while a race runs on it):
context.setTrackData("my_track", "boxes", boxes);
```

Lifecycle notes:

- `onEnable` runs during BoatRacing startup; throwing aborts that extension only (no listener, HUD,
  command or placeholder registrations survive) and BoatRacing keeps running.
- `onDisable` runs when BoatRacing disables the extension or the server stops. Reloads call
  `reloadConfig()`, `reloadMessages()` and then `onReload()`.
- Registered listeners, HUD providers, placeholders and scheduled tasks are removed/cancelled
  automatically when the extension is disabled.
- `/boatracing extensions reload`, `/boatracing reload` and language changes refresh existing
  extensions; new or updated jars in the extensions folder are picked up on server start (or a full
  plugin reload).
- List all loaded extensions with `/boatracing extensions`; `/boatracing debug` includes them.

## Events

Subscribe with the normal Bukkit `@EventHandler`:

| Event | Fired when | Useful data |
|---|---|---|
| `RaceOpenEvent` | Registration opens | `session()` |
| `RaceJoinEvent` / `RaceLeaveEvent` | Player joins/leaves registration | `player()` |
| `RaceStartEvent` | Countdown finished, timing starts | `participants()` |
| `CheckpointReachedEvent` | Racer crosses a checkpoint | `checkpoint()`, `totalCheckpoints()`, `lap()` |
| `LapCompleteEvent` | Racer completes a non-final lap | `lap()`, `lapMillis()` |
| `PitStopEvent` | Racer exits the pit (stop counted) | `totalPitStops()`, `lap()` |
| `RaceFinishEvent` | Racer crosses the finish line | `position()`, `finishTimeMillis()`, `penaltyMillis()` |
| `RaceStopEvent` | Race ends and results are computed | `results()`, `announced()` |
| `RaceForfeitEvent` | Racer forfeits | `lap()`, `checkpoint()` |
| `PracticeStartEvent` / `PracticeFinishEvent` | Solo practice run starts/finishes | `finishTimeMillis()` |

All events extend `BoatRacingRaceEvent` (`session()`, `player()` nullable, `trackName()`).

## Views

`RaceSessionView` (live): `trackName()`, `totalLaps()`, `status()` (`idle|registering|countdown|running|practice`),
`running()`, `registering()`, `countdown()`, `practice()`, `partyEnabled()` (true when the race was
opened with party mode), `participants()`, `registered()`, `playerView(uuid)`, `audience()` (participants + admins).

`PlayerRaceView` (live): `lap()`, `totalLaps()`, `checkpoint()`, `totalCheckpoints()`,
`position()`, `elapsedMillis()`, `finished()`, `forfeited()`, `finishTimeMillis()`.

```java
RaceSessionView session = api.sessionForPlayer(player.getUniqueId());
if (session != null) {
    PlayerRaceView view = session.playerView(player.getUniqueId());
    player.sendMessage("Lap " + view.lap() + "/" + view.totalLaps()
            + " | CP " + view.checkpoint() + "/" + view.totalCheckpoints()
            + " | P" + view.position());
}
```

## HUD integration

Extensions can append their own lines to the race sidebar and text to the action bar without
fighting the base plugin for ownership:

```java
public final class MyHud implements HudProvider {

    @Override
    public List<String> sidebarLines(Player viewer) {
        return List.of("", "&dMy stat: &f" + myStats(viewer));
    }

    @Override
    public String actionBarSuffix(Player viewer) {
        return "&7| &e" + myExtra(viewer);
    }
}
```

Register it once after grabbing the API and unregister it in `onDisable`:

```java
api.registerHudProvider(this, myHud);
// onDisable:
api.unregisterHudProvider(myHud);
```

`sidebarLines` lines use legacy `&` colour codes and are appended after the race lines, capped by
the base scoreboard limit. Providers are called every scoreboard refresh (every 2 ticks) on the
main thread, so keep the calculations cheap and never block.

## Example: grant an item at a checkpoint

```java
@EventHandler
public void onCheckpoint(CheckpointReachedEvent event) {
    Player player = event.player();
    if (player == null) return;
    if (event.checkpoint() % 2 != 0) return; // every other checkpoint
    player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET));
    player.sendActionBar(Component.text("Item granted!"));
}
```

## Runtime model and recommendations

- Events run on the main thread; do not block them.
- Use `event.session().audience()` to talk to racers + admins instead of broadcasting globally.
- Clean up entities you spawn on `RaceStopEvent` and in `onDisable`.
- Do not bypass race rules: checkpoints, laps, pits, penalties and results are validated by
  BoatRacing regardless of what an extension does.
- `config.yml → api.enabled` can disable the API entirely; check `api.enabled()` before hooking.
- `config.yml → api.log-extensions` makes BoatRacing log which installed plugins depend on it.
