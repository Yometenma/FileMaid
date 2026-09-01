package net.filemaid.similarity;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import net.filemaid.MemoryCache;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.SimilarityMetric;

public class SimilarityComparator<T, P>
implements Comparator<T> {
    protected SimilarityMetric metric;
    protected Collection<P> paragon;
    protected Function<T, Collection<P>> mapper;
    private final MemoryCache<T, Double> cache = MemoryCache.forObject();
    private static final double ZERO = 0.0;

    public static <T, S extends CharSequence> SimilarityComparator<T, S> compareTo(S s, Function<T, S> function) {
        return new SimilarityComparator<T, S>(new NameSimilarityMetric(), Collections.singleton(s), function.andThen(Collections::singleton));
    }

    public SimilarityComparator(SimilarityMetric similarityMetric, Collection<P> collection, Function<T, Collection<P>> function) {
        this.metric = similarityMetric;
        this.paragon = collection;
        this.mapper = function;
    }

    @Override
    public int compare(T t, T t2) {
        double d = this.cache.get(t, this::getSimilarity);
        double d2 = this.cache.get(t2, this::getSimilarity);
        return Double.compare(d2, d);
    }

    public double getSimilarity(T t) {
        return this.paragon.stream().mapToDouble(object2 -> this.accumulateSimilarity(object2, t)).max().orElse(0.0);
    }

    private double accumulateSimilarity(P p, T t) {
        if (p == null) {
            return 0.0;
        }
        return this.mapper.apply(t).stream().mapToDouble(object2 -> object2 == null ? 0.0 : (double)this.metric.getSimilarity(p, object2)).max().orElse(0.0);
    }
}

