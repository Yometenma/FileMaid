package net.filemaid.core.model;

import java.util.List;

public record MetadataCandidate(
        String provider,
        String id,
        MetadataType type,
        String title,
        List<String> alternativeTitles,
        Integer year,
        String overview,
        String artworkUrl) {
    public MetadataCandidate(String provider, String id, MetadataType type, String title, List<String> alternativeTitles, Integer year, String overview) {
        this(provider, id, type, title, alternativeTitles, year, overview, null);
    }
    public MetadataCandidate {
        alternativeTitles = alternativeTitles == null ? List.of() : List.copyOf(alternativeTitles);
    }
}
