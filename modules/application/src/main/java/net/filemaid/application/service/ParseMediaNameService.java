package net.filemaid.application.service;

import java.util.List;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.core.model.ParsedMediaName;

public final class ParseMediaNameService {
    private final MediaNameParser parser;

    public ParseMediaNameService(MediaNameParser parser) {
        this.parser = parser;
    }

    public List<ParsedMediaName> parse(List<String> names) {
        if (names == null || names.isEmpty()) throw new IllegalArgumentException("At least one file name is required");
        if (names.size() > 1_000) throw new IllegalArgumentException("A parse request may contain at most 1000 file names");
        return names.stream().map(parser::parse).toList();
    }
}
