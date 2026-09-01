package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.StorageBrowser;
import net.filemaid.core.model.StorageRoot;

public final class BrowseStorageService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final StorageBrowser browser;

    public BrowseStorageService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, StorageBrowser browser) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
        this.browser = browser;
    }

    public DirectoryListing browse(String rootId, String relativePath, String query, int limit) throws IOException {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        Path current = pathPolicy.resolve(root, relativePath);
        if (!java.nio.file.Files.isDirectory(current)) throw new IllegalArgumentException("Requested path is not a directory");
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<DirectoryEntry> entries = browser.directories(current, query, safeLimit).stream()
                .filter(path -> path.normalize().startsWith(root.path()))
                .map(path -> new DirectoryEntry(path.getFileName().toString(), relative(root, path)))
                .toList();
        String currentPath = relative(root, current);
        String parent = current.equals(root.path()) ? null : relative(root, current.getParent());
        return new DirectoryListing(currentPath, parent, entries);
    }

    private String relative(StorageRoot root, Path path) { return root.path().relativize(path).toString().replace('\\', '/'); }
    public record DirectoryListing(String current, String parent, List<DirectoryEntry> entries) { }
    public record DirectoryEntry(String name, String path) { }
}
