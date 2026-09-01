package net.filemaid.similarity;

import net.filemaid.similarity.CommonSequenceMatcher;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SimilarityMetric;

public class SequenceMatchSimilarity
implements SimilarityMetric {
    private final CommonSequenceMatcher commonSequenceMatcher;

    public SequenceMatchSimilarity() {
        this(10, false);
    }

    public SequenceMatchSimilarity(int n, boolean bl) {
        this.commonSequenceMatcher = new CommonSequenceMatcher(n, bl);
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        String string;
        String string2 = this.normalize(object);
        String string3 = this.match(string2, string = this.normalize(object2));
        if (string3 == null || string3.isEmpty()) {
            return 0.0f;
        }
        return this.similarity(string3, string2, string);
    }

    protected float similarity(String string, String string2, String string3) {
        return (float)string.length() / (float)Math.min(string2.length(), string3.length());
    }

    protected String normalize(Object object) {
        return Normalization.normalizePunctuation(object.toString()).toLowerCase();
    }

    protected String match(String string, String string2) {
        return this.commonSequenceMatcher.matchFirstCommonSequence(string, string2);
    }
}

