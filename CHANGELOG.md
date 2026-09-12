# Changelog

## 26.3 — Unreleased
Track onboarding, live viewing, cosmetics, extension system and replay release.

### Added
- **Oriented checkpoint gates (PLANE)**: checkpoints can be oriented rectangles, so diagonal and curved track sections no longer need oversized axis-aligned boxes.
	- New `CheckpointShape` abstraction: `Region` (AABB) and `PlaneCheckpoint` (oriented rectangle) both implement it, so legacy and new gates coexist in the same ordered list.
	- Crossing detection by signed-distance sign change plus a rectangle containment check on the hit point; movement parallel to the gate plane and hits outside the rectangle are ignored.
	- Serialized per checkpoint in `tracks/<name>.yml`: legacy entries keep `world/minX..maxZ` (no `type`); oriented gates use `type: plane` with `world`, `center`, `normal`, `right`, `up`, `halfWidth`, `halfHeight`. Hand-edited vectors accept `Vector`, list and `"x,y,z"` forms.
- **AutoTrace**: `/boatracing setup autotrace start` records one driven lap and generates oriented checkpoint gates automatically.
	- Subcommands: `start`, `stop`, `preview`, `accept`, `cancel`, `status`, `delete <#>` and `resize <# <width> <height>` (permission `boatracing.setup`).
	- Recording samples every `sample-ticks` with `min-distance` filtering and a `max-samples` cap; auto-stop when returning `auto-close-distance` from the start after `auto-close-min-length`.
	- Generation: Ramer–Douglas–Peucker simplification (`simplify-epsilon`) followed by arc-length resampling (`spacing`) and per-gate orientation; optional vertical re-centering on the nearest ice block (`recenter-ice`).
	- Particle preview (`preview`, `preview-period-ticks`, `preview-particle`, `preview-view-distance`) using a shared wireframe helper.
	- `accept` replaces the active track checkpoints in a single file write; `cancel` discards the session without touching the track; disconnected players are cleaned up automatically.
	- UI: `[AutoTrace]` shortcut in the Setup Wizard CHECKPOINTS step and an AutoTrace button in the Admin Race checkpoint editor.
	- Config: `setup.auto-trace.*` (`enabled`, `sample-ticks`, `min-distance`, `max-samples`, `simplify-epsilon`, `spacing`, `half-width`, `half-height`, `auto-close-distance`, `auto-close-min-length`, `recenter-ice`, `preview`, `preview-period-ticks`, `preview-particle`, `preview-view-distance`).
- **Alternate checkpoint gates**: any checkpoint can have one or more alternate gates (pit lanes, bypasses).
	- `CheckpointGroup` wraps a primary gate plus its alternates and travels with its index in the checkpoint list, so reordering/removing checkpoints never desyncs them from their owner.
	- Stored as `alternates: [...]` inside the checkpoint entry; alternates can be AABB or plane gates.
	- Commands `/boatracing setup addalt <#>` (from the current selection) and `clearalt <#>`; the Admin Race checkpoint editor shows `Alternate gates: N`.
- **Spectator mode**: `/boatracing race spectate [track|leave]` (permission `boatracing.race.spectate`, default true).
	- Optional post-finish spectating via `racing.spectate-on-finish.mode: off|free|follow` and `follow-interval-ticks`; `follow` teleports to the current race leader.
	- Restores gamemode and previous location when leaving, when the race ends, or on disconnect/kick; follow tasks are cancelled.
	- Blocks spectating while still racing, and manual spectators are excluded from race movement/scoreboard processing.
- **Victory effects**: `racing.victory-effects.*` (`enabled`, `top-n`, `screen-title`, `fireworks`, `sounds`). Adventure screen title, per-position colored fireworks (gold/silver/bronze) and a level-up sound for the first finishers.
- **Cosmetics menu** (`/boatracing cosmetics`, permission `boatracing.cosmetics`, default true).
	- Three tabs (Trails, Titles, Victory effects) whose icons mirror the equipped trail/effect.
	- 24 trails: `smoke`, `flame`, `soul`, `cloud`, `spark`, `heart`, `happy`, `witch`, `end_rod`, `totem`, `drip`, `enchant`, `bubble`, `snow`, `lava`, `note`, `portal`, `enchanted`, `damage`, `spore`, `sculk`, `cherry`, `drip_lava`, `ink`; each with its own permission `boatracing.cosmetics.trail.<id>` (not granted by `boatracing.*`, intended to be sold with ranks/shops).
	- **Admin-configurable catalog**: disable built-ins with `cosmetics.trails.disabled` and add/override trails with `cosmetics.trails.custom.<id>` (`particle`, `material`, `count`, `spread`, `extra`, `permission`, `enabled`); unknown particles fall back safely.
	- **Selectable victory effects**: `default`, `none`, `gold`, `silver`, `bronze`, `rainbow`, `heart`, `soul`, `party`; `cosmetics.effects.enabled` toggles the feature and `cosmetics.effects.locked` gates chosen ids behind `boatracing.cosmetics.effect.<id>`. Selection is stored in `player-prefs.yml` and applied by `VictoryEffects`.
	- **Selectable victory sounds**: independent from the fireworks/title; `default` keeps the effect's sound, `none` mutes it, and the rest (`level_up`, `chime`, `bell`, `pling`, `firework`, `dragon`, `thunder`, `wither`, `totem`, `beacon`, `portal`, `anvil`, `victory`) can be locked with `cosmetics.victory-sounds.locked` behind `boatracing.cosmetics.sound.<id>`. Stored in `player-prefs.yml` under `victory-sounds`.
	- **Selectable checkpoint effects**: particles + sound when crossing a gate (`none`, `spark`, `flame`, `heart`, `happy`, `soul`, `enchant`, `note`, `portal`, `totem`, `sculk`, `cherry`); `cosmetics.checkpoints.enabled` toggles the feature and `cosmetics.checkpoints.locked` gates ids behind `boatracing.cosmetics.checkpoint.<id>`. Played by `CosmeticFx` on every checkpoint crossing.
	- **Buying/unlocking cosmetics**: grant individual nodes from economy/shop plugins or the wildcard `boatracing.cosmetics.unlock.all` (default false, included in `boatracing.*`/`boatracing.admin`) to unlock every trail, title, victory effect and checkpoint effect at once.
	- **Scope switches**: `cosmetics.enabled` master switch plus per-track `racing.cosmetics-enabled` via `/boatracing setup setcosmetics <true|false>`; trails, victory effect choices and checkpoint effects are skipped on tracks where cosmetics are disabled.
	- **Team profile integration**: the team GUI member/profile views now show the equipped title, trail and checkpoint effect, and the profile has a Cosmetics button that opens the menu.
	- Trails render according to `cosmetics.trails.show-in` (`always`, `race`, `practice`, `race-and-practice`) and `period-ticks`; revoking the permission stops the trail immediately.
	- Win-based titles with configurable thresholds (defaults: `rookie: 0`, `pro: 5`, `elite: 25`, `legend: 100`); players can equip any unlocked title or fall back to the highest unlocked one.
	- Preferences persist in `player-prefs.yml` (own document, independent from teams; invalid/missing data is ignored on load).
	- Placeholders: `%boatracing_title%`, `%boatracing_title_id%`, `%boatracing_title_wins%`, `%boatracing_trail%`.
- **Version-safe particles and icons**: new `ParticleResolver` with cross-version aliases (`VILLAGER_HAPPY`↔`HAPPY_VILLAGER`, `ENCHANTMENT_TABLE`↔`ENCHANT`, `CRIT_MAGIC`↔`ENCHANTED_HIT`, `SPELL_MOB`↔`ENTITY_EFFECT`, ...), caching and safety checks for particles that require extra data. Cosmetics whose particle does not exist on the running version are hidden from the menu and rendering (`cosmetics.unsupported: "hide"`, `"fallback"` available). Icons and sounds are resolved from names (`IconResolver`, namespace sound keys) so renamed/removed materials never throw; every cosmetic now has its own icon.
- **Particle density (player setting)**: the new Settings tab and `/boatracing cosmetics density <low|normal|high>` let players choose 1/2/4 particles per sample, applied to trails, checkpoint effects and victory firework rockets (`cosmetics.density.*`, `max-victory-rockets` cap). Stored per player in `player-prefs.yml`.
- **Cosmetic shop and purchases**: locked cosmetics show a Vault price and a `[Buy]` click when `cosmetics.shop.enabled` is on; prices live under `cosmetics.shop.prices.<category>.<id>` (0 = free, unset = permissions only) and `gated-by-default: true` makes every cosmetic need a permission, a purchase or a free price. Purchases persist in `cosmetic-unlocks.yml` with optional expiry (`CosmeticPurchaseManager`, periodic cleanup).
- **Admin unlock commands**: `/boatracing cosmetics unlock <player> <category|all> <id|*> [30m|12h|7d|0]`, `/boatracing cosmetics revoke <player> <category|all> <id|*>` and `/boatracing cosmetics unlocks <player>` (permission `boatracing.cosmetics.admin`, default op; works from console and with offline players).
- **Vault integration**: optional `softdepend` on Vault with `VaultEconomy` (`balance/withdraw/format`); servers without Vault keep permissions-only gating.
- **New placeholders**: `%boatracing_particle_density%`, `%boatracing_cosmetic_owned_<category>_<id>%` and `%boatracing_cosmetic_expires_<category>_<id>%`.
- **Extension API**: new stable `es.jaie55.boatracing.api` package (`BoatRacingAPI`, `RaceSessionView`, `PlayerRaceView`, `RaceResult`, `HudProvider`) registered through Bukkit's ServicesManager, with race events (`RaceOpenEvent`, `RaceJoinEvent`, `RaceLeaveEvent`, `RaceStartEvent`, `CheckpointReachedEvent`, `LapCompleteEvent`, `PitStopEvent`, `RaceFinishEvent`, `RaceStopEvent`, `RaceForfeitEvent`, `PracticeStartEvent`, `PracticeFinishEvent`). Events are dispatched on the main thread inside try/catch, so a broken extension never breaks a race. Config: `api.enabled` and `api.log-extensions`. See `API.md`.
	- **Extension HUD hook**: `HudProvider` (sidebar lines + action bar suffix) with `registerHudProvider`/`unregisterHudProvider`; lines are appended after the race content, capped by the scoreboard limit and isolated per provider so a failing extension cannot break the race HUD.
