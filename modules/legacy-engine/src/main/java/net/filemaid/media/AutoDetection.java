package net.filemaid.media;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Parallelism;
import net.filemaid.WebServices;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.ICU;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.Series;

public class AutoDetection {
    private File[] files;
    private Locale locale;
    private static final Pattern MOVIE_FOLDER_PATTERN = Pattern.compile("(?:Movie|Film)[s]?", 2);
    private static final Pattern SERIES_FOLDER_PATTERN = Pattern.compile("TV.Shows|TV.Series|Series|Season.\\d+|Documentary|Documentaries", 2);
    private static final Pattern ANIME_FOLDER_PATTERN = Pattern.compile("Anime", 2);
    private static final Pattern ABSOLUTE_EPISODE_PATTERN = Pattern.compile("(?<!\\p{Alnum})E[P]?\\d{1,3}(?!\\p{Alnum})", 2);
    private static final Pattern SERIES_EPISODE_PATTERN = Pattern.compile("(?<!\\p{Alnum})(?:tv[sp]|Season\\D?\\d{1,2}|\\d{4}.S\\d{2})(?!\\p{Alnum})", 2);
    private static final Pattern ANIME_EPISODE_NAME_PATTERN = Pattern.compile("^\\[[^\\]]*Subs[^\\]]*\\]", 2);
    private static final Pattern ANIME_EPISODE_PATH_PATTERN = Pattern.compile("(?<!\\p{Alnum})(?:anidb-\\d+)(?!\\p{Alnum})", 2);
    private static final Pattern ANIME_S2_EE_PATTERN = Pattern.compile("(?<!\\p{Digit})(?:S|s|Season|Series)[._ ]?(\\d{1,4})(?:[._ ]-[._ ])(\\d{1,4}(?:[-]\\d{1,4})*)(?:\\D|$)");
    private static final Pattern MOVIE_KIND_PATTERN = Pattern.compile("(?<!\\p{Alnum})(?:imdb-tt\\d{7,11}|UFC)(?!\\p{Alnum})", 2);
    private static final Pattern JAPANESE_AUDIO_LANGUAGE_PATTERN = Pattern.compile("jpn|Japanese", 2);
    private static final Pattern JAPANESE_SUBTITLE_CODEC_PATTERN = Pattern.compile("ASS|SSA", 2);
    private static final Pattern YEAR = Pattern.compile("\\D(?:19|20)\\d{2}\\D");
    private static final Pattern EPISODE_NUMBERS = Pattern.compile("\\b\\d{1,3}\\b");
    private static final Pattern DASH = Pattern.compile("^.{0,3}\\s[-]\\s.+$", 256);
    private static final Pattern NUMBER_PAIR = Pattern.compile("\\D\\d{1,2}\\D{1,3}\\d{1,2}\\D");
    private static final Pattern YEAR_AND_NUMBER = Pattern.compile("\\b(?:19|20)\\d{2}\\D\\d{2,3}\\b");
    private static final Pattern NON_NUMBER_NAME = Pattern.compile("^[\\p{L}\\p{Space}\\p{Punct}]+$", 256);

    public static Map<Group, List<File>> group(Collection<File> collection, Locale locale) throws Exception {
        if (Parallelism.THREAD_POOL_SIZE.min() > 1) {
            return new AutoDetection(collection, locale).group(Parallelism.commonPool());
        }
        return new AutoDetection(collection, locale).group();
    }

    public static Map<Group, List<File>> groupParallel(Collection<File> collection, Locale locale) throws Exception {
        return new AutoDetection(collection, locale).group(Parallelism.commonPool());
    }

    @Deprecated
    public AutoDetection(Collection<File> collection, boolean bl, Locale locale) {
        this(collection, locale);
    }

    public AutoDetection(Collection<File> collection, Locale locale) {
        this.locale = locale;
        this.files = collection.toArray(new File[0]);
    }

    public List<File> getFiles() {
        return Collections.unmodifiableList(Arrays.asList(this.files));
    }

    public boolean isMusic(File file) {
        return MediaTypes.AUDIO_FILES.accept(file) && !MediaTypes.VIDEO_FILES.accept(file);
    }

