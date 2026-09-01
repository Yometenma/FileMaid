package net.filemaid.infrastructure.metadata;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.RankedCandidate;

/**
 * Ranks candidates by title similarity. Combines a q-gram (3-gram) Dice
 * coefficient with a normalized Levenshtein distance, taking the better of the
 * two, plus a small bonus for a matching year. Pure local computation.
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
        return Math.max(levenshteinScore(x, y), qGramScore(x, y));
    }

    private static float levenshteinScore(String a, String b) {
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 0f : Math.max(0f, 1f - (float) levenshtein(a, b) / max);
    }

    private static float qGramScore(String a, String b) {
        Map<String, Integer> gramsA = nGrams(a);
        Map<String, Integer> gramsB = nGrams(b);
        int intersection = 0;
        for (var entry : gramsA.entrySet()) {
            intersection += Math.min(entry.getValue(), gramsB.getOrDefault(entry.getKey(), 0));
        }
        int totalA = gramsA.values().stream().mapToInt(Integer::intValue).sum();
        int totalB = gramsB.values().stream().mapToInt(Integer::intValue).sum();
        int total = totalA + totalB;
        return total == 0 ? 0f : 2.0f * intersection / total;
    }

    private static Map<String, Integer> nGrams(String value) {
        String padded = "  " + value + "  ";
        Map<String, Integer> grams = new HashMap<>();
        for (int i = 0; i + 3 <= padded.length(); i++) {
            grams.merge(padded.substring(i, i + 3), 1, Integer::sum);
        }
        return grams;
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
