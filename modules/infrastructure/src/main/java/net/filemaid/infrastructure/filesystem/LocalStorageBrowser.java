package net.filemaid.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.filemaid.application.port.StorageBrowser;

public final class LocalStorageBrowser implements StorageBrowser {
    @Override
    public List<Path> directories(Path start, String query, int limit) throws IOException {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int depth = needle.isBlank() ? 1 : 8;
        try (Stream<Path> paths = Files.walk(start, depth)) {
            return paths.filter(path -> !path.equals(start))
                    .filter(Files::isDirectory)
                    .filter(path -> needle.isBlank() ? path.getParent().equals(start) : path.getFileName().toString().toLowerCase(Locale.ROOT).contains(needle))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .limit(limit).toList();
        }
    }
}
