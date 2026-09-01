package net.filemaid.core.model;

import java.nio.file.Path;
import java.util.Objects;

public record StorageRoot(String id, Path path, boolean writable) {
    public StorageRoot {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Storage root id must not be blank");
        Objects.requireNonNull(path, "path");
        path = path.toAbsolutePath().normalize();
    }
}
