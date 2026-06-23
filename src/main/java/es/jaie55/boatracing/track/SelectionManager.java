package es.jaie55.boatracing.track;

import es.jaie55.boatracing.BoatRacingPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.*;

import es.jaie55.boatracing.util.Text;
import net.kyori.adventure.text.Component;
/** Selection manager independent of other plugins. */
public class SelectionManager {
    public static class Sel {
        public String world;
        public int x1, y1, z1;
        public int x2, y2, z2;
    }

    private static final Map<java.util.UUID, Sel> selections = new HashMap<>();
    private static NamespacedKey WAND_KEY;

    public static void init(org.bukkit.plugin.Plugin plugin) {
        WAND_KEY = new NamespacedKey(plugin, "wand");
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(WAND_KEY, PersistentDataType.INTEGER);
    }

    public static ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            BoatRacingPlugin plugin = BoatRacingPlugin.getInstance();
            String wandName = resolveWandMessage(plugin, "setup.wand-name", "setup.usage.wand-name", "&6BoatRacing Selection Tool");
            String loreLeft = resolveWandMessage(plugin, "setup.wand-lore-left", "setup.usage.wand-lore-left", "&eLeft-click: &fmark Corner A");
            String loreRight = resolveWandMessage(plugin, "setup.wand-lore-right", "setup.usage.wand-lore-right", "&eRight-click: &fmark Corner B");
            String loreLights = resolveWandMessage(plugin, "setup.wand-lore-lights", "setup.usage.wand-lore-lights", "&eShift+Left: &fadd light &8| &eShift+Right: &fremove light");
            Component name = Text.item(wandName);
        java.util.List<String> loreLines = java.util.Arrays.asList(
            loreLeft,
            loreRight,
            loreLights
        );
            meta.displayName(name);
            meta.lore(Text.lore(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        return item;
    }
    public static void giveWand(Player p) {
        ItemStack wand = createWand();
        java.util.Map<Integer, ItemStack> left = p.getInventory().addItem(wand);
        if (!left.isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), wand);
    }

    private static String resolveWandMessage(BoatRacingPlugin plugin, String primaryKey, String fallbackKey, String hardDefault) {
        if (plugin == null) return hardDefault;

        String value = plugin.msg().get(primaryKey);
        if (!primaryKey.equals(value)) return value;

        String fallback = plugin.msg().get(fallbackKey);
        if (!fallbackKey.equals(fallback)) return fallback;

        return hardDefault;
    }

    public static void clear(Player p) { selections.remove(p.getUniqueId()); }

    public static void setPos1(Player p, org.bukkit.Location loc) {
        Sel s = selections.computeIfAbsent(p.getUniqueId(), k -> new Sel());
        s.world = loc.getWorld().getName();
        s.x1 = loc.getBlockX(); s.y1 = loc.getBlockY(); s.z1 = loc.getBlockZ();
    }

    public static void setPos2(Player p, org.bukkit.Location loc) {
        Sel s = selections.computeIfAbsent(p.getUniqueId(), k -> new Sel());
        s.world = loc.getWorld().getName();
        s.x2 = loc.getBlockX(); s.y2 = loc.getBlockY(); s.z2 = loc.getBlockZ();
    }

    public static Sel get(Player p) { return selections.get(p.getUniqueId()); }

    public static BoundingBox toBox(Sel s) {
        if (s == null) return null;
        if (s.world == null) return null;
        boolean hasPos1 = !(s.x1 == 0 && s.y1 == 0 && s.z1 == 0);
        boolean hasPos2 = !(s.x2 == 0 && s.y2 == 0 && s.z2 == 0);
        if (!hasPos1 || !hasPos2) return null;
        int minX = Math.min(s.x1, s.x2);
        int minY = Math.min(s.y1, s.y2);
        int minZ = Math.min(s.z1, s.z2);
        int maxX = Math.max(s.x1, s.x2) + 1;
        int maxY = Math.max(s.y1, s.y2) + 1;
        int maxZ = Math.max(s.z1, s.z2) + 1;
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
