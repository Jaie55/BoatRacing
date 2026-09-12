package es.jaie55.boatracing.api.extension;

/**
 * Scheduling helpers owned by BoatRacing. They use BoatRacing's Folia-compatible scheduler, so
 * extensions never need to touch {@code Bukkit.getScheduler()} directly.
 */
public interface ExtensionScheduler {

    /** Cancellable handle returned by every scheduling call. */
    interface TaskHandle {
        void cancel();
    }

    /** Runs the task on the next main-thread tick. */
    TaskHandle runNow(Runnable task);

    /** Runs the task after the given delay (ticks) on the main thread. */
    TaskHandle runLater(Runnable task, long delayTicks);

    /** Runs the task repeatedly on the main thread. */
    TaskHandle runTimer(Runnable task, long delayTicks, long periodTicks);

    /** Runs the task asynchronously as soon as possible. */
    TaskHandle runAsync(Runnable task);

    /** Runs the task repeatedly off the main thread. */
    TaskHandle runAsyncTimer(Runnable task, long delayTicks, long periodTicks);
}
