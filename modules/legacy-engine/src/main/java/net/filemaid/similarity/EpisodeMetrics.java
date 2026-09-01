package net.filemaid.similarity;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.Language;
import net.filemaid.MemoryCache;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.CrossPropertyMetric;
import net.filemaid.similarity.DateMetric;
import net.filemaid.similarity.FileNameMetric;
import net.filemaid.similarity.FileSizeMetric;
import net.filemaid.similarity.ICU;
import net.filemaid.similarity.MetricCascade;
import net.filemaid.similarity.MetricMin;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.NumericSimilarityMetric;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.similarity.SeasonEpisodeMetric;
import net.filemaid.similarity.SequenceMatchSimilarity;
import net.filemaid.similarity.SeriesNameMatcher;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.similarity.SubstringMetric;
import net.filemaid.similarity.TimeStampMetric;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.vfs.FileInfo;
import net.filemaid.web.DiscDB;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.Movie;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;

public class EpisodeMetrics {
    public final SimilarityMetric SeasonEpisode = new SeasonEpisodeMetric(MediaDetection.getSeasonEpisodePatternMatcher()){
        private final MemoryCache<Object, Collection<SeasonEpisodeMatcher.SxE>> cache;
        {
            this.cache = MemoryCache.forObject();
        }

        @Override
        protected Collection<SeasonEpisodeMatcher.SxE> parse(Object object2) {
            if (object2 instanceof Episode) {
                Episode episode = (Episode)object2;
                return this.parse(episode);
            }
            if (object2 instanceof Movie) {
                return Collections.emptySet();
            }
            return this.cache.get(object2, object -> {
                Collection<SeasonEpisodeMatcher.SxE> collection = super.parse(object);
                return collection == null ? Collections.emptySet() : collection;
            });
        }

        protected Set<SeasonEpisodeMatcher.SxE> parse(Episode episode) {
            HashSet<SeasonEpisodeMatcher.SxE> hashSet = new HashSet<SeasonEpisodeMatcher.SxE>(2);
            if (episode.getEpisode() != null) {
                hashSet.add(new SeasonEpisodeMatcher.SxE(episode.getSeason(), episode.getEpisode()));
                if (episode.getAbsolute() != null && episode.getEpisode() < 10000000) {
                    hashSet.add(new SeasonEpisodeMatcher.SxE(null, episode.getAbsolute()));
                }
            } else if (episode.getSpecial() != null) {
                hashSet.add(new SeasonEpisodeMatcher.SxE(0, episode.getSpecial()));
            }
            return hashSet;
        }

        public String toString() {
            return "SeasonEpisode";
        }
    };
    public final SimilarityMetric AirDate = new DateMetric(MediaDetection.getDateMatcher()){
        private final MemoryCache<Object, Optional<SimpleDate>> cache;
        {
            this.cache = MemoryCache.forObject();
        }

        @Override
        public SimpleDate parse(Object object2) {
            if (object2 instanceof Episode) {
                Episode episode = (Episode)object2;
                return episode.getAirdate();
            }
            if (object2 instanceof Movie) {
                return null;
            }
            return this.cache.get(object2, object -> Optional.ofNullable(super.parse(object))).orElse(null);
        }

        public String toString() {
            return "AirDate";
        }
    };
    public final SimilarityMetric AbsoluteEpisode = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = EpisodeMetrics.this.SeasonEpisode.getSimilarity(this.absolute(object), this.absolute(object2));
            float f2 = 0.8f;
            return f < 0.0f ? 0.0f : (f > f2 ? f2 : f);
        }

        protected Object absolute(Object object) {
            if (object instanceof Episode) {
                Integer n = ((Episode)object).getAbsolute();
                return new Episode(null, null, n, null);
            }
            return object;
        }

        public String toString() {
            return "AbsoluteEpisode";
        }
    };
    public final SimilarityMetric Title = new SubstringMetric(){

        @Override
        protected String normalize(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                if (episode.getTitle() == null) {
                    return null;
                }
                String string = EpisodeMetrics.this.normalizeObject(Normalization.removeTrailingBrackets(episode.getTitle()));
                if (string.length() >= 4 && !EpisodeMetrics.this.normalizeObject(episode.getSeriesName()).contains(string)) {
                    return string;
                }
                return null;
            }
            if (object instanceof Movie) {
                return EpisodeMetrics.this.normalizeObject(((Movie)object).getName());
            }
            String string = EpisodeMetrics.this.normalizeObject(object);
            if (string.length() > 4) {
                return string;
            }
            return null;
        }

        public String toString() {
            return "Title";
        }
    };
    public final SimilarityMetric EpisodeIdentifier = new MetricCascade(this.SeasonEpisode, this.AirDate);
    public final SimilarityMetric EpisodeFunnel = new MetricCascade(this.SeasonEpisode, this.AirDate, this.Title);
    public final SimilarityMetric EpisodeBalancer = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f;
            float f2 = EpisodeMetrics.this.EpisodeIdentifier.getSimilarity(object, object2);
            float f3 = f = f2 < 1.0f ? EpisodeMetrics.this.Title.getSimilarity(object, object2) : 1.0f;
            if (f2 < 0.0f && f == 1.0f && EpisodeMetrics.this.EpisodeIdentifier.getSimilarity(this.getTitle(object), this.getTitle(object2)) == 1.0f) {
                f2 = 1.0f;
                f = 0.0f;
            }
            if (f == 1.0f && EpisodeMetrics.this.SeriesName.getSimilarity(object, object2) < 0.4f) {
                f = 0.0f;
            }
            return (float)((double)(Math.max(f2, 0.0f) * f) + Math.floor(f2) / 10.0);
        }

        public Object getTitle(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                return episode.getSeriesName() + " " + episode.getTitle();
            }
            return object;
        }

        public String toString() {
            return "EpisodeBalancer";
        }
    };
    public final SimilarityMetric SubstringFields = new SubstringMetric(){
        protected static final int MAX_FIELDS = 12;

        @Override
        public float getSimilarity(Object object, Object object2) {
            String[] stringArray = this.normalize(this.fields(object));
            String[] stringArray2 = this.normalize(this.fields(object2));
            double d = 0.0;
            for (int i = 0; i < stringArray.length; ++i) {
                if (stringArray[i].isEmpty()) continue;
                for (int j = 0; j < stringArray2.length; ++j) {
                    float f = super.getSimilarity(stringArray[i], stringArray2[j]);
                    if (!(f > 0.0f)) continue;
                    double d2 = 2.0 - Math.sqrt((double)(i + j) / (double)(stringArray.length + stringArray2.length));
                    d += (double)f * d2;
                }
            }
            return (d /= (double)(stringArray.length * stringArray2.length)) > 0.75 ? 1.0f : (d > 0.05 ? 0.5f : (d > 0.0 ? 0.0f : -1.0f));
        }

        protected String[] normalize(Object[] objectArray) {
            return (String[])Arrays.stream(objectArray).map(EpisodeMetrics.this::normalizeObject).toArray(String[]::new);
        }

        protected Object[] fields(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                SeriesInfo seriesInfo = episode.getSeriesInfo();
                ArrayList<String> arrayList = new ArrayList<String>(12);
                arrayList.add(episode.getSeriesName());
                arrayList.add(episode.getTitle());
                if (seriesInfo != null) {
                    if (seriesInfo.getStartDate() != null) {
                        arrayList.add(String.valueOf(seriesInfo.getStartDate().getYear()));
                    }
                    arrayList.add(seriesInfo.getName());
                    seriesInfo.getAliasNames().stream().filter(string -> StringUtilities.isLatin(string)).sorted(Comparator.comparing(String::length)).forEach(arrayList::add);
                }
                Object[] objectArray = arrayList.stream().filter(Objects::nonNull).map(Normalization::removeTrailingBrackets).filter(string -> !string.isEmpty()).distinct().limit(12L).toArray();
                return Arrays.copyOf(objectArray, 12);
            }
            if (object instanceof File) {
                File file = (File)object;
                ArrayList<File> arrayList = new ArrayList<File>(12);
                arrayList.add(file);
                try {
                    FileUtilities.listPathTailReverse(file.getParentFile()).forEach(arrayList::add);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return arrayList.toArray();
            }
            if (object instanceof Movie) {
                Movie movie = (Movie)object;
                return new Object[]{movie.getName(), movie.getYear()};
            }
            return new Object[]{object};
        }

        public String toString() {
            return "SubstringFields";
        }
    };
    public final SimilarityMetric NameSubstringSequence = new SequenceMatchSimilarity(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            String[] stringArray = this.getNormalizedEffectiveIdentifiers(object);
            String[] stringArray2 = this.getNormalizedEffectiveIdentifiers(object2);
            float f = 0.0f;
            for (String string : stringArray) {
                for (String string2 : stringArray2) {
                    f = Math.max(super.getSimilarity(string, string2), f);
                }
            }
            return (float)(Math.floor(f * 4.0f) / 4.0);
        }

        @Override
        protected String normalize(Object object) {
            return object.toString();
        }

        protected String[] getNormalizedEffectiveIdentifiers(Object object2) {
            return (String[])this.getEffectiveIdentifiers(object2).stream().map(object -> EpisodeMetrics.this.normalizeObject(object)).toArray(String[]::new);
        }

        protected Collection<?> getEffectiveIdentifiers(Object object) {
            if (object instanceof Episode) {
                return ((Episode)object).getSeriesNames();
            }
            if (object instanceof Movie) {
                return ((Movie)object).getEffectiveNames();
            }
            if (object instanceof File) {
                return FileUtilities.listPathTailReverse((File)object, 3);
            }
            return Collections.singleton(object);
        }

        public String toString() {
            return "NameSubstringSequence";
        }
    };
    public final SimilarityMetric Name = new NameSimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            return (float)(Math.floor(super.getSimilarity(object, object2) * 4.0f) / 4.0);
        }

        @Override
        protected String normalize(Object object) {
            return EpisodeMetrics.this.normalizeObject(object);
        }

        public String toString() {
            return "Name";
        }
    };
    public final SimilarityMetric SeriesName = new NameSimilarityMetric(){
        private final SeriesNameMatcher seriesNameMatcher = MediaDetection.getSeriesNameMatcher(false);

        @Override
        public float getSimilarity(Object object, Object object2) {
            String[] stringArray = this.getNormalizedEffectiveIdentifiers(object);
            String[] stringArray2 = this.getNormalizedEffectiveIdentifiers(object2);
            float f = 0.0f;
            for (String string : stringArray) {
                for (String string2 : stringArray2) {
                    f = Math.max(super.getSimilarity(string, string2), f);
                }
            }
            return (float)(Math.floor(f * 4.0f) / 4.0);
        }

        @Override
        protected String normalize(Object object) {
            return object.toString();
        }

        protected String[] getNormalizedEffectiveIdentifiers(Object object) {
            return (String[])this.getEffectiveIdentifiers(object).stream().map(EpisodeMetrics.this::normalizeObject).toArray(String[]::new);
        }

        protected List<?> getEffectiveIdentifiers(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                return MediaDetection.stripReleaseInfo(episode.getSeriesNames(), true);
            }
            if (object instanceof File) {
                File file2 = (File)object;
                return FileUtilities.listPathTailReverse(file2, 3).stream().map(file -> {
                    String string = MediaDetection.stripReleaseInfo(FileUtilities.getName(file), true);
                    String string2 = this.seriesNameMatcher.matchByEpisodeIdentifier(string);
                    return string2 == null ? string : string2;
                }).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        public String toString() {
            return "SeriesName";
        }
    };
    public final SimilarityMetric SeriesNameBalancer = new MetricCascade(this.NameSubstringSequence, this.Name, this.SeriesName);
    public final SimilarityMetric FilePath = new NameSimilarityMetric(){

        @Override
        protected String normalize(Object object) {
            if (object instanceof File) {
                object = FileUtilities.normalizePathSeparators(FileUtilities.getRelativePathTail((File)object, 3).getPath());
            }
            return EpisodeMetrics.this.normalizeObject(object.toString());
        }

        public String toString() {
            return "FilePath";
        }
    };
    public final SimilarityMetric FilePathBalancer = new NameSimilarityMetric(){
        protected static final int MIN_LENGTH = 3;

        @Override
        public float getSimilarity(Object object, Object object2) {
            if (this.isErrorProne(object) || this.isErrorProne(object2)) {
                return -1.0f;
            }
            String string = this.strip(EpisodeMetrics.this.normalizeObject(object));
            String string2 = this.strip(EpisodeMetrics.this.normalizeObject(object2));
            int n = Math.min(string.length(), string2.length());
            if (n < 3) {
                return 0.0f;
            }
            string = string.substring(0, n);
            string2 = string2.substring(0, n);
            return (float)(Math.floor(super.getSimilarity(string, string2) * 4.0f) / 4.0);
        }

        protected String strip(String string) {
            String string2 = MediaDetection.stripReleaseInfo(string, false);
            return string2.length() >= 3 ? string2 : string;
        }

        protected boolean isErrorProne(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                String string = episode.getTitle();
                if (string == null || string.isEmpty()) {
                    return true;
                }
                if (string.startsWith("**") && string.endsWith("**")) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected String normalize(Object object) {
            return object.toString();
        }

        public String toString() {
            return "FilePathBalancer";
        }
    };
    public final SimilarityMetric NumericSequence = new SequenceMatchSimilarity(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = super.getSimilarity(this.normalize(object, true), this.normalize(object2, true));
            float f2 = super.getSimilarity(this.normalize(object, false), this.normalize(object2, false));
            return Math.max(f, f2);
        }

        @Override
        protected String normalize(Object object) {
            return object.toString();
        }

        protected String normalize(Object object, boolean bl) {
            Object object2;
            if (object instanceof Episode) {
                object2 = (Episode)object;
                object = bl ? EpisodeFormat.DEFAULT.formatSxE((Episode)object2) : ((Episode)object2).getSeriesName() + " " + EpisodeFormat.DEFAULT.formatSxE((Episode)object2);
            } else if (object instanceof Movie) {
                object2 = (Movie)object;
                object = bl ? Integer.valueOf(((Movie)object2).getYear()) : ((SearchResult)object2).getName() + " " + ((Movie)object2).getYear();
            }
            List<Integer> integers = StringUtilities.matchIntegers(EpisodeMetrics.this.normalizeObject(object));
            return StringUtilities.join(integers, " ");
        }

        public String toString() {
            return "NumericSequence";
        }
    };
    public final SimilarityMetric Numeric = new NumericSimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            String[] stringArray = this.fields(object);
            String[] stringArray2 = this.fields(object2);
            float f = 0.0f;
            for (String string : stringArray) {
                for (String string2 : stringArray2) {
                    if (string == null || string2 == null || !((f = Math.max(super.getSimilarity(string, string2), f)) >= 1.0f)) continue;
                    return f;
                }
            }
            return f;
        }

        protected String[] fields(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                String[] stringArray = new String[]{episode.getSeriesName(), episode.getSpecial() == null ? EpisodeFormat.DEFAULT.formatSxE(episode) : episode.getSpecial().toString(), episode.getAbsolute() == null ? null : episode.getAbsolute().toString()};
                return stringArray;
            }
            if (object instanceof Movie) {
                Movie movie = (Movie)object;
                return new String[]{movie.getName(), String.valueOf(movie.getYear())};
            }
            return new String[]{EpisodeMetrics.this.normalizeObject(object)};
        }

        public String toString() {
            return "Numeric";
        }
    };
    public final SimilarityMetric LookupDiscDB = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            if (object instanceof Episode && object2 instanceof File) {
                Episode episode = (Episode)object;
                File file = (File)object2;
                SeriesInfo seriesInfo = episode.getSeriesInfo();
                if (DiscDB.canMatch(seriesInfo, file)) {
                    try {
                        Episode episode2 = MediaDetection.releaseInfo.getDiscDB().match(seriesInfo, file);
                        if (episode2 != null) {
                            return EpisodeMetrics.this.SeasonEpisode.getSimilarity(episode, episode2);
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            }
            return 0.0f;
        }

        public String toString() {
            return "LookupDiscDB";
        }
    };
    public final SimilarityMetric SpecialNumber = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            return this.getSpecialFactor(object) + this.getSpecialFactor(object2);
        }

        public int getSpecialFactor(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                return episode.getSpecial() != null ? -1 : 1;
            }
            return 0;
        }

        public String toString() {
            return "SpecialNumber";
        }
    };
    public final SimilarityMetric FileSize = new FileSizeMetric(){

        @Override
        protected long getLength(Object object) {
            if (object instanceof FileInfo) {
                return ((FileInfo)object).getLength();
            }
            return super.getLength(object);
        }

        public String toString() {
            return "FileSize";
        }
    };
    public final SimilarityMetric FileName = new FileNameMetric(){

        @Override
        protected String getFileName(Object object) {
            if (object instanceof File || object instanceof FileInfo) {
                return EpisodeMetrics.this.normalizeObject(object);
            }
            return null;
        }

        public String toString() {
            return "FileName";
        }
    };
    public final TimeStampMetric CreationTime = new TimeStampMetric(10, ChronoUnit.YEARS){
        private final MemoryCache<File, Long> cache;
        {
            this.cache = MemoryCache.forObject();
        }

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = super.getSimilarity(object, object2);
            return (double)f >= 0.75 ? 1.0f : (f >= 0.0f ? 0.0f : -1.0f);
        }

        private long getTimeStamp(SimpleDate simpleDate) {
            Instant instant;
            if (simpleDate != null && (instant = simpleDate.toInstant()).isBefore(Instant.now())) {
                return instant.toEpochMilli();
            }
            return -1L;
        }

        private long getTimeStamp(File file) {
            if (CachedMediaCharacteristics.getMediaCharacteristicsParser().canRead()) {
                return this.cache.get(file, file2 -> CachedMediaCharacteristics.getMediaCharacteristics(file, MediaCharacteristics::getCreationTime).map(Instant::toEpochMilli).orElseGet(() -> super.getTimeStamp(file)));
            }
            return -1L;
        }

        @Override
        public long getTimeStamp(Object object) {
            if (object instanceof Episode) {
                Episode episode = (Episode)object;
                if (episode.getEpisode() == null || episode.getEpisode() < 10000000) {
                    return this.getTimeStamp(episode.getAirdate());
                }
            } else {
                if (object instanceof Movie) {
                    Movie movie = (Movie)object;
                    return this.getTimeStamp(new SimpleDate(movie.getYear()));
                }
                if (object instanceof File) {
                    File file = (File)object;
                    return this.getTimeStamp(file);
                }
            }
            return -1L;
        }

        public String toString() {
            return "CreationTime";
        }
    };
    public final SimilarityMetric RecentlyAired = new TimeStampMetric(5, ChronoUnit.DAYS){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = super.getSimilarity(object, object2);
            return f > 0.0f ? 1.0f : 0.0f;
        }

        @Override
        public long getTimeStamp(Object object) {
            if (object instanceof Episode || object instanceof File) {
                return EpisodeMetrics.this.CreationTime.getTimeStamp(object);
            }
            return -1L;
        }

        public String toString() {
            return "RecentlyAired";
        }
    };
    public final SimilarityMetric SeriesRating = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = this.getScore(object);
            float f2 = this.getScore(object2);
            if (f < 0.0f || f2 < 0.0f) {
                return -1.0f;
            }
            return Math.max(f, f2);
        }

        public float getScore(Object object) {
            SeriesInfo seriesInfo;
            if (object instanceof Episode && (seriesInfo = ((Episode)object).getSeriesInfo()) != null) {
                if (seriesInfo.getRatingCount() == null || seriesInfo.getRatingCount() <= 0) {
                    return -1.0f;
                }
                Language language = Language.getLanguage(seriesInfo.getLanguage());
                if (language != null) {
                    if (seriesInfo.getSpokenLanguages().stream().anyMatch(language::matches)) {
                        return 1.0f;
                    }
                }
            }
            return 0.0f;
        }

        public String toString() {
            return "SeriesRating";
        }
    };
    public final SimilarityMetric VoteRate = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            float f = Math.max(this.getScore(object), this.getScore(object2));
            if (f > 1.0f) {
                return 2.0f;
            }
            if ((double)f > 0.1) {
                return 1.0f;
            }
            return 0.0f;
        }

        public float getScore(Object object) {
            long l;
            SeriesInfo seriesInfo;
            if (object instanceof Episode && (seriesInfo = ((Episode)object).getSeriesInfo()) != null && seriesInfo.getRating() != null && seriesInfo.getRatingCount() != null && seriesInfo.getStartDate() != null && (l = ChronoUnit.DAYS.between(seriesInfo.getStartDate().toLocalDate(), LocalDate.now())) > 0L) {
                return seriesInfo.getRatingCount().floatValue() / (float)l * seriesInfo.getRating().floatValue();
            }
            return 0.0f;
        }

        public String toString() {
            return "VoteRate";
        }
    };
    public final SimilarityMetric RegionHint = new SimilarityMetric(){
        private final Pattern hint = Pattern.compile("[(]\\w+[)]$|(?<!^[ A-Z]+)[ ][A-Z]{2}$");
        private final SeriesNameMatcher seriesNameMatcher = MediaDetection.getSeriesNameMatcher(true);

        @Override
        public float getSimilarity(Object object, Object object2) {
            Set<String> set = this.getHint(object);
            if (set.isEmpty()) {
                return 0.0f;
            }
            Set<String> set2 = this.getHint(object2);
            if (set2.isEmpty()) {
                return 0.0f;
            }
            return set.containsAll(set2) || set2.containsAll(set) ? 1.0f : 0.0f;
        }

        public Set<String> getHint(Object object) {
            if (object instanceof Episode) {
                for (String string2 : ((Episode)object).getSeriesNames()) {
                    Matcher matcher = this.hint.matcher(string2);
                    if (!matcher.find()) continue;
                    return Normalization.PUNCTUATION_OR_SPACE.splitAsStream(matcher.group()).filter(string -> !string.isEmpty()).map(String::toLowerCase).collect(Collectors.toSet());
                }
            } else if (object instanceof File) {
                return FileUtilities.listPathTailReverse((File)object, 3).stream().map(file -> {
                    String string = FileUtilities.getName(file);
                    String string2 = this.seriesNameMatcher.matchByEpisodeIdentifier(string);
                    return string2 == null ? string : string2;
                }).flatMap(Normalization.PUNCTUATION_OR_SPACE::splitAsStream).filter(string -> !string.isEmpty()).map(String::toLowerCase).collect(Collectors.toSet());
            }
            return Collections.emptySet();
        }

        public String toString() {
            return "RegionHint";
        }
    };
    public final SimilarityMetric MetaAttributes = new CrossPropertyMetric(){

        @Override
        protected Map<String, Object> getProperties(Object object) {
            if (object instanceof Episode || object instanceof Movie) {
                return super.getProperties(object);
            }
            if (object instanceof File) {
                Object object2 = XattrMetaInfo.xattr.getMetaInfo((File)object);
                return super.getProperties(object2);
            }
            return Collections.emptyMap();
        }

        public String toString() {
            return "MetaAttributes";
        }
    };
    public final SimilarityMetric MediaTags = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            Object object3 = this.getMetaObject(object);
            if (object3 == null) {
                return 0.0f;
            }
            Object object4 = this.getMetaObject(object2);
            if (object4 == null) {
                return 0.0f;
            }
            return EpisodeMetrics.this.EpisodeFunnel.getSimilarity(object3, object4);
        }

        private Object getMetaObject(Object object) {
            if (object instanceof Episode) {
                return object;
            }
            if (object instanceof File) {
                return CachedMediaCharacteristics.getMediaCharacteristics((File)object, MediaCharacteristics::getMediaTags).orElse(null);
            }
            return null;
        }

        public String toString() {
            return "MediaTags";
        }
    };
    public final SimilarityMetric Runtime = new SimilarityMetric(){

        @Override
        public float getSimilarity(Object object, Object object2) {
            Integer n = this.getRuntime(object);
            if (n == null) {
                return 0.0f;
            }
            Integer n2 = this.getRuntime(object2);
            if (n2 == null) {
                return 0.0f;
            }
            int n3 = Math.abs(n - n2);
            if (n3 == 0) {
                return 2.0f;
            }
            if (n3 <= 2) {
                return 1.0f;
            }
            if (n3 >= 20) {
                return -1.0f;
            }
            return 0.0f;
        }

        private Integer getRuntime(Object object) {
            if (object instanceof Episode) {
                return ((Episode)object).getRuntime();
            }
            if (object instanceof File) {
                return CachedMediaCharacteristics.getMediaCharacteristics((File)object, MediaCharacteristics::getDuration).map(duration -> (int)Math.round((double)duration.toMillis() / 60000.0)).orElse(null);
            }
            return null;
        }

        public String toString() {
            return "Runtime";
        }
    };
    protected final MemoryCache<Object, String> transformCache = MemoryCache.forObject();

    protected String normalizeObject(Object object2) {
        if (object2 == null) {
            return "";
        }
        return this.transformCache.get(object2, object -> Normalization.normalizePunctuation(ICU.ASCII.transform(MediaDetection.stripFormatInfo(this.normalizeFileName(object)))).toLowerCase(Locale.ROOT));
    }

    protected String normalizeFileName(Object object) {
        if (object instanceof File) {
            return FileUtilities.getName((File)object);
        }
        if (object instanceof FileInfo) {
            return ((FileInfo)object).getName();
        }
        return object.toString();
    }

    public SimilarityMetric[] matchSequence() {
        return new SimilarityMetric[]{this.EpisodeFunnel, this.EpisodeBalancer, this.AirDate, this.MetaAttributes, this.LookupDiscDB, this.SubstringFields, this.SeriesNameBalancer, this.SeriesName, this.RegionHint, this.SpecialNumber, new MetricCascade(this.Numeric, this.RecentlyAired), this.SeriesRating, new MetricCascade(this.NumericSequence, this.Runtime), this.VoteRate, this.MediaTags, new MetricCascade(this.Title, this.CreationTime), this.RecentlyAired, this.Runtime, this.FilePathBalancer, this.FilePath};
    }

    public SimilarityMetric[] matchFileSequence() {
        return new SimilarityMetric[]{this.FileSize, new MetricCascade(this.FileName, this.EpisodeFunnel), this.EpisodeBalancer, this.AirDate, this.MetaAttributes, this.LookupDiscDB, this.SubstringFields, this.SeriesNameBalancer, this.SeriesName, this.RegionHint, this.SpecialNumber, new MetricCascade(this.Numeric, this.RecentlyAired), this.SeriesRating, new MetricCascade(this.NumericSequence, this.Runtime), this.VoteRate, this.MediaTags, new MetricCascade(this.Title, this.CreationTime), this.RecentlyAired, this.Runtime, this.FilePathBalancer, this.FilePath};
    }

    public SimilarityMetric numbers() {
        return this.EpisodeIdentifier;
    }

    public SimilarityMetric verification() {
        return new MetricCascade(this.FileName, this.SeasonEpisode, this.AirDate, this.AbsoluteEpisode, this.Title, this.Name);
    }

    public SimilarityMetric sanity() {
        return new MetricCascade(new MetricMin(this.FileSize, 0.0f), this.FileName, this.EpisodeIdentifier);
    }
}

