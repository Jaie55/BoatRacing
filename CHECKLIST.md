README — BoatRacing QA checklist (teams, admin, tracks; two-player tests)

## What to verify for 26.3
- Backward compatibility (critical):
	- Load an existing track whose `checkpoints:` entries have no `type` field and verify it loads unchanged, `/boatracing setup show` shows the same checkpoint count, and a race on it behaves exactly as before.
	- Cross a legacy AABB checkpoint at full ice-boat speed and verify it still registers (no skip).
	- Verify legacy AABB entries are still written in the old shape when the track is saved from the GUI (no forced `type` added unless the checkpoint is a plane gate).
- Oriented gates (PLANE):
	- Manually add a `type: plane` checkpoint (`center`, `normal`, `right`, `up`, `halfWidth`, `halfHeight`) to a track YAML, restart/reload, and verify it appears in the Admin Race checkpoint editor with its size description.
	- Start a race and verify the gate triggers only when actually crossing the rectangle (walking parallel to the gate plane must not trigger it).
	- Cross a plane gate at maximum boat speed from both directions and verify it counts without skipping.
	- Stand next to (but not through) a plane gate and verify it does not trigger.
	- Save an AutoTrace-generated track, restart the server and verify the plane gates load back with readable `center/normal/right/up` values (`x,y,z` strings) and trigger identically.
- AutoTrace:
	- Run `/boatracing setup autotrace start` while in a boat, drive one full lap, and verify it auto-stops near the start (or run `/boatracing setup autotrace stop` manually).
	- Verify the start message shows the exact coordinates and clickable `[Help] [Stop] [Preview] [Accept] [Cancel]` buttons; an `AUTOTRACE` title appears, a particle marker stays at the start, the action bar shows time/samples/distance and the periodic chat reminder (every `reminder-seconds`) shows points/time/distance while recording.
	- Run `/boatracing setup autotrace help` and verify the 6-step guide plus buttons; tab-complete of `autotrace` suggests `help`.
	- After `/boatracing setup autotrace accept`, verify the selection wand is given automatically when missing and the `[Setup wizard]`, `[Open registration]`, `[Get wand]`, `[Add start]` and `[Set finish]` helpers work; verify the corner-marking instructions (left-click A, right-click B).
	- While previewing, left-click a gate with the wand to select it (turns into the selection particle, green by default) and right-click to deselect; verify `/boatracing setup autotrace status` reports the selected count.
	- Run `/boatracing setup autotrace resize selected <width> <height>` and `/boatracing setup autotrace delete selected` and verify every selected gate changes together; verify `selected` is suggested in tab-complete and `wand-select: false` disables gate selection.
	- After stopping, run `/boatracing setup autotrace start` again and verify it discards the stopped run and records a fresh lap (no "already running" lock); run `/boatracing setup autotrace stop` on a stopped run and verify it reports the generated gates and next steps.
	- Run `/boatracing setup autotrace cancel` and verify the session is removed, the action bar is cleared and no gates/particles remain.
	- Verify gates are generated and the particle preview is visible; toggle with `/boatracing setup autotrace preview`.
	- `/boatracing setup autotrace status` shows samples/gates/length/preview state.
	- `/boatracing setup autotrace delete <#>` and `/boatracing setup autotrace resize <#> <width> <height>` modify the generated gates.
	- `/boatracing setup autotrace accept` replaces the active track checkpoints and the track YAML now contains `type: plane` entries; `/boatracing setup autotrace cancel` discards the session without touching the track.
	- Disconnect mid-trace and verify no orphan preview/task remains and no console errors appear; re-running start works.
	- Run `/boatracing reload` mid-trace and verify no console errors.
	- Set `setup.auto-trace.enabled: false` and verify start is blocked with the translated message.
	- With `recenter-ice: false`, gate heights follow the driven line; with `true`, they snap to the nearest ice block.
- Wizard and GUI:
	- Wizard CHECKPOINTS step shows the `[AutoTrace]` button and launches the trace.
	- Admin Race → Checkpoints editor shows the AutoTrace button (slot 51) and closes the GUI when starting the trace.
	- Running `/boatracing setup wizard` shows an on-screen title/subtitle for every step (starts, finish, lights, pit, checkpoints, pitstops, laps, regtime) and a completion title when finishing; advancing, skipping and going back update the title accordingly.
	- Two ways to build a track: create an empty track in `/boatracing admin tracks` and complete it from scratch with `/boatracing setup wizard` until DONE (verify each step validates and the track ends race-ready); then do a second track using only manual commands + the gate dyes + AutoTrace and verify both produce the same YAML structure (`starts`, `finish`, `lights`, `checkpoints`, `racing`).
- Config and i18n (lang folder):
	- On first start with the new build, verify every `plugins/BoatRacing/messages_*.yml` is moved into `plugins/BoatRacing/lang/` (legacy copies renamed to `.migrated` when both exist) and that the plugin still loads the active language without raw keys.
	- Verify `plugins/BoatRacing/lang/messages_en.yml` is created if missing and that custom bundles placed in `lang/` are selectable with `language: "<code>"`.
	- Verify `tools/check_locales.py` reads `src/main/resources/lang/` and reports 0 errors for every bundled language.
	- Verify the extension folder keeps `config.yml` at its root and extracts every bundled language into `plugins/BoatRacing/extensions/<name>/lang/`; legacy root message files are moved into `lang/`.
- Setup select, party flag and per-track extension data:
	- Run `/boatracing setup select <track>` (or click a track in `/boatracing admin tracks`) and verify `setup show`/`setup help` operate on that track; tab-completion suggests track names.
	- Verify `/boatracing race open <track> [party|normal]` sets the mode (message to registered players, `race status` shows `Party: ON/OFF`) and that stopping/resetting the race clears the flag.
	- Verify extension setup subcommands appear in `/boatracing setup help` and tab-completion, and that a reserved setup name (`select`, `show`...) is rejected with a warning.
	- Place a marker block with the party add-on: verify it registers a box only for the selected track, the block is not left in the world, and the box is stored inside `tracks/<track>.yml` under `extensions.BoatRacing-PartyExtension.boxes`.
	- Verify per-track extension data survives normal base saves (best times, checkpoints edits) and travels when the track file is renamed/copied; verify `boxes only render/sound` during setup preview or a party race, and never in normal races or practice.
	- Verify a track without the `extensions` section still loads and runs unchanged.
	- On update/reload, every new 26.3 config section is merged into an existing `config.yml` without overwriting user edits: `setup.auto-trace.*`, `setup.gates.*`, `discord.*`, `racing.victory-effects.*`, `racing.spectate-on-finish.*`, `cosmetics.*`, `diagnostics.*` and `replay.capture-race`.
	- Set `language: es` and verify AutoTrace, alternates, spectator, victory, cosmetics and debug messages are in Spanish. Other bundled languages fall back to English without showing raw keys.
	- Note: all bundled languages now include the 26.3 keys; `tools/check_locales.py` must report 0 errors across every `lang/messages_*.yml`.
- Regression checks (26.3 touched existing behavior):
	- Track files: create a track with starts, finish, default pit, team pit, lights, checkpoints, custom start slots and racing overrides; reload and verify `/boatracing setup show` lists everything and the YAML keeps its values.
	- Legacy track compatibility: open an old track (no `type`, no `alternates`) and save it without editing checkpoints; verify the `checkpoints:` entries are still written in the old AABB shape and the file keeps its data.
	- Checkpoint crossing: with an AABB track, cross each checkpoint at full speed in order and verify laps, sector gaps, lap broadcasts, best lap/race stats and finish detection behave exactly as before.
	- Admin Race checkpoint editor: with mixed AABB/plane/alternate checkpoints, verify add from selection, replace, remove, move up/down and page navigation still work; item lore shows the region (and alternate count when present).
	- Gate tools: `/boatracing setup gates` gives the blue and yellow dyes (and `/boatracing setup autotrace start` gives them too); right-click with the blue dye adds a checkpoint facing your view (saved to the track YAML) with its own particle effect (`checkpoint-particle`, blue dust by default), sneak + right-click removes the last one; the yellow dye sets/clears the finish with a different particle (`finish-particle`, flame by default) and the finish has the **same width/height** as checkpoints (only thickness differs); placing the yellow finish while AutoTrace is recording stops it and generates the gates; `/boatracing setup gates off` removes the tools.
	- `clearfinish` and `clearpit` actually remove the finish/pit from `tracks/<track>.yml` (reload the track and verify they do not come back).
	- Setup commands: `addcheckpoint`, `clearcheckpoints`, `setfinish`, `setpit`, `addlight`, `setlaps`, `setpitstops`, `setregtime`, `setlobby`, `setpos` and the wizard still work and auto-advance as before.
	- Race lifecycle: open -> join -> start/force -> countdown/lights -> laps/pit stops -> finish -> results/rewards; then `/boatracing race restart`, `stop`, `forfeit` and `back` still work with no console errors.
	- Practice ghost: with `practice.ghost.enabled: true` and `replay.capture-race: false`, complete a practice run and verify the ghost records and replays exactly as before; with `replay.capture-race: true`, verify the race ghost is stored and the practice ghost still updates independently.
	- HUD/scoreboard: in-race sidebar, action bar, position tie-break and SimpleScore/TAB sidebars still restore correctly after the race.
	- Reload: run `/boatracing reload` and verify players, teams, tracks and messages reload; verify only one trail task runs (no duplicated particles) and no spectator/follow tasks are left behind.
	- Disable/enable: stop the server with a race, a spectator and a practice session active; verify preferences save, no orphan entities/tasks remain and the next startup is clean.
	- Permissions: verify `boatracing.*` still grants everything (including the new leaves) and that per-trail nodes are NOT granted by `boatracing.*` or `boatracing.admin`.
	- PlaceholderAPI: existing `%boatracing_*%` placeholders keep resolving, and the new title/trail ones return values with a player context and empty/`0` without one, without throwing.
	- Commands/permissions coverage: `/boatracing` usage text and root tab-complete list `cosmetics` and `debug`; `boatracing.setup` gates autotrace/addalt/clearalt; `boatracing.race.spectate` gates spectate; `/boatracing debug` works from console and player with no exceptions.
	- Existing command trees: `teams`, `admin`, `stats`, `version` and `reload` still behave as before (GUI clicks, tab-complete, permissions).
	- Console hygiene: startup/shutdown with the new features produces no warnings or stack traces, and `debug: "off"` keeps the new `fine`/`finer` traces silent.
- Extension API:
	- With `api.enabled: true`, verify `BoatRacingProvider.get()` returns a non-null API in another test plugin and that `apiVersion()` matches `BoatRacingAPI.API_VERSION`.
	- Verify events fire in order during a race: open -> join -> start -> checkpoint -> lap -> pit -> finish -> stop; and forfeit/practice variants.
	- Verify the live views report correct lap, checkpoint, position, elapsed/finish time and status.
	- Throw an exception inside a test listener and verify the race continues and the error is only logged at `debug: "fine"`.
	- Set `api.enabled: false`, reload and verify the service is unregistered and `BoatRacingProvider.get()` returns null.
	- Set `api.log-extensions: true` and verify installed plugins that depend on BoatRacing are logged at startup.
	- Register a test `HudProvider`, run a race and verify its sidebar lines and action bar suffix appear next to the base HUD; make it throw and verify the race HUD keeps updating.
	- Without any extension installed, verify races behave exactly as before (no performance or behavior change).
