package es.jaie55.boatracing.ui;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.cosmetics.CheckpointEffectType;
import es.jaie55.boatracing.cosmetics.CosmeticCategory;
import es.jaie55.boatracing.cosmetics.CosmeticsCatalog;
import es.jaie55.boatracing.cosmetics.TitleManager;
import es.jaie55.boatracing.cosmetics.VictoryEffectType;
import es.jaie55.boatracing.cosmetics.VictorySoundType;
import es.jaie55.boatracing.integrations.VaultEconomy;
import es.jaie55.boatracing.util.Text;
import es.jaie55.boatracing.util.TimeFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Player cosmetics menu: trails, win-based titles, victory effects, victory sounds, checkpoint
 * effects, a settings tab (particle density) and the cosmetic shop (Vault + purchases).
 */
public class CosmeticsGUI implements Listener {

    private static final int SIZE = 54;

    private enum LockState { UNLOCKED, BUYABLE, LOCKED }

    private final BoatRacingPlugin plugin;
    private final Component TITLE;
    private final NamespacedKey KEY_ACTION;
    private final Map<UUID, String> pageByPlayer = new HashMap<>();

    public CosmeticsGUI(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.KEY_ACTION = new NamespacedKey(plugin, "cosmetics-action");
        this.TITLE = Text.title(plugin.msg().get("gui.cosmetics.title"));
    }

    public void open(Player player) {
        open(player, pageByPlayer.getOrDefault(player.getUniqueId(), "trails"));
    }

