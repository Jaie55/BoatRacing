package es.jaie55.boatracing.api.extension;

import java.io.IOException;

/**
 * Storage managed by BoatRacing for an extension. Documents are stored relative to the extension
 * folder and follow the configured backend ({@code database.mode}: YAML, SQLITE or MYSQL), so
 * extension data is persisted exactly like the rest of BoatRacing data.
 *
 * Document names are plain file names such as {@code party-stats.yml} or {@code boxes.yml}.
 */
public interface ExtensionStorage {

    /** @return the stored document content, or null when it does not exist. */
    String read(String document) throws IOException;

    /** Writes (creating or replacing) a document. */
    void write(String document, String content) throws IOException;

    /** @return true when the document exists. */
    default boolean exists(String document) {
        try {
            return read(document) != null;
        } catch (IOException exception) {
            return false;
        }
    }
}
