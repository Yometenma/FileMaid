package net.filemaid.similarity;

import java.util.Arrays;
import java.util.stream.Collectors;
import net.filemaid.similarity.SimilarityMetric;

public class MetricAvg
implements SimilarityMetric {
    private final SimilarityMetric[] metrics;

    public MetricAvg(SimilarityMetric ... similarityMetricArray) {
        this.metrics = similarityMetricArray;
    }

    public SimilarityMetric[] getMetrics() {
        return (SimilarityMetric[])this.metrics.clone();
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        float f = 0.0f;
        for (SimilarityMetric similarityMetric : this.metrics) {
            f += similarityMetric.getSimilarity(object, object2);
        }
        return f / (float)this.metrics.length;
    }

    public String toString() {
        return Arrays.stream(this.metrics).map(Object::toString).collect(Collectors.joining(", ", "MetricAvg(", ")"));
    }
}

