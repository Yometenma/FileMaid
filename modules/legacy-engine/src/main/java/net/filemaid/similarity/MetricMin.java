package net.filemaid.similarity;

import net.filemaid.similarity.SimilarityMetric;

public class MetricMin
implements SimilarityMetric {
    private final SimilarityMetric metric;
    private final float minValue;

    public MetricMin(SimilarityMetric similarityMetric, float f) {
        this.metric = similarityMetric;
        this.minValue = f;
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        return Math.max(this.metric.getSimilarity(object, object2), this.minValue);
    }
}

