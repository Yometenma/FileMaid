package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.StorageRoot;

public final class ProbeMediaInfoService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final MediaInfoProvider provider;

    public ProbeMediaInfoService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, MediaInfoProvider provider) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
        this.provider = provider;
    }

    public String providerId() {
        return provider.id();
    }

    public boolean available() {
        return provider.available();
    }

    public Optional<MediaInfo> probe(String rootId, String relativePath) throws IOException {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        Path absolute = pathPolicy.resolve(root, relativePath);
        return provider.probe(absolute);
    }
}
