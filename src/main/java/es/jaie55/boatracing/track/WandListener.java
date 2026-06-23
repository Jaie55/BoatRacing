package es.jaie55.boatracing.track;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.track.TrackConfig;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class WandListener implements Listener {
    private final BoatRacingPlugin plugin;
    public WandListener(BoatRacingPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        if (!SelectionManager.isWand(e.getItem())) return;
        // Shift+click on Redstone Lamp: add/remove start light
        if (e.getPlayer().isSneaking() && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.REDSTONE_LAMP) {
            TrackConfig t = plugin.getRaceManager().getTrack();
            if (t == null) return;
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                boolean ok = t.addLight(e.getClickedBlock());
                if (ok) {
                    e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.light-added",
                        "x", e.getClickedBlock().getX(),
                        "y", e.getClickedBlock().getY(),
                        "z", e.getClickedBlock().getZ())));
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                } else {
                    e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.light-fail")));
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                }
                if (plugin.getSetupWizard() != null) plugin.getSetupWizard().afterAction(e.getPlayer());
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                boolean ok = t.removeLightAt(e.getClickedBlock());
                if (ok) {
                    e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.light-removed",
                        "x", e.getClickedBlock().getX(),
                        "y", e.getClickedBlock().getY(),
                        "z", e.getClickedBlock().getZ())));
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
                } else {
                    e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.light-not-found")));
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                }
                if (plugin.getSetupWizard() != null) plugin.getSetupWizard().afterAction(e.getPlayer());
            }
            e.setCancelled(true);
            return;
        }
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null) {
            SelectionManager.setPos1(e.getPlayer(), e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.selection-corner-a",
                "x", e.getClickedBlock().getX(),
                "y", e.getClickedBlock().getY(),
                "z", e.getClickedBlock().getZ())));
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.3f);
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            SelectionManager.setPos2(e.getPlayer(), e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("setup.selection-corner-b",
                "x", e.getClickedBlock().getX(),
                "y", e.getClickedBlock().getY(),
                "z", e.getClickedBlock().getZ())));
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 0.9f, 1.0f);
            e.setCancelled(true);
        }
    }
}
