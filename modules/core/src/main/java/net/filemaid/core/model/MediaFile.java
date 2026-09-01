package net.filemaid.core.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record MediaFile(String storageRootId, Path relativePath, MediaKind kind, long size, Instant modifiedAt) {
    public MediaFile {
        if (storageRootId == null || storageRootId.isBlank()) throw new IllegalArgumentException("Storage root id must not be blank");
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(modifiedAt, "modifiedAt");
        if (relativePath.isAbsolute()) throw new IllegalArgumentException("Media path must be relative to its storage root");
        if (size < 0) throw new IllegalArgumentException("Media size must not be negative");
    }
}
