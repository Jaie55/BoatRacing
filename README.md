**Important note from the DEV**:

Hey, it's me, Jon! The guy behind Jaie55. This message is for the end user, yeah, you, the one downloading the plugin.

I'm going through a really **really** tough time right now. I got my heart broken back in early June, and because of that, I just don't have the mental energy to keep working on the plugin (or much of anything, honestly). I'm also out of a job and struggling financially. From the time I made this plugin up until today, August 21, 2026, it has only made 4.54 USD, and nobody has tipped me anything on Ko-Fi.
<details>
  
![A picture from revenue panel from Modrinth](https://cdn.modrinth.com/data/cached_images/af8d1ccf9b7039a121fea9e54ccc0eecc22c2917.png)
</details>

As you can probably guess, that doesn't exactly give me the drive or motivation to keep developing it.

I originally made this plugin because I joined a random Minecraft server (PCC Rebuilt) and became friends with the owners.

<details>
  
- Server IP: mc.PocketCreative5.club:25567 (yes, the port included)
- Java Port: 25567
- Bedrock Port: 8027
- Discord Server Invite Link: https://discord.gg/nyc3eqzQ4M
</details>

They needed something that did exactly what this does, so I built it and kept updating it as bugs popped up and they needed new features. It's a super niche plugin, and there's actually a paid one out there that ripped off some of my code, which I'm pretty pissed about.

So, what does this mean for you as a user or server owner? Honestly, not much, since no one reports bugs on our Discord or GitHub anyway. The only real difference is that updating to future Minecraft versions might take longer and also you might have bugs that I haven't found yet. We used to have the plugin ready to go the second a new update dropped, so that might change.

That's about it. 

Thanks to all the server owners for using this plugin, I hope it gave you guys a few good laughs. And remember, if you're going through a rough patch and can't afford professional help, lean on your friends. Cry, vent, try to hang out with them, go grab dinner together, just let it all out. Don't worry about what they'll think, that's what friends are for... <3

[![Built with love](https://forthebadge.com/badges/built-with-love.svg)](https://forthebadge.com)
<!-- Language switcher with flags (hatscripts circle-flags) -->
<p align="right">
	<a href="#en" title="English">
		<img src="https://hatscripts.github.io/circle-flags/flags/gb.svg" width="18" height="18" alt="English" /> English
	</a>
</p>

<a id="en"></a>
# BoatRacing

[![Modrinth](https://img.shields.io/modrinth/v/boatracing?logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/boatracing) [![Downloads](https://img.shields.io/modrinth/dt/boatracing?logo=modrinth&label=Downloads)](https://modrinth.com/plugin/boatracing) [![Minecraft](https://img.shields.io/badge/Minecraft-1.19--26.3-3b82f6)](https://modrinth.com/plugin/boatracing/versions) [![Java](https://img.shields.io/badge/Java-17%2B-22c55e)](https://adoptium.net/) [![Servers](https://img.shields.io/badge/Servers-Bukkit%20%7C%20Spigot%20%7C%20Paper%20%7C%20Purpur-f59e0b)](https://modrinth.com/plugin/boatracing)

[![Compatible with SimpleScore](https://img.shields.io/badge/Compatible%20with-SimpleScore-3fb950)](https://github.com/RuiPereiraDev/SimpleScore) [![Compatible with TAB](https://img.shields.io/badge/Compatible%20with-TAB-3fb950)](https://github.com/NEZNAMY/TAB) [![Compatible with Vault](https://img.shields.io/badge/Compatible%20with-Vault-3fb950)](https://github.com/MilkBowl/Vault)

[![bStats](https://bstats.org/signatures/bukkit/BoatRacing.svg)](https://bstats.org/plugin/bukkit/BoatRacing/26881)

[![Languages](https://img.shields.io/badge/Languages-27-0ea5e9)](#available-languages) [![Official](https://img.shields.io/badge/Official-2-22c55e)](#available-languages) [![Community](https://img.shields.io/badge/Community-25-f59e0b)](#available-languages)

An F1‒style ice boat racing plugin for Bukkit/Spigot (compatible with Paper/Purpur) with a clean, vanilla‒like GUI. Manage teams, build tracks in minutes with AutoTrace and oriented checkpoints, run timed races with pit stops, spectate live, collect and buy cosmetic trails, titles, effects and sounds (optional Vault economy), and report issues easily with the built-in diagnostics command.

> Status: Public release (26.3)
> Author: [Jaie55](https://github.com/Jaie55)
> Contributors: see the [GitHub contributors](https://github.com/Jaie55/BoatRacing/graphs/contributors) list

<a id="snapshot-261-warning"></a>
> [!WARNING]
> Snapshot name: **snapshot-26.1-gui-fallback-01** (historical — no longer needed)
> This snapshot was published for early Paper 26.1 validation.
> **As of 1.1.6, BoatRacing has full support for 26.1 via the feeeedox/AnvilGUI fork.**
> You can safely upgrade to the latest release.

See the changelog in [CHANGELOG.md](https://github.com/Jaie55/BoatRacing/blob/main/CHANGELOG.md).

This is how we test the plugin to validate its behavior after each update: see the QA checklist in [CHECKLIST.md](CHECKLIST.md)

Locale files are validated with `python tools/check_locales.py` (Python 3.8+), which checks key parity, placeholders, duplicated keys, colour/escape differences, and possible untranslated text across every `lang/messages_*.yml`.

<details>
<summary><strong>Table of Contents</strong></summary>

- [Available Languages](#available-languages)
- [Features](#features)
- [Requirements](#requirements)
- [Supported Servers](#supported-servers)
- [Platform Notes](#platform-notes)
- [Install](#install)
- [Usage](#usage)
- [Track Setup](#track-setup)
- [Guided Setup Wizard](#guided-setup-wizard)
- [AutoTrace quick guide](#autotrace-quick-guide)
- [Checkpoint formats](#checkpoint-formats)
- [Racing and Registration](#racing-and-registration)
- [Cosmetics and Titles](#cosmetics-and-titles)
- [Diagnostics and Bug Reporting](#diagnostics-and-bug-reporting)
- [Extension API](#extension-api)
- [Developing Extensions](#developing-extensions)
- [BoatRacing-PartyExtension (private addon)](#boatracing-partyextension-private-addon)
- [Tab Completion](#tab-completion)
- [Admin Commands and GUI](#admin-commands-and-gui)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Updates and Metrics](#updates-and-metrics)
- [Storage](#storage)
- [Compatibility](#compatibility)
- [Placeholders (PlaceholderAPI)](#placeholders-placeholderapi)
- [Notes](#notes)
- [Build (Developers)](#build-developers)
- [License](#license)

</details>

<details>
<summary><strong>What's New (26.3)</strong></summary>

Track onboarding, live viewing, cosmetics, diagnostics and replay release.

Added:
- **AutoTrace — build a track by driving it**: `/boatracing setup autotrace start` records one lap and generates oriented checkpoint gates automatically.
	- Commands: `start`, `stop`, `preview`, `accept`, `cancel`, `status`, `delete <index>` and `resize <index> <width> <height>`.
	- Particle preview before saving; `accept` replaces the active track checkpoints, `cancel` leaves the track untouched.
	- Configurable via `setup.auto-trace.*`: sampling (`sample-ticks`, `min-distance`, `max-samples`), path simplification (`simplify-epsilon`), gate spacing/size (`spacing`, `half-width`, `half-height`), auto-stop (`auto-close-distance`, `auto-close-min-length`), ice re-centering (`recenter-ice`) and preview (`preview`, `preview-period-ticks`, `preview-particle`, `preview-view-distance`).
	- Available as a shortcut in the Setup Wizard CHECKPOINTS step and as a button in the Admin Race checkpoint editor.
- **Oriented gates (`type: plane`)**: checkpoints can be angled rectangles for curved/diagonal sections, with precise segment/plane crossing detection. Legacy axis-aligned checkpoints keep working and old track files load unchanged.
- **Alternate gates**: `/boatracing setup addalt <#>` adds a second gate to a checkpoint (pit lane, bypass...) from your current selection; `/boatracing setup clearalt <#>` removes them. Crossing any gate counts, and alternates stay attached to their checkpoint when you reorder or remove checkpoints.
- **Spectator mode**: `/boatracing race spectate [track]` and `/boatracing race spectate leave` (permission `boatracing.race.spectate`). Optional `racing.spectate-on-finish.mode: free|follow` keeps finishers watching the rest of the race, with `racing.spectate-on-finish.follow-interval-ticks` controlling the follow camera.
- **Victory effects**: `racing.victory-effects.*` adds a screen title, fireworks and a sound for the podium (`top-n`, default 3), each part toggleable.
- **Cosmetics menu** (`/boatracing cosmetics`) with three tabs (Trails, Titles, Victory effects) whose icons mirror your current selection:
	- 24 particle trails including `smoke`, `flame`, `soul`, `cloud`, `spark`, `heart`, `happy`, `witch`, `end_rod`, `totem`, `drip`, `enchant`, `bubble`, `snow`, `lava`, `note`, `portal`, `enchanted`, `damage`, `spore`, `sculk`, `cherry`, `drip_lava` and `ink`, with sellable per-trail permissions `boatracing.cosmetics.trail.<id>`.
	- Admins can disable built-ins (`cosmetics.trails.disabled`) and add or override trails via `cosmetics.trails.custom.<id>` (particle, material, count, spread, extra, permission, enabled).
	- Win-based titles with configurable thresholds (defaults `rookie: 0`, `pro: 5`, `elite: 25`, `legend: 100`); equip any unlocked title from the menu.
	- Selectable victory effects: `default`, `none`, `gold`, `silver`, `bronze`, `rainbow`, `heart`, `soul`, `party`; admins can lock some with `cosmetics.effects.locked` (then they need `boatracing.cosmetics.effect.<id>`).
	- Selectable victory sounds (independent from the fireworks/title): `default`, `none`, `level_up`, `chime`, `bell`, `pling`, `firework`, `dragon`, `thunder`, `wither`, `totem`, `beacon`, `portal`, `anvil`, `victory`; lock them with `cosmetics.victory-sounds.locked` and `boatracing.cosmetics.sound.<id>`.
	- Selectable checkpoint effects (particle + sound when crossing a gate): `none`, `spark`, `flame`, `heart`, `happy`, `soul`, `enchant`, `note`, `portal`, `totem`, `sculk`, `cherry`; lock them with `cosmetics.checkpoints.locked` and `boatracing.cosmetics.checkpoint.<id>`.
	- Selling cosmetics: grant individual permission nodes from economy/shop plugins, or the wildcard `boatracing.cosmetics.unlock.all` to unlock everything at once.
	- Scope switches: `cosmetics.enabled` globally, plus `racing.cosmetics-enabled` per track (`/boatracing setup setcosmetics <true|false>`); trails, victory effect/sound choices and checkpoint effects are skipped on tracks where cosmetics are disabled.
	- The team profile view now shows your title/trail and has a button to open the cosmetics menu.
	- Trails render according to `cosmetics.trails.show-in` (`always`, `race`, `practice`, `race-and-practice`) and `cosmetics.trails.period-ticks`.
	- Preferences (trail, title, victory effect, checkpoint effect) are saved per player in `player-prefs.yml` and titles/trails are exposed via `%boatracing_title%`, `%boatracing_title_id%`, `%boatracing_title_wins%` and `%boatracing_trail%`.
- **Stats GUI**: `/boatracing stats [player]` opens a readable menu with a competitive summary (team, number, boat, title, trail, effect, wins, positions, best race/lap) and a paginated practice breakdown per track; the chat report also lists title and trail, and console keeps the text report.
- **Setup Wizard step titles**: every wizard step now shows an on-screen title/subtitle so admins know exactly which step they are on, plus a completion title.
- **Cosmetics shop and density**: players pick their particle density (low/normal/high) from the new Settings tab, locked cosmetics show a Vault price and a `[Buy]` click, purchases are stored with optional expiry in `cosmetic-unlocks.yml`, and admins manage everything with `/boatracing cosmetics unlock|revoke|unlocks` (single cosmetic, whole category or everything, with `30m|12h|7d|permanent` durations).
- **Version-safe cosmetics**: particle and sound names resolve through version aliases, cosmetics whose particle does not exist on the server version are hidden (`cosmetics.unsupported`), and every cosmetic now has its own icon resolved safely by name.
- **Discord webhook**: post race starts, results and new track records to a channel webhook (`discord.enabled`, `discord.webhook-url`, `discord.username`, `discord.avatar-url` and per-event toggles under `discord.events.*`).
- **Race replay (v1)**: with `replay.capture-race: true`, the winner's run becomes the ghost you race in practice, tagged as a race record; participants get a chat notice when the race ghost is saved.
- **Diagnostics and bug reporting**: `/boatracing debug` is admin-only (`boatracing.debug`, default op) and prints a safe report (plugin, server, API, Java, storage, language, tracks/teams/active sessions) plus the fixed GitHub Issues link. The startup console line also points admins to GitHub Issues.
- **Extension HUD hook**: `HudProvider` lets extensions append sidebar lines and an action bar suffix while BoatRacing keeps owning the race HUD.
- **Base-managed extensions**: BoatRacing loads extension jars from `plugins/BoatRacing/extensions/` (own `extension.yml` descriptor, per-extension config and language folder, BoatRacing-backed storage, scheduler, commands, placeholders) and lists them with `/boatracing extensions`.
- **BoatRacing-PartyExtension (private addon)**: party-style extension loaded by BoatRacing (eight abilities, admin-placed item boxes, party points, HUD and placeholders), living in the untracked `party/` project, commanded through `/boatracing party ...` and built with `scripts/build-all.ps1`.
- **Debug logging**: set `debug: "fine"` (or `"finer"`) to trace AutoTrace, cosmetics, Discord, spectator, victory effects and replay activity.

Changed:
- `/boatracing setup help` and tab-completion now include `autotrace`, `addalt` and `clearalt`; `/boatracing race help` includes `spectate`; the root usage line includes `cosmetics|debug`.
- Checkpoint handling now uses a shared shape abstraction, so oriented gates, axis-aligned gates and groups with alternates coexist in one ordered list.
- `/boatracing debug` is now **admin-only** (`boatracing.debug` default op) and always points to the fixed GitHub Issues URL; the `diagnostics.*` config options were removed so server owners cannot change the report destination.
- The cosmetics Trails/Effects tab icons now mirror the currently equipped trail or victory effect.
- Project version is `26.3` (`pom.xml`) and compatibility is Minecraft 1.19–26.3.

Fixed:
- Spectators are restored to their previous gamemode and location when the race ends, and follow tasks are cancelled on exit, disconnect or kick.
- Race replay capture is stopped and cleared between races, preventing leftover tasks or stale samples.
- AutoTrace cleans up sessions from disconnected players and disposes the preview task when idle.
- Invalid or corrupt entries in `player-prefs.yml` are ignored instead of failing the load.
- Discord webhook failures log a `/boatracing debug` hint instead of a bare error.

Compatibility:
- Fully additive: old `config.yml` files get the new defaults without overwriting values, old `messages_*.yml` bundles fall back to English, old tracks without `type`/`alternates` load as plain checkpoints, and old `practice-ghosts.yml` entries load as practice ghosts. Teams, racers, stats and practice data load unchanged (YAML/SQLite/MySQL).
- Supported servers remain CraftBukkit, Spigot, Paper, Purpur and Folia; Java 17+.

Docs:
- README, CHANGELOG and CHECKLIST updated with every new command, permission, config key, placeholder and a full 26.3 QA pass.

</details>

<details>
<summary><strong>What's New (26.2.1)</strong></summary>

Global race placeholders and translatable status labels:

- **Added**: global race placeholders `%boatracing_race_running%`, `%boatracing_race_registering%`, and `%boatracing_race_status%` resolve across all track sessions, so a single scoreboard/hologram line reacts when any race opens or starts.
- **Added**: `%boatracing_race_status%` labels are translated in `messages_<lang>.yml` under `placeholder.race-status.*` across all 27 bundled languages.
- **Added**: 11 new community language bundles — `uk` (Ukrainian), `id` (Indonesian), `ar` (Arabic), `nl` (Dutch), `cs` (Czech), `vi` (Vietnamese), `th` (Thai), `tl` (Filipino), `da` (Danish), `no` (Norwegian), and `fi` (Finnish) — fully translated with the same keys as English.
- **Docs**: README placeholder tables and CHECKLIST include validation steps for the new global placeholders and language bundles.

</details>

<details>
<summary><strong>What's New (26.2)</strong></summary>

Version numbering aligned with Minecraft's YY.D.H system. Per-track configuration, broadcast mode, and admin targeting release:

Added:
- **New version numbering**: aligned with Minecraft's new `YY.D.H` system (Year.Drop.Hotfix).
- **Per-track broadcast mode**: `racing.broadcast-mode` accepts `global` or `racers`, overridable per track. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Per-track lobby configuration**: lobby settings can now be set per track for dedicated lobbies. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Per-track rewards configuration**: rewards can now be overridden per track. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Admin `-p:` target flag**: admins/console can target other players for commands by appending `-p:<player>`. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Start lights wand UX**: Shift+Left Click lamps to add, Shift+Right Click to remove. Lamp glow + green particles.
- **Restart command**: `/boatracing race restart [track]` stops + re-opens + re-joins + force-starts. Auto-detects track.
- **Per-track registration time**: `/boatracing setup setregtime <seconds>`. Configurable in setup wizard (step 8/8). Reported by [@supershootstudions-lgtm](https://github.com/supershootstudions-lgtm) in [#6](https://github.com/Jaie55/BoatRacing/issues/6).
- **Clearlobby command**: `/boatracing setup clearlobby` to disable the lobby for a track.
- **Debug logging levels**: `debug: "off"/"severe"/"warning"/"info"/"fine"/"finer"/"finest"` in config.yml.

Changed:
- **UpdateChecker**: YY.D.H version comparison support, errors always logged.
- **Wizard LIGHTS step**: translated to all 15 languages.
- **Setup summary**: laps count now shown on wizard completion.

Fixed:
- Action bar now properly cleared after forfeiting a race. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Forfeit command correctly uses resolved player reference instead of raw sender. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Lobby and reward section null-safety, cross-track reward leakage prevented. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Broadcast mode string comparison now uses `.equals()` instead of `==`. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- `-p:` flag no longer interferes with downstream argument length checks. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Setup wizard `setlaps`**: correctly displays and persists custom lap values.
- **Setup show**: untranslated status labels fixed, lap count and registration time displayed.
- **Messages YAML**: indentation fix for all 16 locales.
- **Race finish**: players properly dismount boat when finishing with lobby disabled.

</details>

<details>
<summary><strong>What's New (1.1.6)</strong></summary>

Admin Race GUI workflow, track-session sync, and localization consistency release:

Added:
- Checkpoint editor inside Admin Race GUI with paginated list and click actions: add from selection, replace, remove, move up, and move down.
- Mandatory pit stop controls in Admin Race GUI: quick-set buttons (`0/1/2`) and custom value via anvil input.
- Built-in setup selection visualizer with particles for wand selections, including performance controls in `config.yml` (`setup.selection-visualizer.*`).
- Explicit lap-scoped track placeholders for comparative panels (track + laps), without depending on the currently active lap configuration:
	- `%boatracing_track_best_player_laps_<track>_<laps>%`
	- `%boatracing_track_best_time_laps_<track>_<laps>%`
	- `%boatracing_track_best_time_ms_laps_<track>_<laps>%`
	- `%boatracing_track_top_1|2|3_player_laps_<track>_<laps>%`
	- `%boatracing_track_top_1|2|3_time_laps_<track>_<laps>%`
	- `%boatracing_track_top_1|2|3_time_ms_laps_<track>_<laps>%`
- Stats placeholders now also support track-scoped and lap-scoped best race/lap contexts:
	- Viewer: `%boatracing_player_best_race_track_<track>%`, `%boatracing_player_best_lap_track_<track>%`, `%boatracing_player_best_race_laps_<track>_<laps>%`, `%boatracing_player_best_lap_laps_<track>_<laps>%`
	- Top: `%boatracing_top_player_best_race_name_track_<track>%`, `%boatracing_top_player_best_lap_name_track_<track>%`, `%boatracing_top_player_best_race_name_laps_<track>_<laps>%`, `%boatracing_top_player_best_lap_name_laps_<track>_<laps>%`
- **Forfeit command** (`/boatracing race forfeit`): players can abandon a running race without stopping it for others. Permission: `boatracing.race.forfeit` (default: true). Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#3](https://github.com/Jaie55/BoatRacing/pull/3).
- **Practice ghost replay**: best-run ghosts are captured as sample paths during solo practice and replayed alongside the current run. Ghost entities use no-collision rules, are hidden from other players, and interpolate smoothly between samples with accurate rotation.
- **DocumentStore persistence layer**: pluggable backend (YAML files / SQLite / MySQL) for teams, racers, competitive stats, practice stats, and practice ghosts. Configurable via `config.yml` → `database`.
- **Practice leave flow**: new `/boatracing race practice leave <track>` command and automatic exit on disconnect/quit/kick for practice sessions.
- **AnvilGUI 26.1/26.2 compatibility**: switched to the `feeeedox/AnvilGUI` fork via JitPack, with a local shim for close-event fallback on 26.1 and native 26.2 support via `Wrapper26_R2`.
- **DNF result entries**: forfeited players now appear as "DNF" at the bottom of race results instead of disappearing entirely. Finishers keep normal positions and rewards; forfeited players do not receive rewards or win stats.

Changed:
- Admin Race GUI race/setup actions now resolve and operate on the active track session instead of relying on a single shared runtime manager.
- Track setup overrides changed from Admin Race GUI (laps/pitstops/checkpoints) now persist through track config writes in the same GUI flow.
- Setup commands `/boatracing setup setlaps <n>` and `/boatracing setup setpitstops <n>` now synchronize the currently selected track session in memory when that session already exists.
- Setup command docs now explicitly state that these overrides apply immediately (no restart/reload required).
- Track record storage and placeholders are now lap-aware (`bestTimesByLaps`), so 2-lap bests no longer overwrite or display as 3-lap bests (and vice versa) in `%boatracing_track_best_*%`, `%boatracing_track_top_*%`, and `%boatracing_player_track_best%`.
- Selection visualizer rendering now distributes particle budget across all box edges and culls by selection-box distance, improving shape consistency on large selections.
- Forfeit broadcast scope now uses `raceAudience()` (participants + admins) instead of global `broadcast()` in competitive mode; practice mode shows private messages only to the runner.
- TeamManager, StatsManager, and PracticeStatsManager migrated to use DocumentStore instead of direct file I/O, with automatic legacy YAML-to-database migration on first load.
- **Default storage is now SQLite** (instead of YAML files). The plugin automatically downloads the required JDBC drivers on first startup and stores all persistent data (`teams`, `racers`, `stats`, `practice-stats`, `practice-ghosts`) in a local SQLite database (`boatracing.db`). YAML mode remains available via `database.mode: "YAML"`.
- `config.yml` gained new `database` and `practice.ghost` sections with sensible defaults.
- **Runtime JDBC driver loading**: SQLite and MySQL drivers are downloaded on first use from Maven Central with SHA-256 verification, cached in `plugins/BoatRacing/lib/`, and loaded via an isolated classloader. Falls back to YAML if drivers are unavailable. JAR size stays at ~675 KB.

Fixed:
- `RaceManager` now reloads track data when applying race settings, so `race open <track>` always uses the latest saved `tracks/<track>.yml -> racing.*` overrides even for pre-existing sessions.
- Fixed mismatch where `/boatracing setup setlaps <n>` could confirm one value while `/boatracing race open <track>` still opened registration with an older lap count until restart.
- Fixed equivalent stale-value behavior for per-track `mandatory-pitstops` after `/boatracing setup setpitstops <n>`.
- Setup post-creation editing is now less destructive: you can remove a single start slot/light by index and clear finish/default pit without recreating the whole track setup.
- Removed hardcoded Admin Race GUI user-facing strings and migrated new checkpoint/pitstop/editor texts to message keys.
- Completed localization coverage for the new `gui.race.*` keys across all bundled language files (`messages_*.yml`).
- Stats tab-complete now avoids unsafe offline-name reads during `/boatracing stats <player>` suggestions, preventing repeated Paper 1.21 DataConverter console errors (`Failed to convert json to nbt` / `MalformedJsonException`) on malformed legacy playerdata.
- Fixed setup wand display regression where some servers showed raw message keys (`setup.wand-name`, `setup.wand-lore-left`, `setup.wand-lore-right`) instead of localized text.
- Fixed intermittent wireframe edge truncation in the selection visualizer when particle limits were reached.
- **Scoreboard DataConverter spam fix**: replaced remaining `OfflinePlayer#getName()` calls in the scoreboard timer with a reflection-based safe name resolver, preventing console errors from malformed legacy playerdata on Paper 26.1+.
- **SimpleScore sidebar reclamation fix**: the race scoreboard timer now periodically re-hides SimpleScore during active races and practice sessions, keeping the BoatRacing sidebar visible throughout the session.
- **Practice forfeit UX fix**: forfeiting a practice run no longer shows the `Results:` header and DNF line — the session ends cleanly without competitive result display.
- **Table name sanitization**: the `database.table` config value is validated to prevent accidental or malicious SQL injection through identifier names.
- **Ghost collision safeguards**: UUID-derived collision team names now include bounds protection.
- **Checkpoint/finish fast-crossing detection**: `tickPlayer()` now uses ray-AABB segment intersection so boats moving at high speed (ice boats) no longer skip over checkpoints or the finish line between movement ticks.

Docs:
- README, CHANGELOG, and CHECKLIST now include 1.1.6 release and QA coverage for checkpoint editor, pitstops GUI controls, setup override sync, forfeit, ghost replay, and DocumentStore features.
- Removed 26.1 snapshot warning from README (26.2 is now fully supported).

</details>

<details>
<summary><strong>What's New (1.1.5)</strong></summary>

Practice mode, map vote flow upgrades, admin-track rename support, and track record placeholder refresh fixes:

Added:
- `/boatracing race practice <track>` starts a solo practice race on a ready track, so one player can train even if normal race minimum players is higher.
- `/boatracing stats [player]` shows a compact player stats report (competitive position counts by place, omitting zero-count places, plus per-track practice best/last run/lap/sectors).
- Dedicated permission `boatracing.race.practice` (default `true`) so practice can be granted/revoked independently of race-admin permissions.
- Dedicated permission `boatracing.stats` (default `true`) so player stats can be granted/revoked independently.
- Dedicated permission `boatracing.stats.others` (default `true`) so viewing another player's stats can be granted/revoked independently.
- Dedicated permission `boatracing.race.voteopen` (default `op`) so opening map votes can be granted/revoked independently of race start/stop management.
- `/boatracing admin language <code>` switches the active message bundle at runtime, updates `config.yml`, and reloads messages immediately.
- Dedicated permission `boatracing.admin.language` (default `op`) so language switching can be granted without full admin management access.
- Practice telemetry persisted in `practice-stats.yml` (best/last run, best/last lap, best/last sector per section).
- New practice placeholders for player current-track and explicit track tokens (run/lap/section metrics), plus track-practice state alias `%boatracing_track_practicerunning_<track>%`.
- Bundled Swedish community translation (`messages_sv.yml`, language code `sv`).

Changed:
- Same-track race/practice mutual lock now also applies during pre-start countdown, while other tracks remain independent (you can still practice on track2 if track1 has a race).
- Practice countdown/race split/lap/result messages are private to the practicing player (no global race-style broadcasts).
- During solo practice, the sidebar now shows a localized practice marker (`race.scoreboard.practice-label`), e.g. `PRACTICE` / `PRÁCTICA` depending on configured language.
- Solo practice now captures pre-practice return context, so when the run ends the player is moved to lobby and receives the same clickable `/boatracing race back` hint/window as standard race flows.
- Admin Tracks GUI now supports renaming existing tracks with right-click on a track item (left-click still loads, shift-right-click still deletes).
- Tracks GUI item lore now explicitly shows the rename action (`Right-click`) and this hint is available across bundled language files.
- `/boatracing race voteopen` now supports opening a vote with all saved tracks via `all` (or no explicit track list).
- Vote-start broadcast now includes both clickable UI (`/boatracing race voteui`) and a plain typed command instruction (`/{label} race vote <track>`) for mixed/Bedrock clients.
- When vote ends (timeout or `voteclose`), winner resolution now attempts to auto-open winner registration; if auto-open is not possible, fallback next-step command is sent only to vote-managing users (and console), not to regular players.
- Shade packaging now filters duplicate manifest/signature metadata, removing the previous `maven-shade-plugin` overlap warning for `META-INF/MANIFEST.MF`.
- Admin language switching now uses only `/boatracing admin language <code>`; the short alias `lang` was removed from command handling and tab-completion.

Fixed:
- `%boatracing_track_best_*_<track>%` and `%boatracing_track_top_1..3_*_<track>%` now use the freshest available track data (live race session first, then track file), avoiding stale best-time values.
- `%boatracing_track_best_player%` and `%boatracing_track_best_time%` now follow the same resolution path as token placeholders so current-track records update after an improved race time.
- Track readiness requirement details (`race.track-not-ready`) are now localized through `race.requirements.*` across bundled language files (no mixed EN text in localized messages).
- Community locale wording regressions were corrected in `fr`, `pl`, `pt_PT`, `ru`, and `zh_CN` bundles (including restored `laps-set`/`tracks` phrasing and replacing leaked `BoatType` tokens with localized argument labels).
- Stats chat report readability was improved: noisy sector rows are reduced (no empty `0:00.000` sector values), and unsaved practice track names now use the dedicated localized label `stats.track-unsaved-label` across bundles.

</details>

<details>
<summary><strong>What's New (1.1.4)</strong></summary>

This 1.1.4 release combines everything delivered in `snapshot-26.1-gui-fallback-01` plus the final release additions.

- **Added**: track best-record placeholders by token `%boatracing_track_best_player_<track>%`, `%boatracing_track_best_time_<track>%`, and `%boatracing_track_best_time_ms_<track>%` for per-track record labels.
- **Added**: track top-3 placeholders by token `%boatracing_track_top_1_*_<track>%`, `%boatracing_track_top_2_*_<track>%`, and `%boatracing_track_top_3_*_<track>%` (`player`, `time`, `time_ms`) for podium/leaderboard layouts.
- **Changed**: includes the Paper 26.1 GUI/Anvil reflective compatibility path introduced during the snapshot validation cycle.
- **Changed**: race start respects `racing.min-players-to-start` (global), with optional per-track override in `tracks/<name>.yml` under `racing.min-players-to-start`.
- **Changed**: blocked starts use language key `race.not-enough-players` with `{min}` and `{current}` and enforce the same threshold across `start`, `force`, admin race GUI start, and registration timeout auto-start.
- **Fixed**: scoreboard tie-break by checkpoint arrival; on the same lap/checkpoint, the racer who entered first remains ahead (no equal-checkpoint swap).
- **Fixed**: setup wizard Done step now uses `/boatracing race open unsaved` when no named track is selected, avoiding the invalid `/boatracing race open <track>` placeholder token.
- **Docs**: README documents track record placeholder rows and CHECKLIST includes explicit validation steps.

</details>

<details>
<summary><strong>What's New (1.1.3)</strong></summary>

Track-aware placeholders and spawn reliability improvements:
- **Track-scoped race placeholders**: added `%boatracing_track_race_running_<track>%`, `%boatracing_track_race_registering_<track>%`, and `%boatracing_track_race_status_<track>%` to support per-track displays in scoreboards/holograms.
- **Compatibility aliases for prior naming**: `%boatracing_track_racerunning_<track>%` and `%boatracing_track_raceregistering_<track>%` map to the same values.
- **Parallel race sessions by track**: races are now managed per track session, so different tracks can run registration/races at the same time.
- **Map vote command flow**: added `/boatracing race voteopen`, `vote`, `voteui`, `votestatus`, and `voteclose` to coordinate map selection in chat.
- **Vote-start instructions for mixed clients**: when a map vote opens, players receive both a clickable `/boatracing race voteui` action and a plain typed-command instruction (`/{label} race vote <track>`) for clients where chat click actions are unavailable.
- **Selected boat variant reliability**: race boat/raft variants now re-apply after spawn with delayed retries to reduce cases where selected variants appear as default OAK.
- **No dismount during pre-start and race**: racers are now prevented from manually exiting boats/rafts during the 5-light countdown and while the race is active.
- **Reward command compatibility hardening**: reward parsing now supports both `commands` (list) and legacy `command` (single string), with safer fallback behavior for missing per-position keys (including 1st place).

</details>

<details>
<summary><strong>What's New (1.1.2)</strong></summary>

Placeholders, wizard UX and i18n refinements:
- **PlaceholderAPI integration**: BoatRacing now registers `%boatracing_*%` placeholders for holograms/scoreboards (player/team data, live race values, records, wins and top rankings).
- **Persistent race stats**: new aggregated stats storage (`stats.yml`) for player wins, team wins, best race and best lap.
- **Wizard readability pass**: setup wizard prompts are now more compact and step-focused to reduce chat noise.
- **Registration announce fully i18n-based**: registration broadcast template now lives in `messages_<lang>.yml` (`race.registration.announce`) instead of `config.yml`.
- **Lobby back flow**: added `/boatracing race back`; after race finish/cancel players return to the waiting lobby, get a clickable back hint, and can return to their pre-lobby location within a 3-minute in-memory window.
- **Expanded bundled language coverage**: added and reviewed community bundles for `fr`, `pt_BR`, `pt_PT`, `es_419`, `de`, `it`, `pl`, `tr`, `ja`, and `ko`; Chinese is now split into `zh_TW` (Taiwan, Traditional) and `zh_CN` (Mainland, Simplified).
- **Race boat cleanup reliability**: race-spawned boats/rafts are now tracked and removed on finish/cancel/reset to prevent leftover vehicle entities.

</details>

<details>
<summary><strong>What's New (1.1.1)</strong></summary>

Lobby and stability updates:
- **Optional registration lobby zone**: new `racing.lobby.*` config block. When enabled, players are teleported to a configurable lobby zone/location when they join registration.
- **Quick lobby command**: new `/boatracing setup setlobby` command saves your current position as the registration lobby and enables it automatically.
- **Return to previous location**: with `racing.lobby.return-on-leave: true`, players return to their original location when they leave registration or when registration is cancelled.
- **Active track safety**: race commands no longer force a disk reload when the requested track is already active (notably `unsaved`), avoiding stale in-memory state issues.
- **Track reload consistency**: `TrackConfig.load()` now clears all in-memory collections before reading from disk.
- **SimpleScore compatibility hook**: when SimpleScore is installed, BoatRacing now integrates with its hide/show viewer flow during races to prevent sidebar ownership conflicts and restore the external scoreboard after stop/cancel.
- **Registration restart-loop fix**: fixed a state/timer issue where an old `race open` countdown could survive `start/force/stop` paths and re-trigger race starts unexpectedly.
- **Setup clickable UX**: setup wizard/admin tips now suggest commands in chat when arguments are needed, so players can tab-complete before execution.
- **Lobby messages translated**: lobby teleport/return feedback added to EN/ES/zh_TW/zh_CN/ru message files.

Explicit compatibility:
- **SimpleScore**: BoatRacing includes explicit compatibility with SimpleScore.
- GitHub: https://github.com/RuiPereiraDev/SimpleScore
- Modrinth: https://modrinth.com/plugin/simplescore
- **TAB**: BoatRacing includes explicit compatibility with TAB.
- GitHub: https://github.com/NEZNAMY/TAB
- Modrinth: https://modrinth.com/plugin/tab-was-taken

</details>

<details>
<summary><strong>What's New (1.1.0)</strong></summary>

Languages and player controls:
- **Multi-language support**: messages are now fully translatable. Configure language in config.yml (`language`). Bundled options are `en`, `es`, `es_419`, `fr`, `pt_BR`, `pt_PT`, `de`, `it`, `pl`, `tr`, `ja`, `ko`, `sv`, `zh_TW`, `zh_CN`, and `ru`, and custom bundles are also supported by adding `messages_<lang>.yml` to the plugin folder. Reload with `/boatracing reload` to switch languages without restart.
- **Player-controlled race management**: new config option `player-actions.allow-player-race-start` (default: false) lets non-admin players open, start, force-start and stop races. Can be overridden per-track via `racing.allow-player-start: true` in individual track configs.
- **Reward system**: full customizable race-end rewards. Configure under `racing.rewards` with position-specific commands, messages and broadcasts. Supports placeholders: {player}, {position}, {time}, {track}, {laps}. Per-track rewards override the global config.
- **Performance**: PlayerMoveEvent throttle — checkpoint detection now only triggers when entering a new block, not every sub-meter movement.
- **Complete i18n infrastructure**: all plugin messages (race, setup, team, admin) updated to use the new externalized message system with dynamic placeholder support.

Previous versions:
- **1.0.9**: Compatibility across 1.19–1.21.11; safe boat/raft materials; Bukkit/Spigot classification on Paper.

</details>

<details>
<summary><strong>What's New (1.0.9)</strong></summary>

Compatibility and fixes:
- Official support: Bukkit/Spigot/Paper/Purpur 1.19 → 26.1.1. Requires Java 17+.
- Safer boat types across versions: dynamic Material resolution for boats/rafts (including Bamboo Raft and Pale Oak variants) avoids NoSuchFieldError on older APIs and removes CraftLegacy warnings.
- Classified as a Bukkit/Spigot plugin on Paper (paper-plugin.yml excluded from the JAR). Paper-only APIs replaced with Bukkit-safe calls.
- Docs: README, CHANGELOG and QA checklist updated (EN/ES).
 
Updater cadence:
- Background checks still run every 5 minutes. When a new version is first detected during runtime, a console WARN is printed immediately (once per version).
- Hourly reminder aligned to the top of each hour (00:00, 01:00, …) while outdated (respects `updates.console-warn`).
- Admin join: always notifies in chat (if enabled), never prints to console on player join.

</details>

<details>
<summary><strong>What's New (1.0.8)</strong></summary>

Improvements and toggles:
 - Customizable HUD: new config flags to show/hide parts of the sidebar and ActionBar.
	 - `racing.ui.scoreboard.show-position|show-lap|show-checkpoints|show-pitstops|show-name`
	 - `racing.ui.actionbar.show-lap|show-checkpoints|show-pitstops|show-time`
 - Pitstops on HUD: when `racing.mandatory-pitstops > 0`, show “PIT A/B” on the sidebar and “Pit A/B” in the ActionBar (gated by the toggles above).
 - Registration broadcast now includes the track name and the exact join command (language key `race.registration.announce` in `messages_<lang>.yml`).
 - Sidebar order switched to “L/CP - Name”; removed centering/padding; names shown as-is (keeps leading '.' for Bedrock).
 - Finish attempt message: crossing the finish line without all required checkpoints now shows a clear player message (in addition to the denial sound).
 - Setup Wizard: new optional step “Mandatory pit stops” with quick buttons [0] [1] [2] [3].
 - Setup command: `/boatracing setup setpitstops <n>` sets and persists `racing.mandatory-pitstops`.
 - Results broadcast: podium medals 🥇/🥈/🥉 and rank colors for top‑3; keeps a penalty suffix when present; names rendered safely (keeps leading '.' for Bedrock).
 - Wizard flow: if a default pit is already set in step 4, the wizard automatically advances to Checkpoints (team pits remain optional).
 - Permissions: introduced wildcard `boatracing.*`. Admins still get absolutely all plugin permissions, now by explicit children under `boatracing.admin` to avoid circular inheritance.
 - Tab-complete: players (non-admin) see `join|leave|status|vote|voteui|votestatus` under `/boatracing race`; admin-only verbs (`open|start|force|stop|voteopen|voteclose`) are suggested only to admins.
 
Updater:
 - Console notice restored: a single WARN shortly after startup when outdated, plus an hourly reminder while still outdated (respects `updates.console-warn`).
 - Admin join: when an admin joins, a quick check runs (throttled) and notifies them within seconds if a new update was just published.

</details>

<details>
<summary><strong>What's New (1.0.7)</strong></summary>

Bugfixes and quality-of-life:
 - Console update check noise removed: only a single WARN shortly after startup when you are outdated (respecting `updates.console-warn`). Periodic 5‑minute checks remain but are silent.
 
 - Stability: network errors during update checks are logged at most once per server run.
 
 Removal:
 - The built‑in hiding of vanilla scoreboard numbers has been removed. If you want to hide the sidebar’s right‑side numbers, use an external plugin for now while a future built‑in approach is evaluated.
 
 UI:
 - Scoreboard redesigned: centered rows, compact “Name - L X/Y CP A/B” layout, rank colors (1=gold, 2=silver-ish, 3=bronze-ish), and your own name in green.

</details>

<details>
<summary><strong>What's New (1.0.6)</strong></summary>

Improvements and tweaks:
 - New sidebar leaderboard: the sidebar now shows the top‑10 positions in real time. Personal stats moved to the ActionBar.
 - Personal HUD: your Lap, CP and Elapsed Time now appear in the ActionBar, updated every 0.5s.
 - Sector and finish gaps: compact messages show your time gap vs the lap/finish leader at each checkpoint and at lap finish (and vs winner at race finish).
 - Start lights jitter: optional random jitter added to the lights‑out delay via `racing.lights-out-jitter-seconds`.
 - Live leaderboard: sidebar shows the top‑10 positions; your Lap/CP/Time are shown in the ActionBar (auto‑created on race start and cleaned up on stop/reset).
 - Vanilla numbers hidden: the sidebar’s right‑side numbers are hidden natively when supported by your server (Paper 1.20.5+); no TAB plugin required.
 - Layout polish: names are left‑aligned and the whole " - Lap X/Y [CP]" block is centered. Removed the decorative separator and arrow prefix, compact/dynamic padding based on the longest visible name. Long names are truncated with "..." and no extra padding is added after the ellipsis. Your own name is highlighted in green (no bold).
 - FIN label: standardized to “FINISHED”.
 - Display names: supports EssentialsX displayName and strips common rank wrappers like [Admin]/(Rank) at the start for cleaner alignment.

</details>

<details>
<summary><strong>What's New (1.0.5)</strong></summary>

Fixes and polish:
 - Team member persistence: team members are preserved across updates/reloads/startup; loading restores teams without re‑applying capacity limits.
 - Setup pit command: `/boatracing setup setpit [team]` accepts team names with spaces when quoted (e.g., "/boatracing setup setpit \"Toast Peace\""); tab‑completion suggests quoted names when the input starts with a quote.
 - Config defaults: on plugin update or `/boatracing reload`, new default keys are merged into your existing `config.yml` without overwriting your changes.
 - Boat/Raft type: racers are mounted in their selected wood variant (including chest variants and rafts) instead of always OAK; works across API versions with a safe fallback.

</details>

<details>
<summary><strong>What's New (1.0.4)</strong></summary>

- Team-specific pit areas: new unified command `/boatracing setup setpit [team]` sets the default pit when no team is provided, or the pit for a specific team when a team name is given. Tab‑completion suggests team names.
- Mandatory pitstops: new `racing.mandatory-pitstops` config (default 0). When > 0, racers must complete at least that many pit exits before they are allowed to finish; pitstops are counted on exiting the pit area and persist for the whole race.
- Wizard: Pit step updated to mention default pit vs per‑team pits and to guide the flow with clickable tips.
 - Config updates: on plugin updates/reloads, new `config.yml` keys are merged into your existing file without overwriting your changes.
 - Boat type: racers are mounted in their selected boat/raft wood variant (including chest variants) instead of always OAK; works across API versions with a safe fallback.
- Permissions: players can use `join`, `leave`, and `status` by default; only `open|start|force|stop` remain admin‑only. Removed extra runtime checks that could block players with permissive defaults.
- Boats: spawned boats now respect the player’s selected wood type robustly across API versions; falls back to OAK if the enum value is not available.
- Per‑player start slots and grid ordering: new setup commands `/boatracing setup setpos <player> <slot|auto>` and `/boatracing setup clearpos <player>`. On race start, players bound to a slot are placed there first; remaining racers are ordered by their best recorded race time on that track (fastest first), and racers without a time are placed last.
- Setup show: now also displays the presence of team‑specific pits and the number of custom start positions configured.
 - Wizard (Starts): shows optional buttons for per‑player custom slots — setpos/clearpos/auto — and displays the number of custom slots configured.

</details>

<details>
<summary><strong>What's New (1.0.3)</strong></summary>

- Admin Tracks GUI: manage multiple named tracks — Create and select, Delete (with confirmation), and Reapply selected. Requires `boatracing.setup`.
- Admin Race GUI: manage race lifecycle from a GUI — open/close registration, start/force/stop, quick-set laps, remove registrants, and handy setup tips.
- Terminology: “loaded” → “selected”; “pit lane” → “pit area”.
- All track configuration lives per‑track under `plugins/BoatRacing/tracks/<name>.yml`.
	- On startup, a legacy `track.yml` (if present) is migrated automatically to `tracks/default.yml` (with in‑game admin notice).
- Setup Wizard UX: concise, colorized, clickable. Adds a Laps step and an explicit Finish button; navigation buttons now use emojis (⟵, ℹ, ✖) and the blank line is placed at the top of the block for readability.
- Selection tool: built‑in wand (Blaze Rod). Left‑click = Corner A, right‑click = Corner B. Richer `/boatracing setup selinfo` diagnostics.
- Race commands now require a track argument: `open|join|leave|force|start|stop|status <track>`.
- Race lifecycle: “race stop” cancels registration and any running race for that track. Starts enforce unique grid slots, face forward (pitch 0), and auto‑mount racers into their selected boat. “force” and “start” use only registered participants.
- Tab‑completion: for race subcommands that take `<track>` (including `status`), it suggests existing track names.
- Admin Tracks GUI: after creating a track, sends a clickable tip to paste `/boatracing setup wizard` in chat.
- Messages remain English‑only; denial texts are hardcoded.

- Start lights + false starts: configure exactly 5 Redstone Lamps and enjoy an F1-style left-to-right light-up countdown (no redstone wiring needed). Moving forward during the countdown (false start) applies a configurable time penalty.
 - Race permissions: split by subcommand. Players can `join`, `leave`, and `status` by default; admin actions `open|start|force|stop` require `boatracing.race.admin` (or `boatracing.setup`).
- Pit area and checkpoints are now optional. Track readiness only requires at least one start slot and a finish line; the wizard labels Pit area and Checkpoints as “(optional)” and lets you skip them.
- Removed “Save as…” from the Tracks GUI (create/select, delete, and reapply remain).
 - New: live in‑race scoreboard per participant showing Lap, Checkpoints, and Elapsed Time.
 - New: crossing the pit area counts as finish for lap counting once all lap checkpoints are completed (still applies pit penalty when enabled).

</details>

## Available Languages

Official translations: <img src="https://hatscripts.github.io/circle-flags/flags/gb.svg" width="16" height="16" alt="English" /> <img src="https://hatscripts.github.io/circle-flags/flags/es.svg" width="16" height="16" alt="Espanol" />
[![en official](https://img.shields.io/badge/en-official-22c55e)](#available-languages) [![es official](https://img.shields.io/badge/es-official-22c55e)](#available-languages)

Community translations: <img src="https://hatscripts.github.io/circle-flags/flags/fr.svg" width="16" height="16" alt="French" /> <img src="https://hatscripts.github.io/circle-flags/flags/br.svg" width="16" height="16" alt="Portuguese Brazil" /> <img src="https://hatscripts.github.io/circle-flags/flags/pt.svg" width="16" height="16" alt="Portuguese Portugal" /> <img src="https://hatscripts.github.io/circle-flags/flags/mx.svg" width="16" height="16" alt="Spanish Latin America" /> <img src="https://hatscripts.github.io/circle-flags/flags/de.svg" width="16" height="16" alt="German" /> <img src="https://hatscripts.github.io/circle-flags/flags/it.svg" width="16" height="16" alt="Italian" /> <img src="https://hatscripts.github.io/circle-flags/flags/pl.svg" width="16" height="16" alt="Polish" /> <img src="https://hatscripts.github.io/circle-flags/flags/tr.svg" width="16" height="16" alt="Turkish" /> <img src="https://hatscripts.github.io/circle-flags/flags/jp.svg" width="16" height="16" alt="Japanese" /> <img src="https://hatscripts.github.io/circle-flags/flags/kr.svg" width="16" height="16" alt="Korean" /> <img src="https://hatscripts.github.io/circle-flags/flags/se.svg" width="16" height="16" alt="Swedish" /> <img src="https://hatscripts.github.io/circle-flags/flags/tw.svg" width="16" height="16" alt="Chinese (Taiwan, Traditional)" /> <img src="https://hatscripts.github.io/circle-flags/flags/cn.svg" width="16" height="16" alt="Chinese (Mainland, Simplified)" /> <img src="https://hatscripts.github.io/circle-flags/flags/ru.svg" width="16" height="16" alt="Russian" /> <img src="https://hatscripts.github.io/circle-flags/flags/ua.svg" width="16" height="16" alt="Ukrainian" /> <img src="https://hatscripts.github.io/circle-flags/flags/id.svg" width="16" height="16" alt="Indonesian" /> <img src="https://hatscripts.github.io/circle-flags/flags/sa.svg" width="16" height="16" alt="Arabic" /> <img src="https://hatscripts.github.io/circle-flags/flags/nl.svg" width="16" height="16" alt="Dutch" /> <img src="https://hatscripts.github.io/circle-flags/flags/cz.svg" width="16" height="16" alt="Czech" /> <img src="https://hatscripts.github.io/circle-flags/flags/vn.svg" width="16" height="16" alt="Vietnamese" /> <img src="https://hatscripts.github.io/circle-flags/flags/th.svg" width="16" height="16" alt="Thai" /> <img src="https://hatscripts.github.io/circle-flags/flags/ph.svg" width="16" height="16" alt="Filipino" /> <img src="https://hatscripts.github.io/circle-flags/flags/dk.svg" width="16" height="16" alt="Danish" /> <img src="https://hatscripts.github.io/circle-flags/flags/no.svg" width="16" height="16" alt="Norwegian" /> <img src="https://hatscripts.github.io/circle-flags/flags/fi.svg" width="16" height="16" alt="Finnish" />
[![fr community](https://img.shields.io/badge/fr-community-f59e0b)](#available-languages) [![pt_BR community](https://img.shields.io/badge/pt_BR-community-f59e0b)](#available-languages) [![pt_PT community](https://img.shields.io/badge/pt_PT-community-f59e0b)](#available-languages) [![es_419 community](https://img.shields.io/badge/es_419-community-f59e0b)](#available-languages) [![de community](https://img.shields.io/badge/de-community-f59e0b)](#available-languages) [![it community](https://img.shields.io/badge/it-community-f59e0b)](#available-languages) [![pl community](https://img.shields.io/badge/pl-community-f59e0b)](#available-languages) [![tr community](https://img.shields.io/badge/tr-community-f59e0b)](#available-languages) [![ja community](https://img.shields.io/badge/ja-community-f59e0b)](#available-languages) [![ko community](https://img.shields.io/badge/ko-community-f59e0b)](#available-languages) [![sv community](https://img.shields.io/badge/sv-community-f59e0b)](#available-languages) [![zh_TW community](https://img.shields.io/badge/zh_TW-community-f59e0b)](#available-languages) [![zh_CN community](https://img.shields.io/badge/zh_CN-community-f59e0b)](#available-languages) [![ru community](https://img.shields.io/badge/ru-community-f59e0b)](#available-languages) [![uk community](https://img.shields.io/badge/uk-community-f59e0b)](#available-languages) [![id community](https://img.shields.io/badge/id-community-f59e0b)](#available-languages) [![ar community](https://img.shields.io/badge/ar-community-f59e0b)](#available-languages) [![nl community](https://img.shields.io/badge/nl-community-f59e0b)](#available-languages) [![cs community](https://img.shields.io/badge/cs-community-f59e0b)](#available-languages) [![vi community](https://img.shields.io/badge/vi-community-f59e0b)](#available-languages) [![th community](https://img.shields.io/badge/th-community-f59e0b)](#available-languages) [![tl community](https://img.shields.io/badge/tl-community-f59e0b)](#available-languages) [![da community](https://img.shields.io/badge/da-community-f59e0b)](#available-languages) [![no community](https://img.shields.io/badge/no-community-f59e0b)](#available-languages) [![fi community](https://img.shields.io/badge/fi-community-f59e0b)](#available-languages)

All 27 bundles are complete for 26.3 (`python tools/check_locales.py`: 26 OK, 0 errors); community translations can still be improved via PRs.

Available codes and names:
- `en` English (official)
- `es` Español (España) (official)
- `es_419` Español (Latinoamerica)
- `fr` Francais
- `pt_BR` Portugues (Brasil)
- `pt_PT` Portugues (Portugal)
- `de` Deutsch
- `it` Italiano
- `pl` Polski
- `tr` Turkce
- `ja` Japanese
- `ko` Korean
- `sv` Svenska
- `zh_TW` Chinese (Taiwan, Traditional)
- `zh_CN` Chinese (Mainland, Simplified)
- `ru` Russian
- `uk` Ukrainian
- `id` Bahasa Indonesia
- `ar` Arabic
- `nl` Nederlands
- `cs` Čeština
- `vi` Tiếng Việt
- `th` ไทย
- `tl` Filipino
- `da` Dansk
- `no` Norsk
- `fi` Suomi

## Features
- Team GUI for players: browse teams, open team view, join/leave, manage your racer number and boat type, and optionally rename/change color/disband from the GUI when enabled in config.
- Admin tooling: dedicated GUIs for teams, players, race control, and named tracks, plus command equivalents for scripting or console-style workflows.
- Named tracks: each track lives in its own YAML file and can override core race settings such as laps, mandatory pit stops, registration time, penalties, and player race-start permissions.
- Built-in setup flow: Blaze Rod selection wand, cuboid region tools, AutoTrace (drive a lap to auto-generate oriented checkpoint gates), clickable setup tips, and a compact guided wizard with auto-advance where possible.
- Grid control: custom per-player start slots, auto placement for the rest, and fallback ordering by best recorded track time.
- Race systems: ordered checkpoints (axis-aligned or oriented, with optional alternate gates), optional pit area, optional mandatory pit stops, false-start penalties, registration lobby teleport/return, 5-light countdown, live results broadcasting, forfeit command with DNF display, and practice ghost replay.
- Race replay (v1): the race winner's run is stored as the track ghost (`replay.capture-race`) and can be raced in practice, tagged as a race record.
- Spectator mode: watch running races manually or automatically after finishing (free/follow camera), with victory effects (title, fireworks, sound) for the podium.
- Cosmetics and progression: 24 particle trails (admin-extensible and sellable), win-based titles, selectable victory effects, victory sounds and checkpoint effects, a Vault-powered shop with player-selectable particle density, managed from an in-game menu and persisted per player.
- Team profile and stats: your team profile shows your title/trail and opens the cosmetics menu; `/boatracing stats [player]` opens a readable stats GUI with title, trail and checkpoint effect.
- Multi-track race orchestration: independent race sessions per track and map-vote commands for admins/players in chat.
- HUD and scoreboard: in-race sidebar plus ActionBar with per-section config toggles, safe scoreboard restoration after races, and compatibility flow for external scoreboards.
- Persistent stats: `stats.yml` stores wins, best race and best lap so PlaceholderAPI, holograms, scoreboards, and NPCs can show live and historical data.
- i18n: bundled message packs for `en`, `es`, `es_419`, `fr`, `pt_BR`, `pt_PT`, `de`, `it`, `pl`, `tr`, `ja`, `ko`, `sv`, `zh_TW`, `zh_CN`, `ru`, `uk`, `id`, `ar`, `nl`, `cs`, `vi`, `th`, `tl`, `da`, `no`, and `fi`; `en`/`es` are official and the rest are community translations. All are hot-reloadable with `/boatracing reload`.
- Rewards, updates, metrics and integrations: per-position race rewards, Modrinth update checks, optional bStats metrics, and Discord webhook notifications for race start, results and track records.
- Diagnostics: `/boatracing debug` prints a safe environment report plus the GitHub/Discord bug report links, and the startup console line tells server owners where to report issues.
- Extension API: separate plugins can subscribe to race events and read live race state (see [API.md](API.md)); the base works unchanged without them.

## Requirements
- Java 17+
- A Bukkit-compatible server running Minecraft 1.19 to 26.3
- Optional: [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) for `%boatracing_*%` placeholders
- Optional: [Vault](https://github.com/MilkBowl/Vault) + an economy plugin for the cosmetic shop

## Supported Servers
- CraftBukkit
- Spigot
- Paper
- Purpur
- Folia

Other Bukkit-compatible forks may work, but they are not officially verified in this repository.

## Platform Notes
- Single plugin jar for the Bukkit family. `plugin.yml` targets the Bukkit API and declares Folia support via `folia-supported: true`.
- Optional soft dependencies: PlaceholderAPI (placeholders) and Vault (cosmetic shop economy).
- Proxies such as Velocity and BungeeCord are not execution targets for BoatRacing gameplay logic.
- Sponge and Forge hybrid servers such as Mohist, Magma, or Arclight are not supported by this jar.

## Install
1. Download the latest BoatRacing jar from Modrinth: https://modrinth.com/plugin/boatracing
2. Put it in your `plugins/` folder.
3. Start the server once to generate config, messages, teams, racers, stats, and track files.
4. Optionally install PlaceholderAPI for `%boatracing_*%` placeholders and Vault for the cosmetic shop.

## Usage
Quick flow:
1. Run `/boatracing teams` and create or join a team.
2. Set your racer number and boat type.
3. Create or select a track from the Admin Tracks GUI, or start directly with the setup wand on the active track.
4. Configure starts, finish, lights, and optional pit/checkpoints.
5. Open registration with `/boatracing race open <track>`.
6. Let players join, then start or force-start the race.
7. Optional: players can pick a trail, title or victory effect with `/boatracing cosmetics`, check their stats with `/boatracing stats`, and admins can use `/boatracing debug` when reporting issues.

Root command groups:
- `/boatracing teams` opens the player team GUI.
- `/boatracing race ...` manages registration and race lifecycle.
- `/boatracing stats [player]` opens a stats GUI (summary + paginated practice stats); console receives the text report.
- `/boatracing setup ...` configures the active track.
- `/boatracing admin` opens the admin GUI, and also exposes admin subcommands such as live language switching.
- `/boatracing cosmetics` opens the cosmetics menu.
- `/boatracing cosmetics density <low|normal|high>` sets your particle density.
- `/boatracing cosmetics unlock|revoke|unlocks ...` (admins) manages cosmetic unlocks, categories and durations.
- `/boatracing reload` reloads config, messages, teams, racers, and stats.
- `/boatracing version` shows plugin version and update status.
- `/boatracing debug` (admins) prints a diagnostic report with the fixed GitHub Issues link to report bugs.

## Track Setup
Use the built-in BoatRacing selection wand to define cuboid regions.

- Left-click a block with the wand: set Corner A.
- Right-click a block with the wand: set Corner B.
- Wand item: Blaze Rod named `BoatRacing Selection Tool`.

Setup commands:
- `/boatracing setup help` shows the setup command list.
- `/boatracing setup wand` gives the selection wand.
- `/boatracing setup addstart` adds your current position as a start slot.
- `/boatracing setup clearstarts` removes all start slots.
- `/boatracing setup removestart <slot>` removes one start slot by number (1-based).
- `/boatracing setup setfinish` saves the current selection as the finish region.
- `/boatracing setup clearfinish` removes the finish region.
- `/boatracing setup setpit [team]` saves the current selection as the default pit or a team-specific pit.
- `/boatracing setup clearpit` removes the default pit region.
- `/boatracing setup addcheckpoint` appends a checkpoint in order.
- `/boatracing setup clearcheckpoints` removes all checkpoints.
- `/boatracing setup addalt <#>` adds an alternate gate to a checkpoint from the current selection (crossing either gate advances it).
- `/boatracing setup clearalt <#>` removes the alternate gates from a checkpoint.
- `/boatracing setup autotrace start` records one driven lap and generates oriented checkpoint gates automatically.
- `/boatracing setup autotrace stop` stops recording and generates the gates from the recorded path.
- `/boatracing setup autotrace preview` toggles the particle preview of the generated gates.
- `/boatracing setup autotrace accept` saves the generated gates to the active track (replaces existing checkpoints).
- `/boatracing setup autotrace cancel` discards the current AutoTrace session.
- `/boatracing setup autotrace status` shows samples, gates, path length and preview state.
- `/boatracing setup autotrace delete <index>` removes one generated gate (1-based).
- `/boatracing setup autotrace resize <index> <width> <height>` resizes one generated gate (full block size).
- `/boatracing setup autotrace resize selected <width> <height>` resizes every wand-selected gate at once.
- `/boatracing setup autotrace delete selected` deletes every wand-selected gate.
- `/boatracing setup autotrace help` shows a 6-step guide with clickable buttons.
- `/boatracing setup addlight` adds the Redstone Lamp you are looking at as a start light.
- `/boatracing setup removelight <index>` removes one start light by number (1-based).
- `/boatracing setup clearlights` removes all start lights.
- `/boatracing setup setlaps <n>` saves the lap count for the active track and applies it immediately (no restart/reload required).
- `/boatracing setup setpitstops <n>` saves mandatory pit stops for the active track and applies them immediately (no restart/reload required).
- `/boatracing setup setcosmetics <true|false>` enables or disables cosmetics for the active track (applies immediately).
- `/boatracing setup setlobby` stores your current location as the registration lobby and enables it.
- `/boatracing setup setpos <player> <slot|auto>` binds a player to a custom start slot or clears the binding with `auto`.
- `/boatracing setup clearpos <player>` removes a custom start slot binding.
- `/boatracing setup show` prints a summary of starts, lights, finish, pit, team pits, checkpoints, custom slots, pit stops, and active per-track overrides.
- `/boatracing setup selinfo` prints selection debug information.

Setup rules:
- A track is race-ready when it has at least one start slot and a finish region.
- Pit area is optional.
- Checkpoints are optional.
- Start lights are optional for track validity, but if used the system expects exactly 5.
- Team-specific pit regions are supported alongside the default pit region.
- Checkpoints can be axis-aligned regions (legacy `type: aabb`) or oriented gates (`type: plane`), both in the same ordered list.
- Any checkpoint can have alternate gates (`alternates` in its entry); crossing any of them counts.
- Existing tracks with AABB checkpoints keep working unchanged; entries without `type` are loaded as AABB.

### Checkpoint formats
Checkpoints are stored in order inside `tracks/<name>.yml`. Legacy axis-aligned regions (default, no `type` needed):

```yaml
checkpoints:
  - world: world
    minX: 130.0
    minY: 62.0
    minZ: 310.0
    maxX: 138.0
    maxY: 72.0
    maxZ: 312.0
```

Oriented gates (AutoTrace output or manual):

```yaml
checkpoints:
  - type: plane
    world: world
    center: 134.500,64.000,311.000
    normal: 0.000,0.000,-1.000
    right: 1.000,0.000,0.000
    up: 0.000,1.000,0.000
    halfWidth: 4.5
    halfHeight: 3.0
```

A checkpoint with an alternate gate:

```yaml
checkpoints:
  - type: plane
    world: world
    center: 134.500,64.000,311.000
    normal: 0.000,0.000,-1.000
    right: 1.000,0.000,0.000
    up: 0.000,1.000,0.000
    halfWidth: 4.5
    halfHeight: 3.0
    alternates:
      - world: world
        minX: 120.0
        minY: 62.0
        minZ: 300.0
        maxX: 124.0
        maxY: 72.0
        maxZ: 302.0
```

- `center`, `normal`, `right` and `up` accept `x,y,z` strings, YAML lists or the Bukkit vector format.
- Files without `type` are loaded as axis-aligned regions and are only rewritten when you edit the track.
- `alternates` may contain any mix of axis-aligned and oriented gates.

### Guided Setup Wizard
Start it with `/boatracing setup wizard`.

Wizard flow:
- Starts
- Finish
- Start lights
- Pit area
- Checkpoints
- Mandatory pit stops
- Laps

Wizard behavior:
- The wizard is chat-driven and uses clickable buttons for the next action, and shows an on-screen title/subtitle for every step so admins always know where they are.
- It auto-advances when a step becomes valid.
- Optional steps support skip.
- Sub-actions like `back`, `status`, `cancel`, `skip`, `next`, and `finish` exist, but are intentionally not shown in tab-completion to keep the entrypoint simple.
- The Starts step also exposes quick buttons for custom start-slot management.

### AutoTrace quick guide
1. Stand on the track (a boat works best) and run `/boatracing setup autotrace start`. You get an on-screen `AUTOTRACE` title, the exact start coordinates, a particle marker at the start, an action bar with time/samples/distance and a periodic chat reminder with your points. Clickable `[Help] [Stop] [Preview] [Accept] [Cancel]` buttons are shown in chat.
2. Drive one clean lap. It stops automatically within `auto-close-distance` blocks of the start, or press `[Stop]` to finish anywhere.
3. Review the generated gates with `/boatracing setup autotrace preview`. Hold the selection wand while previewing: **left-click a gate to select it** (it turns green) and **right-click to deselect**. Use `resize selected <width> <height>` or `delete selected` to edit every selected gate at once.
4. Save with `/boatracing setup autotrace accept` (replaces the track checkpoints) or discard with `/boatracing setup autotrace cancel`. After saving you automatically get the selection wand (if you did not have one) plus clickable `[Setup wizard]`, `[Open registration]`, `[Get wand]`, `[Add start]` and `[Set finish]` helpers.
5. `/boatracing setup autotrace help` prints the full 6-step guide at any time.

AutoTrace settings live under `setup.auto-trace.*` in `config.yml` (sampling, simplification, gate size, auto-stop, ice re-centering and preview).

## Racing and Registration
Race commands:
- `/boatracing race help`
- `/boatracing race open <track>`
- `/boatracing race join <track>`
- `/boatracing race leave <track>`
- `/boatracing race back`
- `/boatracing race start <track>`
- `/boatracing race force <track>`
- `/boatracing race restart [track]`
- `/boatracing race practice <track>`
- `/boatracing race practice leave <track>`
- `/boatracing race forfeit`
- `/boatracing race stop <track>`
- `/boatracing race status <track>`
- `/boatracing race voteopen [all|<track1> <track2> ...] [seconds]`
- `/boatracing race vote <track>`
- `/boatracing race voteui`
- `/boatracing race votestatus`
- `/boatracing race voteclose`
- `/boatracing race spectate [leave|<track>]`

Stats command:
- `/boatracing stats` opens your stats GUI (competitive summary + paginated practice page).
- `/boatracing stats <player>` opens another player's stats GUI (requires `boatracing.stats.others`).
- Console and targeted senders get the text report (`/boatracing stats -p:<player>`).

Race behavior:
- Players must belong to a team before joining registration.
- Registration loads track settings before opening, including any per-track `racing.*` overrides.
- Multiple race sessions can run in parallel on different tracks.
- A player can only be registered/racing in one session at a time.
- Tab-complete includes `vote`, `voteui`, and `votestatus` for players, plus `voteopen` and `voteclose` for admins.
- `/boatracing race vote` without `<track>` opens the vote UI when a map vote is active (same behavior as `/boatracing race voteui`).
- `/boatracing race voteopen all [seconds]` (or `/boatracing race voteopen [seconds]`) opens a vote with all saved tracks.
- Opening a vote with `/boatracing race voteopen ...` requires `boatracing.race.voteopen` (also granted by `boatracing.race.admin` or `boatracing.setup`).
- `/boatracing race voteopen ...` announces both a clickable vote UI action (`/boatracing race voteui`) and a plain vote command instruction (`/{label} race vote <track>`).
- When map voting ends, the winning track registration is opened automatically (if the track is ready and not busy), so players immediately receive the normal join command announcement.
- `start` and `force` use registered players only.
- `start`, `force`, GUI start, and registration timeout auto-start require at least `racing.min-players-to-start` online registered players.
- `practice` starts a solo race directly on the selected track (ready track required), without waiting for registration/min-player checks.
- The minimum start threshold defaults to `1`, can be overridden per track via `tracks/<name>.yml` (`racing.min-players-to-start`), and shows `race.not-enough-players` when not met.
- Grid order is: custom start slot bindings first, then best recorded track times, then players without a recorded time.
- Boats are spawned using each player’s selected boat type with cross-version-safe material resolution.
- If checkpoints exist, a lap only counts once all checkpoints have been collected in order.
- Pit entry can apply a time penalty and pit exits count toward mandatory pit-stop requirements.
- False starts can apply an additional penalty during the light countdown.
- If registration lobby is enabled, joining registration teleports players there.
- Leaving registration or registration cancellation can return players to their previous location (`racing.lobby.return-on-leave`).
- After a race finish, race cancel, or solo practice finish, participants are returned to the lobby and receive a clickable `/boatracing race back` hint in chat.
- The saved pre-lobby return location is kept in memory for 3 minutes; after that, `/boatracing race back` expires for that race cycle.
- Results are sorted by elapsed time plus penalties and broadcast to online players.
- The winner updates persistent player/team stats used by placeholders.
- Spectators can watch with `/boatracing race spectate [track]` during a race and leave with `/boatracing race spectate leave`; gamemode and previous location are restored on exit.
- When `racing.spectate-on-finish.mode` is `free` or `follow`, finishers stay spectating until the race ends instead of being sent to the lobby.
- The first finishers receive victory effects (screen title, fireworks, sound) when `racing.victory-effects.enabled` is on.
- When Discord integration is enabled, race starts, results and track records are posted to the configured webhook.
- With `replay.capture-race: true`, the winner's run is stored as the track ghost for practice and shown as a race record; a chat notice tells participants the race ghost was saved.

Player-managed races:
- By default, race control is admin-only.
- Non-admin players can also open/start/force/stop races when `player-actions.allow-player-race-start: true` is enabled globally.
- A track can override that via `tracks/<name>.yml` with `racing.allow-player-start: true|false`.

## Cosmetics and Titles
Open the menu with `/boatracing cosmetics` (permission `boatracing.cosmetics`, default true). The menu has five cosmetic tabs (Trails, Titles, Victory effects, Victory sounds, Checkpoint effects) plus a Settings tab; each tab icon mirrors your current selection.

Trails:
- 24 built-in particle trails: `smoke`, `flame`, `soul`, `cloud`, `spark`, `heart`, `happy`, `witch`, `end_rod`, `totem`, `drip`, `enchant`, `bubble`, `snow`, `lava`, `note`, `portal`, `enchanted`, `damage`, `spore`, `sculk`, `cherry`, `drip_lava`, `ink`.
- Each trail requires `boatracing.cosmetics.trail.<id>` (not granted by `boatracing.*`), so they can be sold with ranks or shops.
- Rendering is controlled by `cosmetics.trails.enabled`, `cosmetics.trails.period-ticks` and `cosmetics.trails.show-in` (`always`, `race`, `practice`, `race-and-practice`).
- Admins can disable built-ins with `cosmetics.trails.disabled` and add or override trails with `cosmetics.trails.custom.<id>` (`particle`, `material`, `count`, `spread`, `extra`, `permission`, `enabled`).

Titles:
- Unlocked by wins using `cosmetics.titles.thresholds` (defaults: `rookie: 0`, `pro: 5`, `elite: 25`, `legend: 100`); a purchase or permission also unlocks them.
- Equip any unlocked title from the menu; without a selection the highest unlocked one is used automatically.
- Display them with `%boatracing_title%` (localized name), `%boatracing_title_id%` and `%boatracing_title_wins%`.

Victory effects:
- Pick which podium effect plays for you: `default`, `none`, `gold`, `silver`, `bronze`, `rainbow`, `heart`, `soul` or `party`.
- Toggle the whole feature with `cosmetics.effects.enabled`.

Victory sounds:
- Pick the sound played when you finish on the podium (independently of the fireworks/title): `default` (the effect's sound), `none` (muted), `level_up`, `chime`, `bell`, `pling`, `firework`, `dragon`, `thunder`, `wither`, `totem`, `beacon`, `portal`, `anvil` or `victory`.
- Toggle with `cosmetics.victory-sounds.enabled`.

Checkpoint effects:
- Pick a particle + sound that plays when you cross a checkpoint: `none`, `spark`, `flame`, `heart`, `happy`, `soul`, `enchant`, `note`, `portal`, `totem`, `sculk` or `cherry`.
- Toggle with `cosmetics.checkpoints.enabled`.

Particle density (player setting):
- The Settings tab lets players choose `low`, `normal` or `high` particle density (defaults 1/2/4 particles per sample), applied to trails, checkpoint effects and victory fireworks.
- Command alternative: `/boatracing cosmetics density <low|normal|high>`.
- Admins control it with `cosmetics.density.*` (`enabled`, `default`, `low`, `normal`, `high`, `max-victory-rockets`).

Version compatibility:
- Particle and sound names are resolved through version aliases, so renamed particles (`VILLAGER_HAPPY`, `ENCHANTMENT_TABLE`, `CRIT_MAGIC`, ...) keep working on every supported version.
- Cosmetics whose particle does not exist on the running server version are hidden from the menu and rendering (`cosmetics.unsupported: "hide"`); set it to `"fallback"` to show them with a safe fallback particle instead.

Shopping and unlocking:
- The shop supports Vault economies: locked items show a price and a `[Buy]` click when `cosmetics.shop.enabled: true`.
- Prices live under `cosmetics.shop.prices.<category>.<id>` (0 = free, unset = permissions only); `gated-by-default: true` means every cosmetic needs a permission, a purchase or a free price.
- Admins can also grant unlocks without Vault:
  - `/boatracing cosmetics unlock <player> <trail|title|effect|sound|checkpoint> <id> [30m|12h|7d]`
  - `/boatracing cosmetics unlock <player> <category> * [duration]` (whole category)
  - `/boatracing cosmetics unlock <player> all [duration]` (everything, including future cosmetics)
  - `/boatracing cosmetics revoke <player> <category> <id|*>`, `/boatracing cosmetics revoke <player> all`
  - `/boatracing cosmetics unlocks <player>` lists active grants and their expiry.
- Permissions still work: individual nodes like `boatracing.cosmetics.trail.flame` or the wildcard `boatracing.cosmetics.unlock.all` (default false; it is **not** granted by `boatracing.*`, only by `boatracing.admin`).

Scope switches:
- `cosmetics.enabled` is the master switch for all cosmetics.
- Per track, `racing.cosmetics-enabled: false` (or `/boatracing setup setcosmetics false`) disables trails, victory effect/sound choices and checkpoint effects for sessions on that track.

Team profile:
- `/boatracing teams` → your profile shows your title and trail and has a Cosmetics button that opens the cosmetics menu.

Stats GUI:
- `/boatracing stats` opens a readable menu (summary + a paginated Practice page); `/boatracing stats <player>` shows another player's menu.
- The summary and the chat report include the player's title, trail and checkpoint effect.
- Console still receives the text report when using `/boatracing stats -p:<player>`.

Preferences (trail, title, victory effect, victory sound, checkpoint effect, density) are stored per player in `plugins/BoatRacing/player-prefs.yml`; purchases are stored in `plugins/BoatRacing/cosmetic-unlocks.yml`.

## Extension API
BoatRacing exposes a small, stable extension API so separate plugins (for example a paid abilities/minigames addon) can hook into races without touching internals. The base plugin behaves exactly the same with or without extensions installed.

- Entry point: `es.jaie55.boatracing.api.BoatRacingProvider` + `BoatRacingAPI`, registered through Bukkit's ServicesManager.
- Events: race open, join, leave, start, checkpoint, lap complete, pit stop, finish, stop, forfeit, and practice start/finish.
- Live read-only views: `RaceSessionView` and `PlayerRaceView` (lap, checkpoint, position, elapsed/finish time, status, participants, audience).
- Safety: events run on the main thread inside a try/catch, so a broken extension never breaks a race; `apiVersion()` guards compatibility.
- HUD hook: `HudProvider` lets extensions append sidebar lines and an action bar suffix while the base plugin keeps owning the race HUD (`registerHudProvider`/`unregisterHudProvider`).
- Toggles: `api.enabled` (default true) and `api.log-extensions` (logs installed plugins that depend on BoatRacing).

### Base-managed extensions
Besides regular plugins that use the API, BoatRacing can load extension jars itself:

- Drop `*.jar` files in `plugins/BoatRacing/extensions/`; each jar needs an `extension.yml` descriptor (`name`, `main`, `version`, `api-version`) and a main class implementing `es.jaie55.boatracing.api.extension.BoatRacingExtension`.
- Extensions are **not** Bukkit plugins: BoatRacing creates `plugins/BoatRacing/extensions/<name>/` with the bundled `config.yml` and a `lang/` folder holding the `messages_<language>.yml` bundles, and provides storage, scheduler, listeners, HUD lines, placeholders and commands through `ExtensionContext`.
- Commands registered by an extension live under the normal root (for example `/boatracing party ...`) and appear in tab completion.
- Extension data is stored through the BoatRacing storage backend, so `database.mode` (YAML/SQLite/MySQL) applies to it.
- Manage them with `/boatracing extensions` (permission `boatracing.extensions`, default op) and `/boatracing extensions reload`; `/boatracing debug` lists them.
- Full guide with examples: [API.md](API.md). The step-by-step guide is in [Developing Extensions](#developing-extensions) below.

## Developing Extensions

There are two supported ways to build on top of BoatRacing:

1. **Base-managed extension** (recommended for addons): a jar BoatRacing loads itself from
   `plugins/BoatRacing/extensions/`. It is not a Bukkit plugin; BoatRacing owns its config,
   language files, storage, scheduler, commands, HUD lines and placeholders.
2. **External plugin**: a normal Bukkit plugin with `depend: [BoatRacing]` that reads the API
   through `BoatRacingProvider`. Use this when the addon needs its own plugin lifecycle, commands
   or dependencies.

Both models use the same public contract: only the `es.jaie55.boatracing.api` package (and its
`.event` and `.extension` subpackages) may be used; everything else is internal and can change.
`BoatRacingAPI.API_VERSION` is only bumped on breaking changes.

### 1. Set up your project

Add the Paper API and the BoatRacing jar (as `provided`) to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>papermc-repo</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.19.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>es.jaie55</groupId>
        <artifactId>boatracing</artifactId>
        <version>26.3</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

The BoatRacing artifact is installed into your local Maven repository with
`.\mvnw.cmd -q -DskipTests clean install` (or `scripts/build-all.ps1`). Use `release 17` for
compilation.

### 2. Write a base-managed extension

**Descriptor** `extension.yml` at the root of your jar:

```yaml
name: MyExtension
main: com.example.MyExtension
version: '26.3'
api-version: 1
description: What your extension does.
authors: [YourName]
```

- `name` becomes the folder `plugins/BoatRacing/extensions/MyExtension/`.
- `version` should match the BoatRacing version you built against (a mismatch only logs a warning).
- `api-version` must be `<=` `BoatRacingAPI.API_VERSION`, otherwise the jar is skipped with a warning.
- `main` must implement `BoatRacingExtension` with a public no-argument constructor.
- `config.yml` and every `lang/messages_*.yml` bundled in the jar are copied to the extension folder on first load.

**Main class** and lifecycle:

```java
package com.example;

import es.jaie55.boatracing.api.extension.BoatRacingExtension;
import es.jaie55.boatracing.api.extension.ExtensionContext;

public final class MyExtension implements BoatRacingExtension {

    private ExtensionContext context;

    @Override
    public void onEnable(ExtensionContext context) {
        this.context = context;

        context.registerListener(new MyListener(this));
        context.registerCommand(new MyCommand(this));
        context.registerHudProvider(viewer -> java.util.List.of("&dMy points: &f" + points(viewer)));
        context.registerPlaceholder("my_points",
                player -> player == null ? "" : String.valueOf(points(player)));

        context.scheduler().runTimer(this::tick, 20L, 20L);
        context.logger().info("MyExtension enabled.");
    }

    @Override
    public void onDisable() {
        // Cancel tasks if needed, remove your entities and save state (storage is managed by BoatRacing).
    }

    @Override
    public void onReload() {
        // Called after /boatracing reload or /boatracing extensions reload.
        // config() and messages are already refreshed at this point.
    }
}
```

**What `ExtensionContext` gives you:**

| Service | Usage |
|---|---|
| `config()` / `reloadConfig()` | `config.yml` from your folder with the jar defaults merged in. |
| `message(key, ...)` / `messageList(key)` / `language()` | `lang/messages_<language>.yml` with English fallback; the language follows the base `language` setting. |
| `storage()` | `read(document)` / `write(document, content)` through BoatRacing, so `database.mode` (YAML/SQLite/MySQL) applies to your data. |
| `scheduler()` | `runNow`, `runLater`, `runTimer`, `runAsync`, `runAsyncTimer` (Folia-aware), returning a cancellable handle. |
| `api()` | The regular `BoatRacingAPI` (events, live views, HUD registration). |
| `registerCommand(...)` | Subcommand under `/boatracing <name> ...`. |
| `registerListener(...)` | Bukkit listener registered by BoatRacing. |
| `registerHudProvider(...)` | Extra sidebar lines / action bar suffix during races. |
| `registerPlaceholder(...)` | A `%boatracing_<identifier>%` value for PlaceholderAPI. |

**Commands** implement `ExtensionCommand`; `args` are the arguments after your subcommand name:

```java
public final class MyCommand implements ExtensionCommand {

    @Override
    public String name() { return "mymode"; } // /boatracing mymode ...

    @Override
    public String permission() { return "boatracing.mymode.use"; }

    @Override
    public boolean allowConsole() { return true; }

    @Override
    public String usage() { return "&cUsage: /{label} mymode start|stop"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("mymode " + String.join(" ", args));
        return true; // false prints usage() instead
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? List.of("start", "stop") : List.of();
    }
}
```

**Events and live views** (same objects as the external plugin API):

```java
@EventHandler
public void onCheckpoint(CheckpointReachedEvent event) {
    Player player = event.player();
    if (player == null) return;
    RaceSessionView session = event.session();
    PlayerRaceView view = session.playerView(player.getUniqueId());
    player.sendMessage("CP " + view.checkpoint() + "/" + view.totalCheckpoints()
            + " on lap " + view.lap() + " - position " + view.position());
}
```

Available events: `RaceOpenEvent`, `RaceJoinEvent`, `RaceLeaveEvent`, `RaceStartEvent`,
`CheckpointReachedEvent`, `LapCompleteEvent`, `PitStopEvent`, `RaceFinishEvent`, `RaceStopEvent`,
`RaceForfeitEvent`, `PracticeStartEvent` and `PracticeFinishEvent`. All of them extend
`BoatRacingRaceEvent` (`session()`, `player()` nullable, `trackName()`), are informational,
run on the main thread and are never cancellable.

**Build and install:** package your jar, then copy it to `plugins/BoatRacing/extensions/`:

```
plugins/BoatRacing/extensions/
├── MyExtension-1.0.0.jar
└── MyExtension/                 <- created by BoatRacing
    ├── config.yml
    └── lang/
        └── messages_en.yml / messages_es.yml / ...
```

New or updated jars are picked up on server start (or a full plugin reload). Config, messages and
storage are reloaded live with `/boatracing extensions reload`, `/boatracing reload` or a language
change. Check the install with `/boatracing extensions`; add `debug: "fine"` to the base config to
trace load failures.

### 3. Or write an external plugin

```yaml
# plugin.yml
name: MyBoatRacingPlugin
main: com.example.MyPlugin
api-version: '1.19'
depend: [BoatRacing]
softdepend: [PlaceholderAPI, Vault]
```

```java
@Override
public void onEnable() {
    BoatRacingAPI api = BoatRacingProvider.get();
    if (api == null || !api.enabled()) {
        getLogger().warning("BoatRacing API not available; disabling.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    if (api.apiVersion() < BoatRacingAPI.API_VERSION) {
        getLogger().warning("BoatRacing is too old for this plugin.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }

    getServer().getPluginManager().registerEvents(new MyListener(api), this);
    api.registerHudProvider(this, new MyHud());
    // Unregister in onDisable: api.unregisterHudProvider(hud);
}
```

`BoatRacingProvider.isCompatible(compiledApiVersion)` combines both checks. Useful read-only
helpers: `sessions()`, `sessionForTrack(name)`, `sessionForPlayer(uuid)`, `isRacing(uuid)`,
`pluginVersion()`.

### 4. Rules and best practices

- **Never block the main thread.** Events, HUD providers and commands run on it; use
  `scheduler().runAsync*` for I/O.
- **Keep HUD providers cheap** (called every 2 ticks) and return short legacy `&`-colored lines.
- **Clean up what you spawn** on `RaceStopEvent` and in `onDisable`; tasks started through
  `scheduler()` are cancelled automatically when the extension is disabled, but the returned handle
  lets you cancel them earlier.
- **Persist through `storage()`**, never write files directly: BoatRacing handles the backend and
  the extension folder.
- **Do not bypass race rules** (checkpoints, laps, pits, penalties and results are validated by
  BoatRacing regardless of what an extension does).
- **Prefer `event.session().audience()`** to message participants + admins instead of broadcasting.
- **A broken extension must not break a race:** listener and command exceptions are caught and
  logged by BoatRacing, but avoid flooding the console and always check for null players/sessions.
- **Compatibility:** keep to the `api` package, check `apiVersion()` and set a correct
  `api-version` in `extension.yml`.

The full contract (all events with their data, views, HUD hook and storage details) is documented
in [API.md](API.md). The private `BoatRacing-PartyExtension` addon in the untracked `party/` folder is a
complete working reference (abilities, item boxes, storage, commands, placeholders, HUD and
multi-language messages); see `party/README.md` locally.

## BoatRacing-PartyExtension (private addon)

The private, paid party-style addon lives in the untracked `party/` folder (excluded from git) and is built as a **base-managed extension**, not a separate plugin. It ships as `BoatRacing-PartyExtension-26.3.jar` and is installed in `plugins/BoatRacing/extensions/`.

- Abilities: Mushroom (boost), Banana (trap), Green Shell (projectile), Lightning (slows everyone), Super Star (temporary invincibility + boost), Blooper (blinds rivals), Coin (party points) and Bob-omb (fuse + knockback).
- Item sources: right after the race start, by chance when crossing checkpoints, and from admin-placed item boxes (`/boatracing party box add` at your position, persisted through BoatRacing storage).
- Party points: earned from Coins and finishing positions, stored by BoatRacing (`party-stats.yml` in YAML mode, database documents in SQLite/MySQL) and shown in the sidebar through the extension HUD hook.
- Commands: `/boatracing party help|points [player]|top|box add|remove|list|reload`; permissions `boatracing.party.use` (default true) and `boatracing.party.admin` (default op).
- Placeholders: `%boatracing_party_points%`, `%boatracing_party_item%`, `%boatracing_party_top1_name%`, `%boatracing_party_top1_points%`.
- Config and messages: `plugins/BoatRacing/extensions/BoatRacing-PartyExtension/` (`config.yml` and `lang/messages_*.yml`; the active language follows `language`).
- Build: run `scripts/build-all.ps1` and copy the extension jar into `plugins/BoatRacing/extensions/`.
- The addon is skipped with a console warning when its `api-version` is newer than the running BoatRacing API.

## Diagnostics and Bug Reporting
- `/boatracing debug` is **admin-only** (`boatracing.debug`, default op). It prints plugin and server versions, API, Java, storage mode, language, debug level, track/team/active-session counts.
- The GitHub Issues URL is fixed (`https://github.com/Jaie55/BoatRacing/issues`) and cannot be changed from the config; include the debug output and your `logs/latest.log` when reporting.
- For detailed tracing set `debug` in `config.yml` to `fine`, `finer` or `finest`; AutoTrace, cosmetics, Discord, spectator, victory effects and replay log accordingly.
- Reports never include secrets (no database passwords, no webhook URLs).

## Tab Completion
Root suggestions:
- `teams`, `race`, `cosmetics`, `setup`, `admin`, `extensions`, `reload`, `version`, `debug` depending on permissions, plus the subcommands of every loaded base-managed extension (for example `party`).

Cosmetics suggestions:
- Everyone sees `density`; admins also see `unlock`, `revoke` and `unlocks`.
- `density` suggests `low`, `normal`, `high`.
- `unlock`/`revoke` suggest categories, known cosmetic ids, `*`, `all` and durations (`30m`, `12h`, `7d`, `0`).

Teams suggestions:
- `create`, `rename`, `color`, `join`, `leave`, `boat`, `number`, `confirm`, `cancel`
- `join` suggests team names.
- `color` suggests all dye colors.
- `boat` suggests supported boat and chest-boat variants, plus bamboo rafts.

Race suggestions:
- Everyone sees `help`, `join`, `leave`, `status`, `vote`, `voteui`, `votestatus`, and (with permission) `spectate`.
- Players with `boatracing.race.back` also see `back`.
- Players with `boatracing.race.practice` also see `practice`.
- Players with `boatracing.race.voteopen` (or admin-capable users) also see `voteopen`.
- Admin-capable users also see `open`, `start`, `force`, `stop`, `voteclose`.
- Track-taking subcommands (`open`, `join`, `leave`, `force`, `start`, `practice`, `stop`, `status`, `vote`) suggest existing named tracks.

Setup suggestions:
- `help`, `addstart`, `clearstarts`, `removestart`, `setfinish`, `clearfinish`, `setpit`, `clearpit`, `addcheckpoint`, `addalt`, `clearalt`, `clearcheckpoints`, `addlight`, `removelight`, `clearlights`, `setlaps`, `setpitstops`, `setregtime`, `setcosmetics`, `setlobby`, `clearlobby`, `setpos`, `clearpos`, `show`, `selinfo`, `wand`, `autotrace`, `wizard`
- `setpit` suggests team names, quoting names with spaces when needed.
- `setpos` and `clearpos` suggest online and known offline player names.
- `setpos` also suggests `auto` and available slot numbers.
- `removestart`, `removelight`, `addalt` and `clearalt` suggest valid numeric indexes.
- `autotrace` suggests `help`, `start`, `stop`, `preview`, `accept`, `cancel`, `status`, `delete` and `resize`.

Admin suggestions:
- `help`, `team`, `player`, `tracks`, `language`
- `team` suggests `create`, `delete`, `rename`, `color`, `add`, `remove`
- `player` suggests `setteam`, `setnumber`, `setboat`
- `language` suggests available language codes discovered from bundled and plugin-folder message files.

## Admin Commands and GUI
`/boatracing admin` opens the admin hub.

Admin GUI sections:
- Teams: create teams, open team view, rename, recolor, add/remove members, delete teams.
- Players: assign/remove team, set racer number, set boat type.
- Race: open/close registration, start/force/stop, quick-set laps, custom laps, and remove registrants.
- Tracks: create/select/rename/delete named tracks and reapply the selected track from disk.

Command equivalents:
- `/boatracing admin team create <name> [color] [firstMember]`
- `/boatracing admin team delete <name>`
- `/boatracing admin team rename <old> <new>`
- `/boatracing admin team color <name> <DyeColor>`
- `/boatracing admin team add <name> <player>`
- `/boatracing admin team remove <name> <player>`
- `/boatracing admin player setteam <player> <team|none>`
- `/boatracing admin player setnumber <player> <1-99>`
- `/boatracing admin player setboat <player> <BoatType>`
- `/boatracing admin tracks`
- `/boatracing admin language <code>`

## Permissions
- `boatracing.*` (default: false): wildcard for the full plugin.
- `boatracing.use` (default: true): base meta permission.
- `boatracing.teams` (default: true): access to `/boatracing teams`.
- `boatracing.stats` (default: true): access to `/boatracing stats [player]`.
- `boatracing.stats.others` (default: true): allow `/boatracing stats <player>` for other players.
- `boatracing.version` (default: true): access to `/boatracing version`.
- `boatracing.cosmetics` (default: true): access to `/boatracing cosmetics` (trails, titles, effects, sounds, checkpoints).
- `boatracing.cosmetics.admin` (default: op): grant/revoke/list cosmetic unlocks with `/boatracing cosmetics unlock|revoke|unlocks`.
- `boatracing.debug` (default: op): show the diagnostic report with `/boatracing debug` (admins only, GitHub Issues URL fixed).
- `boatracing.extensions` (default: op): list installed base-managed extensions and use `/boatracing extensions [reload]`.
- Cosmetic nodes: `boatracing.cosmetics.trail.<id>`, `.title.<id>`, `.effect.<id>`, `.sound.<id>` and `.checkpoint.<id>`. Grant them from economy/shop plugins.
- `boatracing.cosmetics.unlock.all` (default: false): unlocks every cosmetic at once. It is granted by `boatracing.admin` but **not** by `boatracing.*`.
- Per-trail nodes like `boatracing.cosmetics.trail.flame` are not granted by default; hand them out with ranks/shops to monetize cosmetics.
- `boatracing.reload` (default: op): access to `/boatracing reload`.
- `boatracing.update` (default: op): receive in-game update notices.
- `boatracing.setup` (default: op): access to track setup, wizard, selection, and setup GUIs.
- `boatracing.admin` (default: op): access to the admin hub and admin management features.
- `boatracing.admin.language` (default: op): change plugin language with `/boatracing admin language <code>`.
- `boatracing.race.join` (default: true): join registration.
- `boatracing.race.leave` (default: true): leave registration.
- `boatracing.race.back` (default: true): return to the saved pre-lobby location.
- `boatracing.race.status` (default: true): check track race status.
- `boatracing.race.practice` (default: true): start solo practice mode on a ready track.
- `boatracing.race.forfeit` (default: true): forfeit an active race.
- `boatracing.race.spectate` (default: true): spectate a running race.
- `boatracing.race.voteopen` (default: op): open map voting with `/boatracing race voteopen`.
- `boatracing.race.admin` (default: op): manage races with `open`, `start`, `force`, `stop`, and `voteclose`.

Permission notes:
- `boatracing.admin` grants the other plugin permissions through explicit children.
- `boatracing.admin.language` can be granted independently so trusted staff can switch language without broader admin powers.
- Players can always see the `race` root suggestion, but actual subcommands remain permission/config-gated.
- Non-admin race management is controlled by config and per-track overrides, not by a separate extra permission node.

## Configuration
Core:
- `prefix`: chat prefix.
- `language`: bundled values are `en`, `es`, `es_419`, `fr`, `pt_BR`, `pt_PT`, `de`, `it`, `pl`, `tr`, `ja`, `ko`, `sv`, `zh_TW`, `zh_CN`, `ru`, `uk`, `id`, `ar`, `nl`, `cs`, `vi`, `th`, `tl`, `da`, `no`, and `fi`; can be changed live with `/boatracing admin language <code>`.
- `max-members-per-team`: team size limit.

Setup helpers:
- `setup.selection-visualizer.*`: wand selection particle preview (`enabled`, `period-ticks`, `particle`, `spacing`, `max-particles-per-player`, `view-distance`).
- `setup.auto-trace.*`: AutoTrace recording and gate generation (`enabled`, `sample-ticks`, `min-distance`, `max-samples`, `simplify-epsilon`, `spacing`, `half-width`, `half-height`, `auto-close-distance`, `auto-close-min-length`, `recenter-ice`, `start-marker`, `actionbar`, `reminder-seconds`, `preview`, `preview-period-ticks`, `preview-particle`, `preview-view-distance`, `wand-select`, `wand-select-radius`, `selection-particle`).

Extension API:
- `api.enabled`
- `api.log-extensions`
- Base-managed extension jars are loaded from `plugins/BoatRacing/extensions/` (no extra config keys); each one gets its own folder with `config.yml`, a `lang/` folder for the message bundles and storage documents.

Storage:
- `database.mode` (`YAML`, `SQLITE` or `MYSQL`)- `database.migrate-legacy-yaml`
- `database.table`
- `database.sqlite.file`
- `database.mysql.host` / `port` / `database` / `username` / `password` / `parameters`

Practice ghosts:
- `practice.ghost.enabled`, `practice.ghost.only-in-practice`
- `practice.ghost.sample-ticks`, `practice.ghost.playback-ticks`
- `practice.ghost.min-distance`, `practice.ghost.max-samples`, `practice.ghost.show-name`

Player actions:
- `player-actions.allow-team-create`
- `player-actions.allow-team-rename`
- `player-actions.allow-team-color`
- `player-actions.allow-team-disband`
- `player-actions.allow-set-boat`
- `player-actions.allow-set-number`
- `player-actions.allow-player-race-start`

Updates and metrics:
- `updates.enabled`
- `updates.console-warn`
- `updates.notify-admins`
- `bstats.enabled`

Global racing defaults:
- `racing.laps`
- `racing.mandatory-pitstops`
- `racing.pit-penalty-seconds`
- `racing.registration-seconds`
- `racing.min-players-to-start`
- `racing.false-start-penalty-seconds`
- `racing.enable-pit-penalty`
- `racing.enable-false-start-penalty`
- `racing.lights-out-delay-seconds`
- `racing.lights-out-jitter-seconds`

Victory effects:
- `racing.victory-effects.enabled`
- `racing.victory-effects.top-n`
- `racing.victory-effects.screen-title`
- `racing.victory-effects.fireworks`
- `racing.victory-effects.sounds`

Post-finish spectator mode:
- `racing.spectate-on-finish.mode` (`off`, `free`, `follow`)
- `racing.spectate-on-finish.follow-interval-ticks`

Discord webhook:
- `discord.enabled`
- `discord.webhook-url`
- `discord.username`
- `discord.avatar-url`
- `discord.events.race-start`
- `discord.events.race-results`
- `discord.events.record-broken`

Cosmetics:
- `cosmetics.enabled` (master switch)
- `cosmetics.unsupported` (`hide` or `fallback` for particles missing on this server version)
- `cosmetics.density.enabled`
- `cosmetics.density.default` (`low`, `normal`, `high`)
- `cosmetics.density.low` / `cosmetics.density.normal` / `cosmetics.density.high` (particle counts)
- `cosmetics.density.max-victory-rockets` (safety cap)
- `cosmetics.trails.enabled`
- `cosmetics.trails.period-ticks`
- `cosmetics.trails.show-in` (`always`, `race`, `practice`, `race-and-practice`)
- `cosmetics.trails.disabled` (built-in trail ids to disable)
- `cosmetics.trails.custom.<id>` (`particle`, `material`, `count`, `spread`, `extra`, `permission`, `enabled`)
- `cosmetics.effects.enabled`
- `cosmetics.effects.locked` (legacy lock list; requires `boatracing.cosmetics.effect.<id>`)
- `cosmetics.victory-sounds.enabled`
- `cosmetics.victory-sounds.locked` (legacy lock list; requires `boatracing.cosmetics.sound.<id>`)
- `cosmetics.checkpoints.enabled`
- `cosmetics.checkpoints.locked` (legacy lock list; requires `boatracing.cosmetics.checkpoint.<id>`)
- `cosmetics.titles.enabled`
- `cosmetics.titles.thresholds.<id>` (title id -> required wins)
- `cosmetics.shop.enabled`
- `cosmetics.shop.gated-by-default` (true = everything needs permission/purchase/free price)
- `cosmetics.shop.require-vault`
- `cosmetics.shop.currency-name`
- `cosmetics.shop.default-price` (-1 = not purchasable if no explicit price)
- `cosmetics.shop.prices.<category>.<id>` (0 = free, positive = price)

Race replays:
- `replay.capture-race` (store the race winner's run as the track ghost)

Diagnostics:
- `/boatracing debug` is admin-only and always reports to the fixed GitHub Issues URL; there is no diagnostics configuration to change.

Registration lobby:
- `racing.lobby.enabled`
- `racing.lobby.return-on-leave`
- `racing.lobby.back-window-seconds`
- `racing.lobby.world`
- `racing.lobby.x`
- `racing.lobby.y`
- `racing.lobby.z`
- `racing.lobby.yaw`
- `racing.lobby.pitch`

HUD toggles:
- `racing.ui.scoreboard.show-position`
- `racing.ui.scoreboard.show-lap`
- `racing.ui.scoreboard.show-checkpoints`
- `racing.ui.scoreboard.show-pitstops`
- `racing.ui.scoreboard.show-name`
- `racing.ui.actionbar.show-lap`
- `racing.ui.actionbar.show-checkpoints`
- `racing.ui.actionbar.show-pitstops`
- `racing.ui.actionbar.show-time`

Rewards:
- `racing.rewards.enabled`
- `racing.rewards.positions.<place>.commands`
- `racing.rewards.positions.<place>.messages`
- `racing.rewards.positions.<place>.broadcast`
- Legacy compatibility: `racing.rewards.positions.<place>.command` (single string) is also accepted, but `commands` list is recommended.
- Recommended schema hygiene: keep `commands: []` explicitly present for each configured position block (including `1`).
- Supported reward placeholders: `{player}`, `{position}`, `{time}`, `{track}`, `{laps}`

Message templates:
- Registration announce text is language-specific and lives in `messages_<lang>.yml` under `race.registration.announce`.
- That template supports `{track}`, `{laps}`, `{cmd}`, and `{label}`.
- Minimum-player start warning is language-specific under `race.not-enough-players` and supports `{min}` and `{current}`.

Per-track overrides:
- Track files under `plugins/BoatRacing/tracks/<name>.yml` can override `racing.*` values.
- Common override example: `racing.min-players-to-start` for tracks that require larger grids.
- `setup show` prints the active override set for the current track.

## Updates and Metrics
- Update source: Modrinth.
- Startup can print a WARN if the plugin is outdated.
- Silent background checks run every 5 minutes.
- A newly detected version during runtime triggers a one-time console WARN for that version.
- Hourly reminder warnings remain active while outdated if `updates.console-warn` is enabled.
- Admin players with `boatracing.update` can receive in-game notices on join.
- bStats is enabled by default and can be disabled with `bstats.enabled`.

## Storage
- `plugins/BoatRacing/teams.yml`: team membership, colors, names, and current team leader.
- `plugins/BoatRacing/racers.yml`: per-player racer numbers and boat types.
- `plugins/BoatRacing/stats.yml`: player wins, team wins, best race, and best lap.
- `plugins/BoatRacing/player-prefs.yml`: per-player cosmetics (equipped trail, title, victory effect, victory sound, checkpoint effect and particle density).
- `plugins/BoatRacing/cosmetic-unlocks.yml`: shop/purchase unlocks per player (with optional expiry).
- `plugins/BoatRacing/practice-ghosts.yml`: best practice ghost and race replay ghost (v1) per track and lap count.
- `plugins/BoatRacing/tracks/<name>.yml`: one file per track containing starts, finish, pit areas, team pits, checkpoints, lights, custom slots, best times, and per-track racing overrides.

Legacy migration:
- If an old `plugins/BoatRacing/track.yml` is found, it is migrated to `plugins/BoatRacing/tracks/default.yml` (or `default_N.yml` if needed).

Storage backend:
- Every document above is stored through `database.mode` in `config.yml`: `SQLITE` (default; `plugins/BoatRacing/boatracing.db`, table `boatracing_data`), `MYSQL` (external database, useful to share data across servers) or `YAML` (one file per document in the plugin folder).
- With SQLite/MySQL the names above are **document keys inside the database**, not separate files on disk; `database.migrate-legacy-yaml: true` imports existing YAML files on first load.

## Compatibility
- Minecraft: 1.19 to 26.3
- Java: 17+
- Server families: CraftBukkit, Spigot, Paper, Purpur, Folia
- Optional PlaceholderAPI support through a soft dependency
- Optional Vault economy support for the cosmetic shop (prices and `[Buy]` buttons)
- SimpleScore compatibility hook for hiding/restoring external sidebars during races
- Compatible with TAB environments for scoreboard usage, without requiring a TAB-specific dependency
- Bundled languages: English, Spanish, French, Portuguese (Brazil), Portuguese (Portugal), German, Italian, Polish, Turkish, Japanese, Korean, Swedish, Chinese (Taiwan, Traditional), Chinese (Mainland, Simplified), Russian, Ukrainian, Indonesian, Arabic, Dutch, Czech, Vietnamese, Thai, Filipino, Danish, Norwegian, Finnish
- Note: `pt_BR`, `pt_PT`, `de`, `it`, `pl`, `tr`, `ja`, `ko`, `sv`, `uk`, `id`, `ar`, `nl`, `cs`, `vi`, `th`, `tl`, `da`, `no`, and `fi` are bundled community files translated from English; native-speaker refinements are still welcome.
- Custom languages are supported via `messages_<lang>.yml` in the `plugins/BoatRacing/lang/` folder and can be selected with `language: "<lang>"`.

## Placeholders (PlaceholderAPI)
If PlaceholderAPI is installed, BoatRacing registers `%boatracing_*%` placeholders for scoreboards, holograms, NPCs, and static displays.

Resolution rules:
- Missing text-like data usually resolves to `-`.
- Missing numeric data usually resolves to `0` or `-1` depending on the placeholder.
- `%boatracing_player_*%` uses the viewer as context.
- `%boatracing_player_*_<player>%` uses an explicit player name or UUID and is ideal for NPCs and static holograms.
- Team leader placeholders always resolve the current saved leader of that team.
- Track-scoped race placeholders (`%boatracing_track_race_*_<track>%`) resolve against the race session of the requested track.
- Global race placeholders (`%boatracing_race_running%`, `%boatracing_race_registering%`, `%boatracing_race_status%`) resolve across all track sessions, so a single display can react to any track: `status` returns `running` if any race is running, otherwise `registering` if any registration is open, otherwise `idle`. The `status` label is translated from `placeholder.race-status.*` in `messages_<lang>.yml`; the boolean placeholders stay `true`/`false`.
- Track-scoped practice placeholders (`%boatracing_track_practice_running_<track>%`) resolve whether the requested track is currently in practice mode (including countdown).
- Track-scoped best-record placeholders (`%boatracing_track_best_*_<track>%`) resolve against the requested track token, not only the currently selected track.
- Compatibility aliases are available: `%boatracing_track_racerunning_<track>%` and `%boatracing_track_raceregistering_<track>%`.
- Compatibility alias is available for practice-running: `%boatracing_track_practicerunning_<track>%`.
- For track-scoped race placeholders, tracks without an active session resolve as `false` (`running`/`registering`) or `idle` (`status`).
- `<track>` tokens support underscores for spaces (for example `My_Track`).

### Category: Viewer Player and Team

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_player_name%` | Viewer player name | `Driver: jaie55` | Only the viewer sees their own value |
| `%boatracing_player_team_name%` / `%boatracing_player_team_id%` / `%boatracing_player_team_color%` | Viewer team identity | `Team: Sharks` / `Team ID: sharks` / `Color: AQUA` | Viewer context |
| `%boatracing_player_team_leader_name%` / `%boatracing_player_team_leader_id%` | Current leader of the viewer's team | `Leader: jaie55` | Viewer context |
| `%boatracing_player_team_players%` / `%boatracing_player_team_player_count%` | Viewer team roster and size | `Members: jaie55, KiluGod` / `Members: 2` | Viewer context |
| `%boatracing_player_number%` / `%boatracing_player_boat%` | Viewer racer number and selected boat | `Number: 7` / `Boat: OAK_BOAT` | Viewer context |
| `%boatracing_title%` / `%boatracing_title_id%` | Viewer title (equipped or automatic) and its id | `Legend` / `legend` | Viewer context |
| `%boatracing_title_wins%` | Viewer total wins used for title unlocks | `Wins: 42` | Viewer context |
| `%boatracing_trail%` | Viewer equipped trail id (empty when none) | `flame` | Viewer context |
| `%boatracing_particle_density%` | Viewer particle density setting | `normal` | Viewer context |
| `%boatracing_cosmetic_owned_<category>_<id>%` | Whether the viewer has that cosmetic (permission, purchase or free) | `true` | Viewer context |
| `%boatracing_cosmetic_expires_<category>_<id>%` | Epoch millis when a temporary unlock expires (`0` permanent, `-1` no grant) | `1758000000000` | Viewer context |

### Category: Viewer Live Race State

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_player_race_running%` / `%boatracing_player_race_registering%` | Whether the viewer is currently racing or currently registered in their own race session | `Running: true` / `Registering: false` | Viewer context |
| `%boatracing_player_practice_running%` | Whether the viewer is currently in a solo practice session | `Practice running: true` | Viewer context |
| `%boatracing_track_race_running_<track>%` / `%boatracing_track_race_registering_<track>%` / `%boatracing_track_race_status_<track>%` | Track-scoped race state (`running`, `registering`, `idle`) for a specific track token | `Harbor running: true` / `Harbor status: running` / `Desert status: idle` | Same for every viewer |
| `%boatracing_race_running%` / `%boatracing_race_registering%` / `%boatracing_race_status%` | Global race state across all tracks (`running`, `registering`, `idle`) for a single server-wide display | `Any race: true` / `Status: registering` / `Status: idle` | Same for every viewer |
| `%boatracing_track_racerunning_<track>%` / `%boatracing_track_raceregistering_<track>%` | Backward-compatible aliases for track running/registering booleans | `Harbor running(alias): true` / `Desert registering(alias): false` | Same for every viewer |
| `%boatracing_track_practice_running_<track>%` / `%boatracing_track_practicerunning_<track>%` | Track-scoped practice state (true while practice countdown or run is active) | `Harbor practice: true` / `Harbor practice(alias): true` | Same for every viewer |
| `%boatracing_player_current_time%` / `%boatracing_player_current_time_ms%` | Live timer for the viewer | `Time: 1:42.355` / `TimeMs: 102355` | Viewer context |
| `%boatracing_player_current_lap%` / `%boatracing_player_current_checkpoint%` | Viewer lap and next checkpoint progression | `Lap: 2` / `Checkpoint: 5` | Viewer context |
| `%boatracing_player_current_position%` / `%boatracing_player_current_pitstops%` / `%boatracing_player_finished%` | Viewer live position, pit count, and finish state | `Pos: 1` / `Pit stops: 0` / `Finished: false` | Viewer context |

### Category: Viewer Practice Stats

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_player_practice_best_run%` / `%boatracing_player_practice_best_run_ms%` | Viewer best complete practice run on current track | `Practice best run: 1:10.245` | Viewer context |
| `%boatracing_player_practice_last_run%` / `%boatracing_player_practice_last_run_ms%` | Viewer last complete practice run on current track | `Practice last run: 1:12.040` | Viewer context |
| `%boatracing_player_practice_best_lap%` / `%boatracing_player_practice_best_lap_ms%` | Viewer best practice lap on current track | `Practice best lap: 0:33.112` | Viewer context |
| `%boatracing_player_practice_last_lap%` / `%boatracing_player_practice_last_lap_ms%` | Viewer last practice lap on current track | `Practice last lap: 0:34.506` | Viewer context |
| `%boatracing_player_practice_best_sector_<section>%` / `%boatracing_player_practice_best_sector_ms_<section>%` | Viewer best section split for current track | `%boatracing_player_practice_best_sector_2%` -> `0:10.820` | Viewer context |
| `%boatracing_player_practice_last_sector_<section>%` / `%boatracing_player_practice_last_sector_ms_<section>%` | Viewer last section split for current track | `%boatracing_player_practice_last_sector_2%` -> `0:11.064` | Viewer context |
| `%boatracing_player_practice_best_run_<track>%` / `%boatracing_player_practice_best_run_ms_<track>%` | Viewer best complete practice run for a specific track token | `%boatracing_player_practice_best_run_harbor%` -> `1:10.245` | Viewer context |
| `%boatracing_player_practice_last_run_<track>%` / `%boatracing_player_practice_last_run_ms_<track>%` | Viewer last complete practice run for a specific track token | `%boatracing_player_practice_last_run_harbor%` -> `1:12.040` | Viewer context |
| `%boatracing_player_practice_best_lap_<track>%` / `%boatracing_player_practice_best_lap_ms_<track>%` | Viewer best lap for a specific track token | `%boatracing_player_practice_best_lap_harbor%` -> `0:33.112` | Viewer context |
| `%boatracing_player_practice_last_lap_<track>%` / `%boatracing_player_practice_last_lap_ms_<track>%` | Viewer last lap for a specific track token | `%boatracing_player_practice_last_lap_harbor%` -> `0:34.506` | Viewer context |
| `%boatracing_player_practice_best_sector_<track>_<section>%` / `%boatracing_player_practice_best_sector_ms_<track>_<section>%` | Viewer best section split for a specific track token | `%boatracing_player_practice_best_sector_harbor_3%` -> `0:12.404` | Viewer context |
| `%boatracing_player_practice_last_sector_<track>_<section>%` / `%boatracing_player_practice_last_sector_ms_<track>_<section>%` | Viewer last section split for a specific track token | `%boatracing_player_practice_last_sector_harbor_3%` -> `0:12.980` | Viewer context |

### Category: Viewer Records and Wins

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_player_track_best%` / `%boatracing_player_track_best_ms%` | Viewer best time on the current track | `Track PB: 0:59.443` | Viewer context |
| `%boatracing_player_best_race%` / `%boatracing_player_best_race_ms%` | Viewer best race overall | `Best race: 1:40.010` | Viewer context |
| `%boatracing_player_best_race_track_<track>%` / `%boatracing_player_best_race_ms_track_<track>%` | Viewer best race for a specific track token | `%boatracing_player_best_race_track_harbor%` -> `1:40.010` | Viewer context |
| `%boatracing_player_best_race_laps_<track>_<laps>%` / `%boatracing_player_best_race_ms_laps_<track>_<laps>%` | Viewer best race for a specific track token and lap count | `%boatracing_player_best_race_laps_harbor_3%` -> `1:40.010` | Viewer context |
| `%boatracing_player_best_lap%` / `%boatracing_player_best_lap_ms%` | Viewer best lap overall | `Best lap: 0:28.911` | Viewer context |
| `%boatracing_player_best_lap_track_<track>%` / `%boatracing_player_best_lap_ms_track_<track>%` | Viewer best lap for a specific track token | `%boatracing_player_best_lap_track_harbor%` -> `0:28.911` | Viewer context |
| `%boatracing_player_best_lap_laps_<track>_<laps>%` / `%boatracing_player_best_lap_ms_laps_<track>_<laps>%` | Viewer best lap for a specific track token and lap count context | `%boatracing_player_best_lap_laps_harbor_3%` -> `0:29.102` | Viewer context |
| `%boatracing_player_wins%` / `%boatracing_player_team_wins%` | Viewer wins and viewer team wins | `Wins: 12` / `Team wins: 18` | Viewer context |

### Category: Global and Top Stats

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_teams_count%` / `%boatracing_teams_list%` | Total number of teams and the team list | `Teams: 4` / `Teams: Sharks, Rockets, Drift, Wave` | Same for every viewer |
| `%boatracing_track_name%` / `%boatracing_track_best_player%` / `%boatracing_track_best_time%` | Current track name and its best record | `Track: harbor` / `Track record: jaie55 - 0:58.772` | Same for every viewer |
| `%boatracing_track_best_player_<track>%` / `%boatracing_track_best_time_<track>%` / `%boatracing_track_best_time_ms_<track>%` | Best record for a specific track token | `%boatracing_track_best_time_harbor%` -> `0:58.772` / `%boatracing_track_best_player_harbor%` -> `jaie55` | Same for every viewer |
| `%boatracing_track_best_player_laps_<track>_<laps>%` / `%boatracing_track_best_time_laps_<track>_<laps>%` / `%boatracing_track_best_time_ms_laps_<track>_<laps>%` | Best record for a specific track token and lap count | `%boatracing_track_best_time_laps_harbor_2%` -> `0:39.221` / `%boatracing_track_best_time_laps_harbor_3%` -> `0:58.772` | Same for every viewer |
| `%boatracing_track_top_1_player_<track>%` / `%boatracing_track_top_1_time_<track>%` / `%boatracing_track_top_1_time_ms_<track>%` | Track top 1 holder and time | `%boatracing_track_top_1_player_harbor%` -> `jaie55` / `%boatracing_track_top_1_time_harbor%` -> `0:58.772` | Same for every viewer |
| `%boatracing_track_top_2_player_<track>%` / `%boatracing_track_top_2_time_<track>%` / `%boatracing_track_top_2_time_ms_<track>%` | Track top 2 holder and time | `%boatracing_track_top_2_player_harbor%` -> `KiluGod` / `%boatracing_track_top_2_time_harbor%` -> `1:00.120` | Same for every viewer |
| `%boatracing_track_top_3_player_<track>%` / `%boatracing_track_top_3_time_<track>%` / `%boatracing_track_top_3_time_ms_<track>%` | Track top 3 holder and time | `%boatracing_track_top_3_player_harbor%` -> `RacerX` / `%boatracing_track_top_3_time_harbor%` -> `1:01.004` | Same for every viewer |
| `%boatracing_track_top_1_player_laps_<track>_<laps>%` / `%boatracing_track_top_1_time_laps_<track>_<laps>%` / `%boatracing_track_top_1_time_ms_laps_<track>_<laps>%` | Track top 1 for a specific lap count (same pattern for `top_2` and `top_3`) | `%boatracing_track_top_1_time_laps_harbor_3%` -> `0:58.772` / `%boatracing_track_top_2_time_laps_harbor_3%` -> `1:00.120` | Same for every viewer |
| `%boatracing_track_practice_running_<track>%` / `%boatracing_track_practicerunning_<track>%` | Practice-running state for a specific track token | `%boatracing_track_practice_running_harbor%` -> `true` | Same for every viewer |
| `%boatracing_top_player_wins_name%` / `%boatracing_top_player_wins%` | Player with most wins | `Top wins: jaie55 (29)` | Same for every viewer |
| `%boatracing_top_team_wins_name%` / `%boatracing_top_team_wins%` | Team with most wins | `Top team: Sharks (77)` | Same for every viewer |
| `%boatracing_top_player_best_race_name%` / `%boatracing_top_player_best_race%` | Best race holder and time | `Best race: KiluGod - 1:38.404` | Same for every viewer |
| `%boatracing_top_player_best_race_name_track_<track>%` / `%boatracing_top_player_best_race_track_<track>%` | Best race holder and time for a specific track token | `%boatracing_top_player_best_race_name_track_harbor%` -> `KiluGod` | Same for every viewer |
| `%boatracing_top_player_best_race_name_laps_<track>_<laps>%` / `%boatracing_top_player_best_race_laps_<track>_<laps>%` | Best race holder and time for a specific track token and lap count | `%boatracing_top_player_best_race_laps_harbor_3%` -> `1:38.404` | Same for every viewer |
| `%boatracing_top_player_best_lap_name%` / `%boatracing_top_player_best_lap%` | Best lap holder and time | `Best lap: jaie55 - 0:27.950` | Same for every viewer |
| `%boatracing_top_player_best_lap_name_track_<track>%` / `%boatracing_top_player_best_lap_track_<track>%` | Best lap holder and time for a specific track token | `%boatracing_top_player_best_lap_name_track_harbor%` -> `jaie55` | Same for every viewer |
| `%boatracing_top_player_best_lap_name_laps_<track>_<laps>%` / `%boatracing_top_player_best_lap_laps_<track>_<laps>%` | Best lap holder and time for a specific track token and lap count context | `%boatracing_top_player_best_lap_laps_harbor_3%` -> `0:27.950` | Same for every viewer |

### Category: Team Lookup by Name
Use `<team>` with the team token. Team names with spaces can be addressed using underscores in docs/examples.

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_team_leader_name_<team>%` / `%boatracing_team_leader_id_<team>%` | Current leader of a specific team | `%boatracing_team_leader_name_sharks%` -> `Leader: jaie55` | Same for every viewer |
| `%boatracing_team_players_<team>%` | Player list of a specific team | `%boatracing_team_players_sharks%` -> `Members: jaie55, KiluGod` | Same for every viewer |
| `%boatracing_team_player_count_<team>%` | Member count of a specific team | `%boatracing_team_player_count_sharks%` -> `Members: 2` | Same for every viewer |
| `%boatracing_team_wins_<team>%` | Wins of a specific team | `%boatracing_team_wins_sharks%` -> `Wins: 77` | Same for every viewer |

### Category: Player Lookup by Name or UUID
Use `<player>` with an exact player name or UUID. These are the placeholders for NPCs, statue labels, fixed holograms, and leaderboard walls.

| Placeholder(s) | What it shows | Example text on screen | Visibility |
|---|---|---|---|
| `%boatracing_player_name_<player>%` | Target player name | `%boatracing_player_name_jaie55%` -> `Driver: jaie55` | Everyone sees the target player's data |
| `%boatracing_player_wins_<player>%` | Target player wins | `%boatracing_player_wins_jaie55%` -> `Wins: 12` | `jaie55` and `KiluGod` both see Jaie55's wins |
| `%boatracing_player_best_race_<player>%` / `%boatracing_player_best_race_ms_<player>%` | Target player best race | `%boatracing_player_best_race_jaie55%` -> `Best race: 1:40.010` | Everyone sees the target player's data |
| `%boatracing_player_best_lap_<player>%` / `%boatracing_player_best_lap_ms_<player>%` | Target player best lap | `%boatracing_player_best_lap_jaie55%` -> `Best lap: 0:28.911` | Everyone sees the target player's data |
| `%boatracing_player_track_best_<player>%` / `%boatracing_player_track_best_ms_<player>%` | Target player best time on the current track | `%boatracing_player_track_best_jaie55%` -> `Track PB: 0:59.443` | Everyone sees the target player's data |
| `%boatracing_player_team_name_<player>%` / `%boatracing_player_team_id_<player>%` / `%boatracing_player_team_color_<player>%` | Team info of the target player | `Team: Sharks` / `Team ID: sharks` / `Color: AQUA` | Everyone sees the target player's data |
| `%boatracing_player_team_leader_name_<player>%` / `%boatracing_player_team_leader_id_<player>%` | Current leader of the target player's team | `%boatracing_player_team_leader_name_jaie55%` -> `Leader: jaie55` | Everyone sees the target player's data |
| `%boatracing_player_team_wins_<player>%` | Wins of the target player's team | `%boatracing_player_team_wins_jaie55%` -> `Team wins: 18` | Everyone sees the target player's data |
| `%boatracing_player_number_<player>%` / `%boatracing_player_boat_<player>%` | Racer number and selected boat of the target player | `Number: 7` / `Boat: OAK_BOAT` | Everyone sees the target player's data |


## Notes
- Teams can have multiple members and a current saved leader. Leader placeholders always reflect the current leader, not a historical creator value.
- If the current leader leaves the team, leadership falls back to the next stored member automatically.
- Leaving a team as the last member deletes the team automatically.
- User-facing text is message-bundle based and can be customized in `messages_<lang>.yml`, then reloaded with `/boatracing reload`.

## Build (Developers)
- Maven project; produces `BoatRacing.jar` shaded. Run `mvn -DskipTests clean package`.

## License
Distributed under the MIT License. See `LICENSE`.
