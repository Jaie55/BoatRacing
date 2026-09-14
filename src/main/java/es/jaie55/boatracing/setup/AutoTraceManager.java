package es.jaie55.boatracing.setup;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.ParticleWireframe;
import es.jaie55.boatracing.track.PlaneCheckpoint;
import es.jaie55.boatracing.track.SelectionManager;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.SchedulerCompat;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Records an admin driving one lap and generates oriented (plane) checkpoints along the path.
 * This is the fast path for setting up a track: drive once instead of marking every gate by hand.
 *
 * Sessions are per player and can be previewed with particles before being saved to the track.
 */
public class AutoTraceManager {

    private final BoatRacingPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private SchedulerCompat.TaskHandle renderTask;

    public AutoTraceManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    private static final class Session {
        final List<Location> samples = new ArrayList<>();
        final Set<Integer> selected = new LinkedHashSet<>();
        List<PlaneCheckpoint> gates = new ArrayList<>();
        SchedulerCompat.TaskHandle captureTask;
        boolean recording = true;
        boolean preview = true;
        Location first;
        double pathLength;
        long startedAt;
        long lastReminderAt;
        double spacing;
        double halfWidth;
        double halfHeight;
        double epsilon;
        double minDistance;
        double closeDistance;
        double closeMinLength;
        int maxSamples;
        boolean recenterIce;
    }