- Base-managed extensions:
	- Drop a test extension jar with a valid `extension.yml` in `plugins/BoatRacing/extensions/`; verify it loads at startup, its folder is created with `config.yml` and `lang/messages_en.yml`, and `/boatracing extensions` lists name, version, API and commands.
	- Declare a permission in `extension.yml` (`permissions:` with `default: true`) and verify a non-op player receives it after load and loses it after the extension is removed/disabled.
	- Try a jar registering a reserved name (`reload`) or a duplicate name from two extensions: verify it is ignored with a console warning and the built-in command still works.
	- Make a test extension throw during `onEnable` and verify no listener, HUD line, command or placeholder stays registered and the server keeps running.
	- Run `/boatracing party <subcommand>` (or the test extension command) and verify permissions, tab completion and the generic extension-error message when the command throws.
	- Verify the extension config/messages are read from `plugins/BoatRacing/extensions/<name>/` and that `/boatracing extensions reload`, `/boatracing reload` and `/boatracing admin language` refresh them.
	- Verify extension `%boatracing_<id>%` placeholders resolve with player context and return empty without one.
	- Verify extension storage follows `database.mode`: YAML documents under the extension folder, SQLite/MySQL documents in the shared table.
	- Delete the extension jar and restart: the base plugin loads with no errors, the command disappears and the folder is left untouched.
	- Ship a jar with `api-version` newer than `BoatRacingAPI.API_VERSION` and verify it is skipped with a warning.
- BoatRacing-PartyExtension addon (private, untracked `party/`):
	- Build with `scripts/build-all.ps1` and verify `target\BoatRacing-26.3.jar` and `party\target\BoatRacing-PartyExtension-26.3.jar` are produced.
	- Put the extension jar in `plugins/BoatRacing/extensions/` and verify the console logs the load plus "Party mode ready", and that `plugins/BoatRacing/extensions/BoatRacing-PartyExtension/` contains config and language files.
	- Verify `/boatracing party help|points|top` work with `boatracing.party.use` and that `/brparty` no longer exists.
	- Run a race with 2+ racers: starting items are granted, checkpoints can grant items, and right-click/drop uses the ability once (item consumed, global cooldown respected).
	- Test each ability: Mushroom boost, Banana trap slows rivals, Green Shell projectile slows the hit racer, Lightning slows everyone else, Star blocks rival abilities while active, Blooper blinds rivals, Coin adds points, Bob-omb explodes without breaking blocks.
	- `/boatracing party box add` at a track position, `box list`, collect the box while racing, verify respawn cooldown and that boxes survive a restart (extension storage document).
	- Verify party points increase from Coins and finish positions, persist after restart, and appear in the scoreboard HUD; `%boatracing_party_points%`, `%boatracing_party_item%` and the top-1 placeholders resolve.
	- `/boatracing party reload` re-reads config/messages/boxes; set `language: es` and verify addon messages switch with English fallback.
	- Run the private QA pass in `party/CHECKLIST.md` for the full addon matrix.
- Docs and discovery:
	- README shows `Status: Public release (26.3)`, a "What's New (26.3)" block, compatibility 1.19–26.3, and documents every new command, permission, config key and placeholder.
	- README includes a table of contents, an AutoTrace quick guide and checkpoint YAML format examples (axis-aligned, oriented and alternates).
	- `plugin.yml` command usage lists `cosmetics|debug|extensions` and the new permission leaves (including `boatracing.extensions`) are children of the wildcard/meta nodes.
	- `plugin.yml` includes `Vault` in `softdepend` and `boatracing.cosmetics.admin`/`boatracing.cosmetics.unlock.all` leaf nodes.
	- `plugin.yml` declares `folia-supported: true` and the README states Vault compatibility (badge, requirements, platform notes) plus the storage backend explanation.
	- `API.md` documents the extension API contract, events, views, HUD hook and the base-managed extension loader (`extension.yml`, `BoatRacingExtension`, `ExtensionContext`, commands, storage, placeholders), and README/CHANGELOG link to it.
	- README includes a "Developing Extensions" guide (project setup with Maven, `extension.yml`, lifecycle, commands, config/messages, storage, scheduler, HUD, placeholders, external plugin model and best practices) and the table of contents links to it.
	- README explains the two ways to build a track (wizard vs manual commands) and how checkpoints, finish and pit areas work (gates are lines you cross, next-checkpoint order, skipped checkpoints reject the lap, finish and pit rules).
	- `CHANGELOG.md` 26.3 section lists all added features plus the compatibility and docs notes; this CHECKLIST block exists.
	- `/boatracing` root suggests `cosmetics`, `debug` and `extensions`, plus loaded extension subcommands (for example `party`); `/boatracing setup` suggests `autotrace`, `addalt` and `clearalt`; `/boatracing race` suggests `spectate` with `leave` and track names.
	- `/boatracing race help` lists the spectate command; `/boatracing setup help` lists autotrace/addalt/clearalt.
- Discord webhook:
	- Set `discord.enabled: true` with a test webhook URL, open registration and start a race; verify a "Race started" embed arrives with track, laps and racer names.
	- Complete a race and verify the results embed lists positions, times and DNF entries.
	- Set a new track record and verify the record embed arrives exactly once.
	- Disable individual `discord.events.*` entries and verify that event is no longer posted.
	- Put an invalid URL and verify the server logs a warning without breaking races.
	- Verify practice mode does not post anything.
- Victory effects:
	- With `racing.victory-effects.enabled: true`, finish a race in 1st/2nd/3rd and verify screen title, fireworks and sound; finish 4th or lower and verify nothing plays (`top-n: 3`).
	- Disable `fireworks`, `screen-title` and `sounds` individually and verify each toggle is respected.
- Spectator:
	- As a non-participant, run `/boatracing race spectate` during a race; verify spectator gamemode, teleport to the leader and the joined message.
	- Run `/boatracing race spectate leave` and verify gamemode and previous location are restored.
	- Try to spectate while still racing and verify the `while-racing` message blocks it.
	- Set `racing.spectate-on-finish.mode: free` and verify finishers stay in spectator mode until the race ends; with `follow`, verify they follow the leader and stop when the race ends.
	- Disconnect while spectating and verify no leftover follow tasks or console errors; reconnecting works normally.
	- Stop the race with spectators present and verify everyone is restored to their previous location/gamemode.
- Cosmetics (trails):
	- Run `/boatracing cosmetics` with `boatracing.cosmetics` and verify the trails page opens with the 24 built-in trails and that the Trails tab icon mirrors the equipped trail (and returns to gunpowder when none).
	- Equip a trail with permission and verify particles follow the boat/player while racing/practicing (per `cosmetics.trails.show-in`).
	- Without the per-trail permission, verify the item shows the locked lore and clicking does not equip it.
	- Revoke the permission while equipped and verify the trail stops rendering; re-grant and verify it resumes.
	- Set `cosmetics.trails.enabled: false` and verify the page shows the disabled message and no particles render.
	- Add a custom trail under `cosmetics.trails.custom.<id>` and verify it appears after `/boatracing reload`; add a built-in id to `cosmetics.trails.disabled` and verify it disappears.
	- Verify preferences persist across `/boatracing reload` and a server restart (document `player-prefs.yml`).
	- Manually edit `player-prefs.yml` with an unknown trail id and a non-UUID key; verify the plugin loads without errors and ignores them.
- Cosmetics (titles):
	- Verify `%boatracing_title%` shows the automatic title for the player's wins and `%boatracing_title_wins%` matches `/boatracing stats`.
	- Unlock a title (win races or lower a threshold in `cosmetics.titles.thresholds`), equip it from the Titles page and verify `%boatracing_title%` reflects it.
	- Try to equip a locked title and verify the locked message.
	- Set `cosmetics.titles.enabled: false` and verify the page shows the disabled message and `%boatracing_title%` returns empty.
- Cosmetics (victory effects):
	- Open the Victory effects tab and verify the tab icon mirrors the equipped effect; equip `heart`, `rainbow` and `none` and verify each changes the finish effects (fireworks/title/sound), with `none` playing nothing.
	- Verify the selection persists across `/boatracing reload` and a restart, and that clearing resets it to `default`.
	- Set `cosmetics.effects.enabled: false` and verify the page shows the disabled message and the default effect is always used.
	- Add an effect id to `cosmetics.effects.locked` without granting `boatracing.cosmetics.effect.<id>` and verify it shows locked and falls back to `default` at race finish; grant the permission and verify it can be equipped.
- Cosmetics (checkpoint effects):
	- Open the Checkpoint effects tab and verify the tab icon mirrors the equipped effect; equip `spark`, `heart` and `none` and cross a checkpoint in a race/practice and verify the particle/sound plays (and nothing plays with `none`).
	- Verify the selection persists across `/boatracing reload` and a restart, and that clearing resets it.
	- Set `cosmetics.checkpoints.enabled: false` and verify the page shows the disabled message and no checkpoint particles/sounds play.
	- Add an id to `cosmetics.checkpoints.locked` without granting `boatracing.cosmetics.checkpoint.<id>` and verify it shows locked and plays nothing; grant the permission and verify it works.
- Cosmetics (victory sounds):
	- Open the Victory sounds tab and verify the tab icon mirrors the equipped sound; equip `bell`, `dragon` and `none` and finish a race in the podium and verify the matching sound plays (and nothing plays with `none`).
	- Verify `default` plays the sound defined by the selected victory effect, and that muting the global `racing.victory-effects.sounds` disables all of them.
	- Verify the selection persists across `/boatracing reload` and a restart, and that clearing resets it to `default`.
	- Set `cosmetics.victory-sounds.enabled: false` and verify the page shows the disabled message and the effect's default sound is used.
	- Add an id to `cosmetics.victory-sounds.locked` without granting `boatracing.cosmetics.sound.<id>` and verify it shows locked and falls back to `default`; grant the permission (or `boatracing.cosmetics.unlock.all`) and verify it plays.
- Cosmetics (version-safe particles and icons):
	- Run the plugin on 1.19.4 and on 1.21+/26.x and equip `happy`, `enchant`, `witch` and `enchanted`: with aliases they must render the real particle on both versions.
	- On 1.19, verify `snow` and `cherry` are hidden from the menu (particle does not exist) and do not render; with `cosmetics.unsupported: "fallback"` they must appear using the fallback particle instead.
	- Add a custom trail with a data-requiring particle (e.g. `DUST`) and verify it is hidden and logged (no exception when rendering).
	- Verify every cosmetic list entry has a distinct icon and that each tab icon mirrors the equipped cosmetic.
- Cosmetics (particle density):
	- Open the Settings tab and select Low/Normal/High; cross a checkpoint and use a trail and verify the particle count matches 1/2/4 (`cosmetics.density.*`).
	- Verify victory fireworks respect the density with `cosmetics.density.max-victory-rockets` as cap.
	- Run `/boatracing cosmetics density high` (permission `boatracing.cosmetics`) and verify it persists across reload/restart; `/boatracing cosmetics density bogus` shows usage.
	- Set `cosmetics.density.enabled: false` and verify the menu shows the disabled message and always uses `normal`.
- Cosmetics (shop / Vault):
	- With Vault installed and prices configured, verify locked items show the price and `[Buy]`; buying withdraws the money and unlocks the item immediately.
	- Try to buy without enough money and verify the "not enough" message; remove Vault and verify locked items show the permissions-only lore.
	- Verify `cosmetics.shop.prices.<category>.<id>: 0` makes an item free and `gated-by-default: false` restores the previous opt-in behavior.
	- Verify purchases persist across `/boatracing reload` and a restart in `cosmetic-unlocks.yml`.
- Cosmetics (admin unlock commands):
	- `/boatracing cosmetics unlock <player> flame` (category omitted) shows usage; use `/boatracing cosmetics unlock <player> trail flame` and verify the trail unlocks for that player.
	- `/boatracing cosmetics unlock <player> trail * 30m` unlocks the whole category and `/boatracing cosmetics unlock <player> all 7d` unlocks everything; verify expiry in `unlocks` and that expired grants stop working after the cleanup task.
	- `/boatracing cosmetics revoke <player> trail *` and `/boatracing cosmetics revoke <player> all` remove the grants; `/boatracing cosmetics unlocks <player>` lists active grants with their expiry.
	- Works from console and for offline players; tab-complete suggests categories, ids, `*`, `all` and durations.
	- Verify `boatracing.cosmetics.admin` (default op) gates these commands and that `boatracing.cosmetics.unlock.all` is NOT granted by `boatracing.*` but IS granted by `boatracing.admin`.