    public boolean isMovie(File file) {
        return this.anyMatch(file.getParentFile(), MOVIE_FOLDER_PATTERN) || StringUtilities.find(file.getName(), MOVIE_KIND_PATTERN) || MediaDetection.isMovie(file);
    }

    public boolean isEpisode(File file) {
        if (this.anyMatch(file.getParentFile(), SERIES_FOLDER_PATTERN) || StringUtilities.find(file.getPath(), SERIES_EPISODE_PATTERN)) {
            return true;
        }
        if (MediaDetection.isEpisode(file.getName(), true)) {
            return !StringUtilities.find(file.getName(), ANIME_S2_EE_PATTERN) || !this.isAnime(file);
        }
        try {
            return MediaFileUtilities.listStructurePathTail(file.getParentFile()).stream().anyMatch(string -> MediaDetection.isEpisode(string, true));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Ignore parent folder", file, exception));
            Object object = XattrMetaInfo.xattr.getMetaInfo(file);
            if (object instanceof Episode) {
                return !EpisodeUtilities.isAnimeType(object);
            }
            return false;
        }
    }

    public boolean isAnime(File file) {
        if (!this.findEpisodeNumbers(file.getName(), false)) {
            return false;
        }
        if (StringUtilities.find(file.getName(), Normalization.EMBEDDED_CHECKSUM) || StringUtilities.find(file.getName(), ANIME_EPISODE_NAME_PATTERN) || StringUtilities.find(file.getPath(), ANIME_EPISODE_PATH_PATTERN) || this.anyMatch(file.getParentFile(), ANIME_FOLDER_PATTERN)) {
            return true;
        }
        return CachedMediaCharacteristics.getMediaCharacteristics(file, mediaCharacteristics -> mediaCharacteristics.getDuration().toMinutes() < 60L && StringUtilities.find(mediaCharacteristics.getAudioLanguage(), JAPANESE_AUDIO_LANGUAGE_PATTERN) && StringUtilities.find(mediaCharacteristics.getSubtitleCodec(), JAPANESE_SUBTITLE_CODEC_PATTERN)).orElseGet(() -> EpisodeUtilities.isAnimeType(XattrMetaInfo.xattr.getMetaInfo(file)));
    }

    private boolean findEpisodeNumbers(String string, boolean bl) {
        List<SeasonEpisodeMatcher.SxE> list = MediaDetection.parseEpisodeNumber(string, bl);
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (bl) {
            return !list.isEmpty();
        }
        if (list.stream().anyMatch(sxE -> sxE.season >= 0 ? sxE.season < 19 || sxE.season > 20 : sxE.episode < 1920 || sxE.episode > 2040)) {
            return true;
        }
        return this.findEpisodeNumbers(string, true);
    }

    public boolean anyMatch(File file, Pattern pattern) {
        for (File file2 = file; file2 != null && !MediaFileUtilities.isVolumeRoot(file2); file2 = file2.getParentFile()) {
            if (!pattern.matcher(file2.getName()).matches()) continue;
            return true;
        }
        return false;
    }

    public Map<Group, List<File>> group() {
        LinkedHashMap<Group, List<File>> linkedHashMap = new LinkedHashMap<Group, List<File>>();
        for (File file : this.files) {
            Group group2 = this.detectGroup(file);
            linkedHashMap.computeIfAbsent(group2, group -> new ArrayList()).add(file);
        }
        return linkedHashMap;
    }

    public Map<Group, List<File>> group(Parallelism parallelism) throws Exception {
        return parallelism.group(Arrays.asList(this.files), this::detectGroup);
    }

