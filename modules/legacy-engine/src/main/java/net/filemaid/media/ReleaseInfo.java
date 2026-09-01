package net.filemaid.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.ApplicationFolder;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.format.ExpressionFormatMethods;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.ZipEntrySpliterator;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.DiscDB;
import net.filemaid.web.Movie;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import org.tukaani.xz.XZInputStream;

public class ReleaseInfo {
    private Pattern[] groupPattern;
    private Pattern languageTag;
    private Pattern categoryTag;
    private List<String> categoryTags;
    private final MemoryCache matchLastPatternCache = MemoryCache.forObject();
    private final Pattern[][] stopwords = new Pattern[2][];
    private final Pattern[][] blacklist = new Pattern[2][];
    private Set<File> volumeRoots;
    private Pattern structureRootFolderPattern;
    private final Resource<List<String>> releaseGroups = this.lines("release-groups.txt", Cache.ONE_WEEK);
    private final Resource<List<String>> queryBlacklist = this.lines("query-excludes.txt", Cache.ONE_WEEK);
    private final Resource<List<Movie>> movieIndex = this.tsv("moviedb.txt", Cache.ONE_MONTH, this::parseMovie);
    private final Resource<List<Series>> seriesIndex = this.tsv("seriesdb.txt", Cache.ONE_WEEK, this::parseSeries);
    private final Resource<List<SearchResult>> anidbIndex = this.tsv("anidb.txt", Cache.ONE_WEEK, this::parseSearchResult);
    private final Resource<Map<String, Pattern>> seriesMappings = this.mappings("series-mappings.txt", Cache.ONE_WEEK);
    private final Resource<Map<String, Pattern>> mediaSources = this.mappings("media-sources.txt", Cache.ONE_WEEK);
    private final Resource<DiscDB> discDB = this.resource("discdb.txt", Cache.ONE_WEEK, this::parseDiscDB, DiscDB::new).memoize();
    private final Resource<AnimeLists.Model> animeListModel = this.resource("anime-list.xml", Cache.ONE_WEEK, AnimeLists::parseModel, AnimeLists.Model::new).memoize();
    private final Resource<CachedResource.Transform<String, URL>> resources = Resource.lazy(() -> {
        CachedResource.Transform<String, URL> fallback = string -> Settings.getApiResource("data/" + string + ".xz").toURL();
        return ZipEntrySpliterator.stream(this.getClass().getProtectionDomain().getCodeSource().getLocation()).map(Object::hashCode).peek(Logging.severe).<CachedResource.Transform<String, URL>>map(n -> string -> Settings.getApiResource("data/" + n + ".xz").toURL()).findFirst().orElse(fallback);
    });
    private Pattern videoSourcePattern;
    private Pattern[] videoTagPattern;
    private final boolean index;
    private Map<String, Locale> defaultLanguageMap;

    public Map<String, String> getVideoSource(String ... stringArray) throws Exception {
        return this.match(this.getMediaSources(), (CharSequence[])stringArray).flatMap(treeMap -> treeMap.entrySet().stream()).sorted(Comparator.comparingInt(entry -> -((MatchResult)entry.getKey()).group().length())).collect(Collectors.toMap(entry -> (String)entry.getValue(), entry -> ((MatchResult)entry.getKey()).group(), (string, string2) -> string, LinkedHashMap::new));
    }

    public List<String> getVideoTags(String ... stringArray) {
        return this.match(this.getVideoTagPattern(false), (CharSequence[])stringArray).map(string -> {
            if (this.getVideoTagPattern(true).matcher((CharSequence)string).matches()) {
                return Normalization.normalizePunctuation(string);
            }
            return ExpressionFormatMethods.lowerTrail(ExpressionFormatMethods.upperInitial(Normalization.normalizePunctuation(string)));
        }).collect(Collectors.toMap(string -> string.toUpperCase(Locale.ROOT), string -> string, (string, string2) -> string, LinkedHashMap::new)).values().stream().collect(Collectors.toList());
    }

    public String getStereoscopic3D(String ... stringArray) {
        return this.match(this.getStereoscopic3DPattern(), (CharSequence[])stringArray).findFirst().orElse(null);
    }

