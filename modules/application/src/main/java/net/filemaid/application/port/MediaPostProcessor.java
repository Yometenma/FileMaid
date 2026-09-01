package net.filemaid.application.port;

import java.nio.file.Path;
import net.filemaid.core.model.MetadataSelection;

public interface MediaPostProcessor {
    Path writeKodiNfo(Path mediaFile, MetadataSelection metadata) throws Exception;
    Path downloadArtwork(Path mediaFile, String artworkUrl, String artworkType) throws Exception;
}
