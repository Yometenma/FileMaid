package net.filemaid.similarity;

import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;
import net.filemaid.format.PropertyBindings;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.similarity.StringEqualsMetric;

public class CrossPropertyMetric
implements SimilarityMetric {
    private SimilarityMetric metric;

    public CrossPropertyMetric(SimilarityMetric similarityMetric) {
        this.metric = similarityMetric;
    }

    public CrossPropertyMetric() {
        this.metric = new StringEqualsMetric();
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        Map<String, Object> map = this.getProperties(object);
        if (map.isEmpty()) {
            return 0.0f;
        }
        Map<String, Object> map2 = this.getProperties(object2);
        if (map2.isEmpty()) {
            return 0.0f;
        }
        TreeSet<String> treeSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(map.keySet());
        treeSet.retainAll(map2.keySet());
        if (treeSet.isEmpty()) {
            return 0.0f;
        }
        float f = 0.0f;
        for (String string : treeSet) {
            f += this.metric.getSimilarity(map.get(string), map2.get(string));
        }
        return f / (float)treeSet.size();
    }

    protected Map<String, Object> getProperties(Object object) {
        return object == null ? Collections.emptyMap() : new PropertyBindings(object);
    }
}