    public String getReleaseGroup(String ... stringArray) throws Exception {
        if (this.groupPattern == null) {
            this.groupPattern = new Pattern[]{this.getReleaseGroupPattern(true), this.getReleaseGroupPattern(false)};
        }
        for (Pattern pattern : this.groupPattern) {
            String string = this.matchLast(pattern, this.releaseGroups.get(), stringArray);
            if (string == null) continue;
            if (string.lastIndexOf(93) < string.lastIndexOf(91)) {
                return string + "]";
            }
            return string;
        }
        return null;
    }

    public Locale getSubtitleLanguageTag(CharSequence ... charSequenceArray) {
        String string;
        if (this.languageTag == null) {
            this.languageTag = this.getSubtitleLanguageTagPattern();
        }
        return (string = this.matchLast(this.languageTag, null, charSequenceArray)) == null ? null : this.getDefaultLanguageMap().get(string);
    }

    public String getSubtitleCategoryTag(CharSequence ... charSequenceArray) {
        if (this.categoryTag == null || this.categoryTags == null) {
            this.categoryTag = this.getSubtitleCategoryTagPattern();
            this.categoryTags = this.getSubtitleCategoryTags();
        }
        return this.matchLast(this.categoryTag, this.categoryTags, charSequenceArray);
    }

    private String matchLast(Pattern pattern, List<String> list, CharSequence ... charSequenceArray) {
        String string = Arrays.stream(charSequenceArray).filter(Objects::nonNull).map(charSequence -> StringUtilities.matchLastOccurrence(charSequence, pattern)).filter(Objects::nonNull).findFirst().orElse(null);
        if (string != null && list != null) {
            for (String string2 : list) {
                if (string2.startsWith("(") || string2.endsWith(")")) continue;
                Pattern pattern2 = (Pattern)this.matchLastPatternCache.get(string2, object -> this.compileWordPattern(Pattern.quote(string2)));
                string = pattern2.matcher(string).replaceAll(string2);
            }
        }
        return string;
    }

    private Stream<String> match(Pattern pattern, CharSequence ... charSequenceArray) {
        return Arrays.stream(charSequenceArray).filter(Objects::nonNull).flatMap(charSequence -> StringUtilities.streamMatches(charSequence, pattern));
    }

    private Stream<TreeMap<MatchResult, String>> match(Map<String, Pattern> map, CharSequence ... charSequenceArray) {
        return Arrays.stream(charSequenceArray).filter(Objects::nonNull).map(charSequence -> {
            TreeMap<MatchResult, String> treeMap = new TreeMap<>(Comparator.comparingInt(MatchResult::start));
            map.forEach((string, pattern) -> {
                Matcher matcher = pattern.matcher((CharSequence)charSequence);
                while (matcher.find()) {
                    treeMap.put(matcher.toMatchResult(), string);
                }
            });
            return treeMap;
        }).filter(treeMap -> !treeMap.isEmpty());
    }

    public List<String> cleanRelease(Collection<String> collection, boolean bl) throws Exception {
        int n;
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        int n2 = n = bl ? 1 : 0;
        if (this.stopwords[n] == null || this.blacklist[n] == null) {
            Pattern pattern = this.getClutterBracketPattern(bl);
            Pattern pattern2 = this.getReleaseGroupPattern(bl);
            Pattern pattern3 = this.getReleaseGroupTrimPattern();
            Pattern pattern4 = this.getSubtitleLanguageTagPattern();
            Pattern pattern5 = this.getLanguageTagPattern(bl);
            Pattern pattern6 = this.getVideoSourcePattern();
            Pattern pattern7 = this.getVideoTagPattern(false);
            Pattern pattern8 = this.getVideoFormatPattern(bl);
            Pattern pattern9 = this.getStereoscopic3DPattern();
            Pattern pattern10 = this.getResolutionPattern();
            Pattern pattern11 = this.getBlacklistPattern();
            this.stopwords[n] = new Pattern[]{pattern4, pattern5, pattern6, pattern7, pattern8, pattern10, pattern9};
            this.blacklist[n] = new Pattern[]{Normalization.EMBEDDED_CHECKSUM, pattern4, pattern3, pattern11, pattern5, pattern, pattern2, pattern6, pattern7, pattern8, pattern10, pattern9};
        }
        return collection.stream().map(string -> string.replace('_', ' ')).map(string -> {
            String string2 = bl ? this.clean((String)string, this.stopwords[n]) : this.substringBefore((String)string, this.stopwords[n]);
            return Normalization.normalizePunctuation(this.clean(string2, this.blacklist[n]));
        }).filter(string -> !string.isEmpty()).collect(Collectors.toList());
    }