- Cosmetics (buying / permissions):
	- Grant a single trail permission from a shop/economy command and verify only that trail unlocks.
	- Grant `boatracing.cosmetics.unlock.all` and verify every trail, title, victory effect, victory sound and checkpoint effect shows as unlocked and can be equipped without individual nodes.
	- Verify a non-op player with no permissions sees everything locked with `gated-by-default: true`, while `boatracing.admin` holders see everything unlocked.
- Cosmetics scope (global + per-track):
	- Set `cosmetics.enabled: false` and verify no trails, victory choices or checkpoint effects play, while players can still browse the menu.
	- Run `/boatracing setup setcosmetics false` on a track, start a race/practice there and verify trails, selected victory effects and checkpoint effects are disabled; with `true` they work again.
	- Verify `setup show` lists the `cosmetics-enabled` override and that `/boatracing setup help` + tab-complete include `setcosmetics` with `true`/`false` suggestions.
- Team profile and menu integration:
	- Open `/boatracing teams` → your profile and verify your title and trail are shown, and that the Cosmetics button opens the cosmetics menu.
	- Open the team view and verify each member head lore shows title/trail/checkpoint effect when equipped.
- Stats GUI (title/trail):
	- Run `/boatracing stats` and verify the summary page shows team, racer number, boat, title, trail, effect, wins, positions and best race/lap.
	- Verify the console chat report (`/boatracing stats -p:<player>`) also lists the title, trail and checkpoint effect.
	- Run `/boatracing stats <player>` (with `boatracing.stats.others`) and verify the menu shows that player's data; verify the head is their profile.
	- Open the Practice tab, verify per-track best/last run and lap rows and that pagination works with many tracks.
	- Run `/boatracing stats -p:<player>` from console and verify the text report is still printed.
- Debug and bug reports:
	- Run `/boatracing debug` as an operator and verify it prints plugin/server/API/Java/storage/language/tracks/teams/sessions plus the fixed GitHub Issues URL under `https://github.com/Jaie55/BoatRacing/issues`, without secrets (no MySQL password, no webhook URL).
	- Verify a non-operator without `boatracing.debug` cannot run `/boatracing debug`, and that the permission is NOT granted by `boatracing.use` but is granted by `boatracing.admin`/`boatracing.*`.
	- Verify there is no `diagnostics.*` config section and that old `diagnostics.*` keys from a previous config are ignored without errors.
	- Verify the startup console line includes the version and the GitHub Issues URL.
	- Set `debug: "fine"` (or `"finer"`) and verify cosmetics, Discord, spectator, AutoTrace and replay activity appears in the console; then set it back to `"off"`.
- Race replay (discoverability):
	- Complete a race with `replay.capture-race: true` and verify participants receive the `race.replay.stored` chat notice naming the winner and track.
	- Start a practice run on that track and verify the ghost shows the winner's name with the race record suffix; forfeit-only races must not produce a notice.
- Backward compatibility (critical):
	- Start with a `config.yml` from a previous version (no `cosmetics.*`, `discord.*`, `racing.victory-effects.*` or `racing.spectate-on-finish.*`) and verify defaults are merged without touching existing values.
	- Start with a config that still contains the removed `diagnostics.*` keys and verify they are ignored without errors (the report URL is fixed).
	- Use a `messages_*.yml` from a previous version without the new keys and verify English fallback (no raw keys shown).
	- Delete `player-prefs.yml`, start the server and verify it is created on first change without errors.
	- Delete `cosmetic-unlocks.yml`, start the server and verify it is created on first purchase/grant without errors.
	- Verify existing teams, racers, stats and practice data load unchanged (YAML/SQLite/MySQL).
	- Verify old AABB tracks and their races behave exactly as before.
- Checkpoint alternate routes (A3):
	- Add a checkpoint, select a second region and run `/boatracing setup addalt <#>`; verify the Admin Race checkpoint editor shows `Alternate gates: 1` and the track YAML stores `alternates` inside that checkpoint entry.
	- Drive through the primary gate and through the alternate gate: both must advance the checkpoint, including at full ice-boat speed.
	- Run `/boatracing setup clearalt <#>` and verify `alternates` is removed and only the primary gate works.
	- Reorder and remove checkpoints from the editor and verify alternates stay attached to the correct checkpoint.
	- Verify old tracks without `alternates` load and run unchanged.
- Race replay v1 (B5):
	- With `replay.capture-race: true`, complete a race and verify a fine-level console line about the race replay ghost (set `debug: "fine"`).
	- Start practice on that track with `practice.ghost.enabled: true` and verify the ghost shows the winner's name plus the `race.practice.ghost-suffix-race` suffix.
	- Complete a faster practice run and verify it replaces the race ghost; then run a faster race and verify it replaces the practice ghost.
	- With `replay.capture-race: false`, verify no race ghost is stored and existing ghosts are untouched.
	- Forfeit mid-race, then run several races and verify no errors or memory leaks and that only real finishers can store the ghost.
	- Verify old `practice-ghosts.yml` entries without `source` still load as practice ghosts.
- Versioning:
	- Project version is `26.3` in `pom.xml` and the built jar is `BoatRacing-26.3.jar`.
- Folia (if available): run an AutoTrace recording and verify no scheduler errors.

## What to verify for 26.2.1
- Versioning and docs:
	- Project version is 26.2.1 in `pom.xml`.
	- `CHANGELOG.md` contains a 26.2.1 section with the global race placeholders, translatable status labels, and the 11 new language bundles.
	- `CHECKLIST.md` includes this 26.2.1 validation block.
	- `README.md` status shows 26.2.1, and its language badges/counts and available-language list include all 27 bundles (25 community).
- New community language bundles (`uk`, `id`, `ar`, `nl`, `cs`, `vi`, `th`, `tl`, `da`, `no`, `fi`):
	- Each `messages_<code>.yml` has the same 719 keys and 758 physical lines as `messages_en.yml` (no missing or extra keys).
	- Set `language: "uk"` (repeat for each new code) and run `/boatracing reload`; all user-facing messages load without raw keys.
	- `/boatracing admin language` tab-completion lists the new codes.
	- `placeholder.race-status.*` resolves to the localized labels for each new bundle (for example `uk`: `в процесі` / `реєстрація` / `неактивна`).
	- Removing a new bundle from the data folder falls back to English cleanly.
- Locale validation tool:
	- Run `python tools/check_locales.py` from the repository root and verify it reports `0 error(s)` and no `values identical to English` warnings (only colour/escape warnings are expected).
	- Verify every `messages_*.yml` has the same 758 lines as `messages_en.yml` and the same key order/blank lines (no `line count differs` or `key order differs` warnings).
	- Verify the only accepted identical value is `gui.admin.lore-color` in `es`/`es_419` (`Color` is also Spanish), listed in the checker's `ACCEPTED_IDENTICAL` map.
	- Add a temporary broken copy in a scratch folder (missing key, changed `{placeholder}`, or `general: ""` with child keys) and verify the script reports the failure and exits with code 1.
	- Run `python tools/check_locales.py --langs es,uk,fi` and verify only those bundles are checked.
- Global race placeholders:
	- With no race active on any track, `%boatracing_race_status%` shows the idle label and `%boatracing_race_running%`/`%boatracing_race_registering%` show `false`.
	- Open registration on any track and verify `%boatracing_race_registering%` is `true` and `%boatracing_race_status%` shows the registering label.
	- Start a race on any track and verify `%boatracing_race_running%` is `true` and `%boatracing_race_status%` shows the running label.
	- With one track running and another registering, verify `%boatracing_race_status%` shows the running label (running takes precedence).
	- Verify placeholders are viewer-independent (same value for every player, including when parsed without a player context).
	- Use them in a scoreboard/hologram and verify the value updates when a race opens, starts, or ends without manual refresh.
- Translatable status labels:
	- Set `language: "es"` and run `/boatracing reload`; verify `%boatracing_race_status%` shows `en curso`, `registro`, or `inactiva` depending on the state.
	- Spot-check other bundled languages (`fr`, `de`, `it`, `ja`, `ko`, `pl`, `pt_BR`, `pt_PT`, `ru`, `sv`, `tr`, `zh_CN`, `zh_TW`, `uk`, `id`, `ar`, `nl`, `cs`, `vi`, `th`, `tl`, `da`, `no`, `fi`).
	- Verify a custom `messages_<lang>.yml` without `placeholder.race-status.*` falls back to the English labels.
	- Verify labels are returned without colour codes.

## What to verify for 26.2
- Broadcast mode (`racers`): verify broadcasts only reach participants, except during registration.
- Per-track lobby: set a track-level lobby and confirm it overrides the global lobby.
- Per-track rewards: set track-level rewards and confirm they apply instead of global rewards.
- Admin `-p:` flag: verify `/boatracing stats -p:<player>` from console or admin works correctly.
- Forfeit command: verify action bar is cleared after forfeiting.
- Setup wizard `setlaps`: set laps to a custom value (e.g. 2), verify the wizard prompt shows the correct value and the summary includes it.
- Start lights Shift+Click: Shift+Left Click a Redstone Lamp with wand to add (lamp lights up + green particles), Shift+Right Click to remove (red sparks). Verify wand lore shows instructions. Verify feedback messages and wizard integration.
- Setup summary: verify the final wizard summary shows the correct lap count.
- Restart command: open a race, register, force-start, then `/boatracing race restart` (auto-detect) and `/boatracing race restart <track>`. Verify it stops the race, re-opens, re-joins players, and force-starts cleanly.
- Setup show: run `/boatracing setup show` and verify finish/pit show translated status (yes/no), laps and registration time are displayed with correct values.
- Per-track registration time: run `/boatracing setup wizard`, complete through step 8/8, set a custom registration time. Verify `/boatracing setup show` shows the value. Test that the race registration uses the per-track value.
- Clearlobby command: set a lobby via `/boatracing setup setlobby`, then `/boatracing setup clearlobby`. Verify lobby is disabled in setup show.
- Update checker: run `/boatracing version` and verify update check error messages now appear in console.
- Debug logging: set `debug: "finer"` in config.yml and verify detailed log messages appear in console. Set to `"off"` and verify they disappear. Test `/boatracing reload` also applies the setting.
- Race finish vehicle: disable the lobby, have multiple players in a race. Verify the first finisher dismounts their boat properly.

## What to verify for 1.1.6
- Forfeit command:
	- Join a race and run `/boatracing race forfeit` while racing; verify you receive the forfeited confirmation and are teleported to lobby.
	- While other racers are still running, verify you are removed from the race without stopping it for others.
	- Verify forfeited players appear as `DNF {player} (forfeited)` at the bottom of race results after the race ends.
	- Verify forfeited players do not receive rewards, wins, or position stats.
	- Verify the `race.forfeit-other` message is shown to remaining participants and admins, not globally.
- Practice leave:
	- Start a practice run and disconnect mid-race; verify clean exit with practice session cleanup.
	- Run `/boatracing race practice leave <track>` during active practice; verify practice session exits cleanly and player is returned to lobby.
	- Verify practice leave sends only private messages to the runner.
- Practice ghost replay:
	- Start a solo practice run on a configured track with `practice.ghost.enabled: true`.
	- Complete a full run and verify a ghost entity (boat + rider) appears on the next practice start, following the recorded path.
	- Complete a faster run and verify the ghost updates to the new best path.
	- Verify the ghost entity does not collide with the player and is hidden from other players on the server.
	- Disable ghost via config and verify no ghost spawns on next practice run.
