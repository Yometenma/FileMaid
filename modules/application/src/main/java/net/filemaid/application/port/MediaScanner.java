package net.filemaid.application.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.StorageRoot;

public interface MediaScanner {
    List<MediaFile> scan(StorageRoot root, Path start, int maxDepth, int maxFiles) throws IOException;
}
