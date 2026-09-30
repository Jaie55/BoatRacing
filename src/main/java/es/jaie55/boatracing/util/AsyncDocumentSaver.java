package es.jaie55.boatracing.util;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes a document through a {@link DocumentStore} in the background during runtime (skipping
 * stale snapshots) and synchronously during shutdown/reload, with a dirty flag for periodic flushes.
 */
public final class AsyncDocumentSaver {

    private final Plugin plugin;
    private final DocumentStore store;
    private final String documentName;
    private final String label;
    private final AtomicLong scheduled = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private volatile boolean dirty;
    private volatile boolean sync;

    public AsyncDocumentSaver(Plugin plugin, DocumentStore store, String documentName, String label) {
        this.plugin = plugin;
        this.store = store;
        this.documentName = documentName;
        this.label = label;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Writes the serialized content; asynchronous unless shutdown/reload requested sync mode. */
    public void write(String content) {
        if (store == null || content == null) {
            dirty = false;
            return;
        }
        if (sync) {
            writeNow(content);
            return;
        }
        dirty = true;
        long sequence = scheduled.incrementAndGet();
        SchedulerCompat.runAsync(plugin, () -> {
            if (sequence < written.get()) return; // a newer snapshot is already on disk or queued
            try {
                store.write(documentName, content);
                written.set(sequence);
                if (sequence >= scheduled.get()) dirty = false;
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to save " + label + ": " + exception.getMessage());
            }
        });
    }

    private void writeNow(String content) {
        try {
            store.write(documentName, content);
            dirty = false;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to save " + label + ": " + exception.getMessage());
        }
    }
}