- DocumentStore persistence:
	- With default `database.mode: SQLITE`, create a team, set racer number, and restart the server; verify all data persists.
	- Switch to `database.mode: YAML` and verify teams/stats/practice data persist in traditional YAML files.
	- Verify legacy YAML files (teams.yml, stats.yml, practice-stats.yml) are migrated to the database on first load when switching to SQLite.
- AnvilGUI 26.1/26.2 compatibility:
	- On a 26.2 server, open all Anvil flows (team rename, admin team create, custom laps/pitstops, vote seconds) and verify no crashes.
	- On a 26.1 server, verify the LocalShim fallback works and the close-event compatibility warning appears at most once.
- DNF results:
	- Run a multi-player race where one player forfeits before the end; verify results show finishers with positions and forfeited player(s) listed as DNF at the bottom.
	- Verify forfeited player names and DNF labels are localized across all bundled language files.
- Admin Race GUI checkpoint editor flow:
	- Open `/boatracing admin` -> Race GUI and verify a checkpoint editor entry is available.
	- Open editor on a track with checkpoints and verify each checkpoint item shows region + click instructions.
	- Left-click a checkpoint with a valid selection and verify replacement succeeds.
	- Right-click a checkpoint and verify removal succeeds.
	- Shift-left / shift-right on a checkpoint and verify order changes (move up/down).
	- Use clear checkpoints button and verify all checkpoints are removed.
	- Add one checkpoint from current selection and verify it appears in the editor list.
	- With >45 checkpoints, verify prev/next page navigation and page indicator are correct.
- Admin Race GUI pitstops controls:
	- From Race GUI, click quick pitstop buttons (0/1/2) and verify `race status` reflects the new mandatory pitstops value.
	- Use custom pitstops anvil input and verify valid numeric input applies and persists to `tracks/<track>.yml` (`racing.mandatory-pitstops`).
	- Verify custom laps anvil input still applies and persists to `tracks/<track>.yml` (`racing.laps`).
- Track-session aware edits:
	- Select track A, change laps/pitstops/checkpoints from Admin Race GUI, then open status for track A and verify values are updated immediately.
	- Select track B and confirm track A changes did not leak into track B.
- Edit guards during active race states:
	- While registration is open or race/countdown is active, attempt checkpoint/laps/pitstops edits and verify actions are blocked with race-state feedback.
- i18n regression checks for new GUI actions:
	- In at least `en`, `es`, and one community locale (for example `de` or `zh_CN`), open Admin Race GUI/editor and verify no hardcoded English leaks and no raw message keys are shown.
	- Verify new labels/messages render for checkpoint editor, pitstops controls, checkpoint actions, and pagination texts.
- Setup wand and selection visualizer:
	- Run `/boatracing setup wand` and verify item name/lore render localized text (not raw keys like `setup.wand-name` or `setup.wand-lore-left`).
	- With both selection corners set, verify particle wireframe appears while holding the wand (default `setup.selection-visualizer.show-only-with-wand: true`).
	- Create a large selection and verify wireframe edges do not appear abruptly cut/truncated at random corners.
	- Set `setup.selection-visualizer.view-distance: 0` and verify the full selection wireframe still renders consistently regardless of player distance.
	- Reduce `setup.selection-visualizer.max-particles-per-player` and verify shape remains evenly represented (all edges visible, lower density only).
- Stats tab-complete stability (Paper 1.21+):
	- Type `/boatracing stats ` and wait for player-name suggestions; verify suggestions still include online and cached offline names.
	- While suggestions load, confirm console does not print `Failed to convert json to nbt` or `MalformedJsonException` from DataConverter paths.
- Setup post-creation granular edits:
	- With multiple start slots configured, run `/boatracing setup removestart 2` and verify only slot #2 is removed and remaining slots keep valid order.
	- With start lights configured, run `/boatracing setup removelight 1` and verify only the selected light is removed.
	- Run `/boatracing setup clearfinish` and verify track readiness reports missing finish until set again.
	- Run `/boatracing setup clearpit` and verify default pit is removed without affecting unrelated track data.
- Docs consistency:
	- `README.md` status line shows `Public release (1.1.6)`.
	- `README.md` includes a dedicated `What's New (1.1.6)` section.
	- `CHANGELOG.md` includes a dedicated `1.1.6` section with added/changed/fixed/docs bullets.

- Setup override synchronization checks:
- Per-track setup overrides apply immediately (no stale session values):
	- Select/load a named track and run `/boatracing setup setlaps 3`; verify success feedback.
	- Without server restart and without `/boatracing reload`, run `/boatracing race open <track>` and `/boatracing race status <track>`; verify laps shows `3`.
	- Change again to `/boatracing setup setlaps 5`, re-open registration, and verify race status now reports `5` immediately.
	- Repeat with `/boatracing setup setpitstops <n>` and verify `race status` mandatory pitstops reflects the latest value immediately.
	- Verify `tracks/<track>.yml` persists updated `racing.laps` and `racing.mandatory-pitstops` values after each setup command.
	- Ensure this works even when the track already had an existing race session in memory before running setup commands.
- Lap-scoped track records/placeholders:
	- On one track, run a race with `3` laps and set a baseline best time; verify `%boatracing_track_best_time_<track>%` and `%boatracing_track_top_1_time_<track>%` show that 3-lap result.
	- Change to `2` laps, run a faster absolute time, and verify the 2-lap result is shown while `2` laps are active.
	- Switch back to `3` laps and verify placeholders return to the previous 3-lap record (not the 2-lap time).
	- Verify explicit placeholders by lap return stable values regardless of currently active laps: `%boatracing_track_best_time_laps_<track>_2%`, `%boatracing_track_best_time_laps_<track>_3%`, `%boatracing_track_top_1_time_laps_<track>_3%`.
	- Verify `%boatracing_track_top_2_time_laps_<track>_3%` and `%boatracing_track_top_3_time_laps_<track>_3%` follow the same lap-specific ranking context.
	- Verify stats placeholders by track/laps do not mix contexts: `%boatracing_player_best_race_track_<track>%`, `%boatracing_player_best_race_laps_<track>_2%`, `%boatracing_player_best_race_laps_<track>_3%`, `%boatracing_player_best_lap_laps_<track>_3%`.
	- Verify top stats placeholders by track/laps: `%boatracing_top_player_best_race_name_track_<track>%`, `%boatracing_top_player_best_race_name_laps_<track>_3%`, `%boatracing_top_player_best_lap_name_laps_<track>_3%`.
	- Verify `%boatracing_player_track_best%` and `%boatracing_player_track_best_ms%` also change with lap context instead of showing a mixed global minimum.
	- Open registration after changing laps and verify start-grid ordering prefers same-lap personal bests (with legacy fallback only when lap-specific data does not exist).
- Unsaved track regression check:
	- With no named track selected (`unsaved` flow), run `/boatracing setup setlaps <n>` and `/boatracing setup setpitstops <n>` and verify race open/status behavior remains correct.
- Docs consistency:
	- `README.md` status line shows `Public release (1.1.6)`.
	- `README.md` includes a dedicated `What's New (1.1.6)` section.
	- `CHANGELOG.md` includes a dedicated `1.1.6` section with added/changed/fixed/docs bullets for this release.
- Recent bugfix verifications:
	- Start a solo practice run and forfeit mid-race; verify no `Results:` header or DNF line appears — the session ends cleanly.
	- Start a multiplayer race, have one player forfeit, wait for the remaining racer(s) to finish; verify DNF entries appear correctly at the bottom.
	- Verify practice forfeit still saves no practice stats or ghost data.
	- Set `database.table` in config to a value with special characters (e.g. `test;DROP--`) and restart; verify the plugin sanitizes it and creates a valid table name.
	- Start a practice run with ghosts enabled on a server with a long-running uptime; verify the ghost collision team name generation does not throw on UUID manipulation.
	- Set up a track with checkpoints and a finish line; use a high-speed ice boat to cross a checkpoint at maximum velocity; verify the checkpoint registers correctly (not skipped).
	- Cross the finish line at high speed; verify lap completion registers correctly.
	- Start a practice countdown, then immediately use `/boatracing race practice leave <track>`; verify the countdown cancels and no race starts.

## What to verify for 1.1.5
- Solo practice mode:
	- Permission defaults: `boatracing.race.practice` is granted by default to non-op players.
	- Revoke `boatracing.race.practice` from a test player and verify `/boatracing race practice <track>` is denied with no-permission feedback.
	- Set `racing.min-players-to-start: 2` (or higher) and verify normal `race start/force` still requires the configured minimum.
	- Run `/boatracing race practice <track>` with one online player on a ready track and verify race starts (countdown/lights + boat placement) without requiring a second player.
	- While a practice countdown/run is active on track A, verify `race open/start/force` on track A is blocked.
	- While practice is active on track A, verify race operations and practice can still run on track B independently.
	- Verify practice countdown/split/lap/result messages are visible only to the practicing player (not global chat).
	- During active practice, verify the sidebar shows the localized practice marker from `race.scoreboard.practice-label` (for example `PRACTICE` in `en` and `PRÁCTICA` in `es`).
	- With `racing.lobby.enabled: true`, finish a solo practice run and verify the player is teleported to lobby and receives the clickable `/boatracing race back` hint.
	- From that post-practice lobby state, run `/boatracing race back` and verify return to the original pre-practice location.
	- Verify practice command appears in `/boatracing race help` and tab-completion for users with `boatracing.race.practice`.
	- In a non-English language (for example `es`), run practice on a not-ready track and verify missing requirements are fully localized (no raw `finish` / `at least 1 start slot`).
- Stats command:
	- Permission defaults: `boatracing.stats` is granted by default to non-op players.
	- Permission defaults: `boatracing.stats.others` is granted by default to non-op players.
	- Revoke `boatracing.stats` from a test player and verify `/boatracing stats` is denied with no-permission feedback.
	- Run `/boatracing stats` and verify own report renders (competitive positions summary + practice section with per-track metrics when data exists).
	- In an unsaved practice track context, verify track label uses the localized `stats.track-unsaved-label` wording (for example `pista actual (sin guardar)` in `es`).
	- Verify practice sector output does not spam empty entries (for example `S2=0:00.000`) and only shows meaningful best-sector data.
	- Verify competitive section only lists positions with count > 0 (for example, no line like "second place: 0").
	- Run `/boatracing stats <otherPlayer>` and verify the report targets that player.
	- Revoke `boatracing.stats.others` and verify `/boatracing stats <otherPlayer>` is denied with no-permission feedback.
	- With `boatracing.stats.others` revoked, verify `/boatracing stats` (self) still works.
	- Run `/boatracing stats <unknownName>` and verify the command returns player-not-found feedback.
- Admin language switch command:
	- Permission defaults: `boatracing.admin.language` is `op`.
	- Grant only `boatracing.admin.language` (without `boatracing.admin`) to a test user and verify `/boatracing admin language <code>` works.
	- Verify `/boatracing admin lang <code>` is no longer accepted and returns unknown/usage feedback.
	- Run `/boatracing admin language <code>` and verify `config.yml` `language` value is updated and messages switch immediately (no manual config edit, no separate `/boatracing reload` command).
	- Run `/boatracing admin language <currentCode>` and verify already-set feedback is shown.
	- Run `/boatracing admin language invalid-code!` and verify invalid-code feedback is shown.
	- Run `/boatracing admin language xx` (valid format but missing bundle) and verify language-not-found feedback includes the available language list.