    public Group detectGroup(File file) {
        try {
            Group group = new Group();
            if (this.isMusic(file)) {
                return group.music(file);
            }
            if (this.isMovie(file)) {
                return group.movie(this.getMovieMatch(file));
            }
            if (this.isEpisode(file)) {
                return group.series(this.getSeriesMatch(file, false));
            }
            if (this.isAnime(file)) {
                return group.anime(this.getSeriesMatch(file, true));
            }
            if (StringUtilities.find(file.getName(), ABSOLUTE_EPISODE_PATTERN)) {
                return group.series(this.getSeriesMatch(file, false));
            }
            Movie movie = this.getMovieMatch(file);
            Series series = this.getSeriesMatch(file, false);
            if (series == null && movie == null) {
                return group;
            }
            if (series != null && movie == null) {
                return group.series(series);
            }
            if (series == null && movie != null) {
                return group.movie(movie);
            }
            return new Rules(file, series, movie).apply();
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause("AutoDetection", file, exception));
            return Group.None;
        }
    }

    private Series getSeriesMatch(File file, boolean bl) throws Exception {
        List<File> list;
        Set<Series> set = MediaDetection.detectSeries(Collections.singleton(file), bl, this.locale);
        if (set.isEmpty() && (list = this.getVideoFiles(file.getParentFile())).size() >= 5) {
            set = MediaDetection.detectSeries(list, bl, this.locale);
        }
        return set.stream().findFirst().orElse(null);
    }

    private Movie getMovieMatch(File file) throws Exception {
        return MediaDetection.detectMovie(file, WebServices.TheMovieDB, this.locale, false, false).stream().findFirst().orElse(null);
    }

    private List<File> getVideoFiles(File file) {
        return Arrays.stream(this.files).filter(file2 -> file.equals(file2.getParentFile())).filter(MediaTypes.VIDEO_FILES::accept).collect(Collectors.toList());
    }

    public static class Group {
        private final Map<Type, Object> hints;
        public static final Group Movie = new Group(Type.Movie, Boolean.TRUE);
        public static final Group Series = new Group(Type.Series, Boolean.TRUE);
        public static final Group Anime = new Group(Type.Anime, Boolean.TRUE);
        public static final Group Music = new Group(Type.Music, Boolean.TRUE);
        public static final Group None = new Group(null, Boolean.TRUE);

        public Group() {
            this.hints = new EnumMap<Type, Object>(Type.class);
        }

        private Group(Type type, Object object) {
            this.hints = type == null ? Collections.emptyMap() : Collections.unmodifiableMap(Collections.singletonMap(type, object));
        }

        public Group movie(Movie movie) {
            this.hints.put(Type.Movie, movie);
            return this;
        }

        public Group series(Series series) {
            this.hints.put(Type.Series, series);
            return this;
        }

        public Group anime(Series series) {
            this.hints.put(Type.Anime, series);
            return this;
        }

        public Group music(File file) {
            this.hints.put(Type.Music, file == null ? null : file.getParent());
            return this;
        }

        public Object getMovie() {
            return this.hints.get((Object)Type.Movie);
        }

        public Object getSeries() {
            return this.hints.get((Object)Type.Series);
        }

        public Object getAnime() {
            return this.hints.get((Object)Type.Anime);
        }

        public Object getMusic() {
            return this.hints.get((Object)Type.Music);
        }

        public Group setMovie() {
            this.hints.put(Type.Movie, Boolean.TRUE);
            return this;
        }

        public Group setSeries() {
            this.hints.put(Type.Series, Boolean.TRUE);
            return this;
        }

        public Group setAnime() {
            this.hints.put(Type.Anime, Boolean.TRUE);
            return this;
        }

        public Group setMusic() {
            this.hints.put(Type.Music, Boolean.TRUE);
            return this;
        }

        public boolean isMovie() {
            return this.hints.get((Object)Type.Movie) != null;
        }

        public boolean isSeries() {
            return this.hints.get((Object)Type.Series) != null;
        }

        public boolean isAnime() {
            return this.hints.get((Object)Type.Anime) != null;
        }

        public boolean isMusic() {
            return this.hints.get((Object)Type.Music) != null;
        }

        public Type[] types() {
            return (Type[])this.hints.entrySet().stream().filter(entry -> entry.getValue() != null).map(entry -> (Type)((Object)((Object)entry.getKey()))).toArray(Type[]::new);
        }

        public boolean equals(Object object) {
            if (object instanceof Group) {
                Group group = (Group)object;
                return this.hints.equals(group.hints);
            }
            return false;
        }

        public int hashCode() {
            return this.hints.hashCode();
        }

        public String toString() {
            return this.hints.toString();
        }
    }

    private class Rules {
        private final Group group;
        private final File f;
        private final Series s;
        private final Movie m;
        private final String dn;
        private final String fn;
        private final String sn;
        private final String mn;
        private final String asn;
        private final Pattern snm;
        private final Pattern mnm;
        private final Pattern mym;

        public Rules(File file, Series series, Movie movie) throws Exception {
            this.group = new Group().series(series).movie(movie);
            this.f = file;
            this.s = series;
            this.m = movie;
            this.dn = this.normalize(MediaDetection.guessMovieFolder(this.f));
            this.fn = this.normalize(this.f);
            this.sn = this.normalize(this.s.getName());
            this.mn = this.normalize(this.m.getName());
            this.snm = Pattern.compile(this.sn, 16);
            this.mnm = Pattern.compile(this.mn, 16);
            this.mym = Pattern.compile(IntStream.concat(IntStream.of(this.m.getYear()), IntStream.of(this.m.getYear() - 1, this.m.getYear() + 1).filter(n -> n != series.getYear())).mapToObj(Integer::toString).collect(Collectors.joining("|")));
            this.asn = StringUtilities.after(this.fn, this.snm).orElse(this.fn);
        }

        private String normalize(File file) {
            return file == null ? "" : this.normalize(MediaDetection.stripFormatInfo(Normalization.replaceSpace(FileUtilities.getName(file), " ")));
        }

        private String normalize(String string) {
            return string == null ? "" : Normalization.normalizePunctuation(ICU.ASCII.transform(string)).toLowerCase();
        }

        private float getSimilarity(String string, String string2) {
            return new NameSimilarityMetric().getSimilarity(string, string2);
        }

        private boolean matchMovie(String string) {
            return StringUtilities.find(string, YEAR) && !StringUtilities.find(string, YEAR_AND_NUMBER) && !MediaDetection.matchMovieName(Collections.singleton(string), false, 0).isEmpty();
        }

        public Group apply() throws Exception {
            ArrayList<Rule> arrayList = new ArrayList<Rule>(15);
            arrayList.add(new Rule(-1, 0, this::equalsMovieName, "AutoDetection::equalsMovieName"));
            arrayList.add(new Rule(-1, 0, this::containsMovieYear, "AutoDetection::containsMovieYear"));
            arrayList.add(new Rule(-1, 0, this::containsMovieNameYear, "AutoDetection::containsMovieNameYear"));
            arrayList.add(new Rule(5, -1, this::containsEpisodeNumbers, "AutoDetection::containsEpisodeNumbers"));
            arrayList.add(new Rule(5, -1, this::commonNumberPattern, "AutoDetection::commonNumberPattern"));
            arrayList.add(new Rule(1, -1, this::episodeWithoutNumbers, "AutoDetection::episodeWithoutNumbers"));
            arrayList.add(new Rule(1, -1, this::episodeNumbers, "AutoDetection::episodeNumbers"));
            arrayList.add(new Rule(-1, 1, this::hasImdbId, "AutoDetection::hasImdbId"));
            arrayList.add(new Rule(-1, 1, this::nonNumberName, "AutoDetection::nonNumberName"));
            arrayList.add(new Rule(-1, 5, this::exactMovieMatch, "AutoDetection::exactMovieMatch"));
            arrayList.add(new Rule(-1, 1, this::containsMovieName, "AutoDetection::containsMovieName"));
            arrayList.add(new Rule(-1, 1, this::similarNameYear, "AutoDetection::similarNameYear"));
            arrayList.add(new Rule(-1, 1, this::similarNameNoNumbers, "AutoDetection::similarNameNoNumbers"));
            arrayList.add(new Rule(-1, 1, this::aliasNameMatch, "AutoDetection::aliasNameMatch"));
            int n = 0;
            int n2 = 0;
            for (Rule rule : arrayList) {
                if (rule.test()) {
                    Logging.debug.finest(Logging.format("[+] %s", rule));
                    if ((n += rule.s) >= 1 && (n2 += rule.m) <= -1) {
                        Logging.debug.fine(Logging.format("[X] Rule as Series", n, n2));
                        return this.group.movie(null);
                    }
                    if (n2 < 1 || n > -1) continue;
                    Logging.debug.fine(Logging.format("[X] Rule as Movie", n, n2));
                    return this.group.series(null);
                }
                Logging.debug.finest(Logging.format("[-] %s", rule));
            }
            return this.group;
        }

        public boolean equalsMovieName() {
            return this.mn.equals(this.fn);
        }

        public boolean containsMovieYear() {
            return this.m.getYear() >= 1950 && FileUtilities.listPathTailReverse(this.f, 3).stream().anyMatch(file -> StringUtilities.after(file.getName(), this.mym).map(string -> !AutoDetection.this.findEpisodeNumbers((String)string, false)).orElse(false));
        }

        public boolean containsMovieNameYear() {
            return StringUtilities.find(this.mn, this.snm) && Stream.of(this.dn, this.fn).anyMatch(string2 -> StringUtilities.after(string2, YEAR).map(string -> !AutoDetection.this.findEpisodeNumbers((String)string, false)).orElse(false));
        }

        public boolean containsEpisodeNumbers() {
            return AutoDetection.this.findEpisodeNumbers(this.fn, true) || MediaDetection.parseDate(this.fn) != null;
        }

        public boolean commonNumberPattern() {
            return FileUtilities.getChildren(this.f.getParentFile(), MediaTypes.VIDEO_FILES).stream().filter(file -> StringUtilities.find(this.dn, this.snm) || StringUtilities.find(this.normalize(file.getName()), this.snm)).map(file -> StringUtilities.streamMatches(file.getName(), EPISODE_NUMBERS).map(Integer::parseInt).collect(Collectors.toSet())).filter(set -> set.size() > 0).distinct().count() >= 10L;
        }

        public boolean episodeWithoutNumbers() {
            return StringUtilities.find(this.asn, DASH) && !this.matchMovie(this.fn);
        }

        public boolean episodeNumbers() {
            if (StringUtilities.find(this.asn, this.mym)) {
                return false;
            }
            if (!(AutoDetection.this.findEpisodeNumbers(this.asn, false) || StringUtilities.find(this.asn, NUMBER_PAIR) || StringUtilities.find(this.asn, YEAR_AND_NUMBER))) {
                return false;
            }
            return Stream.of(this.dn, this.fn).anyMatch(string -> StringUtilities.find(string, this.snm) && !this.matchMovie((String)string));
        }

        public boolean hasImdbId() {
            return Link.IMDb.findID(this.fn);
        }

        public boolean nonNumberName() {
            return StringUtilities.find(FileUtilities.getName(this.f), NON_NUMBER_NAME);
        }

        public boolean exactMovieMatch() throws Exception {
            List<Movie> list = MediaDetection.detectMovieWithYear(this.f, WebServices.TheMovieDB, AutoDetection.this.locale, true);
            return list != null && !list.isEmpty();
        }

        public boolean containsMovieName() {
            return Stream.of(this.dn, this.fn).anyMatch(string -> string.contains(this.mn) && !AutoDetection.this.findEpisodeNumbers(StringUtilities.after(string, this.mnm).orElse((String)string), false));
        }

        public boolean similarNameYear() {
            return this.getSimilarity(this.mn, this.fn) >= 0.8f || Stream.of(this.dn, this.fn).anyMatch(string -> StringUtilities.find(string, this.mym));
        }

        public boolean similarNameNoNumbers() {
            return Stream.of(this.dn, this.fn).anyMatch(string -> StringUtilities.find(string, this.mnm) && !StringUtilities.find(StringUtilities.after(string, this.mnm).orElse((String)string), EPISODE_NUMBERS) && this.getSimilarity((String)string, this.mn) >= 0.2f + this.getSimilarity((String)string, this.sn));
        }

        public boolean aliasNameMatch() {
            return this.m.getEffectiveNamesWithoutYear().stream().map(this::normalize).filter(string -> string.length() >= 5).anyMatch(this.fn::contains);
        }
    }

    public static enum Type {
        Movie,
        Series,
        Anime,
        Music;

    }

    private static class Rule
    implements Test {
        public final int s;
        public final int m;
        private final Test t;
        private final String name;

        public Rule(int n, int n2, Test test, String string) {
            this.s = n;
            this.m = n2;
            this.t = test;
            this.name = string;
        }

        @Override
        public boolean test() throws Exception {
            return this.t.test();
        }

        public String toString() {
            return this.name;
        }
    }

    @FunctionalInterface
    private static interface Test {
        public boolean test() throws Exception;
    }
}

