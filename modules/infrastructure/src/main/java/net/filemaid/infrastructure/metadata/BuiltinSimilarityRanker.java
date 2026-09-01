package net.filemaid.infrastructure.metadata;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.RankedCandidate;

/**
 * Ranks candidates by title similarity using a built-in normalized Levenshtein
 * distance, plus a small bonus for a matching year. Pure local computation.
 */
public final class BuiltinSimilarityRanker implements SimilarityRanker {
    @Override
    public List<RankedCandidate> rank(String query, Integer year, List<MetadataCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        return candidates.stream()
                .map(candidate -> new RankedCandidate(candidate, score(query, year, candidate)))
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed())
                .toList();
    }

    private float score(String query, Integer year, MetadataCandidate candidate) {
        float best = similarity(query, candidate.title());
        for (String alt : candidate.alternativeTitles()) best = Math.max(best, similarity(query, alt));
        if (year != null && candidate.year() != null && Math.abs(year - candidate.year()) <= 1) best += 0.5f;
        return best;
    }

    static float similarity(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return 0f;
        if (x.equals(y)) return 1f;
        if (x.contains(y) || y.contains(x)) return 0.8f;
        int distance = levenshtein(x, y);
        int max = Math.max(x.length(), y.length());
        return Math.max(0f, 1f - (float) distance / max);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "");
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }
}
