package net.filemaid.similarity;

import java.io.File;
import net.filemaid.similarity.DateMatcher;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.web.SimpleDate;

public class DateMetric
implements SimilarityMetric {
    private final DateMatcher matcher;

    public DateMetric(DateMatcher dateMatcher) {
        this.matcher = dateMatcher;
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        SimpleDate simpleDate = this.parse(object);
        if (simpleDate == null) {
            return 0.0f;
        }
        SimpleDate simpleDate2 = this.parse(object2);
        if (simpleDate2 == null) {
            return 0.0f;
        }
        return simpleDate.equals(simpleDate2) ? 1.0f : -1.0f;
    }

    public SimpleDate parse(Object object) {
        if (object instanceof File) {
            return this.matcher.match((File)object);
        }
        return this.matcher.match(object.toString());
    }
}

