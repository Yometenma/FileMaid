package net.filemaid.core.model;

public record MetadataSelection(
        String source,
        String provider,
        String id,
        MetadataType type,
        String title,
        Integer year) {
    public MetadataSelection {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Metadata selection source must not be blank");
        if (provider == null || provider.isBlank()) throw new IllegalArgumentException("Metadata provider must not be blank");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Metadata id must not be blank");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Metadata title must not be blank");
        if (type == null) throw new IllegalArgumentException("Metadata type must not be null");
    }
}
