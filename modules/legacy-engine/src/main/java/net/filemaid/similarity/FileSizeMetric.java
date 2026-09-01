package net.filemaid.similarity;

import java.io.File;
import net.filemaid.similarity.SimilarityMetric;

public class FileSizeMetric
implements SimilarityMetric {
    @Override
    public float getSimilarity(Object object, Object object2) {
        long l = this.getLength(object);
        if (l < 0L) {
            return 0.0f;
        }
        long l2 = this.getLength(object2);
        if (l2 < 0L) {
            return 0.0f;
        }
        return l == l2 ? 1.0f : -1.0f;
    }

    protected long getLength(Object object) {
        if (object instanceof File) {
            return ((File)object).length();
        }
        return -1L;
    }
}

