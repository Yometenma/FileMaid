package net.filemaid.core.model;

import java.util.List;

/**
 * Immutable post-processing instructions captured together with a confirmed
 * rename plan. Freezing these into the confirmation plan means the executor
 * always runs the same NFO/artwork options the user approved, even if the UI
 * state changes afterwards.
 */
public record PostProcessPlan(boolean generateNfo, boolean downloadArtwork, String artworkType, List<Item> items) {
    public PostProcessPlan {
        items = items == null ? List.of() : List.copyOf(items);
        if (artworkType == null || artworkType.isBlank()) artworkType = "POSTER";
    }

    public boolean enabled() { return generateNfo || downloadArtwork; }

    public record Item(String source, MetadataSelection metadata, String artworkUrl) {
        public Item {
            if (source == null || source.isBlank()) throw new IllegalArgumentException("post-process item source must not be blank");
        }
    }
}
