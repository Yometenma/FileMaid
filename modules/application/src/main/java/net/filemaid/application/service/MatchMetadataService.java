package net.filemaid.application.service;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RankedCandidate;

/**
 * Parses a file name and automatically suggests metadata candidates, ranked by
 * title/year similarity. This is the read-only "matching" half of the media
 * pipeline: it never persists a decision.
 */
public final class MatchMetadataService {
    private final MediaNameParser parser;
    private final SearchMetadataService searchService;
    private final SimilarityRanker ranker;

    public MatchMetadataService(MediaNameParser parser, SearchMetadataService searchService, SimilarityRanker ranker) {
        this.parser = parser;
        this.searchService = searchService;
        this.ranker = ranker;
    }

    public MatchResult match(String fileName, Locale locale, int limit) throws Exception {
        ParsedMediaName media = parser.parse(fileName);
        MetadataType type = media.type() == MediaType.EPISODE ? MetadataType.SERIES : MetadataType.MOVIE;
        List<MetadataCandidate> candidates = searchService.search(media.title(), type, locale, limit);
        List<RankedCandidate> ranked = ranker.rank(media.title(), media.year(), candidates);
        return new MatchResult(media, ranked);
    }

    public record MatchResult(ParsedMediaName media, List<RankedCandidate> candidates) {
        public MatchResult {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }
}