    public String clean(String string, Pattern ... patternArray) {
        for (Pattern pattern : patternArray) {
            string = pattern.matcher(string).replaceAll("");
            string = Normalization.trimTrailingPunctuation(string);
        }
        return string;
    }

    public String substringBefore(String string, Pattern ... patternArray) {
        for (Pattern pattern : patternArray) {
            String string2;
            Matcher matcher = pattern.matcher(string);
            if (!matcher.find() || Normalization.normalizePunctuation(string2 = string.substring(0, matcher.start()).trim()).length() < 3) continue;
            string = string2;
        }
        return string;
    }

    public Set<File> getVolumeRoots() {
        if (this.volumeRoots == null) {
            HashSet<File> hashSet = new HashSet<File>();
            File file = ApplicationFolder.UserHome.getDirectory();
            List<File> list = FileUtilities.getFileSystemRoots();
            hashSet.add(file);
            hashSet.addAll(list);
            if (!Settings.isWindowsApp()) {
                for (File file2 : list) {
                    hashSet.addAll(FileUtilities.getChildren(file2, FileUtilities.FOLDERS));
                }
                for (File file2 : this.getMediaRoots()) {
                    hashSet.addAll(FileUtilities.getChildren(file2, FileUtilities.FOLDERS));
                    hashSet.add(file2);
                }
            }
            if (!Settings.isMacSandbox()) {
                hashSet.addAll(FileUtilities.getChildren(file, FileUtilities.FOLDERS));
            }
            this.volumeRoots = Collections.unmodifiableSet(hashSet);
        }
        return this.volumeRoots;
    }

