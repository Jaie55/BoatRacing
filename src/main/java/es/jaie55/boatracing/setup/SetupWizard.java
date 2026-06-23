package es.jaie55.boatracing.setup;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chat-driven setup assistant for configuring a track.
 * Prompts are shown to operators in English.
 */
public class SetupWizard {
    public enum Step { STARTS, FINISH, LIGHTS, PIT, CHECKPOINTS, PITSTOPS, LAPS, DONE }

    private final BoatRacingPlugin plugin;
    private final Map<UUID, Step> states = new HashMap<>();

    public SetupWizard(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive(Player p) {
        Step s = states.get(p.getUniqueId());
        return s != null && s != Step.DONE;
    }

    public void start(Player p) {
        states.put(p.getUniqueId(), Step.STARTS);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
        prompt(p);
    }

    public void next(Player p) {
        Step s = states.get(p.getUniqueId());
        if (s == null) { start(p); return; }
        TrackConfig t = plugin.getRaceManager().getTrack();
        switch (s) {
            case STARTS -> {
                if (t.getStarts().isEmpty()) { need(p, plugin.msg().get("setup.wizard.error.need-starts")); return; }
                states.put(p.getUniqueId(), Step.FINISH);
            }
            case FINISH -> {
                if (t.getFinish() == null) { need(p, plugin.msg().get("setup.wizard.error.need-finish")); return; }
                states.put(p.getUniqueId(), Step.LIGHTS);
            }
            case LIGHTS -> {
                if (!t.hasFiveLights()) { need(p, plugin.msg().get("setup.wizard.error.need-lights")); return; }
                states.put(p.getUniqueId(), Step.PIT);
            }
            case PIT -> {
                // If a default pit exists, skip team pits and go to CHECKPOINTS
                if (t.getPitlane() != null) {
                    states.put(p.getUniqueId(), Step.CHECKPOINTS);
                } else {
                    // No default pit yet; still allow moving forward (pits are optional)
                    states.put(p.getUniqueId(), Step.CHECKPOINTS);
                }
            }
            case CHECKPOINTS -> { states.put(p.getUniqueId(), Step.PITSTOPS); }
            case PITSTOPS -> { states.put(p.getUniqueId(), Step.LAPS); }
            case LAPS -> { /* wait for explicit finish */ }
            case DONE -> { /* nothing */ }
        }
        prompt(p);
    }

    // Skip optional steps only (Pit, Checkpoints, Pitstops)
    public void skip(Player p) {
        Step s = states.get(p.getUniqueId());
        if (s == null) { start(p); return; }
    if (s == Step.PIT) {
            states.put(p.getUniqueId(), Step.CHECKPOINTS);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.15f);
            prompt(p);
            return;
        }
    if (s == Step.CHECKPOINTS) {
            states.put(p.getUniqueId(), Step.PITSTOPS);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.15f);
            prompt(p);
            return;
        }
        if (s == Step.PITSTOPS) {
            states.put(p.getUniqueId(), Step.LAPS);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.15f);
            prompt(p);
            return;
        }
        need(p, plugin.msg().get("setup.wizard.cannot-skip"));
        prompt(p);
    }

    public void back(Player p) {
        Step s = states.get(p.getUniqueId());
    if (s == null || s == Step.STARTS) { prompt(p); return; }
    if (s == Step.FINISH) states.put(p.getUniqueId(), Step.STARTS);
    else if (s == Step.LIGHTS) states.put(p.getUniqueId(), Step.FINISH);
    else if (s == Step.PIT) states.put(p.getUniqueId(), Step.LIGHTS);
        else if (s == Step.CHECKPOINTS) states.put(p.getUniqueId(), Step.PIT);
        else if (s == Step.PITSTOPS) states.put(p.getUniqueId(), Step.CHECKPOINTS);
        else if (s == Step.LAPS) states.put(p.getUniqueId(), Step.PITSTOPS);
        else if (s == Step.DONE) states.put(p.getUniqueId(), Step.LAPS);
        prompt(p);
    }

    public void cancel(Player p) {
        states.remove(p.getUniqueId());
        p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.wizard.closed")));
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.9f);
    }

    public void status(Player p) { prompt(p); }

    public void afterAction(Player p) {
        if (!isActive(p)) return;
        autoAdvance(p);
        prompt(p);
    }

    private void autoAdvance(Player p) {
        Step s = states.get(p.getUniqueId());
        if (s == null) return;
        TrackConfig t = plugin.getRaceManager().getTrack();
        switch (s) {
            case STARTS -> { if (!t.getStarts().isEmpty()) states.put(p.getUniqueId(), Step.FINISH); }
            case FINISH -> { if (t.getFinish() != null) states.put(p.getUniqueId(), Step.LIGHTS); }
            case LIGHTS -> { if (t.hasFiveLights()) states.put(p.getUniqueId(), Step.PIT); }
            case PIT -> {
                // If a default pit is already set, auto-advance to CHECKPOINTS;
                // team-specific pits are optional and should not block progress.
                if (t.getPitlane() != null) {
                    states.put(p.getUniqueId(), Step.CHECKPOINTS);
                }
            }
            case CHECKPOINTS -> { /* optional; don't auto-advance unless user clicks Next */ }
            case PITSTOPS -> { /* optional; don't auto-advance unless user clicks Next */ }
            case LAPS, DONE -> { /* stay */ }
        }
    }

    private void need(Player p, String msg) {
        p.sendMessage(Text.colorize(plugin.pref() + "&c" + msg));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
    }

    public void prompt(Player p) {
        Step s = states.getOrDefault(p.getUniqueId(), Step.STARTS);
        TrackConfig t = plugin.getRaceManager().getTrack();
        String tname = (plugin.getTrackLibrary() != null && plugin.getTrackLibrary().getCurrent() != null)
            ? plugin.getTrackLibrary().getCurrent() : plugin.msg().get("general.unsaved");
        p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.wizard.header", "track", tname)));
        switch (s) {
            case STARTS -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.starts")));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.starts.custom-slots", "count", t.getCustomStartSlots().size())));
                p.sendMessage(Text.c(plugin.msg().get("setup.wizard.starts.added", "count", t.getStarts().size()) + " &8• ")
                    .append(Text.suggest(plugin.msg().get("setup.wizard.starts.btn-add-start"),"/boatracing setup addstart"))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.starts.btn-clear"),"/boatracing setup clearstarts")));
                p.sendMessage(Text.c(" ")
                    .append(Text.suggest(plugin.msg().get("setup.wizard.starts.btn-set-custom-slot"),"/boatracing setup setpos "))
                    .append(Text.c(" "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.starts.btn-clear-custom-slot"),"/boatracing setup clearpos ")));
                nav(p, s);
            }
            case FINISH -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.finish")));
                p.sendMessage(Text.c(plugin.msg().get("setup.wizard.finish.wand-prompt"))
                    .append(Text.suggest(plugin.msg().get("setup.btn-get-wand"),"/boatracing setup wand"))
                    .append(Text.c(" &8• " + plugin.msg().get("setup.wizard.finish.status", "status", t.getFinish()!=null ? plugin.msg().get("setup.status-yes") : plugin.msg().get("setup.status-no")))));
                p.sendMessage(Text.c(" ").append(Text.suggest(plugin.msg().get("setup.wizard.finish.btn-set-finish"),"/boatracing setup setfinish")));
                nav(p, s);
            }
            case LIGHTS -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.lights")));
                p.sendMessage(Text.c(plugin.msg().get("setup.wizard.lights.added", "count", t.getLights().size()) + " &8• ")
                    .append(Text.c(Text.colorize(plugin.msg().get("setup.wizard.lights.hint"))))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.lights.clear-button"),"/boatracing setup clearlights")));
                nav(p, s);
            }
            case PIT -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.pit")));
                p.sendMessage(Text.c(plugin.msg().get("setup.wizard.pit.status-label"))
                    .append(Text.c(plugin.msg().get("setup.wizard.pit.default-label")))
                    .append(Text.c(t.getPitlane()!=null?"&ayes":"&cno"))
                    .append(Text.c(plugin.msg().get("setup.wizard.pit.team-pits-label", "count", t.getTeamPits().size()))));
                p.sendMessage(Text.c(" ")
                    .append(Text.suggest(plugin.msg().get("setup.wizard.pit.btn-set-pit"),"/boatracing setup setpit"))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.pit.btn-set-team-pit"),"/boatracing setup setpit "))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.pit.btn-get-wand"),"/boatracing setup wand")));
                nav(p, s);
            }
            case CHECKPOINTS -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.checkpoints")));
                p.sendMessage(Text.c(plugin.msg().get("setup.wizard.checkpoints.added", "count", t.getCheckpoints().size()))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.wizard.checkpoints.btn-add-checkpoint"),"/boatracing setup addcheckpoint"))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.btn-get-wand"),"/boatracing setup wand"))
                    .append(Text.c(" &8• "))
                    .append(Text.suggest(plugin.msg().get("setup.btn-clear-checkpoints"),"/boatracing setup clearcheckpoints")));
                nav(p, s);
            }
            case PITSTOPS -> {
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.pitstops")));
                int req = plugin.getRaceManager() != null ? plugin.getRaceManager().getMandatoryPitstops() : 0;
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.pitstops.current", "count", req)));
                p.sendMessage(Text.c(plugin.msg().get("setup.choose"))
                    .append(Text.cmd("&b[0]","/boatracing setup setpitstops 0")).append(Text.c(" "))
                    .append(Text.cmd("&b[1]","/boatracing setup setpitstops 1")).append(Text.c(" "))
                    .append(Text.cmd("&b[2]","/boatracing setup setpitstops 2")).append(Text.c(" "))
                    .append(Text.cmd("&b[3]","/boatracing setup setpitstops 3"))
                );
                p.sendMessage(Text.c(plugin.msg().get("setup.when-ready"))
                    .append(Text.cmd(plugin.msg().get("setup.wizard.pitstops.btn-next"),"/boatracing setup wizard next"))
                );
                nav(p, s);
            }
            case LAPS -> {
                int laps = plugin.getRaceManager().getTotalLaps();
                p.sendMessage(Text.c(" "));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.step.laps")));
                p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.laps.current", "laps", laps)));
                p.sendMessage(Text.c(plugin.msg().get("setup.choose-laps"))
                    .append(Text.cmd("&b[1]","/boatracing setup setlaps 1")).append(Text.c(" "))
                    .append(Text.cmd("&b[3]","/boatracing setup setlaps 3")).append(Text.c(" "))
                    .append(Text.cmd("&b[5]","/boatracing setup setlaps 5"))
                );
                p.sendMessage(Text.c(plugin.msg().get("setup.when-ready"))
                    .append(Text.cmd(plugin.msg().get("setup.wizard.laps.btn-finish"),"/boatracing setup wizard finish"))
                );
                nav(p, s);
            }
            case DONE -> {
                // Not used directly; we finish via finish()
            }
        }
    }

    private void nav(Player p, Step s) {
        var comp = Text.c(plugin.msg().get("setup.nav-label"))
            .append(Text.cmd(plugin.msg().get("setup.nav-back"),"/boatracing setup wizard back")).append(Text.c(" "))
            .append(Text.cmd(plugin.msg().get("setup.nav-status"),"/boatracing setup wizard status")).append(Text.c(" "))
            .append(Text.cmd(plugin.msg().get("setup.nav-cancel"),"/boatracing setup wizard cancel"));
        // Also offer Skip in the navigation row for optional steps
        if (s == Step.PIT || s == Step.CHECKPOINTS) {
            comp = comp.append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.nav-skip"),"/boatracing setup wizard skip"));
        }
        p.sendMessage(comp);
    }

    // Determine the first unfinished step based on current track config
    private Step firstIncomplete(TrackConfig t) {
        if (t.getStarts().isEmpty()) return Step.STARTS;
        if (t.getFinish() == null) return Step.FINISH;
        if (!t.hasFiveLights()) return Step.LIGHTS;
    // Optional steps are not required for completion
        return Step.LAPS;
    }

    // Explicit finish from clickable action or command token
    public void finish(Player p) {
        TrackConfig t = plugin.getRaceManager().getTrack();
        if (t == null) return;
        if (!t.isReady()) {
            java.util.List<String> missing = t.missingRequirements();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-ready", "requirements", plugin.formatTrackRequirements(missing))));
            // Ensure wizard is active and point to the first incomplete step
            states.put(p.getUniqueId(), firstIncomplete(t));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.7f);
            prompt(p);
            return;
        }
        p.sendMessage(Text.colorize(plugin.msg().get("setup.wizard.complete")));
        int laps = plugin.getRaceManager().getTotalLaps();
        p.sendMessage(Text.colorize(plugin.msg().get("setup.summary",
            "starts", t.getStarts().size(),
            "finish", t.getFinish()!=null ? plugin.msg().get("setup.status-yes") : plugin.msg().get("setup.status-no"),
            "pit", t.getPitlane()!=null ? plugin.msg().get("setup.status-yes") : plugin.msg().get("setup.status-no"),
            "checkpoints", t.getCheckpoints().size(),
            "custom_slots", t.getCustomStartSlots().size(),
            "laps", laps)));
        String cur = (plugin.getTrackLibrary() != null && plugin.getTrackLibrary().getCurrent() != null) ? plugin.getTrackLibrary().getCurrent() : null;
        // If no named track is selected yet, use the in-memory unsaved track instead of a placeholder token.
        String openCmd = (cur != null && !cur.isBlank()) ? "/boatracing race open " + cur : "/boatracing race open unsaved";
        p.sendMessage(Text.c(plugin.msg().get("setup.next-label"))
            .append(Text.cmd(plugin.msg().get("setup.btn-open-registration"), openCmd))
            .append(Text.c(plugin.msg().get("setup.details-label")))
            .append(Text.cmd(plugin.msg().get("setup.btn-setup-show"),"/boatracing setup show"))
        );
        states.put(p.getUniqueId(), Step.DONE);
        states.remove(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.3f);
    }
}
