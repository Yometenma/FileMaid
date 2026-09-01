package net.filemaid.media;

import java.io.DataInputStream;
import java.io.File;
import java.io.Serializable;
import java.text.CollationKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.MemoryCache;
import net.filemaid.Resource;
import net.filemaid.WebServices;
import net.filemaid.cli.ScriptBundle;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.HighPerformanceMatcher;
import net.filemaid.media.IndexEntry;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.MovieCharacteristicsOrder;
import net.filemaid.media.ReleaseInfo;
import net.filemaid.media.SmartSeasonEpisodeMatcher;
import net.filemaid.media.SubstringMatcher;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.DateMatcher;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.MetricAvg;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.similarity.SeparatorMatcher;
import net.filemaid.similarity.SequenceMatchSimilarity;
import net.filemaid.similarity.SeriesNameMatcher;
import net.filemaid.similarity.SimilarityComparator;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.similarity.StringEqualsMetric;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MoviePart;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;
import net.filemaid.web.Series;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.XDB;

public class MediaDetection {
    private static final boolean index = SystemProperty.get("net.filemaid.media.index", Boolean::parseBoolean, true);
    private static final int depth = SystemProperty.get("net.filemaid.media.depth", Integer::parseInt, 5);
    private static final SeasonEpisodeMatcher seasonEpisodePatternMatcher = new SmartSeasonEpisodeMatcher(depth, null, false);
    private static final SeasonEpisodeMatcher seasonEpisodeMatcherStrict = new SmartSeasonEpisodeMatcher(depth, SeasonEpisodeMatcher.LENIENT_SANITY, true);
    private static final SeasonEpisodeMatcher seasonEpisodeMatcherNonStrict = new SmartSeasonEpisodeMatcher(depth, SeasonEpisodeMatcher.LENIENT_SANITY, false);
    private static final DateMatcher dateMatcher = new DateMatcher(depth, DateMatcher.DEFAULT_SANITY, Locale.ENGLISH, Locale.getDefault());
    private static final SeparatorMatcher separatorMatcher = new SeparatorMatcher();
    public static final HighPerformanceMatcher highPerformanceMatcher = new HighPerformanceMatcher(Locale.ENGLISH, MemoryCache.forMinutes());
    public static final ReleaseInfo releaseInfo = new ReleaseInfo(index);
    private static final SeasonEpisodeMatcher.SeasonEpisodePattern SXE101 = new SeasonEpisodeMatcher.SeasonEpisodePattern(null, "(?<![\\p{Alnum}-])[Ee]?([0-1]?\\d)?(\\d{2})(?![\\p{Alnum}-])");
    private static final Pattern AKA = Pattern.compile("(?<!\\p{Alnum})(?:AKA|The.Movie|^[0-1]?[0-9](?=[.])|19[3-9][0-9]|20[0-2][0-9])(?!\\p{Alnum})", 2);
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern NAME_YEAR_IN_BRACKETS = Pattern.compile("^(.+?)[\\[\\(]((?:19|20)\\d{2})[\\]\\)]");
    private static final Pattern NAME_YEAR = Pattern.compile("^([^\\[\\(]+?)((?:19|20)\\d{2})");
    private static final Resource<Integer> aliasNames = Resource.lazy(() -> {
        try (DataInputStream dataInputStream = new DataInputStream(ScriptBundle.getBundle().openStream());){
            int n2 = 4;
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(dataInputStream.available());
            byteBufferOutputStream.transferFully(dataInputStream);
            Integer n3 = Stream.of(Boolean.valueOf(ScriptBundle.update(byteBufferOutputStream.getByteBuffer(), 260, n2))).map(bl -> bl != false ? 0 : 32 * n2 + 11).filter(n -> n > 0).peek(Logging.severe).reduce(n2, Integer::sum);
            return n3;
        }
    });
    private static final Resource<List<IndexEntry<Movie>>> movieIndex = MediaDetection.index(releaseInfo::getMovieIndex, highPerformanceMatcher::prepareMovie);
    private static final Resource<List<IndexEntry<Series>>> seriesIndex = MediaDetection.index(releaseInfo::getSeriesIndex, highPerformanceMatcher::prepareSeries);
    private static final Resource<List<IndexEntry<SearchResult>>> animeIndex = MediaDetection.index(releaseInfo::getAnidbIndex, highPerformanceMatcher::prepareSearchResult);
    private static final Resource<SubstringMatcher<Movie>> movieSubstringMatcher = MediaDetection.substringMatcher(releaseInfo::getMovieIndex, Movie::getEffectiveNamesWithoutYear);
    private static final Resource<SubstringMatcher<Series>> seriesSubstringMatcher = MediaDetection.substringMatcher(releaseInfo::getSeriesIndex, SearchResult::getEffectiveNames);
    private static final Resource<SubstringMatcher<SearchResult>> animeSubstringMatcher = MediaDetection.substringMatcher(releaseInfo::getAnidbIndex, SearchResult::getEffectiveNames);
    private static final Pattern formatInfoPattern = releaseInfo.getVideoFormatPattern(true);
    private static final Pattern LEADING_S00E00_PATTERN = Pattern.compile("^\\[(?:(?:[Ss][0-9]+)?(?:[._ -]*[Ee][0-9]+)+)|(?:[1-9][x][0-9]{2})\\]");
    private static final Pattern LEADING_RELEASE_GROUP_PATTERN = Pattern.compile("^\\[([^\\[\\]]+)\\]");
    private static final Pattern ROUND_BRACKETS = Pattern.compile("\\([^\\(]*\\)");
    private static final Pattern SQUARE_BRACKETS = Pattern.compile("\\[[^\\[]*\\]");
    private static final Pattern CURLY_BRACKETS = Pattern.compile("\\{[^\\{]*\\}");
    private static final Pattern ABSOLUTE_EPISODE_RANGE = Pattern.compile("[0-9]+[-][0-9]+");
    private static final Resource<Pattern> blacklistPattern = Resource.lazy(releaseInfo::getBlacklistPattern);

