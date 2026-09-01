package net.filemaid.application.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface StorageBrowser {
    List<Path> directories(Path start, String query, int limit) throws IOException;
}
