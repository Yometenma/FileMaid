package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RankedCandidate;
import org.junit.jupiter.api.Test;

class MatchMetadataServiceTest {
    @Test
    void matchesAndRanksCandidates() throws Exception {
        MediaNameParser parser = name ->
                new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1, List.of(2), ".mkv", 0.9, "test");
        MetadataProvider provider = new MetadataProvider() {
            @Override public String id() { return "test"; }
            @Override public boolean available() { return true; }
            @Override public String status() { return "ok"; }
            @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) {
                return List.of(new MetadataCandidate("test", "1", MetadataType.SERIES, "Example Show", List.of(), 2020, null));
            }
        };
        var searchService = new SearchMetadataService(provider);
        SimilarityRanker ranker = (query, year, candidates) -> candidates.stream()
                .map(c -> new RankedCandidate(c, 1.0f))
                .toList();
        var service = new MatchMetadataService(parser, searchService, ranker);

        var result = service.match("Example.Show.S01E02.mkv", Locale.ROOT, 5);

        assertEquals(1, result.candidates().size());
        assertEquals("Example Show", result.candidates().get(0).candidate().title());
    }
}
