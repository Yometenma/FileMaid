package net.filemaid.similarity;

import java.util.Arrays;
import java.util.stream.Collectors;
import net.filemaid.similarity.SimilarityMetric;

public class MetricCascade
implements SimilarityMetric {
    private final SimilarityMetric[] cascade;

    public MetricCascade(SimilarityMetric ... similarityMetricArray) {
        this.cascade = similarityMetricArray;
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        float f = 0.0f;
        for (SimilarityMetric similarityMetric : this.cascade) {
            float f2 = similarityMetric.getSimilarity(object, object2);
            if (!(Math.abs(f2) >= Math.abs(f))) continue;
            if (f2 >= 1.0f) {
                return f2;
            }
            f = f2;
        }
        return f;
    }

    public String toString() {
        return Arrays.stream(this.cascade).map(Object::toString).collect(Collectors.joining(", ", "MetricCascade(", ")"));
    }
}