- **Base-managed extension system**: BoatRacing now loads extension jars from `plugins/BoatRacing/extensions/` itself instead of requiring a separate Bukkit plugin.
	- New contract in `api/extension`: `BoatRacingExtension`, `ExtensionContext`, `ExtensionCommand`, `ExtensionStorage` and `ExtensionScheduler`; each jar carries an `extension.yml` descriptor (`name`, `main`, `version`, `api-version`) validated against `BoatRacingAPI.API_VERSION`.
	- BoatRacing creates `plugins/BoatRacing/extensions/<name>/` and extracts the bundled `config.yml` and `lang/messages_*.yml`; extensions get their own folder for config and language files while the active language follows the plugin setting (English fallback). Bundled resources are read from the extension jar itself, and keys accidentally copied from the base config into an extension config are removed on load while extension/custom keys are kept.
	- Extension data is persisted through the BoatRacing `DocumentStore`, so extension documents respect `database.mode` (YAML files under the extension folder, or SQLite/MySQL documents in the shared table).
	- Extension commands are registered under the normal root with tab completion (`/boatracing <name> ...`), and extensions can register Bukkit listeners, HUD providers and `%boatracing_<id>%` placeholders through the context.
	- Extensions can declare their own permissions in `extension.yml` (`permissions:`, with `description` and `default`); BoatRacing registers them on load and removes them on disable, so extension nodes respect their declared default instead of falling back to OP.
	- Base subcommands (`race`, `reload`, `version`, ...) are reserved and cannot be shadowed; duplicate command names across extensions are ignored with a warning, and a failing `onEnable` leaves no listener, HUD line, command or placeholder behind.
	- New `/boatracing extensions [reload]` command (permission `boatracing.extensions`, default op) lists loaded extensions with version, API and commands; `/boatracing debug` includes them, `/boatracing reload` and language changes reload them, and `/boatracing` root tab-completion suggests their subcommands.
- **BoatRacing-PartyExtension (private paid addon, untracked `party/` project)**: party-style base-managed extension (not a Bukkit plugin) loaded from `plugins/BoatRacing/extensions/BoatRacing-PartyExtension-26.3.jar` and commanded through `/boatracing party ...`.
	- Eight abilities: Mushroom (boost), Banana (trap), Green Shell (projectile that slows on hit), Lightning (slows every rival), Super Star (invincibility + boost), Blooper (blindness ink), Coin (party points) and Bob-omb (fused explosion with knockback). Items are granted at race start, by chance on checkpoints and from collectible item boxes.
	- Item boxes are stored by BoatRacing (`boxes.yml` document) and managed with `/boatracing party box add|remove|list`; they respawn on a configurable cooldown.
	- Party points from Coins and finish positions, stored by BoatRacing (`party-stats.yml` document), shown in the sidebar via the extension HUD hook; `/boatracing party points [player]` and `/boatracing party top`.
	- Placeholders `%boatracing_party_points%`, `%boatracing_party_item%`, `%boatracing_party_top1_name%` and `%boatracing_party_top1_points%`; messages in `lang/messages_*.yml` (all bundled languages); permissions `boatracing.party.use` (default true) and `boatracing.party.admin` (default op).
	- Build with `scripts/build-all.ps1` (base `clean install` + extension `clean package`); the extension is skipped with a console warning when its `api-version` is newer than the running BoatRacing API.
- **Stats GUI**: `/boatracing stats [player]` opens a readable menu (player head with team/number/boat/title/trail/effect/wins, results and positions grid) plus a paginated Practice page per track built from `PracticeStatsManager`. The chat report now also lists the player's title, trail and checkpoint effect, and console keeps the text report.
- **Setup Wizard step titles**: every wizard step shows an on-screen title/subtitle (`setup.wizard.subtitle.*`) and a completion title, so admins always know which step they are on.
- **Discord webhook integration**: `discord.*` (`enabled`, `webhook-url`, `username`, `avatar-url`, `events.race-start|race-results|record-broken`).
	- Fully async delivery through `HttpClient`; HTTP/transport errors are logged with a reporting hint; practice sessions never post.
- **Race replay (v1)**: `replay.capture-race` stores the race winner's run in the practice ghost store tagged `source: race`.
	- Captured during the race with the shared `practice.ghost.*` sampling settings; only real finishers can store, and only faster runs replace an existing ghost.
	- Practice playback shows the localized `race.practice.ghost-suffix-race` "(race record)" suffix next to the owner name, and participants receive a `race.replay.stored` chat notice when the record is saved.
- **Diagnostics and bug reporting**: `/boatracing debug` is **admin-only** (`boatracing.debug`, default op).
	- Prints plugin, server, API, Java, storage mode, language, debug level, track/team/active-session counts and the fixed GitHub Issues URL; never prints secrets (no MySQL password, no webhook URL).
	- The report URL is hardcoded (`https://github.com/Jaie55/BoatRacing/issues`) and cannot be changed from the config; the old `diagnostics.*` options were removed. The startup console line now points admins to GitHub Issues.
- **Debug tracing**: `fine`/`finer` entries for cosmetics, trails, Discord, spectator, victory effects, AutoTrace and race replay under the existing `debug` levels.
- **New messages**: `setup.autotrace.*`, `setup.alternate-added|cleared|invalid|none`, `setup.error.addalt|clearalt`, `setup.usage.cmd-addalt|cmd-clearalt`, `race.spectate.*`, `race.victory.*`, `race.replay.stored`, `race.practice.ghost-suffix-race`, `gui.cosmetics.*` (selectors, density and shop lore), `gui.stats.*`, `gui.team.*` cosmetics lines, `gui.common.close`, `gui.race.cp-item-lore-alternates`, `cosmetics.*` (categories, density, buy, unlock/revoke/unlocks, durations and errors), `cosmetics.trail.*` (24 trails), `cosmetics.effect.*`, `cosmetics.sound.*`, `cosmetics.checkpoint.*`, `cosmetics.title.*`, `setup.wizard.subtitle.*`, `plugin.extensions-*` and `plugin.debug-*`. Added to every bundled language under `lang/`; any missing key still falls back to English.
- **Language files moved to `lang/`**: every `messages_*.yml` now lives in `plugins/BoatRacing/lang/` (and `plugins/BoatRacing/extensions/<name>/lang/` for extensions) instead of next to `config.yml`. Existing root files are migrated automatically on startup (renamed to `.migrated` when a `lang/` copy already exists), and bundled resources are read from the jar so the base files can never shadow an extension's own bundles.
- **All bundled languages completed**: the 26.3 keys were translated into every one of the 27 bundled languages (locale checker: 26 OK, 0 errors). Party extension language files also ship for all bundles.
- **Permissions**: `boatracing.cosmetics`, `boatracing.cosmetics.admin` (default op), `boatracing.debug` (default op), `boatracing.extensions` (default op), `boatracing.race.spectate` (default true) and the wildcard `boatracing.cosmetics.unlock.all` (default false). `unlock.all` is granted by `boatracing.admin` but **removed from `boatracing.*`** so a plugin wildcard does not give away the shop.
- **Config sections**: `setup.auto-trace.*`, `replay.*`, `discord.*`, `cosmetics.*` (master switch, `unsupported`, density, trails/custom/disabled, effects, victory-sounds, checkpoints, titles and shop with prices/gating), `racing.victory-effects.*` and `racing.spectate-on-finish.*`.

