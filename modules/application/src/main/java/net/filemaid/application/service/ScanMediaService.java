package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.ScanOptions;
import net.filemaid.core.model.StorageRoot;

public final class ScanMediaService {
    private final Map<String, StorageRoot> roots;
    private final MediaScanner scanner;
    private final StoragePathPolicy pathPolicy;
    private final SettingsService settings;

    public ScanMediaService(List<StorageRoot> roots, MediaScanner scanner, StoragePathPolicy pathPolicy, SettingsService settings) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.scanner = scanner;
        this.pathPolicy = pathPolicy;
        this.settings = settings;
    }

    public List<StorageRoot> roots() {
        return roots.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    public List<MediaFile> scan(String rootId, String relativePath, int maxDepth, int maxFiles) throws IOException {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        Path start = pathPolicy.resolve(root, relativePath);
        return scanner.scan(root, start, maxDepth, maxFiles, scanOptions());
    }

    private ScanOptions scanOptions() {
        if (settings == null) return ScanOptions.none();
        Set<String> ignored = split(settings.value("scan.ignorePatterns", "@eaDir,.git,@Recycle"));
        long minimumMb = parseLong(settings.value("scan.minimumFileSizeMb", "0"), 0);
        Set<String> extensions = split(settings.value("scan.extensions", ""));
        return new ScanOptions(ignored, minimumMb * 1024 * 1024, extensions);
    }

    private Set<String> split(String value) {
        String[] parts = (value == null ? "" : value).split(",");
        return Arrays.stream(parts)
                .map(String::trim)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private long parseLong(String value, long fallback) {
        try { return Long.parseLong(value == null ? "" : value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
