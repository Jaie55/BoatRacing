package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.extension.ExtensionStorage;
import es.jaie55.boatracing.util.DocumentStore;

import java.io.IOException;

/**
 * Routes extension documents through BoatRacing's DocumentStore so the configured backend
 * (YAML/SQLite/MySQL) is used transparently. In YAML mode documents land under
 * {@code plugins/BoatRacing/extensions/<name>/}.
 */
public final class ExtensionStorageImpl implements ExtensionStorage {

    private final BoatRacingPlugin plugin;
    private final String prefix;

    ExtensionStorageImpl(BoatRacingPlugin plugin, String extensionName) {
        this.plugin = plugin;
        this.prefix = "extensions/" + extensionName + "/";
    }

    @Override
    public String read(String document) throws IOException {
        DocumentStore store = plugin.getDocumentStore();
        if (store == null) return null;
        return store.read(prefix + sanitize(document));
    }

    @Override
    public void write(String document, String content) throws IOException {
        DocumentStore store = plugin.getDocumentStore();
        if (store == null) return;
        store.write(prefix + sanitize(document), content == null ? "" : content);
    }

    private static String sanitize(String document) {
        String cleaned = document == null ? "data.yml" : document.replace('\\', '/').trim();
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        cleaned = cleaned.replace("..", "")
                .replace(':', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('"', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
        return cleaned.isEmpty() ? "data.yml" : cleaned;
    }
}
