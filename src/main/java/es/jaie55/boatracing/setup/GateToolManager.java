package es.jaie55.boatracing.setup;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.CheckpointGroup;
import es.jaie55.boatracing.track.CheckpointShape;
import es.jaie55.boatracing.track.ParticleWireframe;
import es.jaie55.boatracing.track.PlaneCheckpoint;
import es.jaie55.boatracing.track.Region;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.SchedulerCompat;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Manual gate tools: a blue dye places oriented checkpoint gates and a yellow dye places the
 * finish gate, using the player's position and facing direction. Both tools preview the existing
 * gates with coloured particles (blue checkpoints, yellow finish).
 */
public final class GateToolManager implements Listener {

    private static final String TYPE_CHECKPOINT = "checkpoint";
    private static final String TYPE_FINISH = "finish";

    private final BoatRacingPlugin plugin;
    private final NamespacedKey toolKey;
    private SchedulerCompat.TaskHandle renderTask;

    public GateToolManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "gate_tool");
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("setup.gates.preview", true)) return;
        int period = Math.max(2, plugin.getConfig().getInt("setup.gates.preview-period-ticks", 8));
        renderTask = SchedulerCompat.runTimer(plugin, this::renderTick, period, period);
    }

    public void stop() {
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
    }

    // ----------------------------------------------------------------- items

    public void giveTools(Player player) {
        clearToolItems(player);
        give(player, checkpointTool());
        give(player, finishTool());
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.given")));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.2f);
    }

    public void removeTools(Player player) {
        if (clearToolItems(player)) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.tools-removed")));
        }
    }

    /** Removes any gate tool from the inventory without messaging. */
    private boolean clearToolItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean removed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isTool(contents[i])) {
                player.getInventory().setItem(i, null);
                removed = true;
            }
        }
        if (isTool(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
            removed = true;
        }
        return removed;
    }

    private void give(Player player, ItemStack stack) {
        player.getInventory().addItem(stack).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private ItemStack checkpointTool() {
        return toolItem(Material.LIGHT_BLUE_DYE, TYPE_CHECKPOINT);
    }

    private ItemStack finishTool() {
        return toolItem(Material.YELLOW_DYE, TYPE_FINISH);
    }

    private ItemStack toolItem(Material material, String type) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(plugin.msg().get("setup.gates.tool-" + type)));
            meta.lore(Text.lore(List.of(
                    plugin.msg().get("setup.gates.tool-" + type + "-lore-1"),
                    plugin.msg().get("setup.gates.tool-" + type + "-lore-2"))));
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, type);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isTool(ItemStack stack) {
        return toolType(stack) != null;
    }

    private String toolType(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    // --------------------------------------------------------------- events

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        String type = toolType(event.getItem());
        if (type == null) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("boatracing.setup")) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Location anchor = event.getClickedBlock() != null
                ? event.getClickedBlock().getLocation() : player.getLocation();
        if (TYPE_CHECKPOINT.equals(type)) {
            if (player.isSneaking()) removeLastCheckpoint(player);
            else addCheckpoint(player, anchor);
        } else {
            if (player.isSneaking()) clearFinish(player);
            else setFinish(player, anchor);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isTool(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    // ----------------------------------------------------------- operations

    private void addCheckpoint(Player player, Location anchor) {
        TrackConfig track = plugin.getTrackConfig();
        Vector center = new Vector(anchor.getBlockX() + 0.5, anchor.getBlockY() + 1.0, anchor.getBlockZ() + 0.5);
        PlaneCheckpoint gate = PlaneCheckpoint.facing(anchor.getWorld().getName(), center,
                player.getLocation().getDirection(), gateHalfWidth(), gateHalfHeight());
        track.addCheckpoint(gate);
        int index = track.getCheckpoints().size();
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.checkpoint-added",
                "index", index, "total", index)));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.4f);
        ParticleWireframe.drawGateEffect(player, gate, checkpointParticle(), checkpointColor());
    }

    private void removeLastCheckpoint(Player player) {
        TrackConfig track = plugin.getTrackConfig();
        int size = track.getCheckpoints().size();
        if (size <= 0) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.checkpoint-none")));
            return;
        }
        track.removeCheckpointAt(size - 1);
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.checkpoint-removed",
                "index", size, "total", track.getCheckpoints().size())));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 0.9f);
    }

    private void setFinish(Player player, Location anchor) {
        TrackConfig track = plugin.getTrackConfig();
        double halfWidth = gateHalfWidth();
        double halfHeight = gateHalfHeight();
        double halfDepth = plugin.getConfig().getDouble("setup.gates.finish-half-depth", 0.6);
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        Vector center = new Vector(anchor.getBlockX() + 0.5, anchor.getBlockY() + 1.0, anchor.getBlockZ() + 0.5);
        BoundingBox box = Math.abs(direction.getX()) >= Math.abs(direction.getZ())
                ? BoundingBox.of(center, halfDepth, halfHeight, halfWidth)
                : BoundingBox.of(center, halfWidth, halfHeight, halfDepth);
        track.setFinish(new Region(anchor.getWorld().getName(), box));
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.finish-set")));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
        ParticleWireframe.drawBoxEffect(player, box, finishParticle(), finishColor(), 4);
        // The finish marks the end of the lap: stop an active AutoTrace recording here.
        if (plugin.getAutoTraceManager() != null && plugin.getAutoTraceManager().isRecording(player)) {
            plugin.getAutoTraceManager().stop(player);
        }
    }

    private void clearFinish(Player player) {
        TrackConfig track = plugin.getTrackConfig();
        if (track.getFinish() == null) {
            player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.finish-none")));
            return;
        }
        track.setFinish(null);
        player.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.gates.finish-cleared")));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 0.9f);
    }

    // -------------------------------------------------------------- preview

    private void renderTick() {
        TrackConfig track = plugin.getTrackConfig();
        Particle checkpointParticle = checkpointParticle();
        Color checkpointColor = checkpointColor();
        Particle finishParticle = finishParticle();
        Color finishColor = finishColor();
        double viewDistance = plugin.getConfig().getDouble("setup.gates.view-distance", 96.0);
        double viewSquared = viewDistance * viewDistance;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("boatracing.setup")) continue;
            boolean holdCheckpoint = false;
            boolean holdFinish = false;
            for (ItemStack stack : player.getInventory().getContents()) {
                String type = toolType(stack);
                if (TYPE_CHECKPOINT.equals(type)) holdCheckpoint = true;
                else if (TYPE_FINISH.equals(type)) holdFinish = true;
            }
            if (!holdCheckpoint && !holdFinish) continue;

            if (holdCheckpoint) {
                for (CheckpointShape shape : track.getCheckpoints()) {
                    renderShape(player, shape, checkpointParticle, checkpointColor, viewSquared);
                }
            }
            if (holdFinish) {
                Region finish = track.getFinish();
                if (finish != null && finish.world() != null && finish.world().equals(player.getWorld())) {
                    Vector center = finish.getBox().getCenter();
                    if (player.getLocation().toVector().distanceSquared(center) <= viewSquared) {
                        ParticleWireframe.drawBoxEffect(player, finish.getBox(), finishParticle, finishColor, 4);
                    }
                }
            }
        }
    }

    private void renderShape(Player player, CheckpointShape shape, Particle particle, Color color, double viewSquared) {
        if (shape instanceof CheckpointGroup group) {
            renderShape(player, group.getPrimary(), particle, color, viewSquared);
            for (CheckpointShape alternate : group.getAlternates()) {
                renderShape(player, alternate, particle, color, viewSquared);
            }
            return;
        }
        if (shape instanceof PlaneCheckpoint plane) {
            if (!player.getWorld().getName().equals(plane.worldName())) return;
            if (player.getLocation().toVector().distanceSquared(plane.getCenter()) > viewSquared) return;
            ParticleWireframe.drawGateEffect(player, plane, particle, color);
        } else if (shape instanceof Region region) {
            if (region.world() == null || !region.world().equals(player.getWorld())) return;
            if (player.getLocation().toVector().distanceSquared(region.getBox().getCenter()) > viewSquared) return;
            ParticleWireframe.drawBoxEffect(player, region.getBox(), particle, color, 3);
        }
    }

    // -------------------------------------------------------------- helpers

    /** Checkpoint gate size, falling back to the AutoTrace size so manual gates match. */
    private double gateHalfWidth() {
        return plugin.getConfig().getDouble("setup.gates.half-width",
                plugin.getConfig().getDouble("setup.auto-trace.half-width", 4.5));
    }

    private double gateHalfHeight() {
        return plugin.getConfig().getDouble("setup.gates.half-height",
                plugin.getConfig().getDouble("setup.auto-trace.half-height", 3.0));
    }

    private Particle checkpointParticle() {
        String raw = plugin.getConfig().getString("setup.gates.checkpoint-particle", "DUST");
        if (raw != null && ("DUST".equalsIgnoreCase(raw) || "REDSTONE".equalsIgnoreCase(raw))) {
            Particle dust = ParticleWireframe.dustParticle();
            if (dust != null) return dust;
        }
        return ParticleWireframe.resolve(raw);
    }

    private Particle finishParticle() {
        String raw = plugin.getConfig().getString("setup.gates.finish-particle", "FLAME");
        if (raw != null && ("DUST".equalsIgnoreCase(raw) || "REDSTONE".equalsIgnoreCase(raw))) {
            Particle dust = ParticleWireframe.dustParticle();
            if (dust != null) return dust;
        }
        return ParticleWireframe.resolve(raw);
    }

    private Color checkpointColor() {
        return parseColor(plugin.getConfig().getString("setup.gates.checkpoint-color", "3B82F6"),
                Color.fromRGB(0x3B82F6));
    }

    private Color finishColor() {
        return parseColor(plugin.getConfig().getString("setup.gates.finish-color", "FACC15"),
                Color.fromRGB(0xFACC15));
    }

    private static Color parseColor(String raw, Color fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            String cleaned = raw.trim().replace("#", "");
            return Color.fromRGB(Integer.parseInt(cleaned, 16));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
