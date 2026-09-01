package net.filemaid.core.model;

import java.util.Set;

/**
 * Scanner filtering options supplied by the application layer. An empty
 * {@code allowedExtensions} means no whitelist, {@code ignoredDirectoryNames}
 * matches directory names along the relative path, and a non-positive
 * {@code minimumFileSizeBytes} disables the size threshold.
 */
public record ScanOptions(Set<String> ignoredDirectoryNames, long minimumFileSizeBytes, Set<String> allowedExtensions) {
    public ScanOptions {
        ignoredDirectoryNames = ignoredDirectoryNames == null ? Set.of() : Set.copyOf(ignoredDirectoryNames);
        allowedExtensions = allowedExtensions == null ? Set.of() : Set.copyOf(allowedExtensions);
        if (minimumFileSizeBytes < 0) throw new IllegalArgumentException("minimumFileSizeBytes must not be negative");
    }

    public static ScanOptions none() {
        return new ScanOptions(Set.of(), 0, Set.of());
    }
}
