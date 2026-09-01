package net.filemaid.core.model;

/**
 * A metadata candidate paired with its similarity score against a query. Higher
 * scores rank first; the scale is engine-dependent, so callers should only use
 * it for ordering.
 */
public record RankedCandidate(MetadataCandidate candidate, float score) {
    public RankedCandidate {
        if (candidate == null) throw new IllegalArgumentException("candidate must not be null");
    }
}