### Changed
- **Checkpoint abstraction**: crossing now goes through `CheckpointShape`; the AABB slab algorithm moved unchanged from `RaceManager` to `Geometry.segmentIntersectsBox`; `TrackConfig` stores shapes (with `CheckpointGroup` support) instead of regions only; `RaceManager`, `AdminRaceGUI`, `SetupWizard` and `BoatRacingPlugin` were migrated.
- **Setup help and tab-completion**: the `setup` root now suggests `autotrace`, `addalt`, `clearalt` and `setcosmetics`; `autotrace` suggests its subcommands; `addalt`/`clearalt` suggest valid checkpoint indexes; `setcosmetics` suggests `true`/`false`.
- **Root command discovery**: the root now suggests `extensions` and every subcommand registered by a loaded extension (for example `party`), and extension subcommands get tab completion under `/boatracing <name> ...`.
- **Race help and usage**: `/boatracing race help` lists `spectate`, and the root usage line now reads `teams|race|stats|setup|admin|cosmetics|extensions|reload|version|debug`.
- **Diagnostics scope**: `/boatracing debug` is now op-only and its report URL is fixed to GitHub Issues; the `diagnostics.*` config options were removed (old keys in existing configs are ignored).
- **Cosmetics menu** now has five cosmetic tabs (Trails, Titles, Victory effects, Victory sounds, Checkpoint effects) plus a Settings tab, and every tab icon mirrors the equipped cosmetic through the new per-entry icons.
- **Stats command** now opens a GUI for players; console and targeted senders keep the text report.
- **Sound and particle storage**: victory/checkpoint sounds are stored as namespace keys played through the string API, and cosmetic icons are stored as material names resolved with fallbacks, so renamed or removed enum constants never crash the plugin.
- **Legacy lock lists**: `cosmetics.effects.locked`, `cosmetics.victory-sounds.locked` and `cosmetics.checkpoints.locked` still work, but with `cosmetics.shop.gated-by-default: true` every cosmetic is gated through permissions, purchases or free prices.
- **Versioning**: project version is now `26.3` in `pom.xml`.

### Fixed
- Spectators are fully restored (gamemode and previous location) when a race stops, and their follow tasks are cancelled on exit, disconnect or kick.
- Race replay capture is stopped and cleared on `stopRace()` and `reset()`, preventing leftover tasks or stale samples between races.
- AutoTrace removes sessions for disconnected players and disposes the particle task when no sessions remain.
- Legacy AABB checkpoints keep being written in their original shape; old tracks are never rewritten unless edited.
- `PlayerPrefsManager` skips invalid/corrupt UUID keys instead of failing the whole preferences load.
- Discord webhook failures now log a `/boatracing debug` hint instead of a bare message.
- **AutoTrace restart lock**: after stopping, running `start` again now discards the stopped run and records a fresh lap instead of replying "already running", so the command can no longer get stuck.
- **AutoTrace guidance**: `start` now shows a localized on-screen title (`AUTOTRACE` / "Recording your lap..."), announces the exact start coordinates, keeps a particle marker at the start, updates the action bar (time/samples/distance) and sends a periodic chat reminder with points/time/distance configured by `setup.auto-trace.reminder-seconds`. Clickable `[Help] [Stop] [Preview] [Accept] [Cancel]` buttons are printed after start and after gate generation, `/boatracing setup autotrace help` shows a 6-step guide, and after `accept` the plugin gives the selection wand when missing and shows clickable `[Setup wizard]`, `[Open registration]`, `[Get wand]`, `[Add start]` and `[Set finish]` helpers with corner-marking instructions.
- **AutoTrace wand selection**: while previewing, holding the selection wand lets you left-click the nearest gate to select it (rendered with the selection particle, green by default) and right-click to deselect. `resize selected <width> <height>` and `delete selected` edit all selected gates at once, and `status` reports the selected count. Configurable via `setup.auto-trace.wand-select`, `wand-select-radius` and `selection-particle`.
- New AutoTrace config keys `setup.auto-trace.start-marker`, `setup.auto-trace.actionbar` and `setup.auto-trace.reminder-seconds`, plus messages `setup.autotrace.title|subtitle-recording|reminder|next-steps-*|btn-help|btn-setup|btn-open-race|help.*` (EN/ES).
- **AutoTrace stopped state**: running `stop` on an already stopped run now reports the generated gate count and the next steps (`preview`, `accept`, `cancel`) instead of "no data"; `cancel` also clears the action bar.

### Compatibility
- All 26.3 additions are additive: previous `config.yml` files receive the new defaults without overwriting user values; previous bundles fall back to English for new keys.
- Legacy `messages_*.yml` files saved next to `config.yml` are moved into the new `lang/` folder on startup (renamed to `.migrated` when a `lang/` copy already exists); the plugin keeps working without any manual step.
- Teams, racers, stats, practice stats and track data load unchanged across YAML/SQLite/MySQL.
- Old `practice-ghosts.yml` entries without `source` load as practice ghosts; old tracks without `alternates` or `type` load as plain AABB checkpoints.
- `player-prefs.yml` is a new document created on first use; deleting it is safe. Cosmetic purchases live in a new `cosmetic-unlocks.yml` document, also created on first use.
- **Cosmetic gating change**: with the new default `cosmetics.shop.gated-by-default: true`, victory effects, sounds and checkpoint effects now require a permission, a purchase or a free price (`0`). Set it to `false` to keep the previous opt-in behavior where only ids listed in the `locked` arrays were gated.
- Minecraft compatibility remains 1.19–26.3 across CraftBukkit/Spigot/Paper/Purpur/Folia.

### Docs
- README now states Vault compatibility (badge, optional requirements and platform notes), documents the storage backend (`database.mode`, SQLite/MySQL/YAML documents), the cosmetic shop/density/admin commands, all permissions and placeholders, and the `player-prefs.yml`/`cosmetic-unlocks.yml` documents.
- README gained a "Developing Extensions" guide (Maven setup, `extension.yml`, lifecycle, commands, config/messages, storage, scheduler, HUD, placeholders, external plugin model and best practices) plus a `BoatRacing-PartyExtension` section and `/boatracing extensions` documentation.
- New `API.md`: stable API contract, race events and live views, the HUD hook, the base-managed extension loader (`BoatRacingExtension`, `ExtensionContext`, commands, storage, placeholders) and a full developer guide.
- `plugin.yml` now declares `folia-supported: true` and the `boatracing.extensions` permission leaf so the README's Folia/extension claims actually apply.
- README gained a detailed "What's New (26.3)" block plus updated commands, permissions, configuration, placeholders, data files and compatibility (1.19–26.3).
- CHECKLIST includes the full 26.3 QA block: backward compatibility, PLANE gates, AutoTrace, wizard/GUI, config/i18n, base-managed extensions, docs/discovery, Discord, victory effects, spectator, cosmetics, debug, alternate routes and race replay.
- All 27 bundled bundles are complete for the 26.3 keys and live in `lang/`; `tools/check_locales.py` reads that folder and reports 26 OK / 0 errors.

## 26.2.1 — 11/09/2026
### Added
- **Global race placeholders**: `%boatracing_race_running%`, `%boatracing_race_registering%`, and `%boatracing_race_status%` now resolve across every track session, so a single scoreboard/hologram line can react when any race opens or starts. `%boatracing_race_status%` returns `running`, `registering`, or `idle`, with running taking precedence when multiple tracks are active.
- **Translatable race status labels**: `%boatracing_race_status%` labels now come from `placeholder.race-status.*` in `messages_<lang>.yml` and are translated across all 27 bundled languages (for example `running`/`registering`/`idle` in English and `en curso`/`registro`/`inactiva` in Spanish).
- **11 new community language bundles**: added `uk` (Українська), `id` (Bahasa Indonesia), `ar` (العربية), `nl` (Nederlands), `cs` (Čeština), `vi` (Tiếng Việt), `th` (ไทย), `tl` (Filipino), `da` (Dansk), `no` (Norsk), and `fi` (Suomi). Every bundle is fully translated from the English file with the same 719 keys and line-for-line structure, including localized `placeholder.race-status.*` labels. Bundled languages are now 27 (2 official + 25 community).
- **Locale validation tool**: `tools/check_locales.py` compares every `messages_*.yml` against `messages_en.yml` and reports missing/extra/duplicate keys, placeholder mismatches, colour/escape differences, and possible untranslated text.

### Fixed
- **Duplicate `admin.help.language` key**: removed a wrongly placed usage line that silently overrode the help entry in 11 community bundles (`fr`, `it`, `ja`, `ko`, `pl`, `pt_BR`, `pt_PT`, `ru`, `sv`, `tr`, `zh_CN`).
- **Registration-time messages in community bundles**: `es_419`, `it`, `ja`, `ko`, `pl`, `pt_BR`, `pt_PT`, `ru`, `sv`, `tr`, `zh_CN`, and `zh_TW` now define `setup.show.regtime` and `setup.error.setregtime` (replacing the obsolete `race.status.regtime`), so registration-time output is no longer shown in English.
- **Completed translations in existing bundles**: translated 353 remaining English strings across 22 bundles (start-light messages, registration-time wizard steps, navigation/status/admin labels, download/usage lines, etc.), so no bundled message text falls back to English. The only intentionally identical value is `gui.admin.lore-color` in `es`/`es_419` because `Color` is also the Spanish word.
- **Uniform bundle structure**: all 27 `messages_*.yml` files now mirror `messages_en.yml` exactly (758 lines, same key order, blank lines and CRLF endings), so the bundles stay consistent and diff cleanly. The locale checker now also rejects keys that have an inline value and child keys (a broken YAML shape).

