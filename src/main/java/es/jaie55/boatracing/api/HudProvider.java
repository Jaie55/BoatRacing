package es.jaie55.boatracing.api;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Optional HUD hook for extensions. The base plugin owns the sidebar and action bar during
 * races; a provider can append its own lines/suffix without fighting the race timer.
 *
 * Lines use legacy ampersand colour codes. Providers are called every scoreboard refresh
 * (every 2 ticks) on the main thread, so keep them cheap.
 */
public interface HudProvider {

    /** @return extra sidebar lines for the viewer, or an empty list. */
    List<String> sidebarLines(Player viewer);

    /** @return text appended to the viewer's action bar, or null/empty for none. */
    default String actionBarSuffix(Player viewer) {
        return null;
    }
}
