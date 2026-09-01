package net.filemaid.media;

import java.io.File;
import java.util.List;
import net.filemaid.media.MediaDetection;
import net.filemaid.similarity.SeasonEpisodeMatcher;

public class SmartSeasonEpisodeMatcher
extends SeasonEpisodeMatcher {
    public SmartSeasonEpisodeMatcher(SeasonEpisodeMatcher.SeasonEpisodeFilter seasonEpisodeFilter, boolean bl) {
        super(seasonEpisodeFilter, bl);
    }

    public SmartSeasonEpisodeMatcher(int n, SeasonEpisodeMatcher.SeasonEpisodeFilter seasonEpisodeFilter, boolean bl) {
        super(n, seasonEpisodeFilter, bl);
    }

    private String clean(String string, boolean bl) {
        if (bl) {
            string = MediaDetection.stripBatchInfo(string);
        }
        return MediaDetection.stripFormatInfo(string);
    }

    @Override
    public List<SeasonEpisodeMatcher.SxE> match(CharSequence charSequence) {
        return super.match(this.clean(charSequence.toString(), false));
    }

    @Override
    public String head(String string) {
        return super.head(this.clean(string, false));
    }

    @Override
    protected String[] tokenizeTail(File file) {
        String[] stringArray = super.tokenizeTail(file);
        for (int i = 0; i < stringArray.length; ++i) {
            stringArray[i] = this.clean(stringArray[i], i != 0);
        }
        return stringArray;
    }
}

