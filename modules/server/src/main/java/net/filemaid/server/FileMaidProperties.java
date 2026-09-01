package net.filemaid.server;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("filemaid")
public record FileMaidProperties(List<Root> roots, Scan scan, Metadata metadata, Naming naming) {
    public FileMaidProperties {
        roots = roots == null ? List.of() : List.copyOf(roots);
        scan = scan == null ? new Scan(16, 10_000) : scan;
        metadata = metadata == null ? new Metadata(null) : metadata;
        naming = naming == null ? new Naming(null, null, null) : naming;
    }

    public record Root(String id, Path path, boolean writable) {}

    public record Scan(int maxDepth, int maxFiles) {
        public Scan {
            maxDepth = maxDepth <= 0 ? 16 : maxDepth;
            maxFiles = maxFiles <= 0 ? 10_000 : maxFiles;
        }
    }

    public record Metadata(String tmdbApiKey) {}
    public record Naming(String series, String movie, String unknown) {}
}