    public Pattern getStructureRootPattern() throws Exception {
        if (this.structureRootFolderPattern == null) {
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string : this.queryBlacklist.get()) {
                if (!string.startsWith("^") || !string.endsWith("$")) continue;
                arrayList.add(string);
            }
            this.structureRootFolderPattern = Pattern.compile(this.or(arrayList), 2);
        }
        return this.structureRootFolderPattern;
    }

    public Pattern getLanguageTagPattern(boolean bl) {
        if (bl) {
            return Pattern.compile("(?<=[-\\[\\{\\(])" + this.or(this.quoteAll(this.getDefaultLanguageMap().keySet())) + "(?=[-\\]\\}\\)]|$)", 2);
        }
        List<String> list = this.getDefaultLanguageMap().keySet().stream().map(String::toUpperCase).collect(Collectors.toList());
        list.remove("IT");
        return Pattern.compile("(?<!\\p{Alnum})" + this.or(this.quoteAll(list)) + "(?!\\p{Alnum})");
    }

    public Pattern getSubtitleCategoryTagPattern() {
        return Pattern.compile("(?<=^|\\p{Punct}+)" + this.or(this.getSubtitleCategoryTags()) + "(?=\\p{Punct}*\\p{Alpha}*$)", 2);
    }

    public Pattern getSubtitleLanguageTagPattern() {
        return Pattern.compile("(?<=^(?!(?-i:IT)\\b)|\\p{Punct})" + this.or(this.quoteAll(this.getDefaultLanguageMap().keySet())) + "(?=\\d{0,4}(?:[\\s\\p{Punct}]+\\p{Alnum}{0,6})?[\\s\\p{Punct}]*$)", 2);
    }

    public Pattern getResolutionPattern() {
        return Pattern.compile("(?<!\\p{Alnum})(\\d{4}|[6-9]\\d{2})x(\\d{4}|[4-9]\\d{2})(?!\\p{Alnum})");
    }

    public Pattern getVideoFormatPattern(boolean bl) {
        String string = this.getProperty("pattern.video.format");
        return bl ? Pattern.compile("(?<!\\p{Alpha})(" + string + ")(?!\\p{Alnum})", 2) : Pattern.compile(string, 2);
    }

    public Pattern getStereoscopic3DPattern() {
        return this.compileWordPattern(this.getProperty("pattern.video.s3d"));
    }

    public Pattern getRepackPattern() {
        return this.compileWordPattern(this.getProperty("pattern.video.repack"));
    }

    public Pattern getClutterExcludesPattern() {
        return this.compileWordPattern(this.getProperty("pattern.clutter.excludes"));
    }

    public Pattern getClutterFilePattern() {
        return Pattern.compile(this.getProperty("pattern.clutter.file"), 2);
    }

    public Pattern getClutterFolderPattern() {
        return Pattern.compile(this.getProperty("pattern.clutter.folder"), 2);
    }

    public Pattern getClutterTypesPattern() {
        return Pattern.compile(this.getProperty("pattern.clutter.types"), 2);
    }

    public Pattern getSystemFilesPattern() {
        return Pattern.compile(this.getProperty("pattern.system.files"), 2);
    }

    public Pattern getDiskFolderEntryPattern() {
        return Pattern.compile(this.getProperty("pattern.diskfolder.entry"), 2);
    }

    public Pattern getClutterBracketPattern(boolean bl) {
        String string2 = "()[]{}";
        String string3 = bl ? "[[^a-z0-9]&&[^" + Pattern.quote(string2) + "]]" : "\\p{Alpha}";
        return IntStream.range(0, string2.length() / 2).map(n -> n * 2).mapToObj(n -> {
            String open = Pattern.quote(string2.substring(n, n + 1));
            String close = Pattern.quote(string2.substring(n + 1, n + 2));
            String middle = "[^" + open + close + "]+?";
            return open + "(" + middle + open + middle + ")" + close;
        }).collect(Collectors.collectingAndThen(Collectors.joining("|"), string -> Pattern.compile(string, 2)));
    }

    public Pattern getReleaseGroupPattern(boolean bl) throws Exception {
        String string = bl ? "[._ ]|\\p{Alnum}|['`\u00b4\u2018\u2019\u02bb]" : "\\p{Alnum}|['`\u00b4\u2018\u2019\u02bb]";
        String string2 = "(?:(?<!" + string + ")" + this.or(this.releaseGroups.get()) + "(?!" + string + ")(?![. ])[\\p{Punct}]??)+";
        String string3 = this.or("(?<=^[\\P{Alnum}]*)" + string2, string2 + "(?=[\\P{Alnum}]*$)");
        return Pattern.compile(string3, bl ? 0 : 2);
    }

    public Pattern getReleaseGroupTrimPattern() throws Exception {
        return Pattern.compile("(?<=\\[|\\(|^)" + this.or(this.releaseGroups.get()) + "(?=\\]|\\)|\\-)|(?<=\\[|\\(|\\-)" + this.or(this.releaseGroups.get()) + "(?=\\]|\\)|\\.|$)", 2);
    }

    public Pattern getBlacklistPattern() throws Exception {
        return this.compileWordPattern(this.queryBlacklist.get());
    }

    private Pattern compileWordPattern(List<String> list) {
        return Pattern.compile("(?<!\\p{Alnum})" + this.or(list) + "(?!\\p{Alnum})", 2);
    }

    private Pattern compileWordPattern(String string) {
        return Pattern.compile("(?<!\\p{Alnum})(?:" + string + ")(?!\\p{Alnum})", 2);
    }

    public Map<String, Pattern> getSeriesMappings() throws Exception {
        return this.seriesMappings.get();
    }

    public Map<String, Pattern> getMediaSources() throws Exception {
        return this.mediaSources.get();
    }

    public List<SearchResult> getAnidbIndex() throws Exception {
        if (this.index) {
            return this.anidbIndex.get();
        }
        return Collections.emptyList();
    }

    public List<Series> getSeriesIndex() throws Exception {
        if (this.index) {
            return this.seriesIndex.get();
        }
        return Collections.emptyList();
    }

    public List<Movie> getMovieIndex() throws Exception {
        if (this.index) {
            return this.movieIndex.get();
        }
        return Collections.emptyList();
    }

    public DiscDB getDiscDB() throws Exception {
        return this.discDB.get();
    }

    public AnimeLists.Model getAnimeListModel() throws Exception {
        return this.animeListModel.get();
    }

    public List<File> getMediaRoots() {
        String string = this.getProperty("folder.media.roots");
        return RegularExpressions.COMMA.splitAsStream(string).map(File::new).filter(File::canRead).collect(Collectors.toList());
    }

    public List<String> getSubtitleCategoryTags() {
        String string = this.getProperty("pattern.subtitle.tags");
        return RegularExpressions.PIPE.splitAsStream(string).collect(Collectors.toList());
    }

    private Movie parseMovie(String[] stringArray) {
        int n = Integer.parseInt(stringArray[0]);
        int n2 = Integer.parseInt(stringArray[1]);
        int n3 = Integer.parseInt(stringArray[2]);
        String string = stringArray[3];
        String[] stringArray2 = Arrays.copyOfRange(stringArray, 4, stringArray.length);
        return new Movie(n2, string, stringArray2, n3, n, n2, null);
    }

    private Series parseSeries(String[] stringArray) {
        int n = Integer.parseInt(stringArray[0]);
        int n2 = Integer.parseInt(stringArray[1]);
        int n3 = Integer.parseInt(stringArray[2]);
        int n4 = Integer.parseInt(stringArray[3]);
        int n5 = Integer.parseInt(stringArray[4]);
        String string = stringArray[5];
        String[] stringArray2 = Arrays.copyOfRange(stringArray, 6, stringArray.length);
        return new Series(n, string, stringArray2, n5, n4, Series.XID(n, n2, n3));
    }

    private SearchResult parseSearchResult(String[] stringArray) {
        int n = Integer.parseInt(stringArray[0]);
        String string = stringArray[1];
        String[] stringArray2 = Arrays.copyOfRange(stringArray, 2, stringArray.length);
        return new SearchResult(n, string, stringArray2);
    }

    private DiscDB parseDiscDB(InputStream inputStream) throws Exception {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
            DiscDB discDB = new DiscDB();
            bufferedReader.lines().forEach(string -> {
                String[] stringArray = RegularExpressions.TAB.split((CharSequence)string);
                if (stringArray.length >= 12) {
                    int n = Integer.parseInt(stringArray[0]);
                    String string2 = stringArray[1];
                    int n2 = Integer.parseInt(stringArray[2]);
                    String string3 = stringArray[3];
                    String string4 = stringArray[4];
                    int n3 = Integer.parseInt(stringArray[5]);
                    String string5 = stringArray[6];
                    int[] nArray = StringUtilities.matchIntegers(stringArray[7]).stream().mapToInt(Integer::intValue).toArray();
                    int n4 = Integer.parseInt(stringArray[8]);
                    int n5 = Integer.parseInt(stringArray[9]);
                    int[] nArray2 = StringUtilities.matchIntegers(stringArray[10]).stream().mapToInt(Integer::intValue).toArray();
                    String string6 = stringArray[11];
                    discDB.add(n, string2, n2, string3, string4, n3, string5, nArray, n4, n5, nArray2, string6);
                }
            });
            DiscDB discDB2 = discDB;
            return discDB2;
        }
    }

    protected Resource<List<String>> lines(String string2, Duration duration) {
        return this.rows(string2, duration, string -> string).memoize();
    }

    protected <R> Resource<List<R>> tsv(String string2, Duration duration, Function<String[], R> function) {
        return this.rows(string2, duration, string -> function.apply(RegularExpressions.TAB.split((CharSequence)string))).memoize();
    }

    protected Resource<Map<String, Pattern>> mappings(String string2, Duration duration) {
        return this.rows(string2, duration, string -> {
            String[] stringArray = RegularExpressions.TAB.split((CharSequence)string, 2);
            return Collections.singletonMap(stringArray[0], this.compileWordPattern(stringArray[1]));
        }).<Map<String, Pattern>>transform(list -> {
            LinkedHashMap<String, Pattern> linkedHashMap = new LinkedHashMap<>(list.size());
            list.forEach(linkedHashMap::putAll);
            return linkedHashMap;
        }).memoize();
    }

    protected <R> Resource<List<R>> rows(String string, Duration duration, CachedResource.Transform<String, R> transform) {
        return this.resource(string, duration, inputStream -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)inputStream, StandardCharsets.UTF_8));){
                List list = bufferedReader.lines().filter(line -> !line.isEmpty()).map(line -> {
                    try {
                        return transform.transform(line);
                    }
                    catch (Exception exception) {
                        Logging.debug.severe(Logging.message("Parse Error", exception, line));
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                return list;
            }
        }, () -> Collections.emptyList());
    }

    protected <A> Resource<A> resource(String string, Duration duration, CachedResource.Transform<InputStream, A> transform, Supplier<A> supplier) {
        return () -> {
            try {
                return Cache.getCache("data", CacheType.Monthly).stream(string, this.resources.get(), XZInputStream::new, transform).expire(duration).get();
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to load data file", exception));
                return supplier.get();
            }
        };
    }

    public Pattern getVideoSourcePattern() throws Exception {
        if (this.videoSourcePattern == null) {
            this.videoSourcePattern = Pattern.compile(StringUtilities.join(this.getMediaSources().values(), (CharSequence)"|"), 2);
        }
        return this.videoSourcePattern;
    }

    public Pattern getVideoTagPattern(boolean bl) {
        if (this.videoTagPattern == null) {
            String string = this.getProperty("pattern.video.tags");
            this.videoTagPattern = new Pattern[]{this.compileWordPattern(string), Pattern.compile(string)};
        }
        return this.videoTagPattern[bl ? 1 : 0];
    }

    protected String getProperty(String string) {
        return ResourceBundle.getBundle(ReleaseInfo.class.getName()).getString(string);
    }

    public ReleaseInfo(boolean bl) {
        this.index = bl;
    }

    private String or(String ... stringArray) {
        return this.or(Arrays.asList(stringArray));
    }

    private String or(List<String> list) {
        return list.stream().sorted(Collections.reverseOrder(Comparator.comparing(String::length))).collect(Collectors.joining("|", "(?:", ")"));
    }

    private List<String> quoteAll(Collection<String> collection) {
        return collection.stream().map(string -> Pattern.quote(string)).collect(Collectors.toList());
    }

    public Map<String, Locale> getDefaultLanguageMap() {
        if (this.defaultLanguageMap == null) {
            this.defaultLanguageMap = this.getLanguageMap(Locale.ENGLISH, Locale.getDefault());
        }
        return this.defaultLanguageMap;
    }

    public Map<String, Locale> getLanguageMap(Locale ... localeArray) {
        localeArray = (Locale[])Arrays.stream(localeArray).distinct().toArray(Locale[]::new);
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setDecomposition(2);
        collator.setStrength(0);
        TreeMap<String, Locale> treeMap = new TreeMap<>(collator);
        for (String string : Locale.getISOLanguages()) {
            Locale object = Locale.forLanguageTag(string);
            Locale locale = Locale.forLanguageTag(object.getISO3Language());
            treeMap.put(object.getLanguage(), locale);
            treeMap.put(object.getISO3Language(), locale);
            for (Locale locale2 : localeArray) {
                String string2 = Normalizer.normalize(object.getDisplayLanguage(locale2), Normalizer.Form.NFKD);
                treeMap.put(string2.toLowerCase(Locale.ROOT), locale);
            }
        }
        for (Language language : Language.availableLanguages()) {
            Locale locale = language.getLocale();
            treeMap.put(language.getISO2(), locale);
            treeMap.put(language.getISO3(), locale);
            treeMap.put(language.getISO3B(), locale);
            treeMap.put(language.getTag(), locale);
            for (String string : language.getNames()) {
                treeMap.put(string.toLowerCase(Locale.ROOT), locale);
            }
        }
        Locale locale = Locale.forLanguageTag("pob");
        treeMap.put("brazilian", locale);
        treeMap.put("pb", locale);
        treeMap.put("pob", locale);
        treeMap.put("pt-BR", locale);
        treeMap.put("tib", Locale.forLanguageTag("bod"));
        treeMap.put("cze", Locale.forLanguageTag("ces"));
        treeMap.put("wel", Locale.forLanguageTag("cym"));
        treeMap.put("ger", Locale.forLanguageTag("deu"));
        treeMap.put("gre", Locale.forLanguageTag("ell"));
        treeMap.put("baq", Locale.forLanguageTag("eus"));
        treeMap.put("per", Locale.forLanguageTag("fas"));
        treeMap.put("fre", Locale.forLanguageTag("fra"));
        treeMap.put("arm", Locale.forLanguageTag("hye"));
        treeMap.put("ice", Locale.forLanguageTag("isl"));
        treeMap.put("geo", Locale.forLanguageTag("kat"));
        treeMap.put("mac", Locale.forLanguageTag("mkd"));
        treeMap.put("mao", Locale.forLanguageTag("mri"));
        treeMap.put("may", Locale.forLanguageTag("msa"));
        treeMap.put("bur", Locale.forLanguageTag("mya"));
        treeMap.put("dut", Locale.forLanguageTag("nld"));
        treeMap.put("rum", Locale.forLanguageTag("ron"));
        treeMap.put("slo", Locale.forLanguageTag("slk"));
        treeMap.put("alb", Locale.forLanguageTag("sqi"));
        treeMap.put("chi", Locale.forLanguageTag("zho"));
        treeMap.remove("");
        treeMap.remove("II");
        treeMap.remove("III");
        treeMap.remove("VI");
        treeMap.remove("hi");
        treeMap.remove("uk");
        treeMap.remove("my");
        return Collections.unmodifiableMap(treeMap);
    }
}