- Practice telemetry and placeholders:
	- After a practice run, verify `plugins/BoatRacing/practice-stats.yml` is created and contains entries under `tracks.<track>.players.<uuid>`.
	- In a new lap, beat a previous section split and verify section feedback marks a new best for that section.
	- Beat a previous lap time and verify lap feedback reports improvement (`-{improve}`), then run a slower lap and verify positive delta vs best.
	- Verify `%boatracing_player_practice_running%` toggles true during practice countdown/run and false after finish.
	- Verify `%boatracing_track_practice_running_<track>%` (and alias `%boatracing_track_practicerunning_<track>%`) reflect same-track practice state.
	- Verify `%boatracing_player_practice_best_run%`, `%boatracing_player_practice_last_run%`, `%boatracing_player_practice_best_lap%`, `%boatracing_player_practice_last_lap%` return expected values.
	- Verify section placeholders `%boatracing_player_practice_best_sector_<section>%` / `%..._last_sector_<section>%` match recorded section splits.
	- Verify track-token variants (for example `%boatracing_player_practice_best_lap_<track>%` and `%boatracing_player_practice_best_sector_<track>_<section>%`) resolve correctly.
- Track record placeholders refresh after improved race time:
	- On the same track, set an initial best (for example `9:33.555`) and verify `%boatracing_track_best_time_<track>%` shows it.
	- Beat that time on a later race and verify `%boatracing_track_best_time_<track>%` updates to the new lower value.
	- Verify `%boatracing_track_best_player_<track>%` and `%boatracing_track_best_time_ms_<track>%` update consistently with the improved result.
	- Verify current-track placeholders `%boatracing_track_best_player%` and `%boatracing_track_best_time%` also reflect the improved record.
	- Verify `%boatracing_track_top_1_time_<track>%` / `%boatracing_track_top_1_time_ms_<track>%` stay aligned with the same updated best time.
	- Confirm values remain correct before/after reloading hologram plugin(s) and `/boatracing reload`.
- Map vote flow improvements:
	- `/boatracing race voteopen [all|<track1> <track2> ...] [seconds]` accepts either explicit track list or `all`.
	- `/boatracing race voteopen [seconds]` (no explicit tracks) opens vote with all saved tracks.
	- Vote-open chat includes both clickable `/boatracing race voteui` and plain typed instruction `/{label} race vote <track>`.
	- On vote timeout, winner is announced and plugin attempts to auto-open winner registration.
	- On manual `/boatracing race voteclose`, winner resolution follows the same auto-open behavior.
	- If winner registration cannot auto-open (track not ready/busy), fallback next-step command `/{label} race open {winner}` is shown only to vote-managing users (`boatracing.race.voteopen`/`boatracing.race.admin`/`boatracing.setup`) and console, not regular players.
	- Tab-complete for `race voteopen` suggests `all` and supports optional trailing seconds.
- Admin Tracks GUI rename flow:
	- On a track item, left-click still loads and shift-right-click still opens delete confirmation.
	- Right-click opens Anvil rename for that track and applies the new name when valid.
	- Track item lore shows all 3 actions (load / rename / delete) in the active language.
- Swedish bundle (`sv`):
	- Set `language: "sv"` and reload; all user-facing messages should resolve from `messages_sv.yml` without raw keys.
	- Verify key 1.1.5 texts in Swedish: practice usage/help, private practice feedback (sector/lap/run), and vote-start instruction line.
	- Confirm placeholders and color codes render correctly in Swedish messages (no broken `{placeholder}` tokens).
- Community locale wording regressions:
	- Set `language: "fr"`, run `/boatracing setup laps 5`, and verify setup feedback preserves `Nombre de tours défini ...` wording.
	- Set `language: "pl"`, run `/boatracing setup laps 5`, and verify setup feedback preserves `Okrążenia ustalone ...` wording.
	- Set `language: "pt_PT"`, run `/boatracing admin help`, and verify tracks help line uses `Gerenciar pistas nomeadas via GUI`.
	- Set `language: "ru"`, trigger admin `setboat` help/usage output, and verify argument label is `тип лодки` (not `BoatType`).
	- Set `language: "zh_CN"`, trigger admin `setboat` help/usage output, and verify argument label is `船只类型` (not `BoatType`).
- Packaging/build output:
	- Run `./mvnw.cmd clean package` and verify there is no `maven-shade-plugin` overlap warning for `META-INF/MANIFEST.MF`.

## What to verify for 1.1.4-26.1-SNAPSHOT (snapshot-26.1-gui-fallback-01)
- Versioning and docs:
	- Project version is `1.1.4-26.1-SNAPSHOT` in `pom.xml`.
	- `pom.xml` contains `boatracing.snapshot.name` set to `snapshot-26.1-gui-fallback-01`.
	- `README.md` shows the `WARNING snapshot` badge and documents the temporary AnvilGUI fallback implementation plus GUI risk note.
	- `README.md` placeholder table marks the new 1.1.4 track record placeholders with `NEW (1.1.4)`.
	- `CHANGELOG.md` contains a dedicated snapshot section for `1.1.4-26.1-SNAPSHOT`.
	- `CHECKLIST.md` includes this snapshot validation block.
- Paper 26.1 Anvil/GUI compatibility:
	- Open Admin GUI anvil flows (`rename team`, `add member`, `set racer number`) and verify there is no `NoSuchMethodException` related to `CraftEventFactory.handleInventoryCloseEvent`.
	- Open Tracks GUI create flow via anvil and verify submit/close works without runtime exceptions.
	- Open Admin Race custom laps anvil flow and verify submit/invalid-input retry closes/reopens correctly.
	- Open Vote GUI custom seconds anvil flow and verify submit/invalid-input retry works without event crash.
	- During first fallback use, server log can show a single warning line from `Wrapper26_R1_Fixed`; repeated GUI operations should not spam warnings.
- Regression sanity:
	- On a stable older server target (for example 1.20.1), open the same anvil flows and verify no behavioral regression.
	- Run one full race cycle (`open -> join -> start/force -> stop`) after anvil usage and verify race state remains stable.

## What to verify for stable 1.1.4 promotion (already implemented in 1.1.4-26.1-SNAPSHOT)
- Versioning and docs:
	- Snapshot line is active while release is postponed: project version remains `1.1.4-26.1-SNAPSHOT` in `pom.xml`.
	- When preparing the stable release, set project version to `1.1.4` in `pom.xml`.
	- Confirm planned 1.1.4 features are already present in the snapshot build before tagging stable.
	- `CHANGELOG.md` contains a 1.1.4 section with configurable minimum-player race start threshold, localized minimum-player message coverage, and consistent enforcement across command/GUI/auto-start paths.
	- `CHECKLIST.md` includes this 1.1.4 validation block.
	- `README.md` states 1.1.4 is postponed/not released and snapshot 26.1 is the active publication channel.
- Minimum players to start:
	- `racing.min-players-to-start` exists in `config.yml` and defaults to `1`.
	- With global `racing.min-players-to-start: 2`, try `race start` and `race force` with only 1 registered online player and verify start is blocked with `race.not-enough-players` (`{min}`/`{current}` rendered).
	- With the same global setting (`2`), let registration timeout auto-start with only 1 registered player and verify race does not start and cancellation feedback uses `race.not-enough-players`.
	- Verify Admin Race GUI start button also respects the same threshold.
	- Add `racing.min-players-to-start: 3` in `tracks/<name>.yml` while global stays `2` and verify per-track override takes precedence.
	- Verify `race.not-enough-players` exists and renders in bundled EN/ES and at least one community locale.
- Track record placeholders by token (NEW 1.1.4):
	- `%boatracing_track_best_player_<track>%`, `%boatracing_track_best_time_<track>%`, and `%boatracing_track_best_time_ms_<track>%` resolve using the requested track token (not only the currently selected track).
	- `%boatracing_track_top_1_player_<track>%`, `%boatracing_track_top_1_time_<track>%`, `%boatracing_track_top_1_time_ms_<track>%` resolve correctly for the fastest record on that track.
	- `%boatracing_track_top_2_player_<track>%`, `%boatracing_track_top_2_time_<track>%`, `%boatracing_track_top_2_time_ms_<track>%` resolve correctly for second place when present.
	- `%boatracing_track_top_3_player_<track>%`, `%boatracing_track_top_3_time_<track>%`, `%boatracing_track_top_3_time_ms_<track>%` resolve correctly for third place when present.
	- For tracks without enough records, text placeholders return `-` and `_ms` placeholders return `-1`.
	- `README.md` shows these rows tagged with `NEW (1.1.4)` in the placeholder table.
- Scoreboard tie-break by checkpoint arrival order (NEW 1.1.4):
	- With 2 racers on the same lap, if racer A reaches `CP 3/5` while racer B is still `CP 2/5`, racer A appears ahead.
	- When racer B later reaches the same `CP 3/5`, racer A must stay ahead (no swap) because racer A entered that checkpoint first.
	- Order only changes when racer B actually advances progression (for example reaches `CP 4/5` or higher race state).
	- `%boatracing_player_current_position%` matches the same ordering shown in the race sidebar.
- Setup Wizard open-registration fallback (FIX):
	- In wizard Done step, when working on in-memory track (no named track selected), clicking `[Open registration]` runs `/boatracing race open unsaved`.
	- Confirm wizard never emits `/boatracing race open <track>` literal token and no `track not found <track>` appears.

## What to verify for 1.1.3
- Versioning and docs:
	- Project version is 1.1.3 in `pom.xml`.
	- `CHANGELOG.md` contains a 1.1.3 section with multi-track race sessions, map-vote commands (`voteopen|vote|voteui|votestatus|voteclose`), clickable vote-start UI prompt behavior, reward command compatibility notes, and boat variant spawn reliability.
	- `CHECKLIST.md` includes this 1.1.3 validation block.
	- `README.md` status shows 1.1.3.
- Track-scoped placeholders:
	- `%boatracing_track_race_running_<track>%`, `%boatracing_track_race_registering_<track>%`, and `%boatracing_track_race_status_<track>%` return expected values per requested track session (not only active-track state).
	- Compatibility aliases `%boatracing_track_racerunning_<track>%` and `%boatracing_track_raceregistering_<track>%` resolve to the same values as canonical placeholders.
	- `%boatracing_track_race_status_<track>%` returns only `running`, `registering`, or `idle`.
	- Track tokens with spaces represented as underscores (for example `My_Track`) resolve correctly.
- Simultaneous races by track:
	- Open registration on two different tracks and verify both sessions stay independent (join/leave/status/start/stop affect only the targeted track).
	- Verify a player cannot join/register in track B while already registered or racing in track A.
	- During parallel races, movement/lap progression and no-dismount lock apply correctly in each player’s own race session.
- Map vote commands:
	- `/boatracing race voteopen [all|<track1> <track2> ...] [seconds]` opens a vote with valid options and broadcast instructions.
	- `/boatracing race voteopen all [seconds]` and `/boatracing race voteopen [seconds]` open a vote with all saved tracks.
	- Vote-open announcement includes both a clickable chat action (`/boatracing race voteui`) and a plain typed-command instruction (`/{label} race vote <track>`).
	- `/boatracing race vote <track>` registers or changes a player vote correctly.
	- `/boatracing race vote` (without `<track>`) opens the vote UI for the player when a vote is active.
	- `/boatracing race voteui` opens the vote UI for the player when a vote is active.
	- `/boatracing race votestatus` shows current counts.
	- When vote ends (`timeout` or `voteclose`), winner resolution attempts to auto-open winner registration; on failure, fallback command hint is shown.
- Rewards command compatibility:
	- With `racing.rewards.enabled: true`, first place executes configured reward commands under `racing.rewards.positions.1.commands`.
	- If a position section uses legacy `command: "..."` (single string), it is still executed.
	- If a position-specific `commands` key is missing, fallback behavior remains safe and does not break reward distribution for that finisher.
- Selected boat variant reliability:
	- Selected per-player boat variants (for example `DARK_OAK_BOAT`, `CHERRY_BOAT`, and `BAMBOO_RAFT` where supported) are applied on spawn and do not fall back to `OAK` unless the server version lacks that variant.
	- Variant remains correct after repeated race cycles (`open/start/stop`) and on both initial mount and delayed mount retries.
