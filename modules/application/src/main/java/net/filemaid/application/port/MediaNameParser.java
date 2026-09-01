package net.filemaid.application.port;

import net.filemaid.core.model.ParsedMediaName;

public interface MediaNameParser {
    ParsedMediaName parse(String fileName);
}
