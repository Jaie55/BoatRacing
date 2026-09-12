package es.jaie55.boatracing.ui;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.cosmetics.CosmeticsCatalog;
import es.jaie55.boatracing.cosmetics.VictoryEffectType;
import es.jaie55.boatracing.team.Team;
import es.jaie55.boatracing.util.PracticeStatsManager;
import es.jaie55.boatracing.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Readable stats menu: competitive summary on the main page and a paginated practice
 * per-track breakdown on a second page.
 */
public class StatsGUI implements Listener {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final BoatRacingPlugin plugin;
    private final Component TITLE;
    private final NamespacedKey KEY_ACTION;
    private final Map<UUID, String> pageByPlayer = new HashMap<>();
    private final Map<UUID, Integer> practicePageByPlayer = new HashMap<>();

    public StatsGUI(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.KEY_ACTION = new NamespacedKey(plugin, "stats-action");
        this.TITLE = Text.title(plugin.msg().get("gui.stats.title"));
    }

    public void open(Player viewer, OfflinePlayer target) {
        open(viewer, target, "main", 0);
    }

    public void open(Player viewer, OfflinePlayer target, String page, int practicePage) {
        if (viewer == null || target == null || target.getUniqueId() == null) return;
        if (!viewer.hasPermission("boatracing.stats")) {
            viewer.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.no-permission")));
            return;
        }
        String normalized = "practice".equalsIgnoreCase(page) ? "practice" : "main";
        pageByPlayer.put(viewer.getUniqueId(), normalized);
        practicePageByPlayer.put(viewer.getUniqueId(), Math.max(0, practicePage));
        lastTarget.put(viewer.getUniqueId(), target);

        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fill(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        if ("practice".equals(normalized)) {
            populatePractice(viewer, target, inv);
        } else {
            populateMain(viewer, target, inv);
        }

        inv.setItem(45, action(Material.PLAYER_HEAD, plugin.msg().get("gui.stats.btn-main"), "stats:page:main"));
        inv.setItem(46, action(Material.CLOCK, plugin.msg().get("gui.stats.btn-practice"), "stats:page:practice"));
        inv.setItem(49, action(Material.BARRIER, plugin.msg().get("gui.stats.btn-close"), "stats:close"));
        if ("practice".equals(normalized)) {
            inv.setItem(52, action(Material.ARROW, plugin.msg().get("gui.stats.btn-prev-page"), "stats:prev"));
            inv.setItem(53, action(Material.ARROW, plugin.msg().get("gui.stats.btn-next-page"), "stats:next"));
        }

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private void populateMain(Player viewer, OfflinePlayer target, Inventory inv) {
        UUID id = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : id.toString();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = head.getItemMeta();
        if (headMeta instanceof SkullMeta skull) {
            skull.setOwningPlayer(target);
            skull.displayName(Text.item("&f" + name));
            List<String> lore = new ArrayList<>();
            plugin.getTeamManager().getTeamByMember(id).ifPresent(team -> {
                lore.add(plugin.msg().get("gui.stats.lore-team", "team", team.getName()));
                int number = team.getRacerNumber(id);
                if (number > 0) lore.add(plugin.msg().get("gui.stats.lore-number", "number", String.valueOf(number)));
                String boat = team.getBoatType(id);
                if (boat != null) lore.add(plugin.msg().get("gui.stats.lore-boat", "boat", boat));
            });
            if (plugin.getTitleManager() != null) {
                String titleId = plugin.getTitleManager().resolvedTitle(id);
                if (titleId != null) lore.add(plugin.msg().get("gui.stats.lore-title",
                        "title", plugin.getTitleManager().displayName(titleId)));
            }
            if (plugin.getPlayerPrefsManager() != null) {
                String trailId = plugin.getPlayerPrefsManager().getTrail(id);
                if (trailId != null && plugin.getCosmeticsCatalog() != null) {
                    CosmeticsCatalog.TrailDefinition trail = plugin.getCosmeticsCatalog().trailById(trailId);
                    if (trail != null) lore.add(plugin.msg().get("gui.stats.lore-trail",
                            "trail", plugin.msg().get(trail.messageKey())));
                }
                String effectId = plugin.getPlayerPrefsManager().getVictoryEffect(id);
                VictoryEffectType effect = effectId != null ? VictoryEffectType.byId(effectId) : VictoryEffectType.DEFAULT;
                if (effect != null) lore.add(plugin.msg().get("gui.stats.lore-effect",
                        "effect", plugin.msg().get(effect.messageKey())));
            }
            if (plugin.getStatsManager() != null) {
                lore.add(plugin.msg().get("gui.stats.lore-wins",
                        "wins", String.valueOf(plugin.getStatsManager().getPlayerWins(id))));
            }
            skull.lore(Text.lore(lore));
            skull.addItemFlags(ItemFlag.values());
            head.setItemMeta(skull);
        }
        inv.setItem(13, head);

        if (plugin.getStatsManager() != null) {
            List<String> resultLore = new ArrayList<>();
            Long bestRace = plugin.getStatsManager().getPlayerBestRace(id);
            Long bestLap = plugin.getStatsManager().getPlayerBestLap(id);
            resultLore.add(plugin.msg().get("gui.stats.lore-best-race", "time", formatMillis(bestRace)));
            resultLore.add(plugin.msg().get("gui.stats.lore-best-lap", "time", formatMillis(bestLap)));
            inv.setItem(20, card(Material.GOLD_INGOT, Text.item(plugin.msg().get("gui.stats.btn-results")), resultLore));

            List<String> positionLore = new ArrayList<>();
            Map<Integer, Integer> positions = plugin.getStatsManager().getPlayerPositions(id);
            if (positions.isEmpty()) {
                positionLore.add(plugin.msg().get("stats.positions-none"));
            } else {
                positions.forEach((position, count) -> positionLore.add(
                        plugin.msg().get("gui.stats.lore-position", "position", String.valueOf(position), "count", String.valueOf(count))));
            }
            inv.setItem(22, card(Material.DIAMOND, Text.item(plugin.msg().get("gui.stats.btn-positions")), positionLore));
        }
    }

    private void populatePractice(Player viewer, OfflinePlayer target, Inventory inv) {
        Map<String, PracticeStatsManager.PlayerTrackStatsView> stats = plugin.getPracticeStatsManager() != null
                ? plugin.getPracticeStatsManager().getAllTrackStats(target.getUniqueId())
                : Map.of();
        if (stats.isEmpty()) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.stats.practice-none")), List.of()));
            return;
        }
        List<Map.Entry<String, PracticeStatsManager.PlayerTrackStatsView>> entries = new ArrayList<>(stats.entrySet());
        entries.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        int page = practicePageByPlayer.getOrDefault(viewer.getUniqueId(), 0);
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);
        practicePageByPlayer.put(viewer.getUniqueId(), page);

        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            Map.Entry<String, PracticeStatsManager.PlayerTrackStatsView> entry = entries.get(i);
            PracticeStatsManager.PlayerTrackStatsView view = entry.getValue();
            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().get("gui.stats.lore-best-run", "time", formatMillis(view.getBestRunMillis())));
            lore.add(plugin.msg().get("gui.stats.lore-last-run", "time", formatMillis(view.getLastRunMillis())));
            lore.add(plugin.msg().get("gui.stats.lore-best-lap-practice", "time", formatMillis(view.getBestLapMillis())));
            lore.add(plugin.msg().get("gui.stats.lore-last-lap", "time", formatMillis(view.getLastLapMillis())));
            String sectors = formatSectors(view.getBestSectorMillis());
            if (!sectors.isEmpty()) {
                lore.add(plugin.msg().get("gui.stats.lore-sectors", "sectors", sectors));
            }
            inv.setItem(i - start, card(Material.ICE, Text.item("&b" + entry.getKey()), lore));
        }

        inv.setItem(50, card(Material.NETHER_STAR, Text.item(plugin.msg().get("gui.stats.page",
                "current", String.valueOf(page + 1), "total", String.valueOf(maxPage + 1))), List.of()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!Text.plain(e.getView().title()).equals(Text.plain(TITLE))) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String action = meta.getPersistentDataContainer().get(KEY_ACTION, PersistentDataType.STRING);
        if (action == null) return;
        if (!player.hasPermission("boatracing.stats")) return;

        if (action.startsWith("stats:page:")) {
            open(player, resolveTarget(player), action.substring("stats:page:".length()), 0);
            return;
        }
        if ("stats:close".equals(action)) {
            player.closeInventory();
            return;
        }
        if ("stats:prev".equals(action)) {
            open(player, resolveTarget(player), "practice",
                    practicePageByPlayer.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }
        if ("stats:next".equals(action)) {
            open(player, resolveTarget(player), "practice",
                    practicePageByPlayer.getOrDefault(player.getUniqueId(), 0) + 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null) return;
        if (Text.plain(e.getView().title()).equals(Text.plain(TITLE))) {
            e.setCancelled(true);
        }
    }

    /** Last target opened by this viewer, so pagination keeps showing the same player. */
    private final Map<UUID, OfflinePlayer> lastTarget = new HashMap<>();

    private OfflinePlayer resolveTarget(Player viewer) {
        OfflinePlayer target = lastTarget.get(viewer.getUniqueId());
        return target != null ? target : viewer;
    }

    // --- formatting ---

    private static String formatMillis(Long millis) {
        if (millis == null || millis < 0L) return "-";
        long total = millis;
        long minutes = total / 60000L;
        long seconds = (total % 60000L) / 1000L;
        long ms = total % 1000L;
        if (minutes > 0) return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, ms);
        return String.format(Locale.ROOT, "%d.%03d", seconds, ms);
    }

    private static String formatSectors(Map<Integer, Long> sectors) {
        if (sectors == null || sectors.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        sectors.forEach((index, millis) -> {
            if (out.length() > 0) out.append(" ");
            out.append("S").append(index).append(" ").append(formatMillis(millis));
        });
        return out.toString();
    }

    // --- item helpers ---

    private ItemStack action(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack card(Material material, Component name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (!lore.isEmpty()) meta.lore(Text.lore(lore));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fill(Inventory inv, ItemStack filler) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) inv.setItem(i, filler);
        }
    }
}