- No dismount lock (countdown + race):
	- During the 5-light countdown, racers cannot manually exit boats/rafts.
	- While the race is running, racers cannot manually exit boats/rafts.
	- After race end/cancel/reset cleanup, players are no longer blocked from normal vehicle exit behavior.

## What to verify for 1.1.2
- Versioning and docs:
	- Project version is 1.1.2 in `pom.xml`.
	- `CHANGELOG.md` contains a 1.1.2 section with placeholders, wizard compact text, registration announce i18n-source changes, lobby-back updates, and expanded bundled language coverage.
	- `CHECKLIST.md` includes this 1.1.2 validation block.
	- `README.md` status shows 1.1.2.
	- `README.md` language badges/counts and available language list include both `zh_TW` and `zh_CN`.
- PlaceholderAPI integration:
	- With PlaceholderAPI installed, BoatRacing logs placeholder expansion registration on startup.
	- `%boatracing_player_team_name%`, `%boatracing_player_wins%`, `%boatracing_player_best_race%` resolve correctly for online players.
	- `%boatracing_top_player_wins_name%` and `%boatracing_top_team_wins_name%` resolve without errors.
	- Team lookup placeholders (`%boatracing_team_players_<team>%`, `%boatracing_team_wins_<team>%`) work with names using spaces/underscores.
- Registration announce i18n source:
	- `config.yml` no longer contains `racing.registration-announce`.
	- Registration announce text comes from `messages_<lang>.yml` key `race.registration.announce`.
	- EN/ES/es_419/fr/pt_BR/pt_PT/de/it/pl/tr/ja/ko/zh_TW/zh_CN/ru announce lines include `{cmd}` and render correctly in chat.
	- `language: "zh_TW"` loads Traditional Chinese (Taiwan) messages from `messages_zh_TW.yml`.
	- `language: "zh_CN"` loads Simplified Chinese (Mainland) messages from `messages_zh_CN.yml`.
- Wizard compact text:
	- Wizard step prompts are concise (no long paragraphs) while keeping key action buttons.
	- Navigation row and step status remain clear across EN/ES/es_419/fr/pt_BR/pt_PT/de/it/pl/tr/ja/ko/zh_TW/zh_CN/ru message sets.
	- `README.md` server matrix lists Folia as supported for this jar and clarifies Sponge/Velocity/BungeeCord scope.
- Folia compatibility smoke checks:
	- Open/join/start/force/stop/status race flows run without scheduler/threading errors on Folia.
	- Registration countdown and race timers execute correctly under Folia scheduling.
	- Update checks and periodic tasks run without errors under Folia.
- Platform scope clarity:
	- Documentation clearly states this artifact targets Bukkit-family servers.
	- Documentation clearly states Sponge requires a separate port/jar.
	- Documentation clearly states Velocity/BungeeCord are proxy platforms and cannot execute BoatRacing gameplay logic directly.
- Registration lobby mechanic:
	- With `racing.lobby.enabled: false` (default), registration behavior remains unchanged (no extra teleports).
	- With `racing.lobby.enabled: true`, joining race registration teleports players to the configured lobby zone/location.
	- With `racing.lobby.return-on-leave: true`, leaving registration teleports players back to their pre-lobby location.
	- Cancelling registration (`race stop` or cancellation flow) returns registered players to their pre-lobby location.
	- Starting a race does not return players to pre-lobby; players are placed on start slots as usual.
	- Finishing or cancelling an active race sends participants to the configured lobby location.
	- After race finish/cancel from lobby, players receive a clickable chat hint for `/boatracing race back`.
	- Running or clicking `/boatracing race back` within 3 minutes returns players to their saved pre-lobby location.
	- When the back window expires, players receive an automatic expiration message without needing to execute `/boatracing race back`.
	- After the 3-minute window expires, `/boatracing race back` reports expiration and does not teleport.
	- `racing.lobby.back-window-seconds` changes both the usable back window duration and the automatic expiration timing.
	- While a player is still registered, `/boatracing race back` is denied and instructs the player to leave registration first.
	- If no saved location exists, `/boatracing race back` reports that state cleanly.
	- Back-return locations are in-memory only (not persisted), so after restart/reload there is no old race-back location to restore.
	- `boatracing.race.back` permission exists and defaults to true.
- Scoreboard interoperability with SimpleScore:
	- Documentation explicitly references SimpleScore compatibility and links to:
		- https://github.com/RuiPereiraDev/SimpleScore
		- https://modrinth.com/plugin/simplescore
	- With SimpleScore installed and active, joining/playing a race does not permanently break the external sidebar.
	- During race, BoatRacing and SimpleScore do not enter a sidebar ownership loop.
	- After race stop/cancel/finish, SimpleScore sidebar is restored without requiring player relog.
- Registration timer robustness:
	- Run `race open unsaved`, register players, then use `race force` before the original open countdown ends.
	- After race finish/stop, wait beyond the original registration timeout and verify the race does not auto-restart.
	- Repeated `open -> force/start -> stop` cycles should not leave stale timers or restart loops.
- Race active-track behavior:
	- If the requested race track is already the active one (especially `unsaved`), `race open/join/leave/force/start/stop/status` should not lose in-memory setup state.
	- A track with starts/finish configured in-memory should remain ready when opening registration on that active track.
- Race boat entity cleanup:
	- On race finish, race-spawned boats/rafts are removed from the world (no orphan entities left on start grid/track).
	- On race cancel (`/boatracing race stop <track>` while running), race-spawned boats/rafts are removed from the world.
	- If a player dismounts manually before finish/cancel, their race boat is still removed during race cleanup.
	- Repeating race cycles (`open/start/stop`) does not accumulate stale boats from previous races.
- Setup clickable command UX:
	- Setup wizard buttons that need arguments (e.g., setpos/clearpos/setpit team) should suggest text in chat instead of executing incomplete commands.
	- Buttons for actions like set finish, add light, and add checkpoint are clickable and insert/suggest the correct command flow.

## What to verify for 1.1.0
- Multi-language support:
	- With `language: "en"` (default), all messages appear in English; plugin loads messages_en.yml.
	- With `language: "es"`, all messages appear in Español (España); plugin loads messages_es.yml.
	- With `language: "es_419"`, plugin loads messages_es_419.yml.
	- With `language: "fr"`, all messages appear in French; plugin loads messages_fr.yml.
	- With `language: "pt_BR"`, plugin loads messages_pt_BR.yml.
	- With `language: "pt_PT"`, plugin loads messages_pt_PT.yml.
	- With `language: "de"`, plugin loads messages_de.yml.
	- With `language: "it"`, plugin loads messages_it.yml.
	- With `language: "pl"`, plugin loads messages_pl.yml.
	- With `language: "tr"`, plugin loads messages_tr.yml.
	- With `language: "ja"`, plugin loads messages_ja.yml.
	- With `language: "ko"`, plugin loads messages_ko.yml.
	- With `language: "sv"`, all messages appear in Swedish; plugin loads messages_sv.yml.
	- With `language: "zh_TW"`, all messages appear in Traditional Chinese (Taiwan); plugin loads messages_zh_TW.yml.
	- With `language: "zh_CN"`, all messages appear in Simplified Chinese (Mainland); plugin loads messages_zh_CN.yml.
	- With `language: "ru"`, all messages appear in Russian; plugin loads messages_ru.yml.
	- In `messages_en.yml` and `messages_es.yml`, header comments indicate official translations.
	- In `messages_es_419.yml`, `messages_fr.yml`, `messages_pt_BR.yml`, `messages_pt_PT.yml`, `messages_de.yml`, `messages_it.yml`, `messages_pl.yml`, `messages_tr.yml`, `messages_ja.yml`, `messages_ko.yml`, `messages_sv.yml`, `messages_zh_TW.yml`, `messages_zh_CN.yml`, and `messages_ru.yml`, header comments indicate unofficial community translations and recommend review.
	- Bundled language files exist in the data folder after first run. `/boatracing reload` switches language without restart.
	- Custom language bundles work: set `language: "eo"` (or another code), create `messages_eo.yml` in the `plugins/BoatRacing/lang/` folder, reload, and messages are read from that file.
	- Invalid language values fall back to English.
	- If the configured language file does not exist in plugin folder and is not bundled, the plugin falls back to English cleanly.
	- Setup Wizard navigation/help lines must never show raw keys (e.g. `setup.wizard.nav-label`); labels render localized text correctly.
- Player-controlled race management:
	- With `player-actions.allow-player-race-start: false` (default), only admins can open/start/force/stop races; non-admins get permission denied.
	- With `player-actions.allow-player-race-start: true`, any player can open/start/force/stop races on all tracks.
	- Per-track override: set `racing.allow-player-start: true` in tracks/mytrack.yml to allow player race management only on that track.
	- Admins always bypass the check and can manage races regardless of settings.
- Reward system behavior:
	- With `racing.rewards.enabled: false` (default), no rewards are given; race ends normally.
	- With `racing.rewards.enabled: true`, at race end, rewards are given to racers by position (1st/2nd/3rd/default).
	- Rewards can execute console commands (e.g. give items, add money) with placeholders: {player}, {position}, {time}, {track}, {laps}.
	- Rewards can send player-specific messages and broadcast messages, also supporting placeholders.
	- Custom per-track rewards can be defined (overrides global config for that track).
- PlayerMoveEvent throttle:
	- During a race, checkpoint detection no longer triggers on every sub-meter movement within the same block; only when entering a new block.
	- Performance should improve on high-player-count servers.
- Scoreboard interoperability:
	- With external sidebar plugins (e.g., SimpleScore), BoatRacing race sidebar must not permanently break or replace the external scoreboard.
	- After race stop/cancel/reset, each player gets their previous scoreboard back.
- i18n system:
	- All user-facing text (race, setup, team, admin, plugin messages) is loaded from external files (messages_en.yml or messages_es.yml).
	- `/boatracing reload` reloads messages without restart. New message keys in future versions merge automatically without overwriting custom values.
	- Placeholders in messages work correctly (e.g. {player}, {track}, {laps}).
- Race command flow:
	- `race open/start/force/stop` commands now load the track BEFORE checking permissions, enabling per-track player-start checks.
	- Track existence and validity checks happen first; permission check uses the new canManageRace(Player) helper.
	- Help text shows admin commands only to admins or players with player-start enabled.
- Version and docs:
	- Project version is 1.1.0. CHANGELOG.md has 1.1.0 section. CHECKLIST.md reflects this version.
	- Config includes language setting with clear documentation.

## What to verify for 1.0.9
- Compatibility policy:
	- Server range: 1.19, 1.20.1/1.20.4, 1.21/1.21.1/1.21.11 boot without errors. `/boatracing version` works.
	- Java 17+: starting on Java 17 passes; Java 21 also works. Documented minimum is 17.
- Classification on Paper:
	- On Paper/Purpur, plugin appears under Bukkit/Spigot plugins. No paper-plugin.yml inside the jar.
	- Commands and metadata use Bukkit `getDescription()` (no Paper-only API).
- Boat/Raft materials fix:
	- Team GUI boat picker shows all allowed boats. On 1.21.x, Pale Oak and Rafts render when available; on 1.19, no crashes and no CraftLegacy WARN in console.
	- Spawning and mounting uses dynamic resolution; falls back gracefully when a variant doesn’t exist.
- Updater cadence (regression):
	- Startup: single WARN a few seconds after enable if outdated (config-gated).
	- Runtime detection: background check every 5 minutes; if a new version appears mid-run, print a console WARN immediately (once per version).
	- Hourly reminder: console WARN aligned to 00:00, 01:00, 02:00… while outdated (config-gated).
	- Admin join: always show chat notice to admins (if enabled), no console output on player join.
- Docs:
	- README shows Status 1.0.9 and Requirements: 1.19–1.21.11, Java 17+.
	- CHANGELOG.md has 1.0.9 entry. CHANGELOG.txt present.

