package net.filemaid.subtitle;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.Logging;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.CrossPropertyMetric;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.MetricAvg;
import net.filemaid.similarity.MetricCascade;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.NumericSimilarityMetric;
import net.filemaid.similarity.SequenceMatchSimilarity;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.OpenSubtitlesRestSubtitleDescriptor;
import net.filemaid.web.OpenSubtitlesXmlRpcSubtitleDescriptor;
import net.filemaid.web.SubtitleDescriptor;

public class SubtitleMetrics
extends EpisodeMetrics {
    public final SimilarityMetric AbsoluteSeasonEpisode = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = SubtitleMetrics.this.SeasonEpisode.getSimilarity(object, object2);
            if (f == 0.0f && MediaDetection.getEpisodeIdentifier(object.toString(), true) == null == (MediaDetection.getEpisodeIdentifier(object2.toString(), true) == null)) {
                return 0.0f;
            }
            return f < 1.0f ? -1.0f : 1.0f;
        }

        public String toString() {
            return "AbsoluteSeasonEpisode";
        }
    };
    public final SimilarityMetric DiskNumber = new NumericSimilarityMetric(){
        private final Pattern CDNO = Pattern.compile("(?:CD|DISK)(\\d+)", 2);

        @Override
        public float getSimilarity(Object object, Object object2) {
            int n = this.getDiskNumber(object);
            int n2 = this.getDiskNumber(object2);
            if (n == 0 && n2 == 0) {
                return 0.0f;
            }
            return n == n2 ? 1.0f : -1.0f;
        }

        public int getDiskNumber(Object object) {
            int n = 0;
            Matcher matcher = this.CDNO.matcher(object.toString());
            while (matcher.find()) {
                n = Integer.parseInt(matcher.group(1));
            }
            return n;
        }

        public String toString() {
            return "DiskNumber";
        }
    };
    public final SimilarityMetric NameSubstringSequenceExists = new SequenceMatchSimilarity(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            String[] stringArray = this.getNormalizedEffectiveIdentifiers(object);
            String[] stringArray2 = this.getNormalizedEffectiveIdentifiers(object2);
            for (String string : stringArray) {
                for (String string2 : stringArray2) {
                    if (!(super.getSimilarity(string, string2) >= 1.0f)) continue;
                    return 1.0f;
                }
            }
            return 0.0f;
        }

        @Override
        protected float similarity(String string, String string2, String string3) {
            return string.length() > 0 ? 1.0f : 0.0f;
        }

        @Override
        protected String normalize(Object object) {
            return object.toString();
        }

        protected String[] getNormalizedEffectiveIdentifiers(Object object) {
            List<?> list = this.getEffectiveIdentifiers(object);
            String[] stringArray = new String[list.size()];
            for (int i = 0; i < stringArray.length; ++i) {
                stringArray[i] = SubtitleMetrics.this.normalizeObject(list.get(i));
            }
            return stringArray;
        }

        protected List<?> getEffectiveIdentifiers(Object object) {
            if (object instanceof OpenSubtitlesXmlRpcSubtitleDescriptor) {
                return Collections.singletonList(((OpenSubtitlesXmlRpcSubtitleDescriptor)object).getName());
            }
            if (object instanceof OpenSubtitlesRestSubtitleDescriptor) {
                return Collections.singletonList(((OpenSubtitlesRestSubtitleDescriptor)object).getName());
            }
            if (object instanceof File) {
                return FileUtilities.listPathTailReverse((File)object, 2);
            }
            return Collections.emptyList();
        }

        public String toString() {
            return "NameSubstringSequenceExists";
        }
    };
    public final SimilarityMetric OriginalFileName = new SequenceMatchSimilarity(){

        @Override
        protected float similarity(String string, String string2, String string3) {
            return (double)((float)string.length() / (float)Math.max(string2.length(), string3.length())) > 0.8 ? 1.0f : 0.0f;
        }

        @Override
        public String normalize(Object object) {
            if (object instanceof File) {
                File file = (File)object;
                String string = XattrMetaInfo.xattr.getOriginalName(file);
                if (string == null) {
                    string = file.getName();
                }
                return super.normalize(FileUtilities.getNameWithoutExtension(string));
            }
            if (object instanceof OpenSubtitlesXmlRpcSubtitleDescriptor) {
                String string = ((OpenSubtitlesXmlRpcSubtitleDescriptor)object).getName();
                return super.normalize(string);
            }
            if (object instanceof OpenSubtitlesRestSubtitleDescriptor) {
                String string = ((OpenSubtitlesRestSubtitleDescriptor)object).getName();
                return super.normalize(string);
            }
            return super.normalize(object);
        }

        public String toString() {
            return "OriginalFileName";
        }
    };
    public final SimilarityMetric VideoProperties = new CrossPropertyMetric(){
        private final String FPS = "FPS";
        private final String SECONDS = "SECS";

        @Override
        public float getSimilarity(Object object, Object object2) {
            return object instanceof SubtitleDescriptor ? super.getSimilarity(object, object2) : super.getSimilarity(object2, object);
        }

        @Override
        protected Map<String, Object> getProperties(Object object) {
            if (object instanceof OpenSubtitlesXmlRpcSubtitleDescriptor) {
                return this.getSubtitleProperties((OpenSubtitlesXmlRpcSubtitleDescriptor)object);
            }
            if (object instanceof OpenSubtitlesRestSubtitleDescriptor) {
                return this.getSubtitleProperties((OpenSubtitlesRestSubtitleDescriptor)object);
            }
            if (object instanceof File) {
                return this.getVideoProperties((File)object);
            }
            return Collections.emptyMap();
        }

        private Map<String, Object> getProperties(double d, long l) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>(2);
            if (d > 0.0) {
                hashMap.put("FPS", Math.round(d));
            }
            if (l > 0L) {
                hashMap.put("SECS", Math.round(Math.floor((double)l / 1000.0)));
            }
            return hashMap;
        }

        private Map<String, Object> getSubtitleProperties(OpenSubtitlesXmlRpcSubtitleDescriptor openSubtitlesXmlRpcSubtitleDescriptor) {
            try {
                return this.getProperties(openSubtitlesXmlRpcSubtitleDescriptor.getMovieFPS(), openSubtitlesXmlRpcSubtitleDescriptor.getMovieTimeMS());
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to read subtitle properties", exception));
                return Collections.emptyMap();
            }
        }

        private Map<String, Object> getSubtitleProperties(OpenSubtitlesRestSubtitleDescriptor openSubtitlesRestSubtitleDescriptor) {
            try {
                return this.getProperties(openSubtitlesRestSubtitleDescriptor.getFPS(), 0L);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to read subtitle properties", exception));
                return Collections.emptyMap();
            }
        }

        private Map<String, Object> getVideoProperties(File file) {
            return CachedMediaCharacteristics.getMediaCharacteristics(file, mediaCharacteristics -> {
                double d = Optional.of(mediaCharacteristics).map(MediaCharacteristics::getFrameRate).orElse(0.0);
                long l = Optional.of(mediaCharacteristics).map(MediaCharacteristics::getDuration).map(Duration::toMillis).orElse(0L);
                return this.getProperties(d, l);
            }).orElse(Collections.emptyMap());
        }

        public String toString() {
            return "VideoProperties";
        }
    };
    public final SimilarityMetric FileNameSimilarity = new NameSimilarityMetric(){

        @Override
        protected String normalize(Object object) {
            return SubtitleMetrics.this.normalizeFileName(object);
        }

        public String toString() {
            return "FileNameSimilarity";
        }
    };

    @Override
    public SimilarityMetric[] matchSequence() {
        return new SimilarityMetric[]{this.EpisodeFunnel, this.EpisodeBalancer, this.OriginalFileName, this.NameSubstringSequenceExists, new MetricAvg(this.NameSubstringSequenceExists, this.Name), this.Numeric, this.FileName, this.DiskNumber, this.VideoProperties, this.FileNameSimilarity};
    }

    @Override
    public SimilarityMetric[] matchFileSequence() {
        return this.matchSequence();
    }

    @Override
    public SimilarityMetric verification() {
        return new MetricCascade(this.AbsoluteSeasonEpisode, this.AirDate, new MetricAvg(this.NameSubstringSequenceExists, this.Name), MediaDetection.getMovieMatchMetric(), this.OriginalFileName);
    }

    @Override
    public SimilarityMetric sanity() {
        return this.verification();
    }
}

