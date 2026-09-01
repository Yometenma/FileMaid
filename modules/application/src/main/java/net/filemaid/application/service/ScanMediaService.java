package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.StorageRoot;

public final class ScanMediaService {
    private final Map<String, StorageRoot> roots;
    private final MediaScanner scanner;
    private final StoragePathPolicy pathPolicy;

    public ScanMediaService(List<StorageRoot> roots, MediaScanner scanner, StoragePathPolicy pathPolicy) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.scanner = scanner;
        this.pathPolicy = pathPolicy;
    }

    public List<StorageRoot> roots() {
        return roots.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    public List<MediaFile> scan(String rootId, String relativePath, int maxDepth, int maxFiles) throws IOException {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        Path start = pathPolicy.resolve(root, relativePath);
        return scanner.scan(root, start, maxDepth, maxFiles);
    }
}
