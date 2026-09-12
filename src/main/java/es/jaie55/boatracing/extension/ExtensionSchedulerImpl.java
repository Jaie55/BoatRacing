package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.extension.ExtensionScheduler;
import es.jaie55.boatracing.util.SchedulerCompat;

import java.util.ArrayList;
import java.util.List;

/** Folia-aware scheduler exposed to extensions. Tracks its tasks so they can be cancelled on disable. */
public final class ExtensionSchedulerImpl implements ExtensionScheduler {

    private final BoatRacingPlugin plugin;
    private final List<TaskHandle> handles = new ArrayList<>();

    ExtensionSchedulerImpl(BoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle runNow(Runnable task) {
        return track(wrap(SchedulerCompat.runNow(plugin, task)));
    }

    @Override
    public TaskHandle runLater(Runnable task, long delayTicks) {
        return track(wrap(SchedulerCompat.runLater(plugin, task, delayTicks)));
    }

    @Override
    public TaskHandle runTimer(Runnable task, long delayTicks, long periodTicks) {
        return track(wrap(SchedulerCompat.runTimer(plugin, task, delayTicks, periodTicks)));
    }

    @Override
    public TaskHandle runAsync(Runnable task) {
        return track(wrap(SchedulerCompat.runAsync(plugin, task)));
    }

    @Override
    public TaskHandle runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return track(wrap(SchedulerCompat.runAsyncTimer(plugin, task, delayTicks, periodTicks)));
    }

    /** Cancels every task the extension scheduled. Called when the extension is disabled. */
    public void cancelAll() {
        for (TaskHandle handle : handles) {
            try {
                handle.cancel();
            } catch (Throwable ignored) {
            }
        }
        handles.clear();
    }

    private TaskHandle track(TaskHandle handle) {
        handles.add(handle);
        return handle;
    }

    private static TaskHandle wrap(SchedulerCompat.TaskHandle handle) {
        return handle::cancel;
    }
}
