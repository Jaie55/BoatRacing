package es.jaie55.boatracing.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class YamlFileDocumentStore implements DocumentStore {
    private final Path dataFolder;

    YamlFileDocumentStore(java.io.File dataFolder) {
        this.dataFolder = dataFolder.toPath();
    }

    @Override
    public String read(String documentName) throws IOException {
        Path file = dataFolder.resolve(documentName);
        if (!Files.exists(file)) return null;
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Override
    public void write(String documentName, String content) throws IOException {
        Path file = dataFolder.resolve(documentName);
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
    }
}