    public static boolean isEpisode(File file, boolean bl) {
        Object object = XattrMetaInfo.xattr.getMetaInfo(file);
        if (object instanceof Episode) {
            return true;
        }
        if (MediaDetection.isEpisode(file.getName(), bl)) {
            return true;
        }
        try {
            return MediaFileUtilities.listStructurePathTail(file.getParentFile()).stream().anyMatch(string -> MediaDetection.isEpisode(string, bl));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Ignore parent folder", file, exception));
            return false;
        }
    }

    public static boolean isMovie(File file) {
        Object object = XattrMetaInfo.xattr.getMetaInfo(file);
        if (object != null) {
            return object instanceof Movie;
        }
        if (MediaDetection.isEpisode(file, true)) {
            return false;
        }
        if (Link.IMDb.findID(file.getPath())) {
            return true;
        }
        List<String> list = Stream.of(file, MediaFileUtilities.guessMediaFolder(file)).filter(Objects::nonNull).map(File::getName).collect(Collectors.toList());
        if (MediaDetection.matchMovieName(list, true, 0).size() > 0) {
            return true;
        }
        return MediaDetection.grepMovie(file) != null;
    }

    public static SeasonEpisodeMatcher getSeasonEpisodePatternMatcher() {
        return seasonEpisodePatternMatcher;
    }

    public static SeasonEpisodeMatcher getSeasonEpisodeMatcher(boolean bl) {
        return bl ? seasonEpisodeMatcherStrict : seasonEpisodeMatcherNonStrict;
    }

    public static DateMatcher getDateMatcher() {
        return dateMatcher;
    }

    public static SeparatorMatcher getSeparatorMatcher() {
        return separatorMatcher;
    }

    public static SeriesNameMatcher getSeriesNameMatcher(boolean bl) {
        return new SeriesNameMatcher(bl ? seasonEpisodeMatcherStrict : seasonEpisodeMatcherNonStrict, dateMatcher);
    }

    public static boolean isEpisode(String string, boolean bl) {
        return MediaDetection.parseEpisodeNumber(string, bl) != null || MediaDetection.parseDate(string) != null;
    }

    public static List<SeasonEpisodeMatcher.SxE> parseEpisodeNumber(String string, boolean bl) {
        return MediaDetection.getSeasonEpisodeMatcher(bl).match(string);
    }

    public static List<SeasonEpisodeMatcher.SxE> parseEpisodeNumber(File file, boolean bl) {
        return MediaDetection.getSeasonEpisodeMatcher(bl).match(file);
    }

    public static SimpleDate parseDate(String string) {
        return MediaDetection.getDateMatcher().match(string);
    }

    private static Object guessEpisodeGroup(File file) {
        String var2_6 = null;
        String string;
        Object object;
        for (File object22 : FileUtilities.listPathTailReverse(file, depth)) {
            object = MediaDetection.getEpisodeIdentifier(object22.getName(), true);
            if (object == null) continue;
            return object;
        }
        List<SeasonEpisodeMatcher.SxE> list = SXE101.match(file.getName());
        if (list != null && !list.isEmpty()) {
            return list;
        }
        String string2 = FileUtilities.getName(file);
        if (MediaTypes.SUBTITLE_FILES.accept(file)) {
            object = releaseInfo.getSubtitleLanguageTagPattern().matcher(string2);
            if (((Matcher)object).find()) {
                var2_6 = string2.substring(0, ((Matcher)object).start());
            } else {
                Matcher matcher = releaseInfo.getSubtitleCategoryTagPattern().matcher(string2);
                if (matcher.find()) {
                    var2_6 = string2.substring(0, matcher.start());
                }
            }
        }
        if ((string = MediaDetection.getUniqueQueryKey((String)var2_6)) != null && !string.isEmpty()) {
            return string;
        }
        return Collections.emptyList();
    }

    public static Map<Set<File>, Set<Series>> mapSeriesNamesByFiles(Collection<File> collection, Locale locale, boolean bl) throws Exception {
        Map<File, Set<Series>> seriesByFile = new LinkedHashMap<File, Set<Series>>();
        Map<File, List<File>> map = FileUtilities.mapByFolder(collection);
        for (Map.Entry<File, List<File>> entry : map.entrySet()) {
            seriesByFile.put(entry.getKey(), MediaDetection.detectSeries(entry.getValue(), bl, locale));
        }
        Map<Series, Set<File>> filesBySeries = new LinkedHashMap<Series, Set<File>>();
        for (Set<Series> seriesSet : seriesByFile.values()) {
            for (Series series : seriesSet) {
                Set<File> files = new LinkedHashSet<File>();
                for (Map.Entry<File, Set<Series>> entry2 : seriesByFile.entrySet()) {
                    if (!entry2.getValue().contains(series)) continue;
                    files.add(entry2.getKey());
                }
                filesBySeries.put(series, files);
            }
        }
        Map<Set<File>, Set<Series>> result = new LinkedHashMap<Set<File>, Set<Series>>();
        while (seriesByFile.size() > 0) {
            Set<Series> seriesSet2 = new LinkedHashSet<Series>();
            Set<File> fileSet = new LinkedHashSet<File>();
            fileSet.add(seriesByFile.keySet().iterator().next());
            boolean changed = true;
            while (changed) {
                boolean changed2 = false;
                Iterator<File> iterator = fileSet.iterator();
                while (iterator.hasNext()) {
                    File file2 = iterator.next();
                    changed2 |= seriesSet2.addAll(seriesByFile.get(file2));
                }
                for (Series series : seriesSet2) {
                    changed2 |= fileSet.addAll(filesBySeries.get(series));
                }
                changed = changed2;
            }
            Set<File> files2 = new LinkedHashSet<File>();
            Iterator<File> iterator2 = fileSet.iterator();
            while (iterator2.hasNext()) {
                File file3 = iterator2.next();
                files2.addAll(map.get(file3));
            }
            if (files2.size() > 0) {
                Map<Object, List<File>> groups = files2.stream().collect(Collectors.groupingBy(file -> MediaDetection.guessEpisodeGroup(file), LinkedHashMap::new, Collectors.toList()));
                int i = 0;
                while (true) {
                    Set<File> column = new LinkedHashSet<File>();
                    for (List<File> group : groups.values()) {
                        if (i >= group.size()) continue;
                        column.add(group.get(i));
                    }
                    if (column.isEmpty()) break;
                    files2.removeAll(column);
                    result.put(column, seriesSet2);
                    ++i;
                }
                if (files2.size() > 0) {
                    result.put(files2, seriesSet2);
                }
            }
            seriesByFile.keySet().removeAll(fileSet);
        }
        Set<File> remainder = new LinkedHashSet<File>(collection);
        for (Set<File> set : result.keySet()) {
            remainder.removeAll(set);
        }
        if (remainder.size() > 0) {
            result.put(remainder, null);
        }
        return result;
    }

