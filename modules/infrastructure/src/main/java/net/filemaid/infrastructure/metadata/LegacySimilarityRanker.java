package net.filemaid.infrastructure.metadata;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.RankedCandidate;

/**
 * Ranks candidates using the legacy engine's {@code NameSimilarityMetric} when
 * it is on the classpath, and falls back to a small built-in similarity
 * otherwise. Both paths are local string comparisons — no network access.
 */
public final class LegacySimilarityRanker implements SimilarityRanker {
    private final Object nameMetric;
    private final Method getSimilarity;

    public LegacySimilarityRanker() {
        Object metric = null;
        Method method = null;
        try {
            Class<?> type = Class.forName("net.filemaid.similarity.NameSimilarityMetric");
            metric = type.getConstructor().newInstance();
            method = type.getMethod("getSimilarity", Object.class, Object.class);
        } catch (Throwable ignored) {
            // legacy engine not present; use fallback similarity
        }
        this.nameMetric = metric;
        this.getSimilarity = method;
    }

    public boolean available() {
        return nameMetric != null && getSimilarity != null;
    }

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

    private float similarity(String a, String b) {
        if (available()) {
            try {
                return ((Number) getSimilarity.invoke(nameMetric, a, b)).floatValue();
            } catch (Throwable ignored) {
                // fall through to built-in similarity
            }
        }
        return fallback(a, b);
    }

    static float fallback(String a, String b) {
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
