package net.filemaid.application.port;

import net.filemaid.core.model.ParsedMediaName;

public interface NamingTemplateEngine {
    String format(ParsedMediaName media);
}