    public static Object getEpisodeIdentifier(CharSequence charSequence, boolean bl) {
        List<SeasonEpisodeMatcher.SxE> list = MediaDetection.getSeasonEpisodeMatcher(true).match(charSequence);
        if (list != null) {
            return list;
        }
        SimpleDate simpleDate = MediaDetection.getDateMatcher().match(charSequence);
        if (simpleDate != null) {
            return simpleDate;
        }
        if (!bl) {
            list = MediaDetection.getSeasonEpisodeMatcher(false).match(charSequence);
            if (list != null) {
                return list;
            }
        }
        return null;
    }

    public static Set<Series> detectSeries(Collection<File> collection, boolean bl, Locale locale) throws Exception {
        Set<Series> seriesSet = new LinkedHashSet<Series>();
        Set<File> matchedFiles = new HashSet<File>();
        for (File file : collection) {
            try {
                Series series = MediaDetection.grepSeries(file.getPath(), locale);
                if (series == null) continue;
                seriesSet.add(series.withConfidence(Integer.MAX_VALUE));
                matchedFiles.add(file);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Match Series ID", file, exception));
            }
        }
        if (matchedFiles.size() == collection.size()) {
            Logging.debug.finest(Logging.format("Match Series ID => %s", seriesSet));
            return seriesSet;
        }
        for (File file : collection) {
            Object metaInfo = XattrMetaInfo.xattr.getMetaInfo(file);
            Series externalSeries;
            if (!(metaInfo instanceof Episode) || (externalSeries = EpisodeUtilities.getExternalSeries(((Episode)metaInfo).getSeriesInfo())) == null) continue;
            seriesSet.add(externalSeries.withConfidence(Integer.MAX_VALUE));
            matchedFiles.add(file);
        }
        if (matchedFiles.size() == collection.size()) {
            Logging.debug.finest(Logging.format("Match Series ID => %s", seriesSet));
            return seriesSet;
        }
        seriesSet.addAll(MediaDetection.grepSeriesFromNfoFiles(collection, locale));
        Map<File, Series> map = MediaDetection.matchSeriesMappings(collection);
        if (!map.isEmpty()) {
            seriesSet.addAll(map.values());
            matchedFiles.addAll(map.keySet());
            if (matchedFiles.size() == collection.size()) {
                Logging.debug.finest(Logging.format("Match Series Name => %s", seriesSet));
                return seriesSet;
            }
        }
        SeriesNameMatcher seriesNameMatcher = MediaDetection.getSeriesNameMatcher(true);
        Set<String> parentNames = new LinkedHashSet<String>();
        Set<String> fileNames = new LinkedHashSet<String>();
        for (File file : collection) {
            File parent = file;
            for (int i = 0; i < depth && parent != null && !MediaFileUtilities.isStructureRoot(parent); ++i) {
                String name = MediaDetection.trimReleaseGroup(FileUtilities.getName(parent), false);
                String match = seriesNameMatcher.matchByEpisodeIdentifier(name);
                if (match != null) {
                    name = match;
                }
                if (i == 0) {
                    fileNames.add(name);
                } else {
                    parentNames.add(name);
                }
                parent = parent.getParentFile();
            }
        }
        List<Series> seriesList = new ArrayList<Series>();
        List<String> nameList = new ArrayList<String>();
        seriesList.addAll(MediaDetection.matchSeriesByName(parentNames, 0, bl));
        if (seriesList.isEmpty()) {
            seriesList.addAll(MediaDetection.matchSeriesByName(fileNames, 0, bl));
            seriesList.addAll(MediaDetection.matchSeriesByName(MediaDetection.stripReleaseInfo(fileNames, false), 0, bl));
            if (seriesList.stream().distinct().count() > 1L) {
                nameList.addAll(MediaDetection.stripReleaseInfo(parentNames, false));
            }
        }
        if (seriesList.isEmpty()) {
            seriesList.addAll(MediaDetection.matchSeriesFromStringWithoutSpacing(MediaDetection.stripReleaseInfo(parentNames, false), bl, true));
            seriesList.addAll(MediaDetection.matchSeriesFromStringWithoutSpacing(MediaDetection.stripReleaseInfo(fileNames, false), bl, true));
            seriesList.addAll(MediaDetection.matchSeriesByName(parentNames, 2, bl));
            seriesList.addAll(MediaDetection.matchSeriesByName(fileNames, 2, bl));
        }
        seriesSet.addAll(seriesList);
        nameList.addAll(MediaDetection.matchByCommonWordSequence(collection, true));
        if (nameList.isEmpty()) {
            nameList.addAll(MediaDetection.matchByCommonWordSequence(collection, false));
        }
        if (nameList.isEmpty()) {
            collection.stream().map(file -> MediaFileUtilities.guessMediaFolder(file)).filter(Objects::nonNull).map(File::getName).forEach(nameList::add);
        }
        Logging.debug.finest(Logging.format("Match Series Name => %s %s", seriesSet, nameList));
        List<String> queryNames = new ArrayList<String>();
        List<String> unmatched = new ArrayList<String>();
        Map<String, String> seriesByName = MediaDetection.mapByUniqueQueryKey(seriesSet, SearchResult::getName);
        MediaDetection.mapByUniqueQueryKey(nameList).forEach((queryKey, name) -> {
            String seriesName = seriesByName.get(queryKey);
            if (seriesName != null) {
                queryNames.add(seriesName);
            } else {
                unmatched.add(name);
            }
        });
        if (unmatched.size() > 0) {
            queryNames.addAll(MediaDetection.getUniqueQuerySet(unmatched));
        }
        Logging.debug.finest(Logging.format("Query Series => %s", queryNames));
        queryNames.stream().map(Series::QUERY).forEach(seriesSet::add);
        return seriesSet;
    }

