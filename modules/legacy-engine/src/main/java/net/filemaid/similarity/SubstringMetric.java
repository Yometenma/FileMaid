package net.filemaid.similarity;

import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SimilarityMetric;

public class SubstringMetric
implements SimilarityMetric {
    @Override
    public float getSimilarity(Object object, Object object2) {
        String string = this.normalize(object);
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        String string2 = this.normalize(object2);
        if (string2 == null || string2.isEmpty()) {
            return 0.0f;
        }
        return this.matches(string, string2) || this.matches(string2, string) ? 1.0f : 0.0f;
    }

    protected boolean matches(String string, String string2) {
        int n = string.lastIndexOf(string2);
        if (n < 0) {
            return false;
        }
        if (n - 1 >= 0 && Character.isLetterOrDigit(string.charAt(n - 1))) {
            return false;
        }
        return n + string2.length() >= string.length() || !Character.isLetterOrDigit(string.charAt(n + string2.length()));
    }

    protected String normalize(Object object) {
        if (object == null) {
            return null;
        }
        return Normalization.normalizePunctuation(object.toString()).toLowerCase();
    }
}

