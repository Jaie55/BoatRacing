package es.jaie55.boatracing.ui;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.team.Team;
import es.jaie55.boatracing.team.TeamManager;
import es.jaie55.boatracing.util.Text;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AdminGUI implements Listener {
    private static final String BLANK = " ";
    private final Component TITLE;
    private final Component TITLE_TEAMS;
    private final Component TITLE_TEAM;
    private final Component TITLE_COLOR;
    private final Component TITLE_CONFIRM_DELETE;
    private final Component TITLE_PLAYERS;
    private final Component TITLE_PLAYER;
    private final Component TITLE_ASSIGN_TEAM;
    private final Component TITLE_BOAT_PICKER;
    private final String TEAM_TITLE_PREFIX;

    private final BoatRacingPlugin plugin;
    private final NamespacedKey KEY_TEAM_ID;
    private final NamespacedKey KEY_TARGET_ID;

    public AdminGUI(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.KEY_TEAM_ID = new NamespacedKey(plugin, "adm-team-id");
        this.KEY_TARGET_ID = new NamespacedKey(plugin, "adm-target-id");
        this.TITLE = Text.title(plugin.msg().get("gui.admin.title"));
        this.TITLE_TEAMS = Text.title(plugin.msg().get("gui.admin.title-teams"));
        this.TITLE_TEAM = Text.title(plugin.msg().get("gui.admin.title-team"));
        this.TITLE_COLOR = Text.title(plugin.msg().get("gui.admin.title-team-color"));
        this.TITLE_CONFIRM_DELETE = Text.title(plugin.msg().get("gui.admin.title-confirm-delete"));
        this.TITLE_PLAYERS = Text.title(plugin.msg().get("gui.admin.title-players"));
        this.TITLE_PLAYER = Text.title(plugin.msg().get("gui.admin.title-player"));
        this.TITLE_ASSIGN_TEAM = Text.title(plugin.msg().get("gui.admin.title-assign-team"));
        this.TITLE_BOAT_PICKER = Text.title(plugin.msg().get("gui.admin.title-boat-picker"));
        this.TEAM_TITLE_PREFIX = plugin.msg().get("gui.admin.title-team-prefix");
    }

    public void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
    inv.setItem(10, button(Material.WHITE_BANNER, Text.item(plugin.msg().get("gui.admin.btn-manage-teams"))));
    inv.setItem(12, button(Material.NETHER_STAR, Text.item(plugin.msg().get("gui.admin.btn-manage-race"))));
    inv.setItem(14, button(Material.MAP, Text.item(plugin.msg().get("gui.admin.btn-manage-tracks"))));
    inv.setItem(16, button(Material.PLAYER_HEAD, Text.item(plugin.msg().get("gui.admin.btn-manage-players"))));
    fillEmptyWith(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openTeams(Player p) {
        TeamManager tm = plugin.getTeamManager();
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_TEAMS);
        int slot = 0;
        for (Team t : tm.getTeamsSnapshot()) {
            ItemStack item = new ItemStack(bannerForColor(t.getColor()));
            BannerMeta meta = (BannerMeta) item.getItemMeta();
            if (meta != null) {
                meta.displayName(Text.item("&l" + t.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.msg().get("gui.admin.lore-members", "current", t.getMembers().size(), "max", tm.getMaxMembers()));
                lore.add(plugin.msg().get("gui.admin.lore-color", "color", t.getColor().name()));
                lore.add(" ");
                lore.add(plugin.msg().get("gui.admin.lore-click-open"));
                meta.lore(Text.lore(lore));
                meta.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            if (slot >= size - 9) break;
        }
    int base = size - 9;
    fillRow(inv, base, pane(Material.GRAY_STAINED_GLASS_PANE));
    inv.setItem(base, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
    inv.setItem(base + 4, button(Material.ANVIL, Text.item(plugin.msg().get("gui.admin.btn-create-team"))));
    inv.setItem(base + 7, buttonWithLore(Material.CLOCK, Text.item(plugin.msg().get("gui.admin.btn-refresh")), java.util.Arrays.asList(plugin.msg().get("gui.admin.lore-refresh"))));
    inv.setItem(base + 8, buttonWithLore(Material.NETHER_STAR, Text.item(plugin.msg().get("gui.admin.btn-player-view")), java.util.Arrays.asList(plugin.msg().get("gui.admin.lore-player-view"))));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private static ItemStack buttonWithLore(Material mat, Component name, java.util.List<String> loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(name);
            if (loreLines != null && !loreLines.isEmpty()) {
                im.lore(Text.lore(loreLines));
            }
            im.addItemFlags(ItemFlag.values());
            it.setItemMeta(im);
        }
        return it;
    }

    private void openPlayers(Player p) {
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_PLAYERS);
        int slot = 0;
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (slot >= size - 9) break;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(op);
                sm.displayName(Text.item("&f" + op.getName()));
                List<String> lore = new ArrayList<>();
                plugin.getTeamManager().getTeamByMember(op.getUniqueId()).ifPresentOrElse(team -> {
                    lore.add(plugin.msg().get("gui.admin.lore-team", "team", team.getName()));
                    int num = team.getRacerNumber(op.getUniqueId());
                    String numStr = num == 0 ? plugin.msg().get("gui.common.unset") : String.valueOf(num);
                    lore.add(plugin.msg().get("gui.admin.lore-racer-number", "num", numStr));
                    lore.add(plugin.msg().get("gui.admin.lore-boat", "boat", team.getBoatType(op.getUniqueId())));
                }, () -> lore.add(plugin.msg().get("gui.admin.lore-team-none")));
                lore.add(" ");
                lore.add(plugin.msg().get("gui.admin.lore-click-open"));
                sm.lore(Text.lore(lore));
                sm.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, op.getUniqueId().toString());
                skull.setItemMeta(sm);
            }
            inv.setItem(slot++, skull);
        }
    fillRow(inv, size - 9, pane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(size - 9, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openPlayerView(Player admin, OfflinePlayer target) {
        int size = 45;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_PLAYER);

        // Header with skull
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) skull.getItemMeta();
        if (sm != null) {
            sm.setOwningPlayer(target);
            sm.displayName(Text.item("&l" + (target.getName() != null ? target.getName() : target.getUniqueId())));
            List<String> lore = new ArrayList<>();
            plugin.getTeamManager().getTeamByMember(target.getUniqueId()).ifPresentOrElse(team -> {
                lore.add(plugin.msg().get("gui.admin.lore-team", "team", team.getName()));
                int num = team.getRacerNumber(target.getUniqueId());
                String numStr2 = num == 0 ? plugin.msg().get("gui.common.unset") : String.valueOf(num);
                lore.add(plugin.msg().get("gui.admin.lore-racer-number", "num", numStr2));
                lore.add(plugin.msg().get("gui.admin.lore-boat", "boat", team.getBoatType(target.getUniqueId())));
            }, () -> lore.add(plugin.msg().get("gui.admin.lore-team-none")));
            sm.lore(Text.lore(lore));
            sm.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            skull.setItemMeta(sm);
        }
        inv.setItem(4, skull);

        int base = size - 9;
    inv.setItem(base, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
    inv.setItem(base + 7, button(Material.CLOCK, Text.item(plugin.msg().get("gui.admin.btn-refresh"))));
    inv.setItem(base + 8, button(Material.NETHER_STAR, Text.item(plugin.msg().get("gui.admin.btn-player-view"))));

        // Assign team
        ItemStack assign = new ItemStack(Material.WHITE_BANNER);
        ItemMeta am = assign.getItemMeta();
        if (am != null) {
            am.displayName(Text.item(plugin.msg().get("gui.admin.btn-assign-team")));
            am.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            assign.setItemMeta(am);
        }
        inv.setItem(base + 2, assign);

        // Set racer number
        ItemStack number = new ItemStack(Material.NAME_TAG);
        ItemMeta nm = number.getItemMeta();
        if (nm != null) {
            nm.displayName(Text.item(plugin.msg().get("gui.admin.btn-set-racer-number")));
            nm.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            number.setItemMeta(nm);
        }
        inv.setItem(base + 4, number);

        // Set boat type
        ItemStack boat = new ItemStack(Material.OAK_BOAT);
        ItemMeta bm = boat.getItemMeta();
        if (bm != null) {
            bm.displayName(Text.item(plugin.msg().get("gui.admin.btn-set-boat-type")));
            bm.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            boat.setItemMeta(bm);
        }
        inv.setItem(base + 6, boat);

        // Remove from team (if any)
        ItemStack remove = new ItemStack(Material.BARRIER);
        ItemMeta rim = remove.getItemMeta();
        if (rim != null) {
            rim.displayName(Text.item(plugin.msg().get("gui.admin.btn-remove-from-team")));
            rim.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            remove.setItemMeta(rim);
        }
        inv.setItem(base + 8, remove);

    fillEmptyWith(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        admin.openInventory(inv);
        admin.playSound(admin.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openAssignTeam(Player p, OfflinePlayer target) {
        TeamManager tm = plugin.getTeamManager();
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_ASSIGN_TEAM);
        int slot = 0;
        for (Team t : tm.getTeamsSnapshot()) {
            if (slot >= size - 9) break;
            ItemStack item = new ItemStack(bannerForColor(t.getColor()));
            ItemMeta im = item.getItemMeta();
            if (im != null) {
                im.displayName(Text.item("&f" + t.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.msg().get("gui.admin.lore-members", "current", t.getMembers().size(), "max", tm.getMaxMembers()));
                lore.add(plugin.msg().get("gui.admin.lore-color", "color", t.getColor().name()));
                lore.add(" ");
                lore.add(plugin.msg().get("gui.admin.lore-click-assign"));
                im.lore(Text.lore(lore));
                im.getPersistentDataContainer().set(KEY_TEAM_ID, org.bukkit.persistence.PersistentDataType.STRING, t.getId().toString());
                im.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
                item.setItemMeta(im);
            }
            inv.setItem(slot++, item);
        }
        // None option
        ItemStack none = new ItemStack(Material.BARRIER);
        ItemMeta nm = none.getItemMeta();
        if (nm != null) {
            nm.displayName(Text.item(plugin.msg().get("gui.admin.btn-no-team")));
            nm.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
            none.setItemMeta(nm);
        }
        inv.setItem(size - 10, none);
    fillRow(inv, size - 9, pane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(size - 9, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openBoatPicker(Player p, OfflinePlayer target) {
        List<Material> boats = allowedBoats();
        int rows = ((boats.size() - 1) / 9) + 2;
        int size = Math.min(54, Math.max(18, rows * 9));
        Inventory inv = Bukkit.createInventory(null, size, TITLE_BOAT_PICKER);
        int slot = 0;
        for (Material m : boats) {
            if (slot >= size - 9) break;
            ItemStack it = new ItemStack(m);
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                im.displayName(Text.item("&f" + m.name()));
                im.getPersistentDataContainer().set(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
                it.setItemMeta(im);
            }
            inv.setItem(slot++, it);
        }
    fillRow(inv, size - 9, pane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(size - 9, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openTeam(Player p, Team t) {
        int size = 45;
        Inventory inv = Bukkit.createInventory(null, size, Text.title(TEAM_TITLE_PREFIX + t.getName()));

        // Header
        ItemStack header = new ItemStack(bannerForColor(t.getColor()));
        BannerMeta bm = (BannerMeta) header.getItemMeta();
        if (bm != null) {
            bm.displayName(Text.item("&l" + t.getName()));
            List<String> lore = new ArrayList<>();
                lore.add(plugin.msg().get("gui.admin.lore-members", "current", t.getMembers().size(), "max", plugin.getTeamManager().getMaxMembers()));
            bm.lore(Text.lore(lore));
            bm.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            header.setItemMeta(bm);
        }
        inv.setItem(4, header);

        // Members list
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (UUID m : t.getMembers()) {
            if (idx >= memberSlots.length) break;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            if (sm != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                sm.setOwningPlayer(op);
                sm.displayName(Text.item("&f" + (op.getName() != null ? op.getName() : m.toString())));
                List<String> lore = new ArrayList<>();
                String racerNumStr = t.getRacerNumber(m) == 0 ? plugin.msg().get("gui.common.unset") : String.valueOf(t.getRacerNumber(m));
                lore.add(plugin.msg().get("gui.admin.lore-racer-number", "num", racerNumStr));
                lore.add(plugin.msg().get("gui.admin.lore-boat", "boat", t.getBoatType(m)));
                lore.add(" ");
                lore.add(plugin.msg().get("gui.admin.lore-click-remove-member"));
                sm.lore(Text.lore(lore));
                sm.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
                sm.getPersistentDataContainer().set(KEY_TARGET_ID, PersistentDataType.STRING, m.toString());
                skull.setItemMeta(sm);
            }
            inv.setItem(memberSlots[idx++], skull);
        }

        int base = size - 9;
        inv.setItem(base, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));

        // Rename
        ItemStack rename = new ItemStack(Material.PAPER);
        ItemMeta rim = rename.getItemMeta();
        if (rim != null) {
            rim.displayName(Text.item(plugin.msg().get("gui.admin.btn-rename-team")));
            rim.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            rename.setItemMeta(rim);
        }
        inv.setItem(base + 2, rename);

        // Color (dye item matching current team color)
        ItemStack color = new ItemStack(dyeForColor(t.getColor()));
        ItemMeta cim = color.getItemMeta();
        if (cim != null) {
            cim.displayName(Text.item(plugin.msg().get("gui.admin.btn-change-color")));
            cim.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            color.setItemMeta(cim);
        }
        inv.setItem(base + 3, color);

        // Add member
        ItemStack add = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta aim = add.getItemMeta();
        if (aim != null) {
            aim.displayName(Text.item(plugin.msg().get("gui.admin.btn-add-member")));
            aim.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            add.setItemMeta(aim);
        }
        inv.setItem(base + 5, add);

        // Delete team
        ItemStack del = new ItemStack(Material.TNT);
        ItemMeta dim = del.getItemMeta();
        if (dim != null) {
            dim.displayName(Text.item(plugin.msg().get("gui.admin.btn-delete-team")));
            dim.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            del.setItemMeta(dim);
        }
        inv.setItem(base + 8, del);

        // Background tinted with team color
        fillEmptyWith(inv, pane(paneForColor(t.getColor())));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openColor(Player p, Team t) {
        DyeColor[] colors = DyeColor.values();
        int rows = ((colors.length - 1) / 9) + 2;
        int size = Math.min(54, Math.max(18, rows * 9));
        Inventory inv = Bukkit.createInventory(null, size, TITLE_COLOR);
        int slot = 0;
        for (DyeColor dc : colors) {
            if (slot >= size - 9) break;
            ItemStack banner = new ItemStack(bannerForColor(dc));
            ItemMeta im = banner.getItemMeta();
            if (im != null) {
                im.displayName(Text.item("&f" + dc.name()));
                im.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
                banner.setItemMeta(im);
            }
            inv.setItem(slot++, banner);
        }
    fillRow(inv, size - 9, pane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(size - 9, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openDeleteConfirm(Player p, Team t) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM_DELETE);
        ItemStack yes = new ItemStack(Material.RED_CONCRETE);
        ItemMeta yim = yes.getItemMeta();
        if (yim != null) {
            yim.displayName(Text.item(plugin.msg().get("gui.admin.btn-delete-now")));
            yim.getPersistentDataContainer().set(KEY_TEAM_ID, PersistentDataType.STRING, t.getId().toString());
            yes.setItemMeta(yim);
        }
        inv.setItem(13, yes);
        inv.setItem(18, button(Material.ARROW, Text.item(plugin.msg().get("gui.common.back"))));
        fillEmptyWith(inv, pane(Material.RED_STAINED_GLASS_PANE));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory() == null) return;
        Component title = e.getView().title();
        String plain = Text.plain(title);
        boolean inMain = plain.equals(Text.plain(TITLE));
        boolean inTeams = plain.equals(Text.plain(TITLE_TEAMS));
        boolean inTeam = plain.startsWith(TEAM_TITLE_PREFIX) || plain.equals(Text.plain(TITLE_TEAM));
        boolean inColor = plain.equals(Text.plain(TITLE_COLOR));
        boolean inDelete = plain.equals(Text.plain(TITLE_CONFIRM_DELETE));
        boolean inPlayers = plain.equals(Text.plain(TITLE_PLAYERS));
        boolean inPlayer = plain.equals(Text.plain(TITLE_PLAYER));
        boolean inAssignTeam = plain.equals(Text.plain(TITLE_ASSIGN_TEAM));
        boolean inBoatPicker = plain.equals(Text.plain(TITLE_BOAT_PICKER));
        if (!inMain && !inTeams && !inTeam && !inColor && !inDelete && !inPlayers && !inPlayer && !inAssignTeam && !inBoatPicker) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        HumanEntity he = e.getWhoClicked(); if (!(he instanceof Player)) return; Player p = (Player) he;
        if (!p.hasPermission("boatracing.admin")) { p.closeInventory(); return; }

        int slot = e.getSlot();
        if (inMain) {
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            if (slot == 10) { openTeams(p); return; }
            if (slot == 12) { plugin.getAdminRaceGUI().open(p); return; }
            if (slot == 14) { plugin.getTracksGUI().open(p); return; }
            if (slot == 16) { openPlayers(p); return; }
            return;
        }
    if (inPlayers) {
            if (slot == e.getInventory().getSize() - 9) { openMain(p); return; }
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String pid = im.getPersistentDataContainer().get(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (pid == null) return;
            try {
                UUID uid = UUID.fromString(pid);
                OfflinePlayer target = Bukkit.getOfflinePlayer(uid);
                openPlayerView(p, target);
            } catch (Exception ignored) {}
            return;
        }
        if (inPlayer) {
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            int base = e.getInventory().getSize() - 9;
            if (slot == base) { openPlayers(p); return; }
            String pid = im.getPersistentDataContainer().get(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (pid == null) return;
            OfflinePlayer target;
            try { target = Bukkit.getOfflinePlayer(UUID.fromString(pid)); } catch (Exception ex) { return; }
            Material type = it.getType();
            if (type == Material.WHITE_BANNER) { openAssignTeam(p, target); return; }
            if (type == Material.NAME_TAG) { openAnvilSetNumber(p, target); return; }
            if (type == Material.OAK_BOAT) { openBoatPicker(p, target); return; }
            if (type == Material.BARRIER) {
                plugin.getTeamManager().getTeamByMember(target.getUniqueId()).ifPresent(team -> {
                    String tn = team.getName();
                    team.removeMember(target.getUniqueId());
                    plugin.getTeamManager().save();
                    p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-removed")));
                    // Notify target if online
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-removed-notify", "team", tn)));
                        target.getPlayer().playSound(target.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    }
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                });
                openPlayerView(p, target); return;
            }
            return;
        }
        if (inAssignTeam) {
            if (slot == e.getInventory().getSize() - 9) { p.closeInventory(); return; }
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String pid = im.getPersistentDataContainer().get(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (pid == null) return;
            OfflinePlayer target; try { target = Bukkit.getOfflinePlayer(UUID.fromString(pid)); } catch (Exception ex) { return; }
            if (it.getType() == Material.BARRIER) {
                plugin.getTeamManager().getTeamByMember(target.getUniqueId()).ifPresent(prev -> {
                    String tn = prev.getName();
                    prev.removeMember(target.getUniqueId());
                    // Notify target if online
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-removed-notify", "team", tn)));
                        target.getPlayer().playSound(target.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                    }
                });
                plugin.getTeamManager().save();
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-unset-team")));
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                openPlayerView(p, target); return;
            }
            String tid = im.getPersistentDataContainer().get(KEY_TEAM_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (tid == null) return;
            Team t = getTeam(UUID.fromString(tid)); if (t == null) return;
            plugin.getTeamManager().getTeamByMember(target.getUniqueId()).ifPresent(prev -> prev.removeMember(target.getUniqueId()));
            if (!plugin.getTeamManager().addMember(t, target.getUniqueId())) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-full", "max", String.valueOf(plugin.getTeamManager().getMaxMembers()))));
                return;
            }
            plugin.getTeamManager().save();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-assigned", "team", t.getName())));
            // Notify target if online
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-assigned-notify", "team", t.getName())));
                target.getPlayer().playSound(target.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
            openPlayerView(p, target); return;
        }
        if (inBoatPicker) {
            if (slot == e.getInventory().getSize() - 9) { p.closeInventory(); return; }
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String pid = im.getPersistentDataContainer().get(KEY_TARGET_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (pid == null) return;
            OfflinePlayer target; try { target = Bukkit.getOfflinePlayer(UUID.fromString(pid)); } catch (Exception ex) { return; }
            Material m = it.getType();
            if (!isBoat(m)) return;
            plugin.getTeamManager().getTeamByMember(target.getUniqueId()).ifPresentOrElse(team -> {
                team.setBoatType(target.getUniqueId(), m.name());
                plugin.getTeamManager().save();
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.boat-changed", "boat", m.name())));
                // Notify target if online
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.boat-changed-notify", "boat", m.name())));
                    target.getPlayer().playSound(target.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                }
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                openPlayerView(p, target);
            }, () -> p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-in-team"))));
            return;
        }
        if (inTeams) {
            int base = e.getInventory().getSize() - 9;
            // Back
            if (slot == base) { openMain(p); return; }
            if (slot == base + 7) { openTeams(p); return; }
            if (slot == base + 8) { plugin.getTeamGUI().openMain(p); return; }
            // Create team button
            if (slot == base + 4) { openAnvilCreateTeam(p); return; }
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String tid = im.getPersistentDataContainer().get(KEY_TEAM_ID, PersistentDataType.STRING);
            if (tid == null) return;
            UUID id; try { id = UUID.fromString(tid); } catch (Exception ex) { return; }
            Team t = plugin.getTeamManager().getTeams().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
            if (t == null) return; openTeam(p, t); return;
        }
        if (inTeam) {
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String tid = im.getPersistentDataContainer().get(KEY_TEAM_ID, PersistentDataType.STRING);
            Team t = null; if (tid != null) { try { t = getTeam(UUID.fromString(tid)); } catch (Exception ignored) {} }
            if (t == null) {
                // Fallback from title
                String plainTitle = Text.plain(e.getView().title());
                if (plainTitle.startsWith(TEAM_TITLE_PREFIX)) {
                    String name = plainTitle.substring(TEAM_TITLE_PREFIX.length());
                    t = plugin.getTeamManager().getTeams().stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                }
            }
            if (t == null) return;
            int base = e.getInventory().getSize() - 9;
            if (slot == base) { openTeams(p); return; }
            Material type = it.getType();
            if (type == Material.PAPER) {
                openAnvilRename(p, t);
                return;
            }
            if (isDye(type)) { openColor(p, t); return; }
            if (type == Material.PLAYER_HEAD && im.getPersistentDataContainer().has(KEY_TARGET_ID, PersistentDataType.STRING)) {
                String mid = im.getPersistentDataContainer().get(KEY_TARGET_ID, PersistentDataType.STRING);
                try {
                    UUID target = UUID.fromString(mid);
                    if (t.removeMember(target)) {
                        plugin.getTeamManager().save();
                        p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-removed")));
                        // Notify target if online
                        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                        if (op != null && op.isOnline() && op.getPlayer() != null) {
                            op.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-removed-notify", "team", t.getName())));
                            op.getPlayer().playSound(op.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                        }
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                        openTeam(p, t);
                    }
                } catch (Exception ignored) {}
                return;
            }
            if (type == Material.PLAYER_HEAD && !im.getPersistentDataContainer().has(KEY_TARGET_ID, PersistentDataType.STRING)) {
                // Add member via anvil
                openAnvilAddMember(p, t);
                return;
            }
            if (type == Material.TNT) { openDeleteConfirm(p, t); return; }
            return;
        }
        if (inColor) {
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            if (slot == e.getInventory().getSize() - 9 || it.getType() == Material.ARROW) {
                p.closeInventory(); return;
            }
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String tid = im.getPersistentDataContainer().get(KEY_TEAM_ID, PersistentDataType.STRING);
            if (tid == null) return;
            Team t = getTeam(UUID.fromString(tid)); if (t == null) return;
            DyeColor chosen = dyeFromBanner(it.getType()); if (chosen == null) return;
            t.setColor(chosen); plugin.getTeamManager().save();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-color-set", "color", chosen.name())));
            // Notify team members of color change
            for (UUID m : t.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                if (op.isOnline() && op.getPlayer() != null) {
                    op.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-color-changed-notify", "color", chosen.name())));
                    op.getPlayer().playSound(op.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                }
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
            openTeam(p, t);
            return;
        }
        if (inDelete) {
            ItemStack it = e.getCurrentItem(); if (it == null) return;
            ItemMeta im = it.getItemMeta(); if (im == null) return;
            String tid = im.getPersistentDataContainer().get(KEY_TEAM_ID, PersistentDataType.STRING);
            if (tid == null) { p.closeInventory(); return; }
            Team t = getTeam(UUID.fromString(tid)); if (t == null) { p.closeInventory(); return; }
            if (it.getType() == Material.RED_CONCRETE) {
                // Notify members before deletion
                java.util.List<UUID> members = new java.util.ArrayList<>(t.getMembers());
                plugin.getTeamManager().deleteTeam(t);
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-deleted")));
                for (UUID m : members) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                    if (op.isOnline() && op.getPlayer() != null) {
                        op.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-deleted-notify")));
                        op.getPlayer().playSound(op.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.6f, 0.9f);
                    }
                }
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
                openTeams(p); return;
            }
            if (it.getType() == Material.ARROW) { openTeam(p, t); return; }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory() == null) return;
        String plain = Text.plain(e.getView().title());
        boolean block = plain.equals(Text.plain(TITLE))
                || plain.equals(Text.plain(TITLE_TEAMS))
                || plain.startsWith(TEAM_TITLE_PREFIX) || plain.equals(Text.plain(TITLE_TEAM))
                || plain.equals(Text.plain(TITLE_COLOR))
                || plain.equals(Text.plain(TITLE_CONFIRM_DELETE))
                || plain.equals(Text.plain(TITLE_PLAYERS))
                || plain.equals(Text.plain(TITLE_PLAYER))
                || plain.equals(Text.plain(TITLE_ASSIGN_TEAM))
                || plain.equals(Text.plain(TITLE_BOAT_PICKER));
        if (block) e.setCancelled(true);
    }

    // --- Anvil helpers ---
    private void openAnvilRename(Player p, Team t) { openAnvil(p, "rename", t.getId(), "", plugin.msg().get("gui.admin.anvil-team-name")); }
    private void openAnvilAddMember(Player p, Team t) { openAnvil(p, "add", t.getId(), "", plugin.msg().get("gui.admin.anvil-player-name")); }
    private void openAnvilSetNumber(Player p, OfflinePlayer target) {
        openAnvil(p, "setnum:" + target.getUniqueId(), null, "", plugin.msg().get("gui.admin.anvil-racer-number"));
    }
    private void openAnvilCreateTeam(Player p) { openAnvil(p, "create", null, "", plugin.msg().get("gui.admin.anvil-team-name")); }


    private void openAnvil(Player p, String action, UUID teamId, String initialText, String title) {
        ItemStack left = new ItemStack(Material.PAPER);
        ItemMeta lm = left.getItemMeta(); if (lm != null) { lm.displayName(Component.empty()); left.setItemMeta(lm); }
        new AnvilGUI.Builder()
                .title(title)
                .text((initialText == null || initialText.isEmpty()) ? BLANK : initialText)
                .itemLeft(left)
                .interactableSlots()
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String input = state.getText() == null ? "" : state.getText().trim();
                    return handleAnvil(state.getPlayer(), action, teamId, input);
                })
                .plugin(plugin)
                .open(p);
    }

    private List<AnvilGUI.ResponseAction> handleAnvil(Player p, String action, UUID teamId, String input) {
        // Special case: setting racer number uses action prefix and no teamId
        if (action != null && action.startsWith("setnum:")) {
            String uidStr = action.substring("setnum:".length());
            OfflinePlayer off;
            try { off = Bukkit.getOfflinePlayer(UUID.fromString(uidStr)); } catch (Exception ex) { return Collections.singletonList(AnvilGUI.ResponseAction.close()); }
            int num;
            try { num = Integer.parseInt(input); } catch (Exception ex) { p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.invalid-number"))); return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input.isEmpty()?BLANK:input)); }
            if (num < 1 || num > 99) { p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.number-out-of-range"))); return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input.isEmpty()?BLANK:input)); }
            applyRacerNumber(p, off, num);
            return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openPlayerView(p, off)));
        }
        // Create new team (no initial member)
        if ("create".equals(action)) {
            String err = TeamGUI.validateNameMessage(input);
            if (err != null) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(err)));
                String retry = input.isEmpty() ? BLANK : input;
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(retry));
            }
            if (plugin.getTeamManager().findByName(input).isPresent()) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-name-exists")));
                String retry = input.isEmpty() ? BLANK : input;
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(retry));
            }
            Team created = plugin.getTeamManager().createTeam((UUID) null, sanitizeName(input), org.bukkit.DyeColor.WHITE);
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-created", "name", created.getName())));
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
            return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openTeam(p, created)));
        }
        Team t = getTeam(teamId); if (t == null) return Collections.emptyList();
        if ("rename".equals(action)) {
            String err = TeamGUI.validateNameMessage(input);
            if (err != null) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(err)));
                String retry = input.isEmpty() ? BLANK : input;
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(retry));
            }
            if (plugin.getTeamManager().getTeams().stream().anyMatch(x -> x != t && x.getName().equalsIgnoreCase(input))) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-name-exists")));
                String retry = input.isEmpty() ? BLANK : input;
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(retry));
            }
            t.setName(sanitizeName(input)); plugin.getTeamManager().save();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-renamed", "name", input)));
            // Notify team members
            for (UUID m : t.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(m);
                if (op.isOnline() && op.getPlayer() != null) {
                    op.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-renamed-notify", "name", input)));
                    op.getPlayer().playSound(op.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                }
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
            return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openTeam(p, t)));
        }
        if ("add".equals(action)) {
            if (input.isEmpty()) return Collections.singletonList(AnvilGUI.ResponseAction.close());
            OfflinePlayer off = Bukkit.getOfflinePlayer(input);
            if (off == null || off.getUniqueId() == null) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-not-found")));
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input));
            }
            if (plugin.getTeamManager().getTeamByMember(off.getUniqueId()).isPresent()) {
                plugin.getTeamManager().getTeamByMember(off.getUniqueId()).ifPresent(prev -> prev.removeMember(off.getUniqueId()));
            }
            if (!plugin.getTeamManager().addMember(t, off.getUniqueId())) {
                p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.team-full", "max", plugin.getTeamManager().getMaxMembers())));
                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(input));
            }
            plugin.getTeamManager().save();
            p.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.member-added", "player", (off.getName() != null ? off.getName() : input))));
            // Notify target if online
            if (off.isOnline() && off.getPlayer() != null) {
                off.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.member-added-notify", "team", t.getName())));
                off.getPlayer().playSound(off.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
            return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> openTeam(p, t)));
        }
        return Collections.emptyList();
    }

    private void applyRacerNumber(Player admin, OfflinePlayer target, int num) {
        Optional<Team> teamOpt = plugin.getTeamManager().getTeamByMember(target.getUniqueId());
        if (teamOpt.isEmpty()) {
            admin.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.player-no-team")));
            return;
        }

        teamOpt.get().setRacerNumber(target.getUniqueId(), num);
        plugin.getTeamManager().save();
        admin.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.racer-number-set",
                "player", (target.getName() != null ? target.getName() : target.getUniqueId().toString()),
                "num", num)));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("admin.racer-number-changed-notify", "num", num)));
            target.getPlayer().playSound(target.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
        admin.playSound(admin.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.9f, 1.1f);
    }

    // --- Small utils (duplicated to keep AdminGUI self-contained) ---
    private Team getTeam(UUID id) { if (id == null) return null; return plugin.getTeamManager().getTeams().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null); }
    private static ItemStack button(Material mat, Component name) { ItemStack it = new ItemStack(mat); ItemMeta im = it.getItemMeta(); if (im != null) { im.displayName(name); im.addItemFlags(ItemFlag.values()); it.setItemMeta(im);} return it; }
    private static ItemStack pane(Material mat) { ItemStack it = new ItemStack(mat); ItemMeta im = it.getItemMeta(); if (im != null) { im.displayName(Component.text(" ")); im.addItemFlags(ItemFlag.values()); it.setItemMeta(im);} return it; }
    private static void fillEmptyWith(Inventory inv, ItemStack filler) { for (int i=0;i<inv.getSize();i++) if (inv.getItem(i) == null || inv.getItem(i).getType()==Material.AIR) inv.setItem(i, filler); }
    private static void fillRow(Inventory inv, int start, ItemStack filler) { for (int i=0;i<9;i++) if (inv.getItem(start+i)==null || inv.getItem(start+i).getType()==Material.AIR) inv.setItem(start+i, filler); }
    private static Material bannerForColor(DyeColor color) { try { return Material.valueOf(color.name() + "_BANNER"); } catch (Exception ex) { return Material.WHITE_BANNER; } }
    private static Material paneForColor(DyeColor color) { try { return Material.valueOf(color.name() + "_STAINED_GLASS_PANE"); } catch (Exception ex) { return Material.GRAY_STAINED_GLASS_PANE; } }
    private static Material dyeForColor(DyeColor color) { try { return Material.valueOf(color.name() + "_DYE"); } catch (Exception ex) { return Material.LIME_DYE; } }
    private static DyeColor dyeFromBanner(Material m) { if (m == null || !m.name().endsWith("_BANNER")) return null; String prefix = m.name().substring(0, m.name().length()-"_BANNER".length()); try { return DyeColor.valueOf(prefix); } catch (Exception ex) { return null; } }
    private static String sanitizeName(String raw) { String s = raw.replace("§", "").replace("&", "").trim(); if (s.length()>20) s = s.substring(0,20); return s; }

    private static boolean isBoat(Material m) { return es.jaie55.boatracing.util.BoatTypes.isBoat(m); }
    private static boolean isDye(Material m) { return m != null && m.name().endsWith("_DYE"); }
    private static List<Material> allowedBoats() { return es.jaie55.boatracing.util.BoatTypes.all(); }
}
