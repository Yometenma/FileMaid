package net.filemaid.application.port;

import java.util.List;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.RankedCandidate;

/** Ranks metadata candidates by how well they match a query title and year. */
public interface SimilarityRanker {
    List<RankedCandidate> rank(String query, Integer year, List<MetadataCandidate> candidates);
}
