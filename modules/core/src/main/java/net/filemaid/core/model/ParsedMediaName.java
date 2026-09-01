package net.filemaid.core.model;

import java.util.List;

public record ParsedMediaName(
        String originalName,
        String title,
        MediaType type,
        Integer year,
        Integer season,
        List<Integer> episodes,
        String extension,
        double confidence,
        String parser) {
    public ParsedMediaName {
        episodes = episodes == null ? List.of() : List.copyOf(episodes);
    }
}
