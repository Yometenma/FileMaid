package net.filemaid.similarity;

import net.filemaid.similarity.NumberTokeniser;
import net.filemaid.similarity.SimilarityMetric;
import org.simmetrics.MultisetMetric;
import org.simmetrics.StringMetric;
import org.simmetrics.builders.StringMetricBuilder;
import org.simmetrics.metrics.BlockDistance;
import org.simmetrics.tokenizers.Tokenizer;

public class NumericSimilarityMetric
implements SimilarityMetric {
    private final StringMetric metric = StringMetricBuilder.with((MultisetMetric)new BlockDistance()).tokenize((Tokenizer)new NumberTokeniser()).build();

    @Override
    public float getSimilarity(Object object, Object object2) {
        return this.metric.compare(this.normalize(object), this.normalize(object2));
    }

    protected String normalize(Object object) {
        return object.toString();
    }
}

