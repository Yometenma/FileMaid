package net.filemaid.similarity;

import java.io.File;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.util.FileUtilities;

public class FileNameMetric
implements SimilarityMetric {
    @Override
    public float getSimilarity(Object object, Object object2) {
        String string = this.getFileName(object);
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        String string2 = this.getFileName(object2);
        if (string2 == null || string2.isEmpty()) {
            return 0.0f;
        }
        return string.startsWith(string2) || string2.startsWith(string) ? 1.0f : 0.0f;
    }

    protected String getFileName(Object object) {
        if (object instanceof File) {
            return FileUtilities.getName((File)object).trim().toLowerCase();
        }
        return null;
    }
}

