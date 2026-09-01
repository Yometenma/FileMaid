package net.filemaid.similarity;

import com.google.common.collect.Multiset;
import java.util.Locale;
import net.filemaid.similarity.ICU;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SimilarityMetric;
import org.simmetrics.MultisetMetric;
import org.simmetrics.StringMetric;
import org.simmetrics.builders.StringMetricBuilder;
import org.simmetrics.metrics.BlockDistance;
import org.simmetrics.metrics.GeneralizedOverlapCoefficient;
import org.simmetrics.tokenizers.Tokenizers;

public class NameSimilarityMetric
implements SimilarityMetric,
MultisetMetric<String> {
    private final StringMetric metric = StringMetricBuilder.with((MultisetMetric)this).tokenize(Tokenizers.qGramWithPadding((int)3)).build();
    private final MultisetMetric<String> blockDistance = new BlockDistance();
    private final MultisetMetric<String> overlapCoefficient = new GeneralizedOverlapCoefficient();

    @Override
    public float getSimilarity(Object object, Object object2) {
        return this.metric.compare(this.normalize(object), this.normalize(object2));
    }

    protected String normalize(Object object) {
        return Normalization.normalizePunctuation(ICU.ASCII.transform(object.toString())).toLowerCase(Locale.ROOT);
    }

    public float compare(Multiset<String> multiset, Multiset<String> multiset2) {
        return (this.blockDistance.compare(multiset, multiset2) + this.overlapCoefficient.compare(multiset, multiset2)) / 2.0f;
    }
}