    public boolean isActive(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    public void start(Player player) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("setup.auto-trace.enabled", true)) {
            send(player, "setup.autotrace.disabled", "label", "boatracing");
            return;
        }
        Session existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            if (existing.recording) {
                Location start = existing.first;
                send(player, "setup.autotrace.already-active",
                        "label", "boatracing",
                        "x", start != null ? String.valueOf(start.getBlockX()) : "?",
                        "y", start != null ? String.valueOf(start.getBlockY()) : "?",
                        "z", start != null ? String.valueOf(start.getBlockZ()) : "?",
                        "samples", String.valueOf(existing.samples.size()));
                return;
            }
            // Previous run was stopped but never accepted/discarded: drop it and record again.
            if (existing.captureTask != null) existing.captureTask.cancel();
            sessions.remove(player.getUniqueId());
            plugin.getLogger().fine("AutoTrace: discarding stopped session for " + player.getName() + " and starting a new one.");
            send(player, "setup.autotrace.restarted");
        }

        Session s = new Session();
        s.startedAt = System.currentTimeMillis();
        s.lastReminderAt = s.startedAt;
        s.spacing = Math.max(1.0, plugin.getConfig().getDouble("setup.auto-trace.spacing", 3.0));
        s.halfWidth = Math.max(0.5, plugin.getConfig().getDouble("setup.auto-trace.half-width", 4.5));
        s.halfHeight = Math.max(0.5, plugin.getConfig().getDouble("setup.auto-trace.half-height", 3.0));
        s.epsilon = Math.max(0.05, plugin.getConfig().getDouble("setup.auto-trace.simplify-epsilon", 0.4));
        s.minDistance = Math.max(0.05, plugin.getConfig().getDouble("setup.auto-trace.min-distance", 0.3));
        s.closeDistance = Math.max(2.0, plugin.getConfig().getDouble("setup.auto-trace.auto-close-distance", 6.0));
        s.closeMinLength = Math.max(5.0, plugin.getConfig().getDouble("setup.auto-trace.auto-close-min-length", 30.0));
        s.maxSamples = Math.max(100, plugin.getConfig().getInt("setup.auto-trace.max-samples", 6000));
        s.recenterIce = plugin.getConfig().getBoolean("setup.auto-trace.recenter-ice", true);
        s.preview = plugin.getConfig().getBoolean("setup.auto-trace.preview", true);

        sessions.put(player.getUniqueId(), s);
        ensureRenderTask();

        int period = Math.max(1, plugin.getConfig().getInt("setup.auto-trace.sample-ticks", 2));
        s.captureTask = SchedulerCompat.runTimer(plugin, () -> captureTick(player), period, period);

        Location startLoc = player.getLocation();
        send(player, "setup.autotrace.started",
                "spacing", String.valueOf(s.spacing),
                "distance", String.valueOf(Math.round(s.closeDistance)),
                "x", String.valueOf(startLoc.getBlockX()),
                "y", String.valueOf(startLoc.getBlockY()),
                "z", String.valueOf(startLoc.getBlockZ()),
                "label", "boatracing");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.2f);
        player.showTitle(net.kyori.adventure.title.Title.title(
                Text.c(plugin.msg().get("setup.autotrace.title")),
                Text.c(plugin.msg().get("setup.autotrace.subtitle-recording")),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(200),
                        java.time.Duration.ofMillis(1400),
                        java.time.Duration.ofMillis(300))));
        sendControls(player, false);
        if (plugin.getGateToolManager() != null) {
            plugin.getGateToolManager().giveTools(player);
        }
        plugin.getLogger().fine("AutoTrace started for " + player.getName() + " at "
                + startLoc.getBlockX() + "," + startLoc.getBlockY() + "," + startLoc.getBlockZ()
                + " (spacing=" + s.spacing + ", auto-close=" + s.closeDistance + ").");
    }

    /** @return true when the player has an AutoTrace session actively recording. */
    public boolean isRecording(Player player) {
        if (player == null) return false;
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.recording;
    }

    public void stop(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        if (!s.recording) {
            if (!s.gates.isEmpty()) {
                send(player, "setup.autotrace.stopped-info",
                        "gates", String.valueOf(s.gates.size()),
                        "label", "boatracing");
            } else {
                send(player, "setup.autotrace.none", "label", "boatracing");
            }
            return;
        }
        s.recording = false;
        if (s.captureTask != null) {
            s.captureTask.cancel();
            s.captureTask = null;
        }
        player.sendActionBar(Text.c(" "));
        send(player, "setup.autotrace.stopped",
                "points", String.valueOf(s.samples.size()),
                "length", String.format(Locale.ROOT, "%.0f", s.pathLength));

        s.gates = generate(s);
        s.selected.clear();
        plugin.getLogger().fine("AutoTrace stopped for " + player.getName() + ": samples=" + s.samples.size()
                + ", length=" + Math.round(s.pathLength) + ", gates=" + s.gates.size() + ".");
        if (s.gates.isEmpty()) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            cancel(player, false);
            return;
        }
        send(player, "setup.autotrace.generated",
                "gates", String.valueOf(s.gates.size()),
                "label", "boatracing");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        send(player, "setup.autotrace.select-hint");
        sendControls(player, true);
    }

    public void togglePreview(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        if (s.gates.isEmpty()) {
            send(player, s.recording ? "setup.autotrace.recording" : "setup.autotrace.none",
                    "label", "boatracing");
            return;
        }
        s.preview = !s.preview;
        plugin.getLogger().finer("AutoTrace preview " + (s.preview ? "enabled" : "disabled") + " for " + player.getName() + ".");
        send(player, s.preview ? "setup.autotrace.preview-on" : "setup.autotrace.preview-off");
    }

    public void accept(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.gates.isEmpty()) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        TrackConfig track = plugin.getTrackConfig();
        if (track == null) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        track.setCheckpoints(s.gates);
        plugin.getLogger().fine("AutoTrace accepted by " + player.getName() + ": " + s.gates.size()
                + " gate(s) saved to the active track.");
        send(player, "setup.autotrace.accepted",
                "gates", String.valueOf(s.gates.size()),
                "label", "boatracing");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
        sendNextSteps(player);
        if (plugin.getSetupWizard() != null) plugin.getSetupWizard().afterAction(player);
        cancel(player, false);
    }

    /** Step-by-step guide, also available as `/boatracing setup autotrace help`. */
    public void sendHelp(Player player) {
        if (player == null) return;
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.autotrace.help.header")));
        for (int i = 1; i <= 6; i++) {
            player.sendMessage(Text.colorize(plugin.msg().get("setup.autotrace.help.line-" + i, "label", "boatracing")));
        }
        sendControls(player, true);
    }

    /** Clickable shortcuts to finish the track and open a race after saving AutoTrace gates. */
    private void sendNextSteps(Player player) {
        String track = plugin.getTrackLibrary() != null && plugin.getTrackLibrary().getCurrent() != null
                ? plugin.getTrackLibrary().getCurrent()
                : "unsaved";
        ensureWand(player);
        player.sendMessage(Text.c(plugin.msg().get("setup.autotrace.next-steps-label"))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-setup"), "/boatracing setup wizard"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-open-race"), "/boatracing race open " + track)));
        player.sendMessage(Text.colorize(plugin.msg().get("setup.autotrace.next-steps-hint",
                "label", "boatracing", "track", track)));
        player.sendMessage(Text.colorize(plugin.msg().get("setup.autotrace.next-steps-wand", "label", "boatracing")));
        player.sendMessage(Text.c(" ")
                .append(Text.cmd(plugin.msg().get("setup.btn-get-wand"), "/boatracing setup wand"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.wizard.starts.btn-add-start"), "/boatracing setup addstart"))
                .append(Text.c(" "))
                .append(Text.suggest(plugin.msg().get("setup.wizard.finish.btn-set-finish"), "/boatracing setup setfinish")));
    }

    /** Gives the selection wand after saving when the player does not have one yet. */
    private void ensureWand(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && SelectionManager.isWand(item)) return;
        }
        SelectionManager.giveWand(player);
        send(player, "setup.autotrace.wand-given");
    }

    private void sendRecordingReminder(Player player, Session s, Location loc) {
        long reminderSeconds = Math.max(0L, plugin.getConfig().getLong("setup.auto-trace.reminder-seconds", 15L));
        if (reminderSeconds <= 0L) return;
        long now = System.currentTimeMillis();
        if (now - s.lastReminderAt < reminderSeconds * 1000L) return;
        s.lastReminderAt = now;

        long seconds = Math.max(0L, (now - s.startedAt) / 1000L);
        String distance = "?";
        if (s.first != null && s.first.getWorld() != null && loc.getWorld() != null && loc.getWorld().equals(s.first.getWorld())) {
            distance = String.format(Locale.ROOT, "%.1f", loc.distance(s.first));
        }
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.autotrace.reminder",
                "points", String.valueOf(s.samples.size()),
                "time", String.valueOf(seconds),
                "distance", distance,
                "label", "boatracing")));
    }

    public void cancel(Player player, boolean announce) {
        if (player == null) return;
        Session s = sessions.remove(player.getUniqueId());
        if (s != null && s.captureTask != null) s.captureTask.cancel();
        player.sendActionBar(Text.c(" "));
        if (announce) {
            send(player, "setup.autotrace.cancelled");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        }
        plugin.getLogger().finer("AutoTrace session cleared for " + player.getName() + ".");
    }

    public void status(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        Location start = s.first;
        long elapsed = s.recording
                ? Math.max(0L, (System.currentTimeMillis() - s.startedAt) / 1000L)
                : 0L;
        send(player, "setup.autotrace.status",
                "samples", String.valueOf(s.samples.size()),
                "gates", String.valueOf(s.gates.size()),
                "selected", String.valueOf(s.selected.size()),
                "length", String.format(Locale.ROOT, "%.0f", s.pathLength),
                "preview", plugin.msg().get(s.preview ? "general.yes" : "general.no"),
                "state", plugin.msg().get(s.recording ? "setup.autotrace.state-recording" : "setup.autotrace.state-stopped"),
                "time", String.valueOf(elapsed),
                "x", start != null ? String.valueOf(start.getBlockX()) : "?",
                "y", start != null ? String.valueOf(start.getBlockY()) : "?",
                "z", start != null ? String.valueOf(start.getBlockZ()) : "?");
    }

    public void deleteGate(Player player, int index1Based) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.gates.isEmpty()) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        int idx = index1Based - 1;
        if (idx < 0 || idx >= s.gates.size()) {
            send(player, "setup.autotrace.delete-invalid", "max", String.valueOf(s.gates.size()));
            return;
        }
        s.gates.remove(idx);
        s.selected.clear();
        send(player, "setup.autotrace.deleted", "index", String.valueOf(index1Based));
    }

    public void resizeGate(Player player, int index1Based, double halfWidth, double halfHeight) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.gates.isEmpty()) {
            send(player, "setup.autotrace.none", "label", "boatracing");
            return;
        }
        int idx = index1Based - 1;
        if (idx < 0 || idx >= s.gates.size()) {
            send(player, "setup.autotrace.delete-invalid", "max", String.valueOf(s.gates.size()));
            return;
        }
        PlaneCheckpoint old = s.gates.get(idx);
        PlaneCheckpoint resized = new PlaneCheckpoint(
                old.worldName(), old.getCenter(), old.getNormal(), old.getRight(), old.getUp(),
                halfWidth, halfHeight);
        s.gates.set(idx, resized);
        send(player, "setup.autotrace.resized",
                "index", String.valueOf(index1Based),
                "width", String.format(Locale.ROOT, "%.1f", halfWidth * 2.0),
                "height", String.format(Locale.ROOT, "%.1f", halfHeight * 2.0));
    }

    // ------------------------------------------------------------------
    // Wand gate selection (during preview)
    // ------------------------------------------------------------------

    /**
     * Handles a wand click while gates are being previewed. Left-click selects the nearest gate,
     * right-click deselects it. Returns true when the click was consumed by a gate (so the wand
     * does not fall back to corner selection).
     */
    public boolean handleWandClick(Player player, boolean leftClick, Location target) {
        if (player == null || target == null) return false;
        if (!plugin.getConfig().getBoolean("setup.auto-trace.wand-select", true)) return false;
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.recording || s.gates.isEmpty()) return false;
        if (target.getWorld() == null) return false;

        double radius = Math.max(1.0, plugin.getConfig().getDouble("setup.auto-trace.wand-select-radius", 4.0));
        double radiusSq = radius * radius;
        int nearest = -1;
        double nearestSq = Double.MAX_VALUE;
        for (int i = 0; i < s.gates.size(); i++) {
            PlaneCheckpoint gate = s.gates.get(i);
            if (!target.getWorld().getName().equals(gate.worldName())) continue;
            Location center = gate.getCenter().toLocation(target.getWorld());
            double d = center.distanceSquared(target);
            if (d < nearestSq) {
                nearestSq = d;
                nearest = i;
            }
        }
        if (nearest < 0 || nearestSq > radiusSq) return false;

        int index1Based = nearest + 1;
        if (leftClick) {
            s.selected.add(nearest);
            send(player, "setup.autotrace.gate-selected",
                    "index", String.valueOf(index1Based),
                    "selected", String.valueOf(s.selected.size()));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);
        } else {
            boolean removed = s.selected.remove(nearest);
            if (removed) {
                send(player, "setup.autotrace.gate-deselected",
                        "index", String.valueOf(index1Based),
                        "selected", String.valueOf(s.selected.size()));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
            }
        }
        return true;
    }

    public int gateCount(Player player) {
        Session s = player == null ? null : sessions.get(player.getUniqueId());
        return s == null ? 0 : s.gates.size();
    }

    /** Resizes every selected gate at once (half sizes, like the single-gate resize). */
    public void resizeSelected(Player player, double halfWidth, double halfHeight) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.gates.isEmpty() || s.selected.isEmpty()) {
            send(player, "setup.autotrace.selected-none");
            return;
        }
        for (int idx : s.selected) {
            if (idx < 0 || idx >= s.gates.size()) continue;
            PlaneCheckpoint old = s.gates.get(idx);
            s.gates.set(idx, new PlaneCheckpoint(
                    old.worldName(), old.getCenter(), old.getNormal(), old.getRight(), old.getUp(),
                    halfWidth, halfHeight));
        }
        send(player, "setup.autotrace.selected-resized",
                "count", String.valueOf(s.selected.size()),
                "width", String.format(Locale.ROOT, "%.1f", halfWidth * 2.0),
                "height", String.format(Locale.ROOT, "%.1f", halfHeight * 2.0));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    /** Deletes every selected gate. */
    public void deleteSelected(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null || s.gates.isEmpty() || s.selected.isEmpty()) {
            send(player, "setup.autotrace.selected-none");
            return;
        }
        java.util.List<Integer> indexes = new ArrayList<>(s.selected);
        indexes.sort(java.util.Comparator.reverseOrder());
        int removed = 0;
        for (int idx : indexes) {
            if (idx >= 0 && idx < s.gates.size()) {
                s.gates.remove(idx);
                removed++;
            }
        }
        s.selected.clear();
        send(player, "setup.autotrace.selected-deleted", "count", String.valueOf(removed));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.7f, 0.8f);
    }

    public void shutdown() {
        for (Session s : sessions.values()) {
            if (s.captureTask != null) s.captureTask.cancel();
        }
        sessions.clear();
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
    }

    // ------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------

    private void captureTick(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        if (!player.isOnline()) {
            cancel(player, false);
            return;
        }
        if (!s.recording) return;

        Location loc = player.getLocation();
        Location last = s.samples.isEmpty() ? null : s.samples.get(s.samples.size() - 1);
        if (last != null) {
            double d = loc.distance(last);
            if (d < s.minDistance) return;
            s.pathLength += d;
        }

        if (s.samples.size() >= s.maxSamples) {
            send(player, "setup.autotrace.sample-limit", "max", String.valueOf(s.maxSamples));
            stop(player);
            return;
        }

        if (s.first == null) {
            s.first = loc.clone();
        }
        s.samples.add(loc.clone());
        sendRecordingActionBar(player, s, loc);
        sendRecordingReminder(player, s, loc);

        // Auto-stop once we come back near the start after a meaningful path.
        if (s.first != null && s.samples.size() > 20 && s.pathLength >= s.closeMinLength
                && loc.getWorld() != null && loc.getWorld().equals(s.first.getWorld())
                && loc.distance(s.first) <= s.closeDistance) {
            stop(player);
        }
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    private List<PlaneCheckpoint> generate(Session s) {
        List<PlaneCheckpoint> out = new ArrayList<>();
        if (s.samples.size() < 3) return out;

        List<Location> simplified = simplifyRdp(s.samples, s.epsilon);
        List<Location> resampled = resample(simplified, s.spacing);
        if (resampled.size() > 2) {
            Location first = resampled.get(0);
            Location last = resampled.get(resampled.size() - 1);
            if (last.getWorld() != null && last.getWorld().equals(first.getWorld())
                    && last.distance(first) < s.spacing * 0.75) {
                resampled.remove(resampled.size() - 1);
            }
        }

        for (int i = 0; i < resampled.size(); i++) {
            Location cur = resampled.get(i);
            Location prev = i > 0 ? resampled.get(i - 1) : cur;
            Location next = i < resampled.size() - 1 ? resampled.get(i + 1) : cur;
            Vector dir = next.toVector().subtract(prev.toVector());
            if (dir.lengthSquared() < 1e-8) dir = new Vector(0, 0, 1);

            Location center = s.recenterIce ? recenterOnIce(cur) : cur;
            String worldName = center.getWorld() != null ? center.getWorld().getName() : cur.getWorld().getName();
            out.add(PlaneCheckpoint.facing(worldName, center.toVector(), dir, s.halfWidth, s.halfHeight));
        }
        return out;
    }

    private static List<Location> simplifyRdp(List<Location> points, double epsilon) {
        if (points.size() < 3) return new ArrayList<>(points);
        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, points.size() - 1});
        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int first = range[0];
            int last = range[1];
            double maxDistance = -1.0;
            int index = -1;
            for (int i = first + 1; i < last; i++) {
                double d = perpendicularDistance(points.get(i), points.get(first), points.get(last));
                if (d > maxDistance) {
                    maxDistance = d;
                    index = i;
                }
            }
            if (maxDistance > epsilon && index > 0) {
                keep[index] = true;
                stack.push(new int[]{first, index});
                stack.push(new int[]{index, last});
            }
        }

        List<Location> out = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) out.add(points.get(i));
        }
        return out;
    }

    private static double perpendicularDistance(Location point, Location a, Location b) {
        Vector ab = b.toVector().subtract(a.toVector());
        Vector ap = point.toVector().subtract(a.toVector());
        double abLengthSq = ab.lengthSquared();
        if (abLengthSq < 1e-12) return ap.length();
        double t = Math.max(0.0, Math.min(1.0, ap.dot(ab) / abLengthSq));
        Vector projection = a.toVector().add(ab.multiply(t));
        return point.toVector().distance(projection);
    }

    private static List<Location> resample(List<Location> points, double spacing) {
        List<Location> out = new ArrayList<>();
        if (points.isEmpty()) return out;
        out.add(points.get(0));

        double distanceSinceLast = 0.0;
        Location prev = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            Location cur = points.get(i);
            double segmentLength = prev.distance(cur);
            if (segmentLength < 1e-9) continue;

            Vector dir = cur.toVector().subtract(prev.toVector()).multiply(1.0 / segmentLength);
            double travelled = 0.0;
            while (distanceSinceLast + (segmentLength - travelled) >= spacing) {
                double need = spacing - distanceSinceLast;
                travelled += need;
                distanceSinceLast = 0.0;
                out.add(prev.clone().add(dir.clone().multiply(travelled)));
            }
            distanceSinceLast += segmentLength - travelled;
            prev = cur;
        }

        Location last = points.get(points.size() - 1);
        if (out.get(out.size() - 1).distance(last) > spacing * 0.5) out.add(last);
        return out;
    }

    private static Location recenterOnIce(Location loc) {
        World world = loc.getWorld();
        if (world == null) return loc;
        int baseY = loc.getBlockY();
        Block best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dy = -3; dy <= 3; dy++) {
            Block block = world.getBlockAt(loc.getBlockX(), baseY + dy, loc.getBlockZ());
            Material type = block.getType();
            if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE) {
                int d = Math.abs(dy);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = block;
                }
            }
        }
        if (best == null) return loc;
        Location out = loc.clone();
        out.setY(best.getY() + 1.2);
        return out;
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    private void ensureRenderTask() {
        if (renderTask != null) return;
        int period = Math.max(2, plugin.getConfig().getInt("setup.auto-trace.preview-period-ticks", 10));
        renderTask = SchedulerCompat.runTimer(plugin, this::renderTick, period, period);
    }

    private void renderTick() {
        if (sessions.isEmpty()) {
            if (renderTask != null) {
                renderTask.cancel();
                renderTask = null;
            }
            return;
        }

        Particle particle = ParticleWireframe.resolve(plugin.getConfig().getString("setup.auto-trace.preview-particle", "END_ROD"));
        double viewDistance = plugin.getConfig().getDouble("setup.auto-trace.preview-view-distance", 96.0);
        double viewSq = viewDistance * viewDistance;

        Iterator<Map.Entry<UUID, Session>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session s = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                if (s.captureTask != null) s.captureTask.cancel();
                it.remove();
                continue;
            }
            if (s.recording) {
                if (plugin.getConfig().getBoolean("setup.auto-trace.start-marker", true)
                        && s.first != null && player.getWorld() != null && player.getWorld().equals(s.first.getWorld())) {
                    drawStartMarker(player, particle, s.first);
                }
                continue;
            }
            if (!s.preview || s.gates.isEmpty()) continue;
            Particle selectionParticle = ParticleWireframe.resolve(
                    plugin.getConfig().getString("setup.auto-trace.selection-particle", "VILLAGER_HAPPY"));

            for (int i = 0; i < s.gates.size(); i++) {
                PlaneCheckpoint gate = s.gates.get(i);
                if (!player.getWorld().getName().equals(gate.worldName())) continue;
                if (viewDistance > 0) {
                    Location gateLocation = gate.getCenter().toLocation(player.getWorld());
                    if (player.getLocation().distanceSquared(gateLocation) > viewSq) continue;
                }
                ParticleWireframe.drawGate(player, s.selected.contains(i) ? selectionParticle : particle, gate);
            }
        }
    }

    private void sendRecordingActionBar(Player player, Session s, Location loc) {
        if (!plugin.getConfig().getBoolean("setup.auto-trace.actionbar", true)) return;
        long seconds = Math.max(0L, (System.currentTimeMillis() - s.startedAt) / 1000L);
        String distance = "?";
        if (s.first != null && s.first.getWorld() != null && loc.getWorld() != null && loc.getWorld().equals(s.first.getWorld())) {
            distance = String.format(Locale.ROOT, "%.1f", loc.distance(s.first));
        }
        player.sendActionBar(Text.c(plugin.msg().get("setup.autotrace.actionbar",
                "time", String.valueOf(seconds),
                "samples", String.valueOf(s.samples.size()),
                "distance", distance)));
    }

    private void drawStartMarker(Player player, Particle particle, Location start) {
        for (double dy = 0.0; dy <= 3.0; dy += 0.5) {
            ParticleWireframe.spawnPoint(player, particle, start.getX(), start.getY() + dy, start.getZ());
        }
    }

    /** Clickable Help / Stop / Preview / Accept / Cancel (+ selection actions) buttons. */
    private void sendControls(Player player, boolean withSelectionActions) {
        net.kyori.adventure.text.Component line = Text.c(" ")
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-help"), "/boatracing setup autotrace help"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-stop"), "/boatracing setup autotrace stop"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-preview"), "/boatracing setup autotrace preview"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-accept"), "/boatracing setup autotrace accept"))
                .append(Text.c(" "))
                .append(Text.cmd(plugin.msg().get("setup.autotrace.btn-cancel"), "/boatracing setup autotrace cancel"));
        if (withSelectionActions) {
            line = line
                    .append(Text.c(" "))
                    .append(Text.suggest(plugin.msg().get("setup.autotrace.btn-resize-selected"), "/boatracing setup autotrace resize selected 5 3"))
                    .append(Text.c(" "))
                    .append(Text.suggest(plugin.msg().get("setup.autotrace.btn-delete-selected"), "/boatracing setup autotrace delete selected"));
        }
        player.sendMessage(line);
    }

    private void send(Player player, String key, Object... pairs) {
        if (player == null) return;
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(key, pairs)));
    }
}