    private static Collection<String> matchByCommonWordSequence(Collection<File> collection, boolean bl) {
        SeriesNameMatcher seriesNameMatcher = MediaDetection.getSeriesNameMatcher(bl);
        Collection<String> collection2 = seriesNameMatcher.matchAll(collection, file -> MediaDetection.stripFormatInfo(MediaDetection.stripBatchInfo(FileUtilities.getName(file))));
        if (collection2.size() > 0) {
            return collection2;
        }
        return collection.stream().map(file -> {
            for (File file2 : FileUtilities.listPathTailReverse(file, 2)) {
                String string;
                String string2 = FileUtilities.getName(file2);
                if (!bl && !MediaDetection.parseMovieYear(string2).isEmpty()) {
                    return null;
                }
                String string3 = seriesNameMatcher.matchByEpisodeIdentifier(string2);
                if (string3 == null || string3.isEmpty()) continue;
                if (!bl && (string = MediaDetection.getSeparatorMatcher().match(MediaDetection.stripFormatInfo(string2))) != null && !string.isEmpty() && string.length() < string3.length()) {
                    return string;
                }
                return string3;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static Map<File, Series> matchSeriesMappings(Collection<File> collection) throws Exception {
        LinkedHashMap<File, Series> linkedHashMap = new LinkedHashMap<File, Series>();
        releaseInfo.getSeriesMappings().forEach((string, pattern) -> {
            for (File file : collection) {
                for (File file2 : FileUtilities.listPathTailReverse(file, 2)) {
                    if (!StringUtilities.find(file2.getName(), pattern)) continue;
                    linkedHashMap.put(file, Series.QUERY(string));
                }
            }
        });
        return linkedHashMap;
    }

    public static List<Series> matchSeriesByName(Collection<String> collection, int n, boolean bl) {
        if (collection.size() > 0) {
            try {
                HighPerformanceMatcher highPerformanceMatcher = MediaDetection.highPerformanceMatcher.maxStartIndex(n);
                if (bl) {
                    return highPerformanceMatcher.match(collection, false, animeIndex.get()).stream().map(searchResult -> Series.XDB(XDB.AniDB, searchResult.getId(), searchResult.getName())).collect(Collectors.toList());
                }
                return highPerformanceMatcher.match(collection, false, seriesIndex.get()).stream().collect(Collectors.toList());
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to load series index", exception));
            }
        }
        return Collections.emptyList();
    }

    public static List<Series> matchSeriesFromStringWithoutSpacing(Collection<String> collection, boolean bl, boolean bl2) throws Exception {
        if (collection.size() > 0) {
            if (bl) {
                return animeSubstringMatcher.get().match(collection, bl2).stream().map(searchResult -> Series.XDB(XDB.AniDB, searchResult.getId(), searchResult.getName())).collect(Collectors.toList());
            }
            return seriesSubstringMatcher.get().match(collection, bl2);
        }
        return Collections.emptyList();
    }

    public static List<Movie> detectMovie(File file, MovieLookupService movieLookupService, Locale locale, boolean bl, boolean bl2) throws Exception {
        Set<Movie> movieSet = new LinkedHashSet<Movie>();
        Object object = XattrMetaInfo.xattr.getMetaInfo(file);
        if (object instanceof Movie) {
            Movie movie = (Movie)object;
            if (movie instanceof MoviePart) {
                movie = new Movie(movie);
            }
            movieSet.add(movie);
        }
        if (movieSet.size() == 1 && !bl2) {
            return movieSet.stream().collect(Collectors.toList());
        }
        try {
            Movie movie = MediaDetection.grepMovie(file);
            if (movie != null) {
                Logging.debug.finest(Logging.format("Match Movie ID => %s [%s | %s]", movie, Link.TheMovieDB.getID(movie), Link.IMDb.getID(movie)));
                Movie hydrated = MediaDetection.hydrateMovie(movie, movieLookupService, locale);
                if (hydrated != null) {
                    movieSet.add(hydrated);
                }
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Match Movie ID", file, exception));
        }
        if (movieSet.size() == 1 && !bl2) {
            return movieSet.stream().collect(Collectors.toList());
        }
        List<Movie> movies = movieLookupService.lookupMovie(file, locale);
        if (movies != null) {
            movieSet.addAll(movies);
        }
        if (movieSet.size() == 1 && !bl2) {
            return movieSet.stream().collect(Collectors.toList());
        }
        movieSet.addAll(MediaDetection.grepMovieFromNfoFiles(file, movieLookupService, locale));
        List<String> names = new ArrayList<String>(2);
        names.add(FileUtilities.getName(file));
        File file2 = MediaDetection.guessMovieFolder(file);
        if (file2 != null) {
            names.add(FileUtilities.getName(file2));
        }
        Set<String> nameSet = MediaDetection.reduceMovieNamePermutations(names);
        List<Movie> matches = MediaDetection.matchMovieName(nameSet, true, 0);
        if (matches.size() > 0 && !bl2) {
            movieSet.addAll(matches);
            return MediaDetection.sortByMovieMatchSimilarity(movieSet, nameSet, file);
        }
        matches = MediaDetection.matchMovieName(nameSet, bl, 2);
        if (matches.size() > 0 && movieSet.size() > 0 && !bl2) {
            movieSet.addAll(matches);
            return MediaDetection.sortByMovieMatchSimilarity(movieSet, nameSet, file);
        }
        if (matches.isEmpty() && bl && (matches = MediaDetection.matchMovieName(nameSet, false, 0)).isEmpty()) {
            matches = MediaDetection.matchMovieName(nameSet, false, 2);
        }
        if (matches.size() > 0 && !bl) {
            for (String name : nameSet) {
                List<Integer> years = MediaDetection.parseMovieYear(name);
                if (years.size() <= 0) continue;
                if (!matches.stream().map(Movie::getYear).noneMatch(years::contains)) continue;
                matches.addAll(MediaDetection.matchMovieName(Collections.singleton(name), true, Integer.MAX_VALUE));
            }
        }
        if (matches.isEmpty()) {
            String title = CachedMediaCharacteristics.getMediaCharacteristics(file, MediaCharacteristics::getTitle).orElse(null);
            if (title != null && !title.isEmpty()) {
                Set<String> titleSet = MediaDetection.reduceMovieNamePermutations(Collections.singleton(title));
                matches = MediaDetection.matchMovieName(titleSet, true, 0);
                if (matches.size() > 0 && !bl2) {
                    movieSet.addAll(matches);
                    return MediaDetection.sortByMovieMatchSimilarity(movieSet, titleSet, file);
                }
                matches = MediaDetection.matchMovieName(titleSet, bl, 2);
                nameSet.addAll(titleSet);
            }
        }
        if (matches.isEmpty() && (matches = MediaDetection.matchMovieFromStringWithoutSpacing(nameSet, bl)).isEmpty() && !nameSet.containsAll(MediaDetection.stripReleaseInfo(nameSet, true))) {
            matches = MediaDetection.matchMovieFromStringWithoutSpacing(MediaDetection.stripReleaseInfo(nameSet, true), bl);
        }
        matches = MediaDetection.queryMovieByFileName(nameSet, movieLookupService, locale);
        movieSet.addAll(matches);
        List<String> aka = MediaDetection.akaTerms(nameSet);
        if (!aka.isEmpty() && movieSet.isEmpty()) {
            List<Movie> akaMovies = MediaDetection.queryMovieByFileName(aka, movieLookupService, locale);
            if (!akaMovies.isEmpty()) {
                nameSet.addAll(aka);
                movieSet.addAll(akaMovies);
            }
        }
        movieSet.addAll(matches);
        return MediaDetection.sortByMovieMatchSimilarity(movieSet, nameSet, file);
    }

    private static List<String> akaTerms(Collection<String> collection) {
        return collection.stream().filter(string -> StringUtilities.find(string, AKA)).flatMap(AKA::splitAsStream).map(String::trim).filter(string -> !string.isEmpty()).collect(Collectors.toList());
    }

    public static List<Movie> detectMovieWithYear(File file, MovieLookupService movieLookupService, Locale locale, boolean bl) throws Exception {
        if (!bl) {
            return MediaDetection.detectMovie(file, movieLookupService, locale, bl, false);
        }
        List<Integer> list = MediaDetection.parseMovieYear(FileUtilities.getRelativePathTail(file, 3).getPath());
        if (list.isEmpty() || MediaDetection.isEpisode(file, true)) {
            return null;
        }
        return MediaDetection.detectMovie(file, movieLookupService, locale, bl, false).stream().filter(movie -> list.contains(movie.getYear())).collect(Collectors.toList());
    }

    public static Movie getLocalizedMovie(MovieLookupService movieLookupService, Movie movie, Locale locale) {
        if (movie == null) {
            return null;
        }
        try {
            return movieLookupService.getMovieDescriptor(movie, locale);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to retrieve localized movie information", movie, exception));
            return null;
        }
    }

    public static <T extends SearchResult> List<T> sortBySimilarity(Collection<T> collection, Collection<String> collection2, SimilarityMetric similarityMetric, Comparator<T> comparator) {
        return MediaDetection.sortBySimilarity(collection, collection2, similarityMetric, comparator, SearchResult::getEffectiveNames);
    }

    public static <T extends SearchResult> List<T> sortBySimilarity(Collection<T> collection, Collection<String> collection2, SimilarityMetric similarityMetric, Comparator<T> comparator, Function<T, Collection<String>> function) {
        SimilarityComparator<T, String> similarityComparator = new SimilarityComparator<T, String>(similarityMetric, collection2, function);
        List list = collection.stream().sorted(similarityComparator).distinct().collect(Collectors.toList());
        if (list.size() > 1 && comparator != null) {
            list.subList(0, Math.min(list.size(), depth)).sort(similarityComparator.thenComparing(comparator));
        }
        Logging.debug.finest(Logging.format("Rank %s => %s", collection2, list));
        return list;
    }

    public static <T extends SearchResult> List<T> sortBySeriesMatchSimilarity(Collection<T> collection, String string) {
        return MediaDetection.sortBySimilarity(collection, Collections.singleton(string), MediaDetection.getSeriesMatchMetric(), MediaDetection.getSeriesPopularityOrder());
    }

    public static List<Movie> sortByMovieMatchSimilarity(Collection<Movie> collection, Collection<String> collection2, File file) {
        TreeSet<String> treeSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(MediaDetection.stripReleaseInfo(collection2, true));
        treeSet.addAll(MediaDetection.stripReleaseInfo(collection2, false));
        return MediaDetection.sortBySimilarity(collection, treeSet, MediaDetection.getMovieMatchMetric(), MovieCharacteristicsOrder.compareTo(file), SearchResult::getEffectiveNames);
    }

    public static SimilarityMetric getSeriesMatchMetric() {
        return new MetricAvg(new SequenceMatchSimilarity(), new NameSimilarityMetric(), new SequenceMatchSimilarity(0, true));
    }

    public static <T extends SearchResult> Comparator<T> getSeriesPopularityOrder() {
        return Comparator.comparing(searchResult -> searchResult instanceof SearchResultDetails ? ((SearchResultDetails)searchResult).getPopularity() : null, Comparator.nullsLast(Collections.reverseOrder(Double::compare)));
    }

    public static SimilarityMetric getMovieMatchMetric() {
        return new MetricAvg(new StringEqualsMetric(){

            @Override
            protected String normalize(Object object) {
                return super.normalize(Normalization.removeTrailingBrackets(object.toString()));
            }
        }, MediaDetection.getMovieYearMatchMetric(), new NameSimilarityMetric(), new SequenceMatchSimilarity(), new SequenceMatchSimilarity(1, true));
    }

    public static SimilarityMetric getMovieYearMatchMetric() {
        return new SimilarityMetric(){

            @Override
            public float getSimilarity(Object object, Object object2) {
                List<Integer> list = MediaDetection.parseMovieYear(object.toString());
                List<Integer> list2 = MediaDetection.parseMovieYear(object2.toString());
                for (int n : list) {
                    for (int n2 : list2) {
                        if (n == n2) {
                            return 2.0f;
                        }
                        if (n - 1 != n2 && n != n2 - 1) continue;
                        return 1.5f;
                    }
                }
                return 0.0f;
            }
        };
    }

    public static boolean isEpisodeNumberMatch(File file, Episode episode) {
        float f = new EpisodeMetrics().numbers().getSimilarity(file, episode);
        if (f >= 1.0f) {
            return true;
        }
        if ((double)f >= 0.5 && EpisodeUtilities.isAbsoluteEpisode(episode)) {
            List<SeasonEpisodeMatcher.SxE> list = MediaDetection.parseEpisodeNumber(file, false);
            return list != null && list.stream().anyMatch(sxE -> sxE.season < 0 && sxE.episode == episode.getEpisode());
        }
        return false;
    }

    public static List<Integer> parseMovieYear(String string) {
        return StringUtilities.streamMatches(string, YEAR).map(Integer::parseInt).filter(DateMatcher.DEFAULT_SANITY::acceptYear).collect(Collectors.toList());
    }

    public static String reduceMovieName(String string, boolean bl) {
        Matcher matcher = (bl ? NAME_YEAR_IN_BRACKETS : NAME_YEAR).matcher(string);
        if (matcher.find() && !MediaDetection.parseMovieYear(matcher.group(2)).isEmpty()) {
            return Normalization.trimTrailingPunctuation(matcher.group(1)) + " " + matcher.group(2);
        }
        return null;
    }

    public static Set<String> reduceMovieNamePermutations(Collection<String> collection) {
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        for (String string : collection) {
            String string2 = MediaDetection.reduceMovieName(string, true);
            if (string2 != null) {
                arrayDeque.addFirst(string2);
                continue;
            }
            arrayDeque.addLast(string);
            string2 = MediaDetection.reduceMovieName(string, false);
            if (string2 == null) continue;
            arrayDeque.addLast(string2);
        }
        return new LinkedHashSet<String>(arrayDeque);
    }

    public static File guessMovieFolder(File file) throws Exception {
        File file2 = file.getParentFile();
        if (file.isDirectory()) {
            if (!MediaFileUtilities.isStructureRoot(file2) && MediaDetection.isDoubleNestedMovieFolder(file)) {
                return file2;
            }
            if (!MediaFileUtilities.isStructureRoot(file)) {
                return file;
            }
            return null;
        }
        if (file2 != null && depth > 1) {
            for (boolean bl : new boolean[]{true, false}) {
                File file3 = file2;
                for (int i = 0; file3 != null && i < depth - 1 && !MediaFileUtilities.isStructureRoot(file3); ++i, file3 = file3.getParentFile()) {
                    String string = MediaDetection.stripReleaseInfo(file3.getName());
                    if (string.length() <= 0 || MediaDetection.checkMovie(file3, bl) == null) continue;
                    return file3;
                }
            }
            File object = file2;
            for (int i = 0; object != null && i < depth - 2 && !MediaFileUtilities.isStructureRoot(object); ++i, object = object.getParentFile()) {
                String string = MediaDetection.stripReleaseInfo(object.getName());
                if (string.length() <= 0) continue;
                return MediaDetection.isDoubleNestedMovieFolder(object) ? object.getParentFile() : object;
            }
            if (object != null && !MediaFileUtilities.isStructureRoot(object.getParentFile()) && !MediaDetection.stripReleaseInfo(file2.getName()).isEmpty()) {
                return file2;
            }
        }
        return null;
    }

    private static boolean isDoubleNestedMovieFolder(File file) {
        return MediaDetection.checkMovie(file.getParentFile(), false) != null && MediaDetection.checkMovie(file, false) == null;
    }

    public static Movie checkMovie(File file, boolean bl) {
        if (file == null) {
            return null;
        }
        List<Movie> list = MediaDetection.matchMovieName(Collections.singleton(file.getName()), bl, depth);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static List<Movie> matchMovieName(Collection<String> collection, boolean bl, int n) {
        if (collection.size() > 0) {
            try {
                return highPerformanceMatcher.maxStartIndex(n).match(collection, bl, movieIndex.get());
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to load movie index", exception));
            }
        }
        return Collections.emptyList();
    }

    public static List<Movie> matchMovieFromStringWithoutSpacing(Collection<String> collection, boolean bl) throws Exception {
        return movieSubstringMatcher.get().match(collection, bl);
    }

    private static List<Movie> queryMovieByFileName(Collection<String> collection, MovieLookupService movieLookupService, Locale locale) throws Exception {
        Collection<String> collection2 = MediaDetection.getUniqueQuerySet(collection);
        Logging.debug.finest(Logging.format("Query Movie => %s", collection2));
        LinkedHashMap<Movie, Double> linkedHashMap = new LinkedHashMap<Movie, Double>();
        for (String string : collection2) {
            SimilarityComparator<Movie, String> similarityComparator = new SimilarityComparator<Movie, String>(MediaDetection.getMovieMatchMetric(), Collections.singleton(string), Movie::getEffectiveNames);
            for (Movie movie : movieLookupService.searchMovie(string, locale)) {
                linkedHashMap.computeIfAbsent(movie, similarityComparator::getSimilarity);
            }
        }
        Comparator<Map.Entry> comparator = Comparator.comparing(entry -> (Double)entry.getValue());
        comparator = comparator.thenComparing(Comparator.comparing(entry -> ((Movie)entry.getKey()).getImdbId() > 0));
        comparator = comparator.thenComparing(Comparator.comparing(entry -> ((Movie)entry.getKey()).getYear()));
        return linkedHashMap.entrySet().stream().sorted(Collections.reverseOrder(comparator)).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public static Collection<String> getUniqueQuerySet(Collection<String> collection) {
        Collection<String> collection2 = new LinkedHashSet<String>();
        collection2.addAll(MediaDetection.stripReleaseInfo(collection, true));
        collection2.addAll(MediaDetection.stripReleaseInfo(collection, false));
        collection2 = MediaDetection.filterBlacklistedTerms(collection2);
        return MediaDetection.mapByUniqueQueryKey(collection2).values();
    }

    public static <T> Map<String, String> mapByUniqueQueryKey(Collection<String> collection) {
        return MediaDetection.mapByUniqueQueryKey(collection, string -> Normalization.replaceSpace(string, " "));
    }

    public static <T> Map<String, String> mapByUniqueQueryKey(Collection<T> collection, Function<T, String> function) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (T t : collection) {
            String string = function.apply(t);
            String string2 = MediaDetection.getUniqueQueryKey(string);
            if (string2 == null) continue;
            linkedHashMap.putIfAbsent(string2, string);
        }
        return linkedHashMap;
    }

    public static String getUniqueQueryKey(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        String string2 = Normalization.normalizePunctuation(string).toLowerCase();
        if (string2.isEmpty()) {
            return null;
        }
        return string2;
    }

    public static List<Movie> matchMovieByFileFolderName(File file, Collection<Movie> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = FileUtilities.listPathTailReverse(file, 2).stream().map(File::getName).collect(Collectors.toList());
        return MediaDetection.matchMovieByWordSequence(list, collection, 1);
    }

    public static List<Movie> matchMovieByWordSequence(Collection<String> collection, Collection<Movie> collection2, int n) {
        HighPerformanceMatcher highPerformanceMatcher = MediaDetection.highPerformanceMatcher.maxStartIndex(n);
        List<CollationKey[]> list = highPerformanceMatcher.prepare(collection);
        return collection2.stream().filter(movie -> highPerformanceMatcher.prepare(movie.getEffectiveNames()).stream().anyMatch(collationKeyArray -> list.stream().anyMatch(collationKeyArray2 -> {
            CollationKey[] collationKeyArray3 = highPerformanceMatcher.matchFirstCommonSequence(new CollationKey[][]{collationKeyArray2, collationKeyArray});
            return collationKeyArray3 != null && collationKeyArray3.length >= ((CollationKey[])collationKeyArray).length;
        }))).collect(Collectors.toList());
    }

    public static Movie matchMovie(File file, int n) {
        ArrayList<String> arrayList = new ArrayList<String>(n);
        for (File file2 : FileUtilities.listPathTailReverse(file, n)) {
            arrayList.add(file2.getName());
        }
        List<Movie> list = MediaDetection.matchMovieName(arrayList, true, 0);
        return list.size() > 0 ? (Movie)list.get(0) : null;
    }

    private static <T extends SearchResult> Resource<List<IndexEntry<T>>> index(Resource<List<T>> resource, Function<T, List<IndexEntry<T>>> function) {
        return Resource.lazy(() -> {
            CompletableFuture<Integer> completableFuture = WebServices.requestPool().async(aliasNames::get);
            CompletableFuture<List<T>> completableFuture2 = WebServices.requestPool().async(resource::get);
            List<T> list = completableFuture2.get();
            int n = list.size() * (Integer)completableFuture.join();
            return list.stream().map(function).flatMap(Collection::stream).collect(Collectors.toCollection(() -> new ArrayList<>(n)));
        });
    }

    private static <T extends SearchResult> Resource<SubstringMatcher<T>> substringMatcher(Resource<List<T>> resource, Function<T, Collection<String>> function) {
        return Resource.lazy(() -> new SubstringMatcher((List)resource.get(), function));
    }

    public static String stripFormatInfo(String string) {
        string = string.replace('_', ' ');
        string = Normalization.EMBEDDED_GUID.matcher(string).replaceAll("");
        string = Normalization.EMBEDDED_CHECKSUM.matcher(string).replaceAll("");
        string = formatInfoPattern.matcher(string).replaceAll("");
        return MediaDetection.trimReleaseGroup(string, true);
    }

    public static String trimReleaseGroup(String string, boolean bl) {
        if (bl && LEADING_S00E00_PATTERN.matcher(string).find()) {
            return string;
        }
        return LEADING_RELEASE_GROUP_PATTERN.matcher(string).replaceAll("").trim();
    }

    public static String stripBatchInfo(String string) {
        string = ROUND_BRACKETS.matcher(string).replaceAll("");
        string = SQUARE_BRACKETS.matcher(string).replaceAll("");
        string = CURLY_BRACKETS.matcher(string).replaceAll("");
        string = ABSOLUTE_EPISODE_RANGE.matcher(string).replaceAll("");
        return string.trim();
    }

    public static List<String> stripReleaseInfo(Collection<String> collection, boolean bl) {
        try {
            return releaseInfo.cleanRelease(collection, bl);
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause("Failed to strip release info", exception));
            return Collections.emptyList();
        }
    }

    public static String stripReleaseInfo(String string, boolean bl) {
        List<String> list = MediaDetection.stripReleaseInfo(Collections.singleton(string), bl);
        return list.isEmpty() ? "" : list.get(0);
    }

    public static String stripReleaseInfo(String string) {
        return MediaDetection.stripReleaseInfo(string, true);
    }

    public static String checkMovieStripReleaseInfo(File file, boolean bl) {
        String string = MediaDetection.stripReleaseInfo(FileUtilities.getName(file));
        if (string.length() < 2) {
            try {
                Movie movie = MediaDetection.checkMovie(file, bl);
                if (movie != null) {
                    return movie.getName();
                }
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to strip release info", exception));
            }
        }
        return string;
    }

    private static List<String> filterBlacklistedTerms(Collection<String> collection) {
        try {
            Pattern pattern = blacklistPattern.get();
            return collection.stream().filter(string -> pattern.matcher((CharSequence)string).replaceAll("").trim().length() > 0).collect(Collectors.toList());
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause("Failed to filter blacklisted terms", exception));
            return Collections.emptyList();
        }
    }

    private static List<Movie> grepMovieFromNfoFiles(File file2, MovieLookupService movieLookupService, Locale locale) {
        if (file2.getParentFile() != null && file2.getParentFile().isDirectory()) {
            return FileUtilities.getChildren(file2.getParentFile(), MediaTypes.NFO_FILES).stream().map(file -> {
                try {
                    return MediaDetection.hydrateMovie(MediaDetection.grepMovie(file), movieLookupService, locale);
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Match Movie ID", file, exception));
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static Set<Series> grepSeriesFromNfoFiles(Collection<File> collection, Locale locale) throws Exception {
        LinkedHashSet<Series> linkedHashSet = new LinkedHashSet<Series>();
        TreeSet<File> treeSet = new TreeSet<File>(Collections.reverseOrder(Comparator.comparing(File::getAbsoluteFile)));
        for (File file : collection) {
            treeSet.addAll(FileUtilities.listPathTailReverse(file.getParentFile(), 2));
        }
        for (File file : treeSet) {
            for (File file2 : FileUtilities.getChildren(file, MediaTypes.NFO_FILES)) {
                try {
                    Series series = MediaDetection.grepSeries(FileUtilities.readTextFile(file2), locale);
                    if (series == null) continue;
                    linkedHashSet.add(series.withConfidence(Integer.MAX_VALUE));
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Failed to read NFO file", file2, exception));
                }
            }
        }
        return linkedHashSet;
    }

    private static Movie hydrateMovie(Movie movie, MovieLookupService movieLookupService, Locale locale) throws Exception {
        if (movie == null) {
            return null;
        }
        Movie movie2 = WebServices.TheMovieDB.lookupMovieDescriptor(movie);
        if (movie2 != null) {
            return movie2;
        }
        return movieLookupService.getMovieDescriptor(movie, locale);
    }

    public static Movie grepMovie(File file) {
        Movie movie2 = MediaDetection.grepMovie(file.getPath());
        if (movie2 != null) {
            return movie2;
        }
        if (MediaTypes.MKV.accept(file)) {
            return CachedMediaCharacteristics.getMediaCharacteristics(file, MediaCharacteristics::getMediaTags).map(object -> object instanceof Movie ? (Movie)object : null).filter(movie -> movie.getTmdbId() > 0 || movie.getImdbId() > 0).orElse(null);
        }
        if (MediaTypes.NFO_FILES.accept(file)) {
            try {
                return MediaDetection.grepMovie(FileUtilities.readTextFile(file));
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to read NFO file", file, exception));
            }
        }
        if (file.isDirectory()) {
            for (File file2 : FileUtilities.listFiles(file, MediaTypes.NFO_FILES)) {
                try {
                    return MediaDetection.grepMovie(FileUtilities.readTextFile(file2));
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Failed to read NFO file", file2, exception));
                }
            }
        }
        return null;
    }

    public static Movie grepMovie(CharSequence charSequence) {
        Integer n = Link.TheMovieDB.matchID(charSequence);
        if (n != null) {
            return Movie.TMDB(n);
        }
        Integer n2 = Link.IMDb.matchID(charSequence);
        if (n2 != null) {
            return Movie.IMDB(n2);
        }
        return null;
    }

    public static Series grepSeries(CharSequence charSequence, Locale locale) throws Exception {
        Integer n;
        Integer n2;
        Integer n3;
        Integer n4 = Link.TheMovieDB_TV.matchID(charSequence);
        if (n4 != null) {
            try {
                return XDB.TheMovieDB.getExternalSeries(n4);
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Bad ID", XDB.TheMovieDB + "::" + n4, charSequence, lookupException));
            }
        }
        if ((n3 = Link.TheTVDB.matchID(charSequence)) != null) {
            try {
                return XDB.TheTVDB.getExternalSeries(n3);
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Bad ID", XDB.TheTVDB + "::" + n3, charSequence, lookupException));
            }
        }
        if ((n2 = Link.AniDB.matchID(charSequence)) != null) {
            try {
                return XDB.AniDB.getExternalSeries(n2);
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Bad ID", XDB.AniDB + "::" + n2, charSequence, lookupException));
            }
        }
        if ((n = Link.IMDb.matchID(charSequence)) != null) {
            if (WebServices.TheMovieDB.lookupMovieDescriptor(Movie.IMDB(n)) != null) {
                return null;
            }
            try {
                return XDB.IMDb.getExternalSeries(n);
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Bad ID", XDB.IMDb + "::" + n, charSequence, lookupException));
            }
        }
        return null;
    }

    public static <T extends SearchResult> List<T> getProbableMatches(String string, Collection<T> collection, boolean bl, boolean bl2) {
        if (string == null || string.isEmpty()) {
            return collection.stream().collect(Collectors.toList());
        }
        Function<SearchResult, Collection<String>> function = bl ? SearchResult::getEffectiveNames : searchResult -> Collections.singleton(searchResult.getName());
        ArrayList<T> arrayList = new ArrayList<T>();
        NameSimilarityMetric nameSimilarityMetric = new NameSimilarityMetric();
        float f = bl2 && collection.size() > 1 ? 0.8f : 0.72f;
        float f2 = bl2 && collection.size() > 1 ? 0.5f : 0.2f;
        String string2 = Normalization.removeTrailingBrackets(string).toLowerCase();
        for (T searchResult2 : collection) {
            float f3 = 0.0f;
            for (String string3 : function.apply(searchResult2)) {
                if (!((f3 = Math.max(f3, nameSimilarityMetric.getSimilarity(string2, string3 = Normalization.removeTrailingBrackets(string3).toLowerCase()))) >= f2) || !string3.startsWith(string2) && !string3.endsWith(string2)) continue;
                f3 = 1.0f;
                break;
            }
            if (!(f3 >= f)) continue;
            arrayList.add(searchResult2);
        }
        return MediaDetection.sortBySeriesMatchSimilarity(arrayList, string);
    }

    public static int countSequentiallyNumberedEpisodes(Iterable<String> iterable) {
        int n2 = 0;
        List<Integer> list = Collections.emptyList();
        for (String string : iterable) {
            List<Integer> list2 = MediaDetection.isEpisode(string, true) ? Collections.<Integer>emptyList() : StringUtilities.matchIntegers(string);
            if (list2.stream().filter(SeasonEpisodeMatcher.LENIENT_SANITY::filter).map(n -> n - 1).anyMatch(list::contains)) {
                ++n2;
            }
            list = list2;
        }
        return n2;
    }

    private static /* synthetic */ void lambda$detectSeries$3(Map map, Collection collection, Collection collection2, String string, String string2) {
        String string3 = (String)map.get(string);
        if (string3 != null) {
            collection.add(string3);
        } else {
            collection2.add(string2);
        }
    }
}

