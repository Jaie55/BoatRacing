package es.jaie55.boatracing.ui;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.race.RaceManager;
import es.jaie55.boatracing.track.CheckpointShape;
import es.jaie55.boatracing.track.Region;
import es.jaie55.boatracing.track.SelectionUtils;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.SchedulerCompat;
import es.jaie55.boatracing.util.Text;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminRaceGUI implements Listener {
    private final Component TITLE;
    private final BoatRacingPlugin plugin;
    private final org.bukkit.NamespacedKey KEY_ACTION;
    private final org.bukkit.NamespacedKey KEY_PLAYER_ID;
    private final java.util.Set<java.util.UUID> checkpointsView = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, Integer> checkpointPageByPlayer = new java.util.HashMap<>();
    private static final int CHECKPOINT_PAGE_SIZE = 45;

    public AdminRaceGUI(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.KEY_ACTION = new org.bukkit.NamespacedKey(plugin, "adm-race-action");
        this.KEY_PLAYER_ID = new org.bukkit.NamespacedKey(plugin, "adm-race-player");
        this.TITLE = Text.title(plugin.msg().get("gui.race.title"));
    }

    public void open(Player p) {
        openMain(p);
    }

    private void openMain(Player p) {
        checkpointsView.remove(p.getUniqueId());
        checkpointPageByPlayer.remove(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv, pane(Material.GRAY_STAINED_GLASS_PANE));

        String trackKey = activeTrackKey();
        RaceManager rm = resolveRaceManager(trackKey);
        if (rm == null) rm = plugin.getRaceManager();
        TrackConfig tc = rm.getTrack();
        String cur = activeTrackLabel(trackKey);
        boolean running = rm.isRunning();
        boolean registering = rm.isRegistering();
        int regs = rm.getRegistered().size();
        int laps = rm.getTotalLaps();
        int pitstops = rm.getMandatoryPitstops();
        int participants = running ? rm.getParticipants().size() : 0;
        boolean ready = tc.isReady();

        // Status card (center top)
        inv.setItem(4, card(
            Material.NETHER_STAR,
            Text.item(plugin.msg().get("gui.race.btn-status")),
            java.util.Arrays.asList(
                plugin.msg().get("gui.race.lore-track", "track", cur),
                plugin.msg().get(ready ? "gui.race.lore-ready-yes" : "gui.race.lore-ready-no"),
                plugin.msg().get("gui.race.lore-laps", "laps", laps),
                plugin.msg().get("race.status.mandatory-pitstops", "count", pitstops),
                plugin.msg().get("race.status.checkpoints", "count", tc.getCheckpoints().size()),
                plugin.msg().get(registering ? "gui.race.lore-registration-open" : "gui.race.lore-registration-closed", "count", regs),
                plugin.msg().get(running ? "gui.race.lore-running-yes" : "gui.race.lore-running-no", "count", participants)
            )
        ));

        // Fast navigation/edit actions
        inv.setItem(7, action(Material.COMPASS, plugin.msg().get("gui.race.btn-checkpoints-editor"), "cp:editor"));
        inv.setItem(8, action(Material.MAP, plugin.msg().get("gui.admin.btn-manage-tracks"), "tracks"));

        // Controls row
        inv.setItem(18, action(Material.LIME_DYE, plugin.msg().get("gui.race.btn-open-reg"), "open"));
        inv.setItem(19, action(Material.RED_DYE, plugin.msg().get("gui.race.btn-close-reg"), "close"));
        inv.setItem(20, action(Material.GOLD_BLOCK, plugin.msg().get("gui.race.btn-start"), "start"));
        inv.setItem(21, action(Material.REDSTONE_TORCH, plugin.msg().get("gui.race.btn-force"), "force"));
        inv.setItem(22, action(Material.BARRIER, plugin.msg().get("gui.race.btn-stop"), "stop"));
        String voteState = plugin.msg().get(plugin.isMapVoteOpen() ? "gui.race.lore-vote-open" : "gui.race.lore-vote-closed");
        inv.setItem(23, actionWithLore(Material.CARTOGRAPHY_TABLE, plugin.msg().get("gui.race.btn-vote-setup"), "vote:setup", java.util.Arrays.asList(voteState)));
        inv.setItem(24, action(Material.BARRIER, plugin.msg().get("gui.race.btn-vote-close"), "vote:close"));

        // Laps quick set
        inv.setItem(27, action(Material.PAPER, plugin.msg().get("gui.race.btn-set-laps", "n", "1"), "laps:1"));
        inv.setItem(28, action(Material.PAPER, plugin.msg().get("gui.race.btn-set-laps", "n", "3"), "laps:3"));
        inv.setItem(29, action(Material.PAPER, plugin.msg().get("gui.race.btn-set-laps", "n", "5"), "laps:5"));
        inv.setItem(30, action(Material.PAPER, plugin.msg().get("gui.race.btn-set-laps", "n", "10"), "laps:10"));
        inv.setItem(31, action(Material.NAME_TAG, plugin.msg().get("gui.race.btn-custom-laps"), "laps:custom"));

        // Pit stop quick set
        inv.setItem(32, action(Material.HOPPER, plugin.msg().get("gui.race.btn-set-pitstops", "n", "0"), "pitstops:0"));
        inv.setItem(33, action(Material.HOPPER, plugin.msg().get("gui.race.btn-set-pitstops", "n", "1"), "pitstops:1"));
        inv.setItem(34, action(Material.HOPPER, plugin.msg().get("gui.race.btn-set-pitstops", "n", "2"), "pitstops:2"));
        inv.setItem(35, action(Material.NAME_TAG, plugin.msg().get("gui.race.btn-custom-pitstops"), "pitstops:custom"));

        // Track edit shortcuts (some are command tips)
        inv.setItem(36, action(Material.WHITE_CONCRETE, plugin.msg().get("gui.race.btn-add-start-tip"), "tip:addstart"));
        inv.setItem(37, action(Material.TNT, plugin.msg().get("gui.race.btn-clear-starts"), "clear:starts"));
        inv.setItem(38, action(Material.YELLOW_CONCRETE, plugin.msg().get("gui.race.btn-set-finish-tip"), "tip:setfinish"));
        inv.setItem(39, action(Material.BARRIER, plugin.msg().get("gui.race.btn-clear-finish"), "clear:finish"));
        inv.setItem(40, action(Material.LIGHT_BLUE_CONCRETE, plugin.msg().get("gui.race.btn-set-pit-tip"), "tip:setpit"));
        inv.setItem(41, action(Material.BARRIER, plugin.msg().get("gui.race.btn-clear-pit"), "clear:pit"));
        inv.setItem(42, action(Material.LIGHT_GRAY_CONCRETE, plugin.msg().get("gui.race.btn-add-checkpoint-tip"), "tip:addcheckpoint"));
        inv.setItem(43, action(Material.TNT, plugin.msg().get("gui.race.btn-clear-checkpoints"), "clear:checkpoints"));
        inv.setItem(44, action(Material.BOOK, plugin.msg().get("gui.race.btn-open-wizard-tip"), "tip:wizard"));

        // Participants heads (registered online players)
        int slot = 9;
        for (UUID id : rm.getRegistered()) {
            Player op = Bukkit.getPlayer(id);
            if (op == null) continue;
            if (slot >= 18) break; // reserve row 2 for controls
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(op);
                sm.displayName(Text.item("&f" + op.getName()));
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(plugin.msg().get("gui.race.lore-click-remove-reg"));
                sm.lore(Text.lore(lore));
                sm.getPersistentDataContainer().set(KEY_PLAYER_ID, org.bukkit.persistence.PersistentDataType.STRING, id.toString());
                sm.getPersistentDataContainer().set(KEY_ACTION, org.bukkit.persistence.PersistentDataType.STRING, "kickreg");
                head.setItemMeta(sm);
            }
            inv.setItem(slot++, head);
        }

    // Back button
    inv.setItem(45, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));

        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory() == null) return;
        if (!Text.plain(e.getView().title()).equals(Text.plain(TITLE))) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        HumanEntity he = e.getWhoClicked(); if (!(he instanceof Player)) return; Player p = (Player) he;
        if (!p.hasPermission("boatracing.setup")) { p.closeInventory(); return; }
        ItemStack it = e.getCurrentItem(); if (it == null) return;
        ItemMeta im = it.getItemMeta(); if (im == null) return;
        String action = im.getPersistentDataContainer().get(KEY_ACTION, org.bukkit.persistence.PersistentDataType.STRING);
        if (action == null) {
            // Back button
            if (it.getType() == Material.ARROW) {
                if (checkpointsView.contains(p.getUniqueId())) {
                    openMain(p);
                } else {
                    plugin.getAdminGUI().openMain(p);
                }
                return;
            }
            return;
        }

        if (checkpointsView.contains(p.getUniqueId())) {
            handleCheckpointAction(p, e, action);
            return;
        }

        // Some actions open an Anvil (e.g., custom laps). If we refresh immediately,
        // it will close the Anvil. Skip auto-refresh for those.
        boolean skipRefresh = "laps:custom".equals(action)
                || "pitstops:custom".equals(action)
                || "vote:setup".equals(action)
                || "cp:editor".equals(action)
                || "tracks".equals(action);
        handle(p, action, im);
        if (!skipRefresh) {
            // Refresh UI after action
            SchedulerCompat.runNow(plugin, () -> openMain(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory() == null) return;
        if (Text.plain(e.getView().title()).equals(Text.plain(TITLE))) e.setCancelled(true);
    }

    private void handle(Player p, String action, ItemMeta im) {
        String trackKey = activeTrackKey();
        RaceManager rm = resolveRaceManager(trackKey);
        if (rm == null) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-found", "track", trackKey)));
            return;
        }
        TrackConfig tc = rm.getTrack();
        switch (action) {
            case "open" -> {
                if (!tc.isReady()) {
                    p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-ready", "requirements", plugin.formatTrackRequirements(tc.missingRequirements()))));
                    return;
                }
                rm.loadSettings();
                int laps = rm.getTotalLaps();
                boolean ok = rm.openRegistration(laps, null);
                if (!ok) p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.cannot-open-registration")));
            }
            case "close" -> rm.cancelRegistration(true);
            case "start" -> {
                if (rm.isRunning()) { p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.already-running"))); break; }
                rm.loadSettings();
                java.util.Set<java.util.UUID> regs = new java.util.LinkedHashSet<>(rm.getRegistered());
                java.util.List<Player> participants = new java.util.ArrayList<>();
                for (java.util.UUID id : regs) { Player op = Bukkit.getPlayer(id); if (op != null) participants.add(op); }
                if (participants.isEmpty()) { p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.no-participants", "label", "boatracing"))); break; }
                int minPlayers = rm.getMinPlayersToStart();
                if (participants.size() < minPlayers) {
                    p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(
                            "race.not-enough-players",
                            "min", String.valueOf(minPlayers),
                            "current", String.valueOf(participants.size())
                    )));
                    break;
                }
                rm.forceStart();
            }
            case "force" -> rm.forceStart();
            case "stop" -> {
                boolean any = false;
                if (rm.isRegistering()) any |= rm.cancelRegistration(true);
                if (rm.isRunning()) any |= rm.cancelRace();
                if (!any) p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.nothing-to-stop")));
            }
            case "vote:setup" -> {
                if (plugin.getVoteGUI() != null) plugin.getVoteGUI().openAdminSetup(p);
            }
            case "vote:close" -> plugin.closeMapVoteFromAdmin(p);
            case "tracks" -> plugin.getTracksGUI().open(p);
            case "cp:editor" -> openCheckpointEditor(p, 0);
            default -> {
                if (action.startsWith("laps:")) {
                    String v = action.substring("laps:".length());
                    if ("custom".equals(v)) { openAnvilLaps(p, trackKey); return; }
                    try {
                        if (!ensureTrackEditable(p, trackKey, rm)) return;
                        int n = Math.max(1, Integer.parseInt(v));
                        rm.setTotalLaps(n);
                        tc.setRacingOverride("laps", n);
                        p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.laps-set", "laps", n, "track_info", trackInfoSuffix(trackKey))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                    } catch (NumberFormatException ignored) {}
                    return;
                }
                if (action.startsWith("pitstops:")) {
                    String v = action.substring("pitstops:".length());
                    if ("custom".equals(v)) { openAnvilPitstops(p, trackKey); return; }
                    try {
                        if (!ensureTrackEditable(p, trackKey, rm)) return;
                        int n = Math.max(0, Integer.parseInt(v));
                        rm.setMandatoryPitstops(n);
                        tc.setRacingOverride("mandatory-pitstops", n);
                        p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.pitstops-set", "count", n, "track_info", trackInfoSuffix(trackKey))));
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.2f);
                    } catch (NumberFormatException ignored) {}
                    return;
                }
                if (action.startsWith("tip:")) {
                    switch (action.substring(4)) {
                        case "addstart" -> p.sendMessage(Text.c(plugin.msg().get("setup.tip-label")).append(Text.suggest(plugin.msg().get("setup.tip-paste-addstart"),"/boatracing setup addstart")));
                        case "setfinish" -> p.sendMessage(Text.c(plugin.msg().get("setup.tip-label")).append(Text.suggest(plugin.msg().get("setup.tip-paste-setfinish"),"/boatracing setup setfinish")));
                        case "setpit" -> p.sendMessage(Text.c(plugin.msg().get("setup.tip-label")).append(Text.suggest(plugin.msg().get("setup.tip-paste-setpit"),"/boatracing setup setpit ")));
                        case "addcheckpoint" -> p.sendMessage(Text.c(plugin.msg().get("setup.tip-label")).append(Text.suggest(plugin.msg().get("setup.tip-paste-addcheckpoint"),"/boatracing setup addcheckpoint")));
                        case "wizard" -> p.sendMessage(Text.c(plugin.msg().get("setup.tip-label")).append(Text.suggest(plugin.msg().get("setup.tip-open-wizard"),"/boatracing setup wizard")));
                    }
                    return;
                }
                if (action.startsWith("clear:")) {
                    if (!ensureTrackEditable(p, trackKey, rm)) return;
                    switch (action.substring(6)) {
                        case "starts" -> { tc.clearStarts(); p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.starts-cleared"))); }
                        case "finish" -> { tc.setFinish(null); p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.finish-cleared"))); }
                        case "pit" -> { tc.setPitlane(null); p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.pit-cleared"))); }
                        case "checkpoints" -> { tc.clearCheckpoints(); p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.checkpoints-cleared"))); }
                    }
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                    return;
                }
                if ("kickreg".equals(action)) {
                    String uid = im.getPersistentDataContainer().get(KEY_PLAYER_ID, org.bukkit.persistence.PersistentDataType.STRING);
                    if (uid != null) {
                        Player op = Bukkit.getPlayer(java.util.UUID.fromString(uid));
                        if (op != null) rm.leave(op);
                    }
                }
            }
        }
    }

    private void handleCheckpointAction(Player p, InventoryClickEvent e, String action) {
        String trackKey = activeTrackKey();
        RaceManager rm = resolveRaceManager(trackKey);
        if (rm == null) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-found", "track", trackKey)));
            openMain(p);
            return;
        }
        TrackConfig tc = rm.getTrack();

        if ("cp:refresh".equals(action)) { openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0)); return; }
        if ("cp:prev".equals(action)) { openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0) - 1); return; }
        if ("cp:next".equals(action)) { openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0) + 1); return; }
        if ("tracks".equals(action)) { plugin.getTracksGUI().open(p); return; }

        if ("cp:add".equals(action)) {
            if (!ensureTrackEditable(p, trackKey, rm)) return;
            Region r = selectedRegion(p);
            if (r == null) return;
            tc.addCheckpoint(r);
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.checkpoint-added", "num", tc.getCheckpoints().size(), "box", fmtBox(r.getBox()))));
            int addedIndex = Math.max(0, tc.getCheckpoints().size() - 1);
            openCheckpointEditor(p, addedIndex / CHECKPOINT_PAGE_SIZE);
            return;
        }

        if ("cp:clear".equals(action)) {
            if (!ensureTrackEditable(p, trackKey, rm)) return;
            tc.clearCheckpoints();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.checkpoints-cleared")));
            openCheckpointEditor(p, 0);
            return;
        }

        if ("cp:autotrace".equals(action)) {
            if (!ensureTrackEditable(p, trackKey, rm)) return;
            if (plugin.getAutoTraceManager() != null) plugin.getAutoTraceManager().start(p);
            p.closeInventory();
            return;
        }

        if (!action.startsWith("cp:item:")) return;
        int idx;
        try {
            idx = Integer.parseInt(action.substring("cp:item:".length()));
        } catch (NumberFormatException ex) {
            openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
            return;
        }

        if (idx < 0 || idx >= tc.getCheckpoints().size()) {
            openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
            return;
        }

        if (!ensureTrackEditable(p, trackKey, rm)) return;

        org.bukkit.event.inventory.ClickType click = e.getClick();
        if (click == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
            if (!tc.moveCheckpoint(idx, idx - 1)) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-move-up-fail")));
            } else {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-moved-up", "index", String.valueOf(idx + 1))));
            }
            openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
            return;
        }

        if (click == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
            if (!tc.moveCheckpoint(idx, idx + 1)) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-move-down-fail")));
            } else {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-moved-down", "index", String.valueOf(idx + 1))));
            }
            openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
            return;
        }

        if (click == org.bukkit.event.inventory.ClickType.RIGHT) {
            if (!tc.removeCheckpointAt(idx)) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-remove-fail")));
            } else {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-removed", "index", String.valueOf(idx + 1))));
            }
            openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
            return;
        }

        Region selected = selectedRegion(p);
        if (selected == null) return;
        if (!tc.replaceCheckpoint(idx, selected)) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-replace-fail")));
        } else {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.race.cp-action-replaced", "index", String.valueOf(idx + 1))));
        }
        openCheckpointEditor(p, checkpointPageByPlayer.getOrDefault(p.getUniqueId(), 0));
    }

    private void openCheckpointEditor(Player p, int requestedPage) {
        String trackKey = activeTrackKey();
        RaceManager rm = resolveRaceManager(trackKey);
        if (rm == null) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-found", "track", trackKey)));
            openMain(p);
            return;
        }
        TrackConfig tc = rm.getTrack();
        checkpointsView.add(p.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv, pane(Material.GRAY_STAINED_GLASS_PANE));

        int total = tc.getCheckpoints().size();
        int maxPage = total <= 0 ? 0 : (total - 1) / CHECKPOINT_PAGE_SIZE;
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        checkpointPageByPlayer.put(p.getUniqueId(), page);

        int start = page * CHECKPOINT_PAGE_SIZE;
        int end = Math.min(total, start + CHECKPOINT_PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CheckpointShape r = tc.getCheckpoints().get(i);
            int slot = i - start;
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(plugin.msg().get("gui.race.cp-item-lore-region", "region", summarizeRegion(r)));
            if (r.alternateCount() > 0) {
                lore.add(plugin.msg().get("gui.race.cp-item-lore-alternates", "count", r.alternateCount()));
            }
            lore.add(plugin.msg().get("gui.race.cp-item-lore-replace"));
            lore.add(plugin.msg().get("gui.race.cp-item-lore-remove"));
            lore.add(plugin.msg().get("gui.race.cp-item-lore-move-up"));
            lore.add(plugin.msg().get("gui.race.cp-item-lore-move-down"));
            inv.setItem(slot, actionWithLore(
                    Material.LIGHT_BLUE_STAINED_GLASS,
                    plugin.msg().get("gui.race.cp-item-title", "index", String.valueOf(i + 1)),
                    "cp:item:" + i,
                    lore
            ));
        }

        inv.setItem(45, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        inv.setItem(46, action(Material.LIGHT_GRAY_CONCRETE, plugin.msg().get("gui.race.btn-add-checkpoint-tip"), "cp:add"));
        inv.setItem(47, action(Material.TNT, plugin.msg().get("gui.race.btn-clear-checkpoints"), "cp:clear"));
        inv.setItem(48, action(Material.MAP, plugin.msg().get("gui.admin.btn-manage-tracks"), "tracks"));
        inv.setItem(49, action(Material.CLOCK, plugin.msg().get("gui.admin.btn-refresh"), "cp:refresh"));
        inv.setItem(50, card(
                Material.NETHER_STAR,
                Text.item(plugin.msg().get("gui.race.btn-status")),
                java.util.Arrays.asList(
                        plugin.msg().get("gui.race.lore-track", "track", activeTrackLabel(trackKey)),
                        plugin.msg().get("race.status.checkpoints", "count", total),
                plugin.msg().get("gui.race.cp-page", "current", String.valueOf(page + 1), "total", String.valueOf(maxPage + 1))
                )
        ));
        inv.setItem(51, action(Material.ENDER_EYE, plugin.msg().get("gui.race.btn-autotrace"), "cp:autotrace"));
        inv.setItem(52, action(Material.ARROW, plugin.msg().get("gui.race.btn-prev-page"), "cp:prev"));
        inv.setItem(53, action(Material.ARROW, plugin.msg().get("gui.race.btn-next-page"), "cp:next"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private String activeTrackKey() {
        if (plugin.getTrackLibrary() == null) return "unsaved";
        String current = plugin.getTrackLibrary().getCurrent();
        return (current == null || current.isBlank()) ? "unsaved" : current;
    }

    private String activeTrackLabel(String trackKey) {
        if (trackKey == null || trackKey.isBlank() || "unsaved".equalsIgnoreCase(trackKey)) {
            return plugin.msg().get("general.unsaved");
        }
        return trackKey;
    }

    private RaceManager resolveRaceManager(String trackKey) {
        RaceManager rm = plugin.getOrCreateRaceManagerByTrack(trackKey);
        if (rm != null) return rm;
        if ("unsaved".equalsIgnoreCase(trackKey)) return plugin.getRaceManager();
        return null;
    }

    private String trackInfoSuffix(String trackKey) {
        if (trackKey == null || trackKey.isBlank() || "unsaved".equalsIgnoreCase(trackKey)) return "";
        return plugin.msg().get("setup.track-info", "track", trackKey);
    }

    private boolean ensureTrackEditable(Player p, String trackKey, RaceManager rm) {
        if (rm != null && (rm.isRunning() || rm.isCountdownActive())) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.already-running")));
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return false;
        }
        if (rm != null && rm.isRegistering()) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.cannot-open-registration")));
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return false;
        }
        return true;
    }

    private Region selectedRegion(Player p) {
        var sel = SelectionUtils.getSelectionDetailed(p);
        if (sel == null) {
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.no-selection")));
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return null;
        }
        return new Region(sel.worldName, sel.box);
    }

    private void openAnvilLaps(Player p, String trackKey) {
        ItemStack left = new ItemStack(Material.PAPER);
        ItemMeta lm = left.getItemMeta(); if (lm != null) { lm.displayName(Component.empty()); left.setItemMeta(lm); }
        new AnvilGUI.Builder()
            .title(plugin.msg().get("gui.race.anvil-set-laps"))
            .text(" ")
            .itemLeft(left)
            .interactableSlots()
            .onClick((slot, state) -> {
                if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();
                String input = state.getText() == null ? "" : state.getText().trim();
                try {
                    int n = Math.max(1, Integer.parseInt(input));
                    Player sp = state.getPlayer();
                    RaceManager rm = resolveRaceManager(trackKey);
                    if (rm == null) {
                        sp.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-found", "track", trackKey)));
                        return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                    }
                    if (!ensureTrackEditable(sp, trackKey, rm)) {
                        return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                    }
                    TrackConfig tc = rm.getTrack();
                    rm.setTotalLaps(n);
                    tc.setRacingOverride("laps", n);
                    sp.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.laps-set", "laps", n, "track_info", trackInfoSuffix(trackKey))));
                    return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                } catch (NumberFormatException ex) {
                    return java.util.Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input));
                }
            })
            .plugin(plugin)
            .open(p);
    }

    private void openAnvilPitstops(Player p, String trackKey) {
        ItemStack left = new ItemStack(Material.PAPER);
        ItemMeta lm = left.getItemMeta(); if (lm != null) { lm.displayName(Component.empty()); left.setItemMeta(lm); }
        new AnvilGUI.Builder()
                .title(plugin.msg().get("gui.race.anvil-set-pitstops"))
                .text(" ")
                .itemLeft(left)
                .interactableSlots()
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();
                    String input = state.getText() == null ? "" : state.getText().trim();
                    try {
                        int n = Math.max(0, Integer.parseInt(input));
                        Player sp = state.getPlayer();
                        RaceManager rm = resolveRaceManager(trackKey);
                        if (rm == null) {
                            sp.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("race.track-not-found", "track", trackKey)));
                            return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                        }
                        if (!ensureTrackEditable(sp, trackKey, rm)) {
                            return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                        }
                        TrackConfig tc = rm.getTrack();
                        rm.setMandatoryPitstops(n);
                        tc.setRacingOverride("mandatory-pitstops", n);
                        sp.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.pitstops-set", "count", n, "track_info", trackInfoSuffix(trackKey))));
                        return java.util.Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openMain(sp)));
                    } catch (NumberFormatException ex) {
                        return java.util.Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input));
                    }
                })
                .plugin(plugin)
                .open(p);
    }

    private String summarizeRegion(CheckpointShape r) {
        if (r == null) return plugin.msg().get("gui.race.cp-invalid-region");
        return r.worldName() + " " + r.describe();
    }

    private static String fmtBox(org.bukkit.util.BoundingBox b) {
        return String.format(java.util.Locale.ROOT, "(%d,%d,%d) -> (%d,%d,%d)",
                (int) Math.floor(b.getMinX()), (int) Math.floor(b.getMinY()), (int) Math.floor(b.getMinZ()),
                (int) Math.floor(b.getMaxX()), (int) Math.floor(b.getMaxY()), (int) Math.floor(b.getMaxZ()));
    }

    // Helpers
    private static ItemStack button(Material mat, Component name) { ItemStack it = new ItemStack(mat); ItemMeta im = it.getItemMeta(); if (im != null) { im.displayName(name); im.addItemFlags(ItemFlag.values()); it.setItemMeta(im);} return it; }
    private static ItemStack pane(Material mat) { ItemStack it = new ItemStack(mat); ItemMeta im = it.getItemMeta(); if (im != null) { im.displayName(Component.text(" ")); im.addItemFlags(ItemFlag.values()); it.setItemMeta(im);} return it; }
    private static void fill(Inventory inv, ItemStack filler) { for (int i=0;i<inv.getSize();i++) if (inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR) inv.setItem(i, filler); }
    private ItemStack card(Material mat, Component name, java.util.List<String> loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(name);
            if (loreLines != null && !loreLines.isEmpty()) im.lore(Text.lore(loreLines));
            im.addItemFlags(ItemFlag.values());
            it.setItemMeta(im);
        }
        return it;
    }
    private ItemStack action(Material mat, String titleLegacy, String action) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Text.item(titleLegacy));
            im.getPersistentDataContainer().set(KEY_ACTION, org.bukkit.persistence.PersistentDataType.STRING, action);
            im.addItemFlags(ItemFlag.values());
            it.setItemMeta(im);
        }
        return it;
    }

    private ItemStack actionWithLore(Material mat, String titleLegacy, String action, java.util.List<String> loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Text.item(titleLegacy));
            if (loreLines != null && !loreLines.isEmpty()) im.lore(Text.lore(loreLines));
            im.getPersistentDataContainer().set(KEY_ACTION, org.bukkit.persistence.PersistentDataType.STRING, action);
            im.addItemFlags(ItemFlag.values());
            it.setItemMeta(im);
        }
        return it;
    }
}