## What to verify for 1.0.8
- Permissions and tab-complete:
	- `boatracing.admin` grants every permission via the wildcard `boatracing.*`. Removing `boatracing.admin` should revoke all admin-only actions.
	- Non-admin players can tab-complete `/boatracing race` with `join`, `leave`, and `status`, and see track name suggestions where applicable. Admin-only verbs (`open/start/force/stop`) are suggested only to admins.
	- `/boatracing race status` works for non-admin players (default: true).
- Sidebar leaderboard (top‑10):
	- Order and format: `L <curr>/<total>[  CP <done>/<total>]  -  <Name>` (no centering or padding). If there are no checkpoints, CP is omitted.
	- Visibility toggles in config: `racing.ui.scoreboard.show-position`, `show-lap`, `show-checkpoints`, `show-pitstops`, `show-name` (all default: true).
	- Names are shown verbatim; preserve leading `.` for Bedrock usernames.
 	- When `racing.mandatory-pitstops > 0` and `show-pitstops` is true, a `PIT A/B` segment appears after CP.
- ActionBar HUD:
	- Controlled by config toggles: `racing.ui.actionbar.show-lap`, `show-checkpoints`, `show-pitstops`, `show-time` (all default: true).
 	- When `racing.mandatory-pitstops > 0` and `show-pitstops` is true, a `Pit A/B` segment appears.
- Registration announcement:
	- When registration opens, the global broadcast includes the track name and the exact join command. It uses the template `racing.registration-announce` with placeholders `{track}`, `{laps}`, `{cmd}`.
- Config and docs:
	- Config text is English-only. On reload/update, new keys appear without overwriting customized values.
 	- New keys present: `racing.ui.scoreboard.show-pitstops` and `racing.ui.actionbar.show-pitstops` (default true); `racing.mandatory-pitstops` exists and defaults to 0.
- Release polish:
	- Project version is 1.0.8. Update checks reference Modrinth; a single startup WARN is expected when a newer version exists.

- Results broadcast:
	- At race end, the broadcast lists results by total time and highlights the top‑3 with 🥇/🥈/🥉 and rank colors (gold/silver-ish/bronze-ish). A penalty suffix “(+X penalty)” appears when applicable. Names are rendered safely (strip rank wrappers; preserve leading '.').

- Wizard flow (Pit step):
	- With a default pit already set, invoking any pit action or pressing Next should auto‑advance to Checkpoints. Team-specific pits are optional and must not block progress.

## What to verify for 1.0.6
- Start lights jitter and delay:
	- Configure 5 start lights and set `racing.lights-out-delay-seconds` (e.g., 1.0) and `racing.lights-out-jitter-seconds` (e.g., 0.5–1.5).
	- Start a race and observe after the 5th light: GO occurs after the fixed delay plus a small random jitter (0..value seconds).
- Sidebar leaderboard (top‑10):
	- During a race, the sidebar shows up to 10 positions sorted by: finished (time), lap desc, checkpoint desc, checkpoint-arrival order (first in stays ahead), then total time asc as final fallback.
	- Finished entries show “FINISHED <time>”. Unfinished show “Lap <curr>/<total>  [CP <done>/<total>]”.
	- The entire " - Lap X/Y [CP]" segment is centered; names are left‑aligned with compact/dynamic padding. Truncated names (with “...”) do not add extra padding after the ellipsis.
	- Right‑side vanilla numbers are hidden when supported by the server (Paper 1.20.5+).
- ActionBar HUD:
	- Each player sees “Lap X/Y  CP A/B  Time M:SS.mmm” updating about twice per second.
	- The old per‑player sidebar lines (Lap/CP/Time) are no longer present.
 - Display names:
	- With EssentialsX installed, nicknames (displayName) should be used and common rank wrappers like [Admin]/(Rank) should be stripped from the start. Alignment should remain correct.
- Sector and finish gaps:
	- At each checkpoint, a compact message announces the gap vs sector leader for the current lap (first crosser sets the reference).
	- At lap finish (except final lap), a gap vs lap leader is broadcast.
	- At final finish, a gap vs race winner is broadcast for non‑winners.
- Config defaults:
	- Ensure `racing.lights-out-jitter-seconds` appears in `config.yml` after update/reload (merge without overwriting custom values).

## What to verify for 1.0.5 (quick bugfix validation)
- Team persistence: after restart/update, no team members are lost; loading does not enforce capacity caps on existing teams.
- Setup pit quoting: `/boatracing setup setpit "Team With Spaces"` works; tab‑completion suggests quoted names when starting with a quote.
- Config defaults merge: delete/comment a known key (e.g., `racing.false-start-penalty-seconds`), then `/boatracing reload` — the key should reappear with default value, and existing custom values remain unchanged.
- Boat/raft type: on start, racers are mounted in their selected wood variant (including chest variants). If RAFT types exist in your server build, they are respected; otherwise BOAT types apply. No forced OAK unless fallback is required.



## Prerequisites

- Paper server 1.21.11, Java 21.
- Plugin file: `plugins/BoatRacing.jar` (shaded).
- Two online accounts: Player A and Player B.
- Minimum permission: `boatracing.use` (default: true).
- Admin permission: `boatracing.admin` (default: op). Optional for track/race setup: `boatracing.setup`. For reload: `boatracing.reload`.
- Optional language-switch permission: `boatracing.admin.language` (default: op) for `/boatracing admin language <code>` without full admin management access.
- Extra note: `boatracing.setup` also enables the Tracks GUI.
 - Race permissions by subcommand:
	 - `boatracing.race.join` / `boatracing.race.leave` / `boatracing.race.status` (default: true)
	 - `boatracing.stats` (default: true)
	 - `boatracing.stats.others` (default: true)
	 - `boatracing.race.admin` (open/start/force/stop) (default: op)
- Optional: temporarily OP both players to speed up testing.

## Getting started

1. Copy `BoatRacing.jar` to `plugins/`.
2. Start the server; verify no startup errors.
3. Join with both players.
4. Optional: tweak `max-members-per-team` in `config.yml` to test limits; restart or run `/boatracing reload`.

## Quick UI sanity

- Run `/boatracing teams` as Player A; the main GUI opens.
- Close button works; inventory drag/move is blocked in all plugin GUIs.
- Interface text is English-only; no italics in names or lore.
- Buttons “Admin panel”, “Player view”, and “Refresh” show a short tooltip (lore) on hover.
- Footer bars use darker gray (GRAY_STAINED_GLASS_PANE).
 - Command `/boatracing setup wand` gives the built-in selection tool (Blaze Rod, “BoatRacing Selection Tool”).
- Setup Wizard shows concise, colorized English prompts (green/red states), clickable actions, and selected track name. Navigation uses emojis (⟵ Back, ℹ Status, ✖ Cancel) on every step; a blank line at the top improves readability.
 - Registration broadcasts: when registration opens, the broadcast includes the track name and the exact join command; join/leave are announced server-wide in English.

## Core: create teams and list

- From the main GUI, Player A clicks “Create team”:
	- Opens an Anvil with blank input; enter a valid name (<= 20 chars, letters/numbers/spaces/-/_).
	- Team is created (toast sound) and its view opens.
- Duplicate/state cases:
	- Creating with an existing name → “A team with that name already exists.”
	- Already in a team → “You are already in a team. Leave it first.”
- Team list:
	- After creating a team, it appears immediately in Teams.
	- After deleting a team (Admin), it disappears without restart.
	- Team banner shows name, color, and members with racer numbers; clicking opens the team view.

## Team view (player)

- Header shows team name and members with racer #.
- Back button returns to main GUI and plays `UI_BUTTON_CLICK`.
- Background decoration uses team color.
- Clicking your own head opens “Your profile”.
- No actions on other members (leaderless model).
- If `player-actions.allow-team-rename` or `player-actions.allow-team-color` are `true`, buttons appear:
	- Rename team (Anvil): validates length/characters/duplicates; on success notifies teammates.
	- Change color (DyeColor picker): applies color and notifies teammates.
 - If `player-actions.allow-team-disband` is `true`, a “Disband team” (TNT) appears only to team members:
 	- Clicking opens confirmation “Disband team?”
 	- Confirm dissolves the team; all online members see “Your team was disbanded.”
 	- If the flag is `false`, the button must not appear.

## Join / Leave (with confirmation)

- Player B (teamless) opens the team list and enters Player A’s team view:
	- If the team isn’t full, “Join team” appears.
	- Clicking Join → “You joined <Team>”; teammates are notified; appropriate sound.
- If Player B tries to join while already in a team → denial message.
- Player B clicks “Leave team”:
	- Confirmation menu “Leave team?” opens.
	- Back returns; confirm leaves the team; player gets a message and teammates are notified; plays XP sound.
	- If leaving would make the team empty, the team is auto-deleted; the list updates.
	- Confirm/success messages are English and notify the rest of the team.

## Player profile (per-player settings)

- “Your profile” shows Player name, Team, Racer #, and Boat.

### Racer number (Anvil)
- Valid (1–99) saves and returns with XP sound: “Your racer # set to N.”
- Invalid (non-digits, 0, 100+) shows a message and keeps input.

### Boat selector
- Allows picking any allowed boat (`oak/spruce/birch/jungle/acacia/dark_oak/mangrove/cherry/pale_oak` and chest variants).
- After selecting → “Boat type set to <TYPE>.” and returns to profile.

## Admin-only actions

Admins (`boatracing.admin`) get a dedicated GUI and commands.

### Admin GUI — Teams
- Open: `/boatracing admin`.
- “Teams” view:
	- List existing teams (click to open team view).
	- “Create team” button: Anvil for name; creates team without initial member; opens its view.
	- “Player view” button: quick return to player view.
	- UI open sound present on all Admin GUI screens.
- Team view (Admin):
	- Rename team (Anvil, validates duplicates/regex/length) → success message + `ENTITY_PLAYER_LEVELUP`.
	- Change color (click any `*_DYE` to open `DyeColor` picker) → applies to team, reflected in decoration; notifies members.
	- Add member (by name; requires online player) → notifies player and team.
	- Remove member (click head) → notifies affected and team; sound to Admin.
	- Delete team (confirmation) → deletes team and updates lists.
	- “Refresh” button: reloads current list/view.

### Admin GUI — Players
- “Players” view:
	- Assign team / remove from team (“none”).
	- Set racer number (1–99) using Anvil.
	- Set boat type (same as player selector).
- Notifications and sounds are sent to affected players (both from GUI and equivalent commands).

### Admin GUI — Tracks
- Open: `/boatracing admin` → “Manage tracks” (requires `boatracing.setup`).
- Saved tracks list (MAP icon), marking “(selected)” on the active one.
- Actions:
	- Click: select track (`tracks/<name>.yml`) and apply.
	- Right-click: rename track via Anvil.
	- Shift-right-click: confirmation “Delete track?”; confirm deletes file.
	- Create and select: creates `tracks/<name>.yml`, auto-selects it, and suggests starting the wizard.
	- Reapply selected: reapplies current track.
 - Drag blocking and UI sounds present.

Recommended flow:
- Select or create a track with the Tracks GUI. Configure it with the wizard.

### Admin GUI — Race
- Open from Admin: “Manage race”.
- Status card shows selected track, readiness, laps, registration, and race state.
- Buttons: Open/Close registration, Start, Force start, Stop.
- Laps: quick buttons (1/3/5/10) and custom input (Anvil).
- Registrants list (heads) allows removing a player.

### Admin commands (alternatives)
- Teams:
	- `/boatracing admin team create <name> [color] [firstMember]`
	- `/boatracing admin team delete <name>`
	- `/boatracing admin team rename <old> <new>`
	- `/boatracing admin team color <name> <DyeColor>`
	- `/boatracing admin team add <name> <player>`
	- `/boatracing admin team remove <name> <player>`
