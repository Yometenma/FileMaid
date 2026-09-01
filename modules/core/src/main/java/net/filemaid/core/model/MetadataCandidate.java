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
        String artworkUrl,
        String fanartUrl) {
    public MetadataCandidate(String provider, String id, MetadataType type, String title, List<String> alternativeTitles, Integer year, String overview, String artworkUrl) {
        this(provider, id, type, title, alternativeTitles, year, overview, artworkUrl, null);
    }
    public MetadataCandidate(String provider, String id, MetadataType type, String title, List<String> alternativeTitles, Integer year, String overview) {
        this(provider, id, type, title, alternativeTitles, year, overview, null, null);
    }
    public MetadataCandidate {
        alternativeTitles = alternativeTitles == null ? List.of() : List.copyOf(alternativeTitles);
    }
}
