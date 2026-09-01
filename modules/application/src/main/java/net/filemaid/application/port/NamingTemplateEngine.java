package net.filemaid.application.port;

import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.ParsedMediaName;

public interface NamingTemplateEngine {
    String format(ParsedMediaName media);

    /** Formats with optional media characteristics; the default ignores them. */
    default String format(ParsedMediaName media, MediaInfo mediaInfo) {
        return format(media);
    }

    default java.util.Map<String, String> templates() { return java.util.Map.of(); }
}