- Players:
	- `/boatracing admin player setteam <player> <team|none>`
	- `/boatracing admin player setnumber <player> <1-99>`
	- `/boatracing admin player setboat <player> <BoatType>`
- Language:
	- `/boatracing admin language <code>`

## Chat commands (players)

### Open GUI
- `/boatracing teams` → opens main GUI.

- If you have admin permission, Teams GUI shows an “Admin panel” button that opens the admin panel directly.
- Teams GUI includes a “Refresh” button to reload the list.

### Create / Join / Leave
- `/boatracing teams create <name>`; duplicate/name validation applies; if already in a team, denied.
- `/boatracing teams join <team name>`; denied if full; on success, teammates are notified.
- `/boatracing teams leave` → opens confirmation and performs leave; if the team becomes empty it’s deleted.

### Number and Boat
- `/boatracing teams number <1-99>`; validation errors handled.
 - `/boatracing teams boat <MATERIAL>`; only `*_BOAT` types accepted; others rejected. Chest variants are listed after normal boats.

### Protected actions (Admin only)
- Rename/color/delete are Admin-only; via command use the `boatracing admin ...` namespace above.

## Tab-completion

- `/boatracing` → `teams`, `race`, `stats`, `setup`, `reload`, `version`, `admin` (filtered by permissions; `admin` visible with permission).
- `/boatracing teams` → `create`, `join`, `leave`, `boat`, `number` (and, when applicable, `rename`, `color` for admins only).
- `/boatracing teams color` → list of dye colors.
- `/boatracing teams boat` → list of allowed boats (normal and chest).
- `/boatracing setup setpos` → suggests online player names; for the 2nd arg suggests `auto` and valid slot numbers (1-based).
- `/boatracing setup clearpos` → suggests online player names.
- `/boatracing admin team ...` and `/boatracing admin player ...` → subcommand/parameter completion (team/player names).
- `/boatracing admin language` → suggest available language codes.
- With `boatracing.admin.language` (without `boatracing.admin`), `/boatracing` still suggests `admin`, and `/boatracing admin` suggests `language`.
	- Verify `/boatracing admin` tab-complete does not suggest `lang` anymore.
 - `/boatracing race` → non-admins see `join`, `leave`, and `status`; admins also see `open`, `start`, `force`, `stop`. Track names are suggested where an argument `<track>` is required.

## Persistence and reload

- Files: `teams.yml` and `racers.yml` (team/player data).
- Tracks: per-track files in `tracks/<name>.yml` (managed via Tracks GUI). No central `track.yml`.
- After server restart:
	- Teams, colors, and membership persist.
	- Player boat and racer number persist.
- Legacy migration (when applicable):
	- If `plugins/BoatRacing/track.yml` exists on startup, it’s migrated to `tracks/default.yml` (or `default_N.yml`). Legacy file removal is attempted; in-game notice to admins (`boatracing.setup`).
- Reload (admin):
	- Without `boatracing.reload`, `/boatracing reload` → no permission.
	- With permission, `/boatracing reload` → “Plugin reloaded.” + click sound; no console errors.
	- Change `config.yml` (e.g., max members) → `/boatracing reload` → new limits apply instantly (verify join denial by capacity and button update).
	- Existing data remains after reload; GUIs may need reopening if they were open.

Updated persistence notes:
- `config.yml` no longer contains `teams:` nor messages. Persistent data lives in:
	- `plugins/BoatRacing/teams.yml` → teams { id -> name, color, members }
	- `plugins/BoatRacing/racers.yml` → per-player data (team id, number, boat)
	- `plugins/BoatRacing/tracks/<name>.yml` → per-track configuration (legacy `track.yml` is migrated on startup)

## UI polish and behavior

- No italics; item names/lore use vanilla style.
- Titles: “Teams”, “Team • <name>”, “Your profile”, “Choose team color”, “Choose your boat”, “Delete team?” (Admin), “Remove member?” (Admin), “Leave team?”, “Setup Wizard • Track: <name>”.
- Sounds:
	- Clicks/back: `UI_BUTTON_CLICK`
	- Rename success: `ENTITY_PLAYER_LEVELUP`
	- Join/leave/boat/number success: `ENTITY_EXPERIENCE_ORB_PICKUP` or similar
	- Admin create team / standout actions: `UI_TOAST_CHALLENGE_COMPLETE`
	- Delete team (confirmed): `ENTITY_GENERIC_EXPLODE`
	- Denials/errors: `BLOCK_NOTE_BLOCK_BASS`
- Inventory drag and item movement blocked in all plugin GUIs.
- Open sounds present when entering Admin GUI screens.

## Edge cases and denials

- Trying to create/join while already in a team → denial.
- Trying to join a full team → “This team is full.”
- Anvil validations keep input and play error sound on invalid entries.
- Admin-only actions (rename/color/delete/add/remove/assign) denied to players without permission.
- Admin: adding a player already in the team → proper message; removing a non-member → proper message.
- Admin: creating a team with a duplicate name → rejected with message.

Neutral phrasing:
- If an admin dissolves their own team, the message must not say “by an admin/administrator”. Keep it neutral.

## Optional checks

- Back buttons present and working in: Color picker (Admin), Boat picker, Leave confirm, Delete team confirm (Admin), Remove member confirm (Admin).
- Clicking another member’s head as a player offers no actions (leaderless model).
- After deleting a team as Admin, it disappears from lists and views after reopening.
- After leaving a team that becomes empty, the team is deleted and no longer listed.

## Compatibility notes (short)

- Built-in BoatRacing selection tool (no WorldEdit/FAWE required).
- In-game messaging is English-only. This checklist is now in English.

— Español (resumen 1.0.9) —
- Rango soporte: 1.19–1.21.11; Java 17+.
- Clasificación Bukkit/Spigot en Paper (sin paper-plugin.yml).
- Arreglo barcos/rafts (incluye Bamboo Raft y Pale Oak) sin errores ni avisos CraftLegacy.
- README/CHANGELOG/CHECKLIST actualizados.
 - Penalties: disable via `racing.enable-pit-penalty` and/or `racing.enable-false-start-penalty`. False-start penalty seconds via `racing.false-start-penalty-seconds`. Mandatory pitstops via `racing.mandatory-pitstops` (0 = disabled).

## Track setup (built-in tool) and wizard

Goal: make a functional track with starts and finish (required). Pit and checkpoints are optional. Use the built-in selection tool only.

1) Wand and selection
- Run `/boatracing setup wand` → you should receive the tool (“BoatRacing Selection Tool”).
- With the tool: left click = Corner A; right click = Corner B.
- Verify selection with `/boatracing setup selinfo` → should show min/max and world.

2) Start the wizard
- `/boatracing setup wizard` → shows “1/7 Starts” with clickable actions (e.g., `[Add start]`).
- Per-step navigation: clickable emojis ⟵ Back, ℹ Status, ✖ Cancel.
Clickable verifications (all should paste the command to chat with hover “Click to paste: …”):
- Starts: `[Add start]`, `[Clear starts]`
 - Starts (optional custom positions): `[Set custom slot]` → `/boatracing setup setpos <player> <slot>`, `[Clear custom slot]` → `/boatracing setup clearpos <player>`, `[Auto assign]` → `/boatracing setup setpos <player> auto`. The wizard should also display “Custom slots configured: N”.
- Finish: `[Set finish]`, `[Get wand]`
- Pit area (optional): `[Set pit]` (default pit) or `[Set pit <team>]` for team-specific pits (tab‑complete), `[Get wand]`
 - Quoted names: if a team name contains spaces, set a team-specific pit using quotes, e.g., `/boatracing setup setpit "Toast Peace"`. Tab‑completion should suggest quoted names when the input starts with a quote.
- Checkpoints (optional): `[Add checkpoint]`, `[Get wand]`
- Mandatory pit stops (optional): shows current value and quick options `[0] [1] [2] [3]` (click to paste `/boatracing setup setpitstops <n>`)
- Laps: `[1]` `[3]` `[5]` and `[Finish]`
- Done: `[Open registration]`, `[Setup show]` (no “Start now” from the wizard)

3) Starts (grid slots)
- Stand on the exact block for each slot and run `/boatracing setup addstart` as many times as needed.
- After each `addstart`, the wizard returns to what’s next.
- If you make a mistake, run `/boatracing setup clearstarts`.
 - Alternative: use `[Add start]` and `[Clear starts]` clickable buttons from the wizard message.
 - Optional (custom per-player slots): `/boatracing setup setpos <player> <slot|auto>` to bind a player to a specific start slot (1-based) or remove binding with `auto`/`clearpos`. These bindings take priority at race start.

4) Finish
- Make a cuboid selection around the finish line and run `/boatracing setup setfinish`.
- The wizard advances to the next step.
 - Alternative: click `[Set finish]` (and `[Get wand]` if you need the tool).

5) Pit area (optional)
- Select the pit region and run `/boatracing setup setpit`.
 - Alternative: click `[Set pit]`.

6) Checkpoints (optional)
- For each checkpoint in order, select a region and run `/boatracing setup addcheckpoint` (A, then B, then C, ...).
- If you need to reset, run `/boatracing setup clearcheckpoints`.
 - Alternative: click `[Add checkpoint]`.

7) Review
- Run `/boatracing setup show` to see the summary (includes “Track: <name>` if saved/loaded from Admin Tracks GUI). It now also shows if there are team-specific pits and how many custom start positions exist.
- Optional: use the Tracks GUI to create multiple tracks.
 - After creating a track in the Tracks GUI, a clickable tip should appear to paste `/boatracing setup wizard`.
 - On finishing the wizard, ensure the “Summary” line includes “Custom slots N”.

## Quick race test

- With two players in teams, run `/boatracing race open <track>` (laps come from config/track). Choose the track with the Tracks GUI or pass its name.
- Both run `/boatracing race join <track>`.
 - Confirm that a non-OP player can `join` (default-true permission).
 - Verify global broadcast on join: "<Player> has registered for the race (... total)."

- Run `/boatracing race start <track>` to place players on starts and begin (no laps argument).
- Verify on start each racer is:
	- Placed on a unique start.
	- Facing forward (pitch 0).
	- Mounted in their selected boat/raft type (including chest variants). If RAFT types exist on your Paper build, they should be respected; otherwise BOAT types apply. No forced OAK unless fallback is required.
	- Grid priority respected: players with custom start slot bindings are placed on those slots first; remaining racers are ordered by best recorded time on that track (fastest first); racers without time are placed last.
- Verify that with checkpoints configured, passing them in order and crossing finish counts laps; without checkpoints, crossing finish counts laps directly; race ends after configured laps.

3) Pit penalty and pit-as-finish (optional)
- If a pit area is configured, enter during the race and confirm time penalty per `racing.pit-penalty-seconds`.
 - If start lights are configured and `racing.enable-false-start-penalty` is enabled, moving forward during the countdown applies a false-start penalty per `racing.false-start-penalty-seconds`.
 - Crossing the pit area should also count as finish for lap progression once all lap checkpoints are completed.
 - Mandatory pitstops: if `racing.mandatory-pitstops > 0`, verify HUD segments appear and that players can’t finish until they have completed at least that many pit exits.
 - Finish without checkpoints: attempt to cross finish without all required checkpoints for the lap; expect a clear denial message plus a sound.

4) Results
- At the end, results are announced by total time (elapsed + penalties).

- “force” and “start” use only registered participants; if none are registered, the command must warn.
- `/boatracing race status <track>` shows status, selected track, and counts (starts/finish/pit/checkpoints). Tab-completion suggests track names for race subcommands that require `<track>`.
 - With a non-OP player, `status` must work; `open/start/force/stop` must be denied unless they have `boatracing.race.admin` or `boatracing.setup`.

## Selection diagnostics

- With an active selection, run `/boatracing setup selinfo`.
- It should show min/max and the world of the selection.
- If there’s no valid selection, it should show a clear English message on how to set Corner A/B with the tool.