    public void open(Player player, String page) {
        if (player == null) return;
        if (!player.hasPermission("boatracing.cosmetics")
                && !player.hasPermission(CosmeticsCatalog.UNLOCK_ALL_PERMISSION)) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.no-permission")));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        String normalized = normalizePage(page);
        pageByPlayer.put(player.getUniqueId(), normalized);

        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fill(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        switch (normalized) {
            case "titles" -> populateTitles(player, inv);
            case "effects" -> populateEffects(player, inv);
            case "sounds" -> populateSounds(player, inv);
            case "checkpoints" -> populateCheckpoints(player, inv);
            case "settings" -> populateSettings(player, inv);
            default -> populateTrails(player, inv);
        }

        inv.setItem(45, action(trailsIcon(player), plugin.msg().get("gui.cosmetics.btn-trails"), "cos:page:trails"));
        inv.setItem(46, action(Material.NAME_TAG, plugin.msg().get("gui.cosmetics.btn-titles"), "cos:page:titles"));
        inv.setItem(47, action(effectsIcon(player), plugin.msg().get("gui.cosmetics.btn-effects"), "cos:page:effects"));
        inv.setItem(48, action(soundsIcon(player), plugin.msg().get("gui.cosmetics.btn-sounds"), "cos:page:sounds"));
        inv.setItem(49, action(checkpointsIcon(player), plugin.msg().get("gui.cosmetics.btn-checkpoints"), "cos:page:checkpoints"));
        inv.setItem(50, action(Material.COMPARATOR, plugin.msg().get("gui.cosmetics.btn-settings"), "cos:page:settings"));
        inv.setItem(51, action(Material.BARRIER, plugin.msg().get("gui.cosmetics.btn-close"), "cos:close"));
        inv.setItem(53, action(Material.LIME_DYE, plugin.msg().get("gui.cosmetics.btn-clear"), "cos:clear"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private String normalizePage(String page) {
        String requested = page == null ? "" : page.toLowerCase(Locale.ROOT);
        return switch (requested) {
            case "titles" -> "titles";
            case "effects" -> "effects";
            case "sounds" -> "sounds";
            case "checkpoints" -> "checkpoints";
            case "settings" -> "settings";
            default -> "trails";
        };
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    private void populateTrails(Player player, Inventory inv) {
        if (!plugin.getConfig().getBoolean("cosmetics.trails.enabled", true)) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.cosmetics.trail-disabled")), List.of()));
            return;
        }
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getTrail(player.getUniqueId())
                : null;
        CosmeticsCatalog catalog = plugin.getCosmeticsCatalog();

        int slot = 0;
        if (catalog != null) {
            for (CosmeticsCatalog.TrailDefinition trail : catalog.trails()) {
                if (slot >= 45) break;
                boolean isEquipped = trail.id().equals(equipped);
                LockState state = lockState(player, CosmeticCategory.TRAIL, trail.id(), trail.permission());

                List<String> lore = new ArrayList<>();
                lore.add(plugin.msg().get("gui.cosmetics.lore-trail", "trail", plugin.msg().get(trail.messageKey())));
                appendOwnershipLore(player, lore, CosmeticCategory.TRAIL, trail.id(), trail.permission(), state, isEquipped);
                inv.setItem(slot++, item(trail.icon(), plugin.msg().get(trail.messageKey()),
                        lore, "cos:trail:" + trail.id(), "cos:buy:" + CosmeticCategory.TRAIL.id() + ":" + trail.id(),
                        state == LockState.BUYABLE));
            }
        }
    }

    private void populateTitles(Player player, Inventory inv) {
        TitleManager titles = plugin.getTitleManager();
        if (titles == null || !titles.isEnabled()) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.cosmetics.titles-disabled")), List.of()));
            return;
        }
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getTitle(player.getUniqueId())
                : null;

        int slot = 0;
        for (String titleId : titles.orderedTitleIds()) {
            if (slot >= 45) break;
            String permission = "boatracing.cosmetics.title." + titleId;
            boolean byWins = titles.isUnlocked(player.getUniqueId(), titleId);
            LockState state = byWins ? LockState.UNLOCKED
                    : lockState(player, CosmeticCategory.TITLE, titleId, permission);
            boolean isEquipped = titleId.equals(equipped);

            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().get("gui.cosmetics.lore-title", "title", titles.displayName(titleId)));
            if (!byWins && state == LockState.UNLOCKED && !isEquipped) {
                lore.add(plugin.msg().get("gui.cosmetics.lore-click"));
            }
            if (!byWins) {
                lore.add(plugin.msg().get("gui.cosmetics.lore-title-locked", "wins", String.valueOf(titles.threshold(titleId))));
            }
            appendOwnershipLore(player, lore, CosmeticCategory.TITLE, titleId, permission, state, isEquipped);
            inv.setItem(slot++, item(state == LockState.LOCKED && !byWins ? Material.GRAY_DYE : Material.NAME_TAG,
                    titles.displayName(titleId), lore,
                    "cos:title:" + titleId, "cos:buy:" + CosmeticCategory.TITLE.id() + ":" + titleId,
                    state == LockState.BUYABLE));
        }
    }

    private void populateEffects(Player player, Inventory inv) {
        if (!plugin.getConfig().getBoolean("cosmetics.effects.enabled", true)) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.cosmetics.effects-disabled")), List.of()));
            return;
        }
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictoryEffect(player.getUniqueId())
                : null;

        int slot = 0;
        for (VictoryEffectType effect : VictoryEffectType.values()) {
            if (slot >= 45) break;
            boolean isEquipped = effect.id().equals(equipped);
            LockState state = lockState(player, CosmeticCategory.EFFECT, effect.id(), effect.permission());

            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().get("gui.cosmetics.lore-effect", "effect", plugin.msg().get(effect.messageKey())));
            appendOwnershipLore(player, lore, CosmeticCategory.EFFECT, effect.id(), effect.permission(), state, isEquipped);
            inv.setItem(slot++, item(effect.icon(), plugin.msg().get(effect.messageKey()), lore,
                    "cos:effect:" + effect.id(), "cos:buy:" + CosmeticCategory.EFFECT.id() + ":" + effect.id(),
                    state == LockState.BUYABLE));
        }
    }

    private void populateSounds(Player player, Inventory inv) {
        if (!plugin.getConfig().getBoolean("cosmetics.victory-sounds.enabled", true)) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.cosmetics.sounds-disabled")), List.of()));
            return;
        }
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictorySound(player.getUniqueId())
                : null;

