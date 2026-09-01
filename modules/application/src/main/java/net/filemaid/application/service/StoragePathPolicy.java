package net.filemaid.application.service;

import java.nio.file.Path;
import net.filemaid.core.model.StorageRoot;

public final class StoragePathPolicy {
    public Path resolve(StorageRoot root, String relativePath) {
        String requested = relativePath == null ? "" : relativePath.trim();
        Path relative = requested.isEmpty() ? Path.of("") : Path.of(requested);
        if (relative.isAbsolute()) throw new IllegalArgumentException("Only paths relative to a configured storage root are allowed");
        Path resolved = root.path().resolve(relative).normalize();
        if (!resolved.startsWith(root.path())) throw new IllegalArgumentException("Path escapes the configured storage root");
        return resolved;
    }
}
