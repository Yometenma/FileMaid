package net.filemaid.similarity;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.temporal.ChronoUnit;
import net.filemaid.Logging;
import net.filemaid.similarity.SimilarityMetric;

public class TimeStampMetric
implements SimilarityMetric {
    private final long epoch;

    public TimeStampMetric(int n, ChronoUnit chronoUnit) {
        this.epoch = chronoUnit.getDuration().multipliedBy(n).toMillis();
    }

    public long getEpochDuration() {
        return this.epoch;
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        long l = this.getTimeStamp(object);
        if (l <= 0L) {
            return -1.0f;
        }
        long l2 = this.getTimeStamp(object2);
        if (l2 <= 0L) {
            return -1.0f;
        }
        float f = Math.abs(l - l2);
        return f > (float)this.epoch ? 0.0f : 1.0f - f / (float)this.epoch;
    }

    public long getTimeStamp(Object object) {
        if (object instanceof File) {
            File file = (File)object;
            try {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class, new LinkOption[0]);
                long l = basicFileAttributes.creationTime().toMillis();
                if (l > 0L) {
                    return l;
                }
                return basicFileAttributes.lastModifiedTime().toMillis();
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(exception));
            }
        }
        return -1L;
    }
}

