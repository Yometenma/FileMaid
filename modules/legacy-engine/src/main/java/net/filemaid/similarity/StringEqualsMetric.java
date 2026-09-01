package net.filemaid.similarity;

import net.filemaid.similarity.SimilarityMetric;

public class StringEqualsMetric
implements SimilarityMetric {
    @Override
    public float getSimilarity(Object object, Object object2) {
        if (object == null || object2 == null) {
            return 0.0f;
        }
        String string = this.normalize(object);
        String string2 = this.normalize(object2);
        if (string.isEmpty() || string2.isEmpty()) {
            return 0.0f;
        }
        return string.equals(string2) ? 1.0f : 0.0f;
    }

    protected String normalize(Object object) {
        return object.toString().trim().toLowerCase();
    }
}

