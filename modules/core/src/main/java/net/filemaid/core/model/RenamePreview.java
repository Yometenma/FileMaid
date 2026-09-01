package net.filemaid.core.model;

import java.util.List;

public record RenamePreview(
        String source,
        String target,
        ParsedMediaName media,
        MetadataSelection metadata,
        MediaInfo mediaInfo,
        List<String> warnings) {
    public RenamePreview {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