## 26.2 — 24/06/2026
### Added
- **MC-MrBirdy credited as a GitHub contributor** for ongoing contributions across multiple releases.
- **New version numbering**: aligned with Minecraft's new `YY.D.H` system (Year.Drop.Hotfix).
- **Per-track broadcast mode**: `racing.broadcast-mode` accepts `global` or `racers`, overridable per track. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Per-track lobby configuration**: lobby settings can now be set per track for dedicated lobbies. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Per-track rewards configuration**: rewards can now be overridden per track. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Admin `-p:` target flag**: admins/console can target other players for commands by appending `-p:<player>`. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Start lights wand UX**: Shift+Left Click a Redstone Lamp with the wand to add it as a start light, Shift+Right Click to remove. Lamp lights up with green particles on add, smoke on remove. Wand lore shows light instructions in all 15 languages.
- **Restart command**: `/boatracing race restart [track]` stops any running race, re-opens registration, re-joins previous participants, and force-starts. Auto-detects track if sender is in an active race. Permission: `boatracing.race.admin` or `boatracing.setup`.
- **Per-track registration time**: `registration-seconds` can now be set per track via `/boatracing setup setregtime <seconds>`. Configurable in the setup wizard as step 8/8. Displayed in `setup show`. Reported by [@supershootstudions-lgtm](https://github.com/supershootstudions-lgtm) in [#6](https://github.com/Jaie55/BoatRacing/issues/6).
- **Clearlobby command**: `/boatracing setup clearlobby` disables the registration lobby for the current track.
- **Debug logging levels**: set `debug` in `config.yml` to `"off"` (default), `"severe"`, `"warning"`, `"info"`, `"fine"`, `"finer"`, or `"finest"`. Controls console log verbosity. Also applies on `/boatracing reload`.

### Changed
- **UpdateChecker**: version comparison now supports `YY.D.H` format natively. Errors always logged to console.
- **Wizard LIGHTS step**: `description` and `hint` now translated to all 15 languages.
- **Setup summary**: laps count now included in the final setup wizard summary.
- **Wizard step numbering**: updated from 1/7–7/7 to 1/8–8/8 to include the new REGTIME step.

### Fixed
- Action bar now properly cleared after forfeiting a race. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Forfeit command correctly uses resolved player reference instead of raw sender. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Lobby section null-safety when configuration is missing. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Reward section null-safety when configuration is missing. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Cross-track reward leakage prevented (stable global fallback). Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- Broadcast mode string comparison now uses `.equals()` instead of `==`. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- `-p:` flag no longer interferes with downstream argument length checks. Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#5](https://github.com/Jaie55/BoatRacing/pull/5).
- **Setup wizard `setlaps`**: now correctly displays and persists custom lap values in the wizard prompt.
- **Setup show**: fixed untranslated status labels (`general.yes`/`general.no` → `setup.status-yes`/`setup.status-no`). Added lap count and registration time display. Excluded laps and registration-seconds from the raw overrides section.
- **Messages YAML**: indentation corrected for `cancelled-no-participants` and `restarted` keys across all 16 locales.
- **Race finish vehicle cleanup**: players now properly dismount their boat when finishing a race with the lobby disabled.

## 1.1.6 — 17/06/2026
### Added
- **Admin Race checkpoint editor UI**: added checkpoint management directly in the race admin inventory with paginated listing and in-place actions (add from selection, replace, remove, move up/down).
- **Admin Race pitstop quick controls**: added mandatory pitstop quick-set actions (`0/1/2`) plus custom anvil input from the race admin inventory.
- **Setup selection particle visualizer**: added built-in wand selection wireframe rendering using particles, configurable under `setup.selection-visualizer.*` (`enabled`, `period-ticks`, `particle`, `spacing`, `max-particles-per-player`, `view-distance`, `show-only-with-wand`).
- **Explicit track+laps placeholders**: added lap-scoped variants for comparative panels: `%boatracing_track_best_player_laps_<track>_<laps>%`, `%boatracing_track_best_time_laps_<track>_<laps>%`, `%boatracing_track_best_time_ms_laps_<track>_<laps>%`, plus `%boatracing_track_top_1|2|3_player|time|time_ms_laps_<track>_<laps>%`.
- **Stats placeholders by track and laps**: added best-race/best-lap variants for viewer and top contexts scoped by track and by track+laps (for example `%boatracing_player_best_race_laps_<track>_<laps>%` and `%boatracing_top_player_best_race_name_laps_<track>_<laps>%`).
- **Forfeit command** (`/boatracing race forfeit`): players can abandon a running race without stopping it for others. Permission `boatracing.race.forfeit` (default: true). Contributed by [@MC-MrBirdy](https://github.com/MC-MrBirdy) in [#3](https://github.com/Jaie55/BoatRacing/pull/3).
- **DNF results display**: forfeited players appear as `DNF {player} (forfeited)` at the bottom of race results, separated from finishers. Finishers keep normal positions and rewards; forfeited players do not receive rewards or win/loss stats.
- **Practice ghost replay (advanced vanilla MVP)**: solo practice sessions now capture sample paths and replay the best recorded ghost alongside the current run. Ghost entities (boat + armorstand) use no-collision rules, are hidden from other players, and interpolate smoothly with angular rotation.
- **PracticeGhostManager**: new manager class for ghost persistence per track and lap count, using the DocumentStore backend. Ghosts are stored in `practice-ghosts.yml`.
- **DocumentStore persistence layer**: new pluggable storage interface with three backends — YAML files (`YamlFileDocumentStore`), local SQLite (`JdbcDocumentStore` with SQLite dialect), and remote MySQL (`JdbcDocumentStore` with MySQL dialect). Backend selected via `config.yml` → `database.mode`.
- **DocumentStoreFactory**: resolves the active store implementation from config and provides automatic legacy-YAML migration on first load when switching to SQLite/MySQL.
- **Database config section**: new `database` block in `config.yml` with mode (`YAML`/`SQLITE`/`MYSQL`), table name, SQLite file path, and MySQL connection settings.
- **Practice ghost config section**: new `practice.ghost` block in `config.yml` with enable toggle, sample/playback tick rates, minimum distance filter, sample cap, and name display option.
- **Practice leave command**: `/boatracing race practice leave <track>` (and `exit` alias) to cleanly exit a practice session. Also handles disconnect/quit/kick via `PlayerQuitEvent` and `PlayerKickEvent` handlers.
- **AnvilGUI 26.1/26.2 compatibility**: switched from upstream `net.wesjd:anvilgui:1.10.12-SNAPSHOT` to the `feeeedox/AnvilGUI` fork via JitPack. Added `Wrapper26_R1_LocalShim` as compatibility fallback for 26.1 close-event handling. Extended `VersionMatcher` with 1.19.x mappings, `26.1.2`, and `26.2`/`26.2.1`→`26_R2` entries, loading native `Wrapper26_R2` from the fork on 26.2.
- **Race result forfeited message**: added `race.results.forfeited` key to all 16 bundled language files with translations.

### Changed
- **Track-scoped Admin Race actions**: race/setup operations in Admin Race GUI now resolve the active track session explicitly, so edits and status reflect the selected track context.
- **GUI setup persistence path**: laps/pitstops/checkpoint edits issued from Admin Race GUI now write through track configuration overrides as part of the same action flow.
- **Setup override synchronization**: `/boatracing setup setlaps <n>` and `/boatracing setup setpitstops <n>` now also update the active in-memory race session for the selected track when that session already exists.
- **Setup command docs clarity**: README setup command descriptions now state that laps/pitstops overrides apply immediately without restart/reload.
- **Lap-context track records/placeholders**: track bests are now stored and resolved per lap count (`bestTimesByLaps`), preventing 2-lap records from overriding 3-lap placeholders (and vice versa) across `%boatracing_track_best_*%`, `%boatracing_track_top_*%`, and `%boatracing_player_track_best%`.
- **Selection visualizer edge distribution**: particle allocation is now balanced across all box edges and visibility culling is performed against the full selection box distance, reducing inconsistent partial rendering.
- **Forfeit broadcast scoping**: `forfeit()` now uses `raceAudience()` (participants + admins) instead of global `broadcast()` for the `{player} forfeited` message in competitive mode. Practice mode forfeits show a private `You left the practice session` message only to the runner.
- **TeamManager, StatsManager, PracticeStatsManager refactored**: all three managers now read/write through `DocumentStore` instead of direct `File` + `YamlConfiguration.save/load`. Legacy YAML files are automatically migrated to the database on first load when using SQLite/MySQL mode.
- **config.yml restructured** with section header comments and new `database` / `practice.ghost` blocks.
- **Race state marking**: `RaceState` now includes a `forfeited` flag. `forfeit()` marks the state instead of removing it, so `checkAllFinished()` triggers correctly and `announceResults()` can display the player as DNF.
- **Project version**: bumped to 1.1.6. Minecraft support range extended from 26.1 → 26.2.

### Fixed
- **Stale per-track override reads in existing sessions**: race settings load now refreshes track data before reading `racing.*` overrides, preventing old values from being reused in cached track sessions.
- **`setlaps` runtime mismatch**: fixed issue where `/boatracing setup setlaps <n>` acknowledged the new value but `/boatracing race open <track>` could still open registration with a previous lap count until server restart.
- **`setpitstops` runtime mismatch**: fixed equivalent stale-value behavior for per-track `mandatory-pitstops` after `/boatracing setup setpitstops <n>`.
- **Track setup editability after creation**: added granular setup editing commands so admins can remove one start/light by index (`removestart`, `removelight`) and clear finish/default pit (`clearfinish`, `clearpit`) without rebuilding the whole track config.
- **Admin Race GUI i18n regression**: removed newly introduced hardcoded user-facing strings and routed checkpoint/pitstop/editor labels, lore, and action feedback through message keys.
- **Localized coverage gap for new GUI keys**: added missing `gui.race.*` keys for all bundled locales so non-EN bundles no longer miss editor/pagination/pitstop texts.
- **Stats tab-complete DataConverter spam**: player-name suggestions for `/boatracing stats <player>` no longer rely on `OfflinePlayer#getName()` in tab-complete paths, avoiding repeated Paper 1.21 `Failed to convert json to nbt` / `MalformedJsonException` console errors on malformed legacy playerdata.
- **Setup wand text key fallback**: fixed wand name/lore rendering when message bundles store those entries under `setup.usage.*`; setup wand now resolves localized text instead of showing raw key strings.
- **Selection wireframe truncation under tight budgets**: fixed abrupt edge cutoffs in large selections when particle limits are reached.
- **DocumentStore initialization order**: `DocumentStoreFactory.create()` now runs before `TeamManager` instantiation in `onEnable()`, so TeamManager receives a valid store reference instead of null (which caused silent team data loss).
- **Scoreboard DataConverter spam on Paper 26.1+**: replaced remaining `OfflinePlayer#getName()` calls in the scoreboard timer and result name resolution with `safeOfflineName()` that reads the player profile via reflection, avoiding `Failed to convert json to nbt` console errors from malformed legacy playerdata.
- **SimpleScore sidebar reclamation during races**: the scoreboard timer now periodically re-hides SimpleScore (every ~2 seconds) during active races and practice sessions, preventing the external sidebar from overwriting the BoatRacing race display mid-session.
- **`leavePractice()` vehicle cleanup**: changed from per-player `cleanupRaceVehicleForPlayer()` to full `cleanupRaceVehicles()` for consistency.
- **Removed unused `{track}` placeholder** from `race.practice.left` message call in `leavePractice()`.
- **Runtime JDBC driver loading**: SQLite and MySQL drivers are no longer shaded into the JAR. On first startup, the plugin downloads the required JDBC JARs (`sqlite-jdbc`, `mysql-connector-j`, `slf4j-api`, `slf4j-simple`) from Maven Central with SHA-256 checksum verification and caches them in `plugins/BoatRacing/lib/`. If the libs are unavailable (no internet, download failure, checksum mismatch) the plugin falls back gracefully to YAML mode. This keeps the plugin JAR at ~675 KB instead of ~18 MB.
- **Practice forfeit no longer shows results header**: `forfeit()` in practice mode now calls `stopRace(false)` instead of `checkAllFinished()` → `stopRace(true)`, skipping the `Results:`/DNF display since there is no competitive outcome for a forfeited practice run.
- **SQL injection hardening in JdbcDocumentStore**: the `database.table` config value is now sanitized to `[A-Za-z0-9_]` before use in DDL statements, preventing injection through the table name.
- **Ghost collision team name bounds**: UUID substring extraction for the no-collision team name now uses `Math.min(9, uuidStr.length())` to guard against edge cases.
- **Checkpoint/finish line fast-crossing detection**: `tickPlayer()` now uses ray-AABB segment intersection to detect when a boat passes through a checkpoint or finish region between movement ticks, preventing missed crossings at high boat speeds (ice boats, speed boosts).
- **Countdown cancellation on practice leave**: `clearCountdownLock()` now cancels the scheduled countdown timer tasks, so leaving practice during pre-start lights no longer allows the countdown to continue and start a race.
- **Hardcoded usage message removed**: the practice leave usage text now uses the `race.usage.practice-leave` message key across all language bundles.
- **SimpleScore sidebar flickering**: the scoreboard timer now re-hides SimpleScore immediately on the first tick instead of waiting 2 seconds, preventing brief sidebar takeover at race/practice start.

### Stats Persistence
- **`stats.yml` (competitive aggregate)** stores `playerWins`.
- **`stats.yml` (competitive aggregate)** stores `teamWins`.
- **`stats.yml` (competitive aggregate)** stores `playerPositions`.
- **`stats.yml` (competitive aggregate)** stores `playerBestRace` (global per player).
- **`stats.yml` (competitive aggregate)** stores `playerBestLap` (global per player).
- **`stats.yml` (competitive aggregate)** stores `playerBestRaceByTrack` (per player and track).
- **`stats.yml` (competitive aggregate)** stores `playerBestLapByTrack` (per player and track).
- **`stats.yml` (competitive aggregate)** stores `playerBestRaceByTrackLaps` (per player, track, and lap count).
- **`stats.yml` (competitive aggregate)** stores `playerBestLapByTrackLaps` (per player, track, and lap-context).
- **`practice-stats.yml` (practice telemetry)** remains separate and stores run/lap/sector best/last per player and track.

### Docs
- Added 1.1.6 release notes and QA coverage in README, CHANGELOG, and CHECKLIST (including checkpoint editor, pitstops GUI checks, forfeit, ghost replay, and DocumentStore features).
- Removed 26.1 snapshot warning from README (26.2 is now fully supported).

## 1.1.5 — 06/04/2026
### Added
- **Solo practice mode command**: added `/boatracing race practice <track>` to start a one-player practice race on a ready track without requiring the minimum race player threshold.
- **Player stats command**: added `/boatracing stats [player]` to show competitive position counts by place (showing only non-zero places) and practice snapshots (best/last run/lap/sectors) for self or target player.
- **Practice permission node**: added `boatracing.race.practice` with default `true`, so servers can grant/restrict solo practice independently from race-admin permissions.
- **Stats permission node**: added `boatracing.stats` with default `true`, so servers can grant/restrict stats access independently.
- **Stats-others permission node**: added `boatracing.stats.others` with default `true`, so servers can separately control viewing other players via `/boatracing stats <player>`.
- **Map-vote open permission node**: added `boatracing.race.voteopen` with default `op`, so opening map votes can be granted/restricted independently from race start/stop management.
- **Admin language switch command**: added `/boatracing admin language <code>` to change the active plugin language at runtime.
- **Admin language permission node**: added `boatracing.admin.language` with default `op`, so language switching can be delegated without full admin management access.
- **Persistent practice telemetry**: added `practice-stats.yml` storing per-player/per-track practice metrics (best/last run, best/last lap, best/last sector split).
- **Practice placeholders**: added `%boatracing_player_practice_*%` placeholders (current-track and `<track>` token variants) and `%boatracing_track_practice_running_<track>%` for practice state displays.
- **Practice placeholder compatibility alias**: added `%boatracing_track_practicerunning_<track>%` as alias for track practice-running state.
- **Swedish language bundle**: added bundled `messages_sv.yml` as an unofficial community translation (`sv`) with full message coverage and preserved placeholders/color codes.

### Changed
- **Same-track race/practice locking during countdown**: a track now stays busy while practice countdown is active, preventing race open/start/force on that same track until practice ends (other tracks remain independent).
- **Practice chat scope**: practice countdown/split/lap/result updates are now private to the practicing player instead of global race-style broadcasts.
- **Localized practice marker in sidebar**: when solo practice is active, the race sidebar now renders a language-aware label from `race.scoreboard.practice-label` (for example `PRACTICE`/`PRÁCTICA`).
- **Practice lobby return parity**: solo practice now preserves pre-practice location context so finish flow uses the same lobby/back UX (`/boatracing race back` window + hint) as standard race finish/cancel.
- **Admin Tracks GUI interaction model**: track items now support right-click rename (left-click load, shift-right-click delete), and the lore now shows all three actions in bundled languages.
- **Map vote flow for mixed clients**: `/boatracing race voteopen` now supports opening all saved tracks (`all` or no explicit track list), broadcasts a plain vote command hint (`/{label} race vote <track>`) alongside clickable UI, and auto-opens registration on the voted winner when possible.
- **Map vote command syntax and tab-complete**: admin usage/help and tab suggestions now include `voteopen [all|<track1> <track2> ...] [seconds]` with `all`/`*` support.
- **Runtime language switching flow**: changing language through `/boatracing admin language <code>` now persists into `config.yml` and reloads message bundles immediately (no separate reload command required by the user).
- **Admin language command alias cleanup**: removed `/boatracing admin lang <code>`; only `/boatracing admin language <code>` is now accepted and suggested.
- **Shade packaging metadata handling**: Maven shade config now filters duplicate `META-INF` manifest/signature files, removing noisy overlap warnings during package builds.

### Fixed
- **Track record placeholders not refreshing after improved times**: `%boatracing_track_best_*_<track>%` and `%boatracing_track_top_1..3_*_<track>%` now prefer live race-session data (or track file data) instead of stale in-memory selected-track data, so better race times update correctly.
- **Current-track best placeholders consistency**: `%boatracing_track_best_player%` and `%boatracing_track_best_time%` now resolve through the same track-token path to avoid stale values when races run in separate track sessions.
- **Map-vote fallback visibility**: when winner auto-open fails, the `race.vote.next-step` hint is no longer broadcast to all players; it is now sent only to vote-managing users (and console).
- **Localized track readiness requirements**: `race.track-not-ready` requirement details are now resolved through `race.requirements.*` keys across all bundled locales, avoiding mixed-language output.
- **Community locale wording regressions**: restored expected phrasing for `setup.laps-set` (FR/PL), restored PT-PT admin tracks help wording, and replaced leaked `BoatType` tokens with localized labels in RU (`тип лодки`) and zh_CN (`船只类型`) admin `setboat` help/usage lines.
- **Stats chat readability**: practice stats output now suppresses empty sector noise (e.g. `0:00.000` entries), and unsaved practice tracks use localized `stats.track-unsaved-label` names across bundled locales.

## 1.1.4 — 04/04/2026
### Added
- **Track best-record placeholders by token (NEW 1.1.4)**: `%boatracing_track_best_player_<track>%`, `%boatracing_track_best_time_<track>%`, and `%boatracing_track_best_time_ms_<track>%` for per-track record labels in static scoreboards/holograms.
- **Track top-3 placeholders by token (NEW 1.1.4)**: `%boatracing_track_top_1_*_<track>%`, `%boatracing_track_top_2_*_<track>%`, and `%boatracing_track_top_3_*_<track>%` (`player`, `time`, `time_ms`) for podium/leaderboard layouts per track.

### Fixed
- **Scoreboard tie-break by checkpoint arrival order**: when racers are on the same lap and same checkpoint count, the one who reached that checkpoint first stays ahead. This avoids position swaps on equal checkpoint entry.
- **Setup wizard open-registration fallback command**: if no named track is selected yet, wizard Done now uses `/boatracing race open unsaved` instead of an invalid placeholder token (`/boatracing race open <track>`), preventing `track not found <track>`.

### Docs
- **Placeholder docs for 1.1.4**: README placeholder table now marks `%boatracing_track_best_*_<track>%` and `%boatracing_track_top_1..3_*_<track>%` as `NEW (1.1.4)`, and CHECKLIST includes explicit QA checks for these placeholders.

## 1.1.4-26.1-SNAPSHOT (snapshot-26.1-gui-fallback-01) — 02/04/2026
### Added
- **Snapshot channel for Paper 26.1 validation**: introduced the first 26.1 snapshot build named `snapshot-26.1-gui-fallback-01`.
- **Snapshot warning docs/badges**: README now includes explicit `WARNING snapshot` and implementation/risk badges for this build stream.
- **Configurable minimum racers to start**: added `racing.min-players-to-start` as a global race-start threshold, with per-track override support via `tracks/<name>.yml` under `racing.min-players-to-start`.
- **Localized minimum-player warning**: added `race.not-enough-players` to bundled language files to report required/current participant counts when start is blocked.

### Changed
- **Project versioning for pre-release**: project version moved to `1.1.4-26.1-SNAPSHOT` in `pom.xml`.
- **Stable tag status**: stable `1.1.4` publication is postponed due to the Paper 26.1 GUI/Anvil compatibility issue; the planned 1.1.4 feature set is delivered in this snapshot channel.

### Fixed
- **Paper 26.1 Anvil close-event compatibility**: added a custom AnvilGUI version-matcher path plus `Wrapper26_R1_Fixed` fallback implementation that probes compatible `handleInventoryCloseEvent` signatures and applies a safe close-container fallback when upstream signature assumptions fail.
- **Race start gating consistency**: `start`, `force`, admin race GUI start, and registration timeout auto-start now consistently enforce the configured minimum-player threshold before race launch.

### Docs
- **Minimum-player start docs**: README/CHECKLIST now document `racing.min-players-to-start`, per-track override behavior, and validation expectations for blocked starts.

### Known Issues
- **GUI risk on evolving 26.1 builds**: because this snapshot relies on reflective compatibility over unstable internals, some GUI/Anvil flows can still fail on certain Paper 26.1 dev builds or non-Paper forks.

## 1.1.3 — 30/03/2026
### Added
- **Parallel race sessions by track**: race lifecycle commands now operate per track session, allowing multiple tracks to run registration/races at the same time.
- **Map vote commands**: added `/boatracing race voteopen <track1> <track2> [seconds]`, `/boatracing race vote <track>`, `/boatracing race voteui`, `/boatracing race votestatus`, and `/boatracing race voteclose`.
- **Track-scoped race placeholders**: `%boatracing_track_race_running_<track>%`, `%boatracing_track_race_registering_<track>%`, and `%boatracing_track_race_status_<track>%` now resolve against the requested track session.

### Fixed
- **Selected boat variant persistence on spawn**: race boats now re-apply the selected boat/raft variant right after spawn and on delayed retries, fixing cases where a chosen variant (for example `DARK_OAK`) appeared as default `OAK`.
- **No dismount during countdown/race**: racers can no longer manually exit boats/rafts during the 5-light countdown or while the race is running.
- **Vote-open UX prompt**: map-vote start now includes a clickable chat action to open vote UI (`/boatracing race voteui`) instead of only a typed command hint.
- **Reward command parsing compatibility**: reward distribution now supports both `commands` (list) and legacy `command` (single string), with safer fallback behavior for missing per-position keys (including position `1`).

### Docs
- **Track placeholder docs**: README now documents track-scoped placeholders, compatibility aliases, and per-track session evaluation behavior.
- **Race/map-vote docs**: README now includes the new map vote commands (including `voteui`) and multi-track race behavior notes.
- **QA additions for 1.1.3**: CHECKLIST now includes explicit validation steps for track-scoped placeholders and selected boat variant spawn reliability.
- **Rewards schema guidance**: README/CHECKLIST now document reward command compatibility and recommend explicit `commands: []` in each position block.

## 1.1.2 — 29/03/2026
### Added
- **PlaceholderAPI integration**: BoatRacing now registers `%boatracing_*%` placeholders for holograms/scoreboards.
- **Persistent aggregate stats**: new `stats.yml` storage for player wins, team wins, best race, and best lap values used by placeholders.
- **Race back command**: added `/boatracing race back` so players can manually return to their saved pre-lobby location after race flows.
- **Post-race lobby return UX**: after a race ends or is cancelled, participants are teleported to the configured race lobby, receive a clickable back shortcut, and have a 3-minute window to return.
- **French language bundle**: added bundled `messages_fr.yml` (community translation) with complete race/setup/team/admin/gui coverage.
- **Additional community language bundles**: added bundled `messages_es_419.yml`, `messages_pt_BR.yml`, `messages_pt_PT.yml`, `messages_de.yml`, `messages_it.yml`, `messages_pl.yml`, `messages_tr.yml`, `messages_ja.yml`, and `messages_ko.yml`, expanding built-in language coverage.

### Changed
- **Setup Wizard compact mode**: wizard prompts were shortened and reorganized by step to reduce chat text while keeping actionable buttons.
- **Registration announce source**: registration announce text is now language-specific in `messages_<lang>.yml` (`race.registration.announce`) instead of `config.yml`.
- **Registration announce placeholders**: `race.registration.announce` now consistently supports `{track}`, `{laps}`, `{cmd}`, `{label}` across bundled language files.
- **Lobby return flow**: pre-lobby locations are preserved during race start, and race-back return entries now expire automatically after 3 minutes (in-memory only).
- **Race help and tab-complete**: `race back` is now included in help output and tab suggestions for players with `boatracing.race.back`.
- **Language bundle loading**: language selection now supports bundled and custom `messages_<lang>.yml` files from the plugin folder, with safe fallback to English.
- **Translation status headers**: bundled language headers now consistently mark `messages_en.yml` and `messages_es.yml` as official translations, and all other bundled languages as unofficial community translations.
- **Chinese locale split**: kept `messages_zh_TW.yml` for Taiwan Traditional Chinese and added `messages_zh_CN.yml` for Mainland Simplified Chinese.
- **Race back window configurability**: the `/boatracing race back` availability window is now configurable with `racing.lobby.back-window-seconds`.

### Fixed
- **Build break in TeamManager**: fixed malformed package declaration in `TeamManager.java` that caused compilation failure.
- **Race boat entity cleanup**: race-spawned boats/rafts are now tracked and removed deterministically on finish/cancel/reset, preventing leftover vehicle entities in the world.
- **Race back expiry notification**: players now receive an automatic message when the back window expires, instead of only seeing it after manually running `/boatracing race back`.

### Docs
- **Placeholder reference added**: README now documents available `%boatracing_*%` placeholders and team lookup formats.
- **Lobby-back docs updated**: README and QA checklist now document the clickable `race back` flow and 3-minute return window as part of 1.1.2.

## 1.1.1 — 13/03/2026
### Added
- **Registration lobby mechanic (optional)**: new `racing.lobby.*` config block to send registered players to a lobby location while registration is open, with optional return to their previous location on leave/cancel.
- **Lobby setup command**: added `/boatracing setup setlobby` to save the admin's current position as the registration lobby and enable it instantly.
- **Lobby i18n messages**: added localized feedback when players are teleported to the race lobby and when they are returned.
- **SimpleScore integration hook**: when SimpleScore is present, BoatRacing now uses its viewer hide/show flow during races to avoid sidebar ownership conflicts and restore the external scoreboard cleanly after stop/cancel.

### Fixed
- **Race track selection flow for active track**: `race open/join/leave/force/start/stop/status` no longer force a disk reload when the requested track is already the active one (notably `unsaved`), preventing stale-state issues.
- **Track reload consistency**: `TrackConfig.load()` now clears all in-memory collections (`customStartSlots`, `bestTimes`, and others) before reading from disk.
- **Registration timer race-loop bug**: fixed a state bug where `race open` timer callbacks could survive manual `start/force/stop` flows and re-trigger race starts (infinite restart behavior). Registration sessions are now invalidated/cancelled atomically.
- **Manual start stale-callback guard**: `/boatracing race start <track>` now closes the registration window first to prevent old registration callbacks from restarting/overlapping race state.
- **External sidebar handoff reliability**: improved race HUD/sidebar cleanup so external sidebar plugins (notably SimpleScore and TAB) recover cleanly after race stop/cancel without requiring relog.

### Changed
- **Setup click actions UX**: setup wizard and admin setup tips now use suggest-in-chat behavior for commands requiring arguments, so players can tab-complete before executing.
- **Compatibility matrix clarified**: this plugin jar is intended for Bukkit-family servers (CraftBukkit/Spigot/Paper/Purpur) and now includes Folia-compatible scheduling paths.

### Docs
- **Platform scope documented**: README/CHECKLIST now explicitly state that Sponge requires a separate platform port and that Velocity/BungeeCord (proxy layer) cannot run gameplay logic from this plugin jar.
- **Explicit SimpleScore compatibility note**: docs now explicitly list compatibility with SimpleScore (GitHub: https://github.com/RuiPereiraDev/SimpleScore, Modrinth: https://modrinth.com/plugin/simplescore).
- **Explicit TAB compatibility note**: docs now explicitly list compatibility with TAB (GitHub: https://github.com/NEZNAMY/TAB, Modrinth: https://modrinth.com/plugin/tab-was-taken).

## 1.1.0 — 12/03/2026
### Added
- **Multi-language support**: messages system now supports English (default) and Español (España). Configure the language in config.yml via the `language` setting ("en" or "es"). All user-facing text is externalized in messages_en.yml or messages_es.yml (stored in the plugin data folder after first run).
- **New community translations**: added Traditional Chinese (`messages_zh_TW.yml`) and Russian (`messages_ru.yml`) language files. Both include a clear warning that they are unofficial translations and should be reviewed.
- **Player-controlled race management**: new config option `player-actions.allow-player-race-start` (default: false) lets non-admin players open, start, force-start and stop races globally. Per-track override available via `racing.allow-player-start` in tracks/<name>.yml (uses the track config override system to allow selective enablement per track).
- **Reward system**: fully customizable race completion rewards with support for finishing positions, configurable commands, player messages, and broadcast announcements. Positioned in config.yml under `racing.rewards` with enable/disable toggle, position-specific settings (1st/2nd/3rd/default), and placeholder support ({player}, {position}, {time}, {track}, {laps}).
- **Race performance optimization**: PlayerMoveEvent listener now throttles unnecessary checkpoint checks by comparing only when the player moves to a different block (not within the same block).
- **Complete i18n infrastructure**: MessageManager utility loads language files dynamically; all race, setup, team, and admin commands now use externalized messages. New messages can be added and will merge with defaults on reload without overwriting customizations.

### Changed
- **Permission flow refinement for race commands**: `race open/start/force/stop` now check track existence and load per-track settings before validating permissions, enabling per-track player-start override checks in a single canManageRace() helper.
- All hardcoded messages across the plugin (BoatRacingPlugin, RaceManager, AdminGUI, AdminRaceGUI, AdminTracksGUI, UpdateNotifier, SetupWizard, TeamGUI, TrackConfig) replaced with i18n `msg().get()` calls.
- Config option for team actions restructured: moved and renamed from various checks to `player-actions` section for consistency.

### Fixed
- **Setup Wizard i18n key mapping**: fixed wrong translation key paths in wizard navigation/summary prompts that could show raw keys (for example `setup.wizard.nav-label`) instead of localized text.
- **Scoreboard compatibility with external plugins**: race sidebar now preserves and restores each player's previous scoreboard instead of forcing the main scoreboard, preventing conflicts with plugins such as SimpleScore.

### Docs
- CHANGELOG and CHECKLIST updated for 1.1.0 with full feature list and verification steps.
- Version number incremented from 1.0.9 to 1.1.0.

## 1.0.9 — 19/08/2025
### Added / Changed
- Official support range declared: 1.19 → 1.21.11 (Bukkit/Spigot compatible; works on Paper/Purpur). Requires Java 17+; plugin.yml api-version set to 1.19.
- Documented supported servers: Purpur, Paper, Spigot, CraftBukkit (Bukkit-compatible forks may work; Folia/Sponge/Forge hybrids not supported).
- Classified as Bukkit/Spigot on Paper by excluding `paper-plugin.yml` from the jar and using only Bukkit-safe APIs for metadata.
- Documentation updated: README, CHANGELOG and QA checklist in EN/ES.
- Updater cadence: background check every 5 minutes; when a new version is first detected during runtime, print a console WARN immediately (once per version). Hourly console reminder aligned to each hour (00:00, 01:00, …) while outdated (config-gated). Admin join always shows an in‑game chat notice (if enabled) and never prints to console.

### Fixed
- Boat/Raft materials on mixed APIs: dynamic Material resolution for boat/raft variants (including Bamboo Raft and Pale Oak) removes NoSuchFieldError on older bases and avoids CraftLegacy warnings.
- Command metadata on Spigot: restored by replacing Paper-only `getPluginMeta()` with Bukkit `getDescription()`.

## 1.0.8 — 19/08/2025
### Added
- Config toggles to customize the sidebar and ActionBar visibility:
	- `racing.ui.scoreboard.show-position|show-lap|show-checkpoints|show-pitstops|show-name`
	- `racing.ui.actionbar.show-lap|show-checkpoints|show-pitstops|show-time`
- HUD pitstops: when `racing.mandatory-pitstops > 0`, show “PIT A/B” on the sidebar and ActionBar (config‑gated).
- Registration broadcast now includes the track name and the exact join command using `racing.registration-announce` template.
- Setup Wizard: new optional step to set “Mandatory pit stops” with quick buttons [0] [1] [2] [3].
- Setup command `/boatracing setup setpitstops <n>` to update and persist `racing.mandatory-pitstops`.
- Finish‑without‑checkpoints: added a clear player message when trying to finish without all required checkpoints for the lap (sound remains).
- Results broadcast now highlights the podium: 🥇/🥈/🥉 medals and rank colors for the top‑3.

### Changed
 - Race tab-complete now shows `join|leave|status` to all players; admin actions suggested only to admins.
- `race status` can be viewed by any player (keeps default permission true).
- Sidebar order switched to “L/CP - Name” and removed centering/padding.
- Names are shown as-is (keeps leading '.' for Bedrock players via Geyser/Velocity).
 - Results lines use safe name rendering (strip rank wrappers; preserve leading '.') and keep a penalty suffix when applicable.

### Fixed
- Minor cleanup and removal of unused variables in scoreboard rendering.
 - Prevented a potential permission recursion by defining `boatracing.admin` with explicit children instead of inheriting `boatracing.*`.
 - `/boatracing race leave <track>` now replies when registration is closed or when the player isn’t registered (no more silent no-op).
 - Setup Wizard (Pit): no longer repeats waiting for team pits when a default pit exists; the wizard now advances to Checkpoints automatically (team pits remain optional).
 - Updater: fixed missing console notice; now logs a WARN once on startup (if outdated) and also every hour while outdated. When an admin joins, a quick check runs (throttled) to notify them within seconds if a new update was just published.

## 1.0.7 — 15/08/2025
### Changed
- Update checks: removed periodic console spam; keep a single WARN shortly after startup when outdated (honors `updates.console-warn`). Periodic 5‑minute checks remain silent.
 - Scoreboard: redesigned layout with centered rows, compact labels, rank colors, and viewer highlight.
### Added
 
### Fixed
- Update checker logs network errors at most once per server run.
### Removed
- Internal hiding of vanilla scoreboard sidebar numbers has been removed entirely. If you want to hide the right‑side numbers, please use an external plugin while we work on a future built‑in implementation.
# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [Unreleased]

## [1.0.6] - 14/08/2025
### Added
- Leaderboard sidebar: top‑10 live positions; personal stats moved to ActionBar.
- Configurable start “lights out” delay via `racing.lights-out-delay-seconds` to slow down the transition from all lit to GO.
- Optional “lights out” jitter via `racing.lights-out-jitter-seconds` (random 0..value seconds added to the delay).
- Sector and finish gaps: broadcasts a compact gap vs lap leader at each checkpoint and at lap finish; at race finish, gap vs winner.
 - Updates now point to Modrinth for downloads: https://modrinth.com/plugin/boatracing
 

### Fixed
- Pitstop as finish: crossing the configured pit area now counts as finish for lap progression once all checkpoints for the lap have been collected (pit time penalty still applies when enabled).
- Setup Wizard UX: clearer pit step text and a clickable “Clear checkpoints” action; checkpoints removal command advertised from the wizard.
 - Scoreboard layout polish: names left-aligned, the entire " - Lap X/Y [CP]" segment centered, compact/dynamic padding, and no extra padding after truncated names ("..."). Removed decorative separator and arrow prefix. Self entry highlighted in green (no bold). FIN label standardized to “FINISHED”.
 - Display name handling: prefer EssentialsX displayName when available and strip leading wrappers like [Rank]/(Rank)/{Rank} and punctuation for alignment.


## [1.0.5] - 13/08/2025
### Fixed
- Team member persistence: no members are lost after updates/reloads/startup; loader now restores members without enforcing capacity constraints.
- Setup pit command: `/boatracing setup setpit [team]` now supports team names with spaces by quoting them (e.g., "/boatracing setup setpit \"Toast Peace\""); tab‑completion suggests quoted names when the input starts with a quote.
- Config defaults: `config.yml` now merges new default keys on update/reload without overwriting user changes.
- Boat/raft type: placed vehicles now match the racer’s selected wood variant (including chest variants); no longer always spawns OAK. Compatible across API versions with safe fallbacks.


## [1.0.4] - 13/08/2025
### Added
- Team-specific pit areas via unified command `/boatracing setup setpit [team]` (tab‑completion for team names). Wizard updated accordingly.
- Mandatory pitstops via config `racing.mandatory-pitstops` (default 0). Pitstops increment on pit exit and are required to finish when > 0.
 - Config defaults: `config.yml` now merges new default keys on update/reload without overwriting user changes.
 - Boat/raft type: placed vehicles now match the racer’s selected wood variant (including chest variants); no longer always spawns OAK. Compatible across API versions with safe fallbacks.
 - Per-player custom start slots with `/boatracing setup setpos <player> <slot|auto>` and `/boatracing setup clearpos <player>`; tab completion for player names, `auto`, and slot numbers. Slots are 1-based in the command and stored 0-based.
 - Grid ordering by best recorded time per track (fastest first); racers without a time are placed after those with times.
 - Setup show now includes the presence of team-specific pits and the number of custom start positions configured.
 - Wizard (Starts): added optional clickable actions for custom start slots (setpos/clearpos/auto) and a counter of configured custom slots.

### Changed
- Permissions: players can use `join`, `leave`, and `status` by default; only `open|start|force|stop` remain admin‑only. Removed extra runtime permission checks for join/leave.

### Fixed
- Boats now spawn with the player’s selected wood type using a resilient enum mapping with safe fallback to OAK across API versions.

 

## [1.0.3]
### Added
- Admin Tracks GUI: create, select, save as, delete named tracks (with confirmation). Requires `boatracing.setup`.
- Admin Race GUI: manage race lifecycle (open/close registration, start/force/stop), quick-set/custom laps, and registrant removal.
- Active (selected) track name is displayed in Setup Wizard prompts, `/boatracing setup show`, and `/boatracing race status`.
- Tooltips (lore) for “Admin panel”, “Player view”, and “Refresh” buttons in GUIs.
- Quick navigation: from Teams GUI to Admin panel (admins only), and from Admin GUI back to player view.
- Refresh buttons in Teams and Admin GUIs.
 - Guided setup wizard with concise, colorized prompts and clickable actions. Adds a Laps step and an explicit Finish button; navigation buttons now use emojis (⟵, ℹ, ✖) and spacing puts a blank line at the top of the block.
 - Convenience selector: `/boatracing setup wand` to give the built-in selection tool.
- Team GUI: members can rename the team and change the team color when enabled via config (`player-actions.allow-team-rename` / `allow-team-color`). These actions notify all teammates.
 - Team GUI: optional member disband via config (`player-actions.allow-team-disband`). Disband uses a confirmation screen and notifies all teammates.
 - Per‑track storage: all track configuration is saved under `plugins/BoatRacing/tracks/<name>.yml`. On startup, a legacy `track.yml` is migrated to `tracks/default.yml` (or `default_N.yml`) with an in‑game admin notice.
 - Race commands now require a track argument: `open|join|leave|force|start|stop|status <track>`. Tab‑completion suggests existing track names for these.
 - Admin Tracks GUI: after creating a track, sends a clickable tip to paste `/boatracing setup wizard`.
 - Wizard labels Pit area and Checkpoints as “(optional)” and allows skipping them. Readiness requires only Starts and Finish.
 - Start lights: configure exactly 5 Redstone Lamps; race start uses an F1-style left-to-right countdown that lights lamps via block data (no redstone). New setup commands: `addlight` and `clearlights`. Wizard adds a dedicated “Start lights” step.
 - Registration: server-wide broadcast when a player joins or leaves registration.
 - False start penalties: moving forward during the start-light countdown applies a configurable time penalty (`racing.false-start-penalty-seconds`, default 3.0). Messages are in English.
 - New config flags: `racing.enable-pit-penalty` and `racing.enable-false-start-penalty` to toggle pit and false-start penalties.
 - Race permissions split: default-true for `boatracing.race.join`, `boatracing.race.leave`, and `boatracing.race.status`; admin actions require `boatracing.race.admin` (or `boatracing.setup`).
 - Live scoreboard: per-player sidebar showing Lap, Checkpoints, and Elapsed Time with periodic updates; created on race start and cleared on stop/reset/cancel.
 - Pitstop as finish: crossing the configured pit area now counts as finish for lap progression once all checkpoints for the lap have been collected (pit time penalty still applies when enabled).

### Changed
- Footer fillers switched from LIGHT_GRAY_STAINED_GLASS_PANE to GRAY_STAINED_GLASS_PANE for a darker look.
- Denial messages for protected actions are now hardcoded in English (no longer read from config).
- Configuration cleanup: removed obsolete `teams:` section and `messages.disallowed` from `config.yml`; documentation now reflects per‑track storage only.
- README updated to document `player-actions.*` flags and storage files.
 - Setup help and tab-completion updated to include `wizard` and `wand`.
 - Admin/user notifications for team deletion/removal now use neutral phrasing (no “by an admin”).
 - Tracks GUI: terminology switched to “selected” for the active track; "Create and select" loads the newly created track and suggests starting the setup wizard.
 - Disband button is hidden for members when `player-actions.allow-team-disband` is false.
- Race lifecycle: stop cancels registration and running race; start/force operate only on registered participants. Placement enforces unique starts, pitch=0, and auto‑mounts the selected boat.
- Terminology: “loaded” → “selected”; “pit lane” → “pit area”.
 - Race commands `open` and `start` no longer accept a laps argument; laps come from configuration/track.
 - Pit area and checkpoints made optional across setup, readiness checks, and runtime logic/documentation.
 - Tracks GUI: removed “Save as…”.
 - Setup help and tab-completion now include `addlight` and `clearlights`. `/boatracing race status` and `setup show` display the number of start lights.

### Fixed
- Removed the last references to `messages.disallowed.*` in `TeamGUI` that could cause confusion.
 - Selection handling improved: built-in selection tool (left/right click); `/boatracing setup selinfo` shows richer diagnostics.
- Admin GUI: clicking any dye item in the team view now opens the color picker (previously only LIME_DYE was handled).
 - Crash on boat spawn fixed by spawning BOAT/CHEST_BOAT directly and mounting players; removed unsupported boat wood-type setter.
 - Checkpoints persistence: saving as a list and loading from both list and legacy section formats; wizard and readiness checks now correctly detect added checkpoints. Fixed a false “missing checkpoint” on `race open` after setup.
 - Players wrongly blocked from `/boatracing race join` due to a global permission gate. Now join/leave/status are allowed by default and only admin actions are gated.

## [1.0.3] - 12/08/2025
- Public release noted in README. Core gameplay, teams, GUIs, WE/FAWE setup, racing, and update checks.

 [Unreleased]: https://github.com/Jaie55/BoatRacing/compare/v1.0.6...HEAD

[1.0.6]: https://github.com/Jaie55/BoatRacing/releases/tag/v1.0.6

[1.0.5]: https://github.com/Jaie55/BoatRacing/releases/tag/v1.0.5

[1.0.4]: https://github.com/Jaie55/BoatRacing/releases/tag/v1.0.4

[1.0.3]: https://github.com/Jaie55/BoatRacing/releases/tag/v1.0.3

