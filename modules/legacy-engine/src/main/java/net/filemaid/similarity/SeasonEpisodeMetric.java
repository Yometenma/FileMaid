package net.filemaid.similarity;

import java.io.File;
import java.util.Collection;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;

public class SeasonEpisodeMetric
implements SimilarityMetric {
    private SeasonEpisodeMatcher seasonEpisodeMatcher;

    public SeasonEpisodeMetric(SeasonEpisodeMatcher seasonEpisodeMatcher) {
        this.seasonEpisodeMatcher = seasonEpisodeMatcher;
    }

    @Override
    public float getSimilarity(Object object, Object object2) {
        Collection<SeasonEpisodeMatcher.SxE> collection = this.parse(object);
        if (collection == null || collection.isEmpty()) {
            return 0.0f;
        }
        Collection<SeasonEpisodeMatcher.SxE> collection2 = this.parse(object2);
        if (collection2 == null || collection2.isEmpty()) {
            return 0.0f;
        }
        float f = -1.0f;
        for (SeasonEpisodeMatcher.SxE sxE : collection) {
            for (SeasonEpisodeMatcher.SxE sxE2 : collection2) {
                if (sxE.episode == sxE2.episode && sxE.episode >= 0) {
                    if (sxE.season == sxE2.season && sxE.season >= 0) {
                        return 1.0f;
                    }
                    if (sxE.season < 0 && sxE2.season < 0 && (this.isAbsolute(object) || this.isAbsolute(object2))) {
                        return 1.0f;
                    }
                    f = 0.5f;
                    continue;
                }
                if (sxE.season != sxE2.season || sxE.season < 0) continue;
                f = 0.5f;
            }
        }
        return f;
    }

    protected boolean isAbsolute(Object object) {
        if (object instanceof Episode) {
            Episode episode = (Episode)object;
            return EpisodeUtilities.isAbsoluteEpisode(episode);
        }
        return false;
    }

    protected Collection<SeasonEpisodeMatcher.SxE> parse(Object object) {
        if (object instanceof File) {
            return this.seasonEpisodeMatcher.match((File)object);
        }
        return this.seasonEpisodeMatcher.match(object.toString());
    }
}

