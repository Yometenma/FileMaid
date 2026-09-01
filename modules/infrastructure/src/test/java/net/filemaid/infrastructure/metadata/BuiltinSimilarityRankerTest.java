package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;

class BuiltinSimilarityRankerTest {
    private final BuiltinSimilarityRanker ranker = new BuiltinSimilarityRanker();

    @Test
    void similarityReturnsOneForIdentical() {
        assertEquals(1f, BuiltinSimilarityRanker.similarity("Example Show", "Example Show"), 1e-6);
    }

    @Test
    void similarityIsHigherForCloserTitles() {
        float close = BuiltinSimilarityRanker.similarity("Example Show", "Example Shows");
        float far = BuiltinSimilarityRanker.similarity("Example Show", "Totally Different");
        assertTrue(close > far);
    }

    @Test
    void rankOrdersCandidatesBySimilarity() {
        var exact = candidate("exact", "Example Show", 2020);
        var close = candidate("close", "Example Shows", 2020);
        var far = candidate("far", "Something Else", 2020);
        var ranked = ranker.rank("Example Show", 2020, List.of(far, exact, close));
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(ranked.get(i - 1).score() >= ranked.get(i).score(), "scores must be descending");
        }
        assertEquals("exact", ranked.get(0).candidate().id());
    }

    @Test
    void yearMatchGetsBonus() {
        var rightYear = candidate("right", "Example", 2020);
        var wrongYear = candidate("wrong", "Example", 1999);
        var ranked = ranker.rank("Example", 2020, List.of(wrongYear, rightYear));
        assertEquals("right", ranked.get(0).candidate().id());
    }

    @Test
    void similarityHandlesMinorTypos() {
        float typo = BuiltinSimilarityRanker.similarity("Example Show", "Exampl Show");
        float unrelated = BuiltinSimilarityRanker.similarity("Example Show", "Something Else");
        assertTrue(typo > unrelated);
    }

    private static MetadataCandidate candidate(String id, String title, int year) {
        return new MetadataCandidate("tmdb", id, MetadataType.MOVIE, title, List.of(), year, null);
    }
}
