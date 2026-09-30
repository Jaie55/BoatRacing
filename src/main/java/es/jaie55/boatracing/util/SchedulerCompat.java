package es.jaie55.boatracing.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler compatibility layer for Bukkit/Paper and Folia.
 * Uses reflection for Folia-specific schedulers so we keep compile compatibility.
 */
public final class SchedulerCompat {
    private SchedulerCompat() {}

    public interface TaskHandle {
        void cancel();
    }

    private static final class ReflectHandle implements TaskHandle {
        private final Object delegate;

        private ReflectHandle(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            if (delegate == null) return;
            try {
                Method m = delegate.getClass().getMethod("cancel");
                m.invoke(delegate);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class BukkitHandle implements TaskHandle {
        private final BukkitTask task;

        private BukkitHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (task != null) task.cancel();
        }
    }

    public static boolean isFolia() {
        try {
            Object server = Bukkit.getServer();
            server.getClass().getMethod("getGlobalRegionScheduler");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Runs the task on the region that owns the location (Folia) or on the main thread (Bukkit).
     * Use this before spawning entities in world locations when Folia support matters.
     */
    public static TaskHandle runAtLocation(Plugin plugin, org.bukkit.Location location, Runnable runnable) {
        if (location == null || !isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        }
        try {
            Object scheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler").invoke(Bukkit.getServer());
            try {
                Method execute = scheduler.getClass().getMethod("execute", Plugin.class, org.bukkit.Location.class, Runnable.class);
                execute.invoke(scheduler, plugin, location, runnable);
            } catch (NoSuchMethodException older) {
                int chunkX = location.getBlockX() >> 4;
                int chunkZ = location.getBlockZ() >> 4;
                Method execute = scheduler.getClass().getMethod("execute", Plugin.class, org.bukkit.World.class, int.class, int.class, Runnable.class);
                execute.invoke(scheduler, plugin, location.getWorld(), chunkX, chunkZ, runnable);
            }
            return () -> {
            };
        } catch (Exception exception) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        }
    }

    public static TaskHandle runNow(Plugin plugin, Runnable runnable) {
        if (!isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        }
        try {
            Object global = getGlobalRegionScheduler();
            Method run = global.getClass().getMethod("run", Plugin.class, Consumer.class);
            Object task = run.invoke(global, plugin, (Consumer<Object>) t -> runnable.run());
            return new ReflectHandle(task);
        } catch (Exception ex) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        }
    }

    public static TaskHandle runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (!isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
        }
        try {
            Object global = getGlobalRegionScheduler();
            Method runDelayed = global.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            Object task = runDelayed.invoke(global, plugin, (Consumer<Object>) t -> runnable.run(), delayTicks);
            return new ReflectHandle(task);
        } catch (Exception ex) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
        }
    }

    public static TaskHandle runTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (!isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks));
        }
        try {
            Object global = getGlobalRegionScheduler();
            Method runAtFixedRate = global.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            Object task = runAtFixedRate.invoke(global, plugin, (Consumer<Object>) t -> runnable.run(), delayTicks, periodTicks);
            return new ReflectHandle(task);
        } catch (Exception ex) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks));
        }
    }

    public static TaskHandle runAsyncNow(Plugin plugin, Runnable runnable) {
        if (!isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        }
        try {
            Object async = getAsyncScheduler();
            Method runNow = async.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            Object task = runNow.invoke(async, plugin, (Consumer<Object>) t -> runnable.run());
            return new ReflectHandle(task);
        } catch (Exception ex) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        }
    }

    // Backward-compatible alias used by existing call sites.
    public static TaskHandle runAsync(Plugin plugin, Runnable runnable) {
        return runAsyncNow(plugin, runnable);
    }

    public static TaskHandle runAsyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (!isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks));
        }
        try {
            Object async = getAsyncScheduler();
            Method runAtFixedRate = async.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
            long delayMs = Math.max(0L, delayTicks) * 50L;
            long periodMs = Math.max(1L, periodTicks) * 50L;
            Object task = runAtFixedRate.invoke(async, plugin, (Consumer<Object>) t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
            return new ReflectHandle(task);
        } catch (Exception ex) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks));
        }
    }

    private static Object getGlobalRegionScheduler() throws Exception {
        Object server = Bukkit.getServer();
        Method m = server.getClass().getMethod("getGlobalRegionScheduler");
        return m.invoke(server);
    }

    private static Object getAsyncScheduler() throws Exception {
        Object server = Bukkit.getServer();
        Method m = server.getClass().getMethod("getAsyncScheduler");
        return m.invoke(server);
    }
}
