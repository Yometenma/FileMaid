package net.filemaid.application.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.filemaid.core.model.MediaInfo;

/**
 * Reads media characteristics from a single video file. The absolute path is
 * produced by the application layer after storage-root validation, so adapters
 * must treat it as trusted input and must not perform write operations.
 */
public interface MediaInfoProvider {
    String id();

    /** Whether the underlying probe tool is currently usable. */
    boolean available();

    /** Returns empty when the file cannot be probed (missing, not media, or tool failure). */
    Optional<MediaInfo> probe(Path absolutePath) throws IOException;
}
