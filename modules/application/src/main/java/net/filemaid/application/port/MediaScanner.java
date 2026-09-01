package net.filemaid.application.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.ScanOptions;
import net.filemaid.core.model.StorageRoot;

public interface MediaScanner {
    /** Backwards-compatible scan with no filtering options. */
    default List<MediaFile> scan(StorageRoot root, Path start, int maxDepth, int maxFiles) throws IOException {
        return scan(root, start, maxDepth, maxFiles, ScanOptions.none());
    }

    List<MediaFile> scan(StorageRoot root, Path start, int maxDepth, int maxFiles, ScanOptions options) throws IOException;
}