        int slot = 0;
        for (VictorySoundType sound : VictorySoundType.values()) {
            if (slot >= 45) break;
            boolean isEquipped = sound.id().equals(equipped);
            LockState state = lockState(player, CosmeticCategory.SOUND, sound.id(), sound.permission());

            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().get("gui.cosmetics.lore-sound", "sound", plugin.msg().get(sound.messageKey())));
            appendOwnershipLore(player, lore, CosmeticCategory.SOUND, sound.id(), sound.permission(), state, isEquipped);
            inv.setItem(slot++, item(sound.icon(), plugin.msg().get(sound.messageKey()), lore,
                    "cos:sound:" + sound.id(), "cos:buy:" + CosmeticCategory.SOUND.id() + ":" + sound.id(),
                    state == LockState.BUYABLE));
        }
    }

    private void populateCheckpoints(Player player, Inventory inv) {
        if (!plugin.getConfig().getBoolean("cosmetics.checkpoints.enabled", true)) {
            inv.setItem(22, card(Material.BARRIER, Text.item(plugin.msg().get("gui.cosmetics.checkpoints-disabled")), List.of()));
            return;
        }
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getCheckpointEffect(player.getUniqueId())
                : null;

        int slot = 0;
        for (CheckpointEffectType effect : CheckpointEffectType.values()) {
            if (slot >= 45) break;
            if (plugin.getCosmeticsCatalog() != null
                    && !plugin.getCosmeticsCatalog().isAvailable(CosmeticCategory.CHECKPOINT, effect.id())) continue;
            boolean isEquipped = effect.id().equals(equipped);
            LockState state = lockState(player, CosmeticCategory.CHECKPOINT, effect.id(), effect.permission());

            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().get("gui.cosmetics.lore-checkpoint", "effect", plugin.msg().get(effect.messageKey())));
            appendOwnershipLore(player, lore, CosmeticCategory.CHECKPOINT, effect.id(), effect.permission(), state, isEquipped);
            inv.setItem(slot++, item(effect.icon(), plugin.msg().get(effect.messageKey()), lore,
                    "cos:checkpoint:" + effect.id(), "cos:buy:" + CosmeticCategory.CHECKPOINT.id() + ":" + effect.id(),
                    state == LockState.BUYABLE));
        }
    }

    private void populateSettings(Player player, Inventory inv) {
        boolean enabled = plugin.getConfig().getBoolean("cosmetics.density.enabled", true);
        String current = currentDensityId(player);
        inv.setItem(20, densityItem(player, "low", Material.FEATHER, enabled, current));
        inv.setItem(22, densityItem(player, "normal", Material.GUNPOWDER, enabled, current));
        inv.setItem(24, densityItem(player, "high", Material.FIREWORK_ROCKET, enabled, current));
    }

    private ItemStack densityItem(Player player, String level, Material icon, boolean enabled, String current) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.msg().get("gui.cosmetics.density-label", "level", plugin.msg().get("gui.cosmetics.density-" + level)));
        if (!enabled) {
            lore.add(plugin.msg().get("gui.cosmetics.density-disabled"));
        } else if (level.equalsIgnoreCase(current)) {
            lore.add(plugin.msg().get("gui.cosmetics.density-current"));
        } else {
            lore.add(plugin.msg().get("gui.cosmetics.density-click"));
        }
        return item(icon, plugin.msg().get("gui.cosmetics.density-" + level), lore, "cos:density:" + level, null, false);
    }

    // ------------------------------------------------------------------
    // Clicks
    // ------------------------------------------------------------------

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

        if (!player.hasPermission("boatracing.cosmetics")
                && !player.hasPermission(CosmeticsCatalog.UNLOCK_ALL_PERMISSION)) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.no-permission")));
            return;
        }

        if (action.startsWith("cos:page:")) {
            open(player, action.substring("cos:page:".length()));
            return;
        }
        if ("cos:close".equals(action)) {
            player.closeInventory();
            return;
        }
        if ("cos:clear".equals(action)) {
            clearPage(player);
            return;
        }
        if (action.startsWith("cos:density:")) {
            setDensity(player, action.substring("cos:density:".length()));
            return;
        }
        if (action.startsWith("cos:buy:")) {
            handleBuy(player, action.substring("cos:buy:".length()));
            return;
        }
        if (action.startsWith("cos:trail:")) {
            String id = action.substring("cos:trail:".length());
            CosmeticsCatalog catalog = plugin.getCosmeticsCatalog();
            CosmeticsCatalog.TrailDefinition trail = catalog != null ? catalog.trailById(id) : null;
            if (trail == null) return;
            if (!catalog.hasAccess(player, CosmeticCategory.TRAIL, trail.id(), trail.permission())) {
                deny(player);
                return;
            }
            String current = plugin.getPlayerPrefsManager().getTrail(player.getUniqueId());
            boolean removing = trail.id().equals(current);
            plugin.getPlayerPrefsManager().setTrail(player.getUniqueId(), removing ? null : trail.id());
            String key = removing ? "gui.cosmetics.trail-cleared" : "gui.cosmetics.trail-equipped";
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(key, "trail", plugin.msg().get(trail.messageKey()))));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            open(player, "trails");
            return;
        }
        if (action.startsWith("cos:title:")) {
            String id = action.substring("cos:title:".length());
            TitleManager titles = plugin.getTitleManager();
            if (titles == null || !titles.isEnabled()) return;
            boolean byWins = titles.isUnlocked(player.getUniqueId(), id);
            String permission = "boatracing.cosmetics.title." + id;
            if (!byWins && !plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.TITLE, id, permission)) {
                deny(player);
                return;
            }
            plugin.getPlayerPrefsManager().setTitle(player.getUniqueId(), id);
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.title-equipped",
                    "title", titles.displayName(id))));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            open(player, "titles");
            return;
        }
        if (action.startsWith("cos:effect:")) {
            equipVictoryEffect(player, action.substring("cos:effect:".length()));
            return;
        }
        if (action.startsWith("cos:sound:")) {
            equipVictorySound(player, action.substring("cos:sound:".length()));
            return;
        }
        if (action.startsWith("cos:checkpoint:")) {
            equipCheckpointEffect(player, action.substring("cos:checkpoint:".length()));
        }
    }

    private void equipVictoryEffect(Player player, String id) {
        VictoryEffectType effect = VictoryEffectType.byId(id);
        if (effect == null) return;
        if (!plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.EFFECT, effect.id(), effect.permission())) {
            deny(player);
            return;
        }
        String current = plugin.getPlayerPrefsManager().getVictoryEffect(player.getUniqueId());
        boolean removing = effect.id().equals(current);
        plugin.getPlayerPrefsManager().setVictoryEffect(player.getUniqueId(), removing ? null : effect.id());
        String key = removing ? "gui.cosmetics.effect-cleared" : "gui.cosmetics.effect-equipped";
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(key, "effect", plugin.msg().get(effect.messageKey()))));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        open(player, "effects");
    }

    private void equipVictorySound(Player player, String id) {
        VictorySoundType sound = VictorySoundType.byId(id);
        if (sound == null) return;
        if (!plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.SOUND, sound.id(), sound.permission())) {
            deny(player);
            return;
        }
        String current = plugin.getPlayerPrefsManager().getVictorySound(player.getUniqueId());
        boolean removing = sound.id().equals(current);
        plugin.getPlayerPrefsManager().setVictorySound(player.getUniqueId(), removing ? null : sound.id());
        String key = removing ? "gui.cosmetics.sound-cleared" : "gui.cosmetics.sound-equipped";
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(key, "sound", plugin.msg().get(sound.messageKey()))));
        if (sound.soundKey() != null) {
            player.playSound(player.getLocation(), sound.soundKey(), 0.9f, sound.pitch());
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
        open(player, "sounds");
    }

    private void equipCheckpointEffect(Player player, String id) {
        CheckpointEffectType effect = CheckpointEffectType.byId(id);
        if (effect == null || !effect.available()) return;
        if (!plugin.getCosmeticsCatalog().hasAccess(player, CosmeticCategory.CHECKPOINT, effect.id(), effect.permission())) {
            deny(player);
            return;
        }
        String current = plugin.getPlayerPrefsManager().getCheckpointEffect(player.getUniqueId());
        boolean removing = effect.id().equals(current);
        plugin.getPlayerPrefsManager().setCheckpointEffect(player.getUniqueId(), removing ? null : effect.id());
        String key = removing ? "gui.cosmetics.checkpoint-cleared" : "gui.cosmetics.checkpoint-equipped";
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get(key, "effect", plugin.msg().get(effect.messageKey()))));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        open(player, "checkpoints");
    }

    private void handleBuy(Player player, String payload) {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) return;
        CosmeticCategory category = CosmeticCategory.byId(parts[0]);
        String id = parts[1];
        if (category == null) return;
        if (!plugin.getCosmeticsCatalog().shopEnabled()) return;

        int price = plugin.getCosmeticsCatalog().price(category, id);
        if (price < 0) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.buy-no-sale")));
            return;
        }
        VaultEconomy vault = plugin.getVaultEconomy();
        if (vault == null || !vault.isEnabled()) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.buy-no-vault")));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        if (!vault.withdraw(player, price)) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.buy-no-funds", "price", vault.format(price))));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        plugin.getPurchaseManager().grant(player.getUniqueId(), category, id, 0L);
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.buy-success",
                "cosmetic", cosmeticName(category, id), "price", vault.format(price))));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.3f);
        open(player, pageFor(category));
    }

    private static String pageFor(CosmeticCategory category) {
        return switch (category) {
            case TRAIL -> "trails";
            case TITLE -> "titles";
            case EFFECT -> "effects";
            case SOUND -> "sounds";
            case CHECKPOINT -> "checkpoints";
        };
    }

    private void setDensity(Player player, String level) {
        if (!plugin.getConfig().getBoolean("cosmetics.density.enabled", true)) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.density-disabled")));
            return;
        }
        String normalized = level.toLowerCase(Locale.ROOT);
        if (!normalized.equals("low") && !normalized.equals("normal") && !normalized.equals("high")) return;
        plugin.getPlayerPrefsManager().setParticleDensity(player.getUniqueId(),
                normalized.equals(currentDefaultDensity()) ? null : normalized);
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.density-set",
                "level", plugin.msg().get("gui.cosmetics.density-" + normalized))));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        open(player, "settings");
    }

    private void clearPage(Player player) {
        String page = pageByPlayer.getOrDefault(player.getUniqueId(), "trails");
        switch (page) {
            case "titles" -> {
                plugin.getPlayerPrefsManager().setTitle(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.title-cleared")));
            }
            case "effects" -> {
                plugin.getPlayerPrefsManager().setVictoryEffect(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.effect-cleared")));
            }
            case "sounds" -> {
                plugin.getPlayerPrefsManager().setVictorySound(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.sound-cleared")));
            }
            case "checkpoints" -> {
                plugin.getPlayerPrefsManager().setCheckpointEffect(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.checkpoint-cleared")));
            }
            case "settings" -> {
                plugin.getPlayerPrefsManager().setParticleDensity(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("cosmetics.density-reset")));
            }
            default -> {
                plugin.getPlayerPrefsManager().setTrail(player.getUniqueId(), null);
                player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.trail-cleared")));
            }
        }
        plugin.getLogger().fine("Player " + player.getName() + " cleared " + page + " cosmetic selection.");
        open(player, page);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null) return;
        if (Text.plain(e.getView().title()).equals(Text.plain(TITLE))) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    // Lore / state helpers
    // ------------------------------------------------------------------

    private LockState lockState(Player player, CosmeticCategory category, String id, String permission) {
        CosmeticsCatalog catalog = plugin.getCosmeticsCatalog();
        if (catalog == null) return LockState.LOCKED;
        if (catalog.hasAccess(player, category, id, permission)) return LockState.UNLOCKED;
        if (catalog.shopEnabled() && catalog.price(category, id) > 0) return LockState.BUYABLE;
        return LockState.LOCKED;
    }

    private void appendOwnershipLore(Player player, List<String> lore, CosmeticCategory category, String id,
                                     String permission, LockState state, boolean equipped) {
        if (state == LockState.UNLOCKED) {
            if (equipped) {
                lore.add(plugin.msg().get("gui.cosmetics.lore-equipped"));
            } else {
                lore.add(plugin.msg().get("gui.cosmetics.lore-click"));
            }
            long expiresAt = plugin.getPurchaseManager() != null
                    ? plugin.getPurchaseManager().expiresAt(player.getUniqueId(), category, id)
                    : -1L;
            if (expiresAt > 0L) {
                lore.add(plugin.msg().get("gui.cosmetics.lore-owned-temporary",
                        "time", TimeFormat.formatRemaining(expiresAt)));
            }
            return;
        }
        if (state == LockState.BUYABLE) {
            int price = plugin.getCosmeticsCatalog().price(category, id);
            VaultEconomy vault = plugin.getVaultEconomy();
            if (vault != null && vault.isEnabled()) {
                lore.add(plugin.msg().get("gui.cosmetics.lore-buy-price", "price", vault.format(price)));
                lore.add(plugin.msg().get("gui.cosmetics.lore-buy-click"));
            } else {
                lore.add(plugin.msg().get("gui.cosmetics.lore-buy-no-vault"));
                lore.add(plugin.msg().get("gui.cosmetics.lore-locked", "perm", permission));
            }
            return;
        }
        lore.add(plugin.msg().get("gui.cosmetics.lore-locked", "perm", permission));
    }

    private String cosmeticName(CosmeticCategory category, String id) {
        return switch (category) {
            case TRAIL -> {
                CosmeticsCatalog.TrailDefinition trail = plugin.getCosmeticsCatalog().trailById(id);
                yield trail != null ? plugin.msg().get(trail.messageKey()) : id;
            }
            case TITLE -> plugin.getTitleManager() != null ? plugin.getTitleManager().displayName(id) : id;
            case EFFECT -> {
                VictoryEffectType effect = VictoryEffectType.byId(id);
                yield effect != null ? plugin.msg().get(effect.messageKey()) : id;
            }
            case SOUND -> {
                VictorySoundType sound = VictorySoundType.byId(id);
                yield sound != null ? plugin.msg().get(sound.messageKey()) : id;
            }
            case CHECKPOINT -> {
                CheckpointEffectType effect = CheckpointEffectType.byId(id);
                yield effect != null ? plugin.msg().get(effect.messageKey()) : id;
            }
        };
    }

    private String currentDefaultDensity() {
        String value = plugin.getConfig().getString("cosmetics.density.default", "normal");
        return value == null ? "normal" : value.toLowerCase(Locale.ROOT);
    }

    private String currentDensityId(Player player) {
        String selected = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getParticleDensity(player.getUniqueId())
                : null;
        return selected != null ? selected : currentDefaultDensity();
    }

    private void deny(Player player) {
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("gui.cosmetics.locked")));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
    }

    // ------------------------------------------------------------------
    // Icons
    // ------------------------------------------------------------------

    private Material trailsIcon(Player player) {
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getTrail(player.getUniqueId())
                : null;
        if (equipped != null && plugin.getCosmeticsCatalog() != null) {
            CosmeticsCatalog.TrailDefinition trail = plugin.getCosmeticsCatalog().trailById(equipped);
            if (trail != null) return trail.icon();
        }
        return Material.GUNPOWDER;
    }

    private Material effectsIcon(Player player) {
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictoryEffect(player.getUniqueId())
                : null;
        VictoryEffectType effect = equipped != null ? VictoryEffectType.byId(equipped) : null;
        return effect != null ? effect.icon() : Material.FIREWORK_ROCKET;
    }

    private Material soundsIcon(Player player) {
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getVictorySound(player.getUniqueId())
                : null;
        VictorySoundType sound = equipped != null ? VictorySoundType.byId(equipped) : null;
        return sound != null ? sound.icon() : Material.BELL;
    }

    private Material checkpointsIcon(Player player) {
        String equipped = plugin.getPlayerPrefsManager() != null
                ? plugin.getPlayerPrefsManager().getCheckpointEffect(player.getUniqueId())
                : null;
        CheckpointEffectType effect = equipped != null ? CheckpointEffectType.byId(equipped) : null;
        return effect != null ? effect.icon() : Material.LIGHTNING_ROD;
    }

    // ------------------------------------------------------------------
    // Item helpers
    // ------------------------------------------------------------------

    private ItemStack item(Material material, String name, List<String> lore, String action,
                           String buyAction, boolean buyable) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            if (!lore.isEmpty()) meta.lore(Text.lore(lore));
            meta.addItemFlags(ItemFlag.values());
            String effectiveAction = buyable && buyAction != null ? buyAction : action;
            if (effectiveAction != null) {
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, effectiveAction);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

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
