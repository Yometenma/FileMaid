package net.filemaid.web;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.AbstractEpisodeListProvider;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.Person;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;
import net.filemaid.web.XDB;

public class TheTVDBClientV2
extends AbstractEpisodeListProvider
implements ArtworkProvider {
    private final URI api;
    private final String apikey;
    private static final EnumSet<SortOrder> SUPPORTED_ORDER = EnumSet.of(SortOrder.Airdate, SortOrder.DVD, SortOrder.Absolute, SortOrder.Date);
    private static final int ID_FLOOR = 70000;
    private String token = null;
    private Instant tokenExpireInstant = null;
    private Duration tokenExpireDuration = Duration.ofHours(23L);
    private final Object authorizationLock = new Object();
    private static final Duration UPDATE_LIMIT = Duration.ofDays(7L);

    private static URI defaultEndpoint() {
        return URI.create(System.getProperty("net.filemaid.TheTVDB.url", "https://api.thetvdb.com/"));
    }

    private static URI defaultBannerMirror() {
        return URI.create(System.getProperty("net.filemaid.TheTVDB.banner.url", "https://artworks.thetvdb.com/banners/"));
    }

    public TheTVDBClientV2(URI uRI, String string) {
        this.api = uRI;
        this.apikey = string;
    }

    public TheTVDBClientV2(String string) {
        this(TheTVDBClientV2.defaultEndpoint(), string);
    }

    @Override
    public String getName() {
        return "TheTVDB";
    }

    @Override
    public String getIdentifier() {
        return "TheTVDB";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.thetvdb");
    }

    @Override
    public boolean hasSeasonSupport() {
        return true;
    }

    @Override
    public SortOrder vetoRequestParameter(SortOrder sortOrder) {
        return SUPPORTED_ORDER.contains((Object)sortOrder) ? sortOrder : SortOrder.Airdate;
    }

    private Locale getDefaultLocale() {
        return Locale.ENGLISH;
    }

    private boolean isDefaultLocale(Locale locale) {
        return locale != null && this.getDefaultLocale().getLanguage().equals(locale.getLanguage());
    }

    @Override
    public SearchResult id(String string) throws Exception {
        SearchResult searchResult = super.id(string);
        return searchResult == null || searchResult.getId() < 70000 ? null : searchResult;
    }

    @Override
    public SearchResult id(Series series) {
        Integer n = series.getExternalId(XDB.TheTVDB);
        return n == null ? null : new SearchResult((int)n, series.getName(), series.getAliasNames());
    }

    protected Object postJson(String string, Object object) throws Exception {
        ByteBuffer byteBuffer = WebRequest.post(this.getEndpoint(string), JsonUtilities.json(object).getBytes(StandardCharsets.UTF_8), "application/json", null);
        Logging.debug.finest(WebRequest.log(byteBuffer));
        return JsonUtilities.readJson(StandardCharsets.UTF_8.decode(byteBuffer));
    }

    protected Object requestJson(String string, Locale locale, Duration duration) throws Exception {
        Cache cache = Cache.getConcurrentCache((String)(locale == null || locale == Locale.ROOT ? this.getName() : this.getName() + "_" + locale.getLanguage()), CacheType.Monthly);
        return cache.json(string, this::getEndpoint).fetch(CachedResource.fetchIfModified(() -> this.getRequestHeader(locale))).expire(duration).get();
    }

    protected URL getEndpoint(String string) throws Exception {
        return WebRequest.newURL(this.api, string);
    }

    private Map<String, String> getRequestHeader(Locale locale) throws Exception {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(3);
        String string = this.getLanguageCode(locale);
        if (string != null) {
            linkedHashMap.put("Accept-Language", string);
        }
        linkedHashMap.put("Accept", "application/json");
        linkedHashMap.put("Authorization", "Bearer " + this.getAuthorizationToken());
        return linkedHashMap;
    }

    private String getAuthorizationToken() throws Exception {
        Object object = this.authorizationLock;
        synchronized (object) {
            if (this.token == null || this.tokenExpireInstant != null && Instant.now().isAfter(this.tokenExpireInstant)) {
                Object object2 = this.postJson("login", Collections.singletonMap("apikey", this.apikey));
                this.token = JsonUtilities.getString(object2, "token");
                this.tokenExpireInstant = Instant.now().plus(this.tokenExpireDuration);
            }
            return this.token;
        }
    }

    protected List<SearchResult> search(String string, Map<String, Object> map2, Locale locale, Duration duration) throws Exception {
        Object object = this.requestJson(string + "?" + WebRequest.encodeParameters(map2), locale, duration);
        return JsonUtilities.streamJsonObjects(object, "data").map(map -> {
            int n = JsonUtilities.getInteger(map, "id");
            String seriesName = JsonUtilities.getString(map, "seriesName");
            if (seriesName == null || seriesName.startsWith("**") || seriesName.endsWith("**") || seriesName.startsWith("DUPLICATE")) {
                Logging.debug.finest(Logging.format("Ignore invalid series: %s [%s]", seriesName, n));
                return null;
            }
            String[] stringArray = (String[])Arrays.stream(JsonUtilities.getArray(map, "aliases")).toArray(String[]::new);
            String string2 = JsonUtilities.getString(map, "slug");
            String string3 = JsonUtilities.getString(map, "network");
            String string4 = JsonUtilities.getString(map, "status");
            SimpleDate simpleDate = JsonUtilities.getStringValue(map, "firstAired", SimpleDate::parse);
            String string5 = JsonUtilities.getString(map, "overview");
            return new SearchResultDetails(n, seriesName, stringArray, simpleDate, string5, null, null, null, null, string2, string3, string4);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    protected List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
        Movie movie;
        List<SearchResult> list = this.search("search/series", Collections.singletonMap("name", string), locale, Cache.ONE_WEEK);
        if (list.isEmpty() && (movie = Movie.matchNameYear(string)) != null) {
            list = this.fetchSearchResult(movie.getName(), locale);
        }
        if (list.isEmpty() && !this.isDefaultLocale(locale)) {
            list = this.fetchSearchResult(string, this.getDefaultLocale());
        }
        return list;
    }

    @Override
    public SeriesDetails getSeriesInfo(int n, Locale locale) throws Exception {
        return this.getSeriesInfo(new SearchResult(n), locale);
    }

    public Object requestSeriesInfo(int n, Locale locale) throws Exception {
        return this.requestJson("series/" + n, locale, Cache.ONE_MONTH);
    }

    /*
     * Exception decompiling
     */
    @Override
    public SeriesDetails getSeriesInfo(SearchResult var1_1, Locale var2_2) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriterToArgs(StaticFunctionInvokation.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriter(StaticFunctionInvokation.java:90)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.rewriteExpressions(StructuredExpressionStatement.java:70)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    protected Object requestEpisodes(SeriesInfo seriesInfo, int n, Duration duration) throws Exception {
        try {
            return this.requestJson("series/" + seriesInfo.getId() + "/episodes?page=" + n, seriesInfo.getLanguage(), duration);
        }
        catch (InvalidResponseException invalidResponseException) {
            if (n != 1) {
                throw invalidResponseException;
            }
            Logging.debug.warning(Logging.cause(seriesInfo, invalidResponseException));
            return null;
        }
    }

    @Override
    protected AbstractEpisodeListProvider.SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        SeriesDetails seriesDetails = this.getSeriesInfo(searchResult, locale);
        seriesDetails.setOrder(sortOrder.name());
        boolean bl = seriesDetails.isContinuing() && seriesDetails.isAiringToday();
        boolean bl2 = !seriesDetails.isContinuing() && !seriesDetails.isRecent();
        ArrayList<Episode> arrayList = new ArrayList<Episode>();
        ArrayList<Episode> arrayList2 = new ArrayList<Episode>();
        int n = 1;
        for (int i = 1; i <= n; ++i) {
            Duration duration = bl2 ? Cache.ONE_MONTH : (bl && i == n ? Cache.ONE_DAY : Cache.ONE_WEEK);
            Object object = this.requestEpisodes(seriesDetails, i, duration);
            Integer n2 = JsonUtilities.getInteger(JsonUtilities.getMap(object, "links"), "last");
            if (n2 != null) {
                n = n2;
            }
            JsonUtilities.streamJsonObjects(object, "data").forEach(map -> {
                Episode episode = this.getEpisodeValue(map, sortOrder, seriesDetails);
                if (episode.getTitle() == null && !this.isDefaultLocale(seriesDetails.getLanguage())) {
                    episode = this.localizeTitle(episode, searchResult, sortOrder, this.getDefaultLocale());
                }
                if (episode.getSpecial() == null) {
                    arrayList.add(episode);
                } else {
                    arrayList2.add(episode);
                }
            });
        }
        arrayList.sort(EpisodeUtilities.episodeComparator());
        arrayList2.sort(EpisodeUtilities.episodeComparator());
        arrayList.addAll(arrayList2);
        return new AbstractEpisodeListProvider.SeriesData(seriesDetails, arrayList);
    }

    private Episode localizeTitle(Episode episode, SearchResult searchResult, SortOrder sortOrder, Locale locale) {
        try {
            return this.getEpisodeList(searchResult, sortOrder, locale).stream().filter(episode::equals).findFirst().map(Episode::getTitle).map(episode::title).orElse(episode);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Failed to retrieve localized episode title", locale, exception));
            return episode;
        }
    }

    private Episode getEpisodeValue(Object object, SortOrder sortOrder, SeriesInfo seriesInfo) {
        Integer n = JsonUtilities.getInteger(object, "id");
        String string = JsonUtilities.getString(object, "episodeName");
        Integer n2 = JsonUtilities.getInteger(object, "absoluteNumber");
        SimpleDate simpleDate = JsonUtilities.getStringValue(object, "firstAired", SimpleDate::parse);
        Integer n3 = JsonUtilities.getInteger(object, "airedEpisodeNumber");
        Integer n4 = JsonUtilities.getInteger(object, "airedSeason");
        if (sortOrder == SortOrder.DVD) {
            Integer n5 = JsonUtilities.getInteger(object, "dvdSeason");
            BigDecimal bigDecimal = JsonUtilities.getDecimal(object, "dvdEpisodeNumber");
            if (n5 != null && bigDecimal != null) {
                n4 = n5;
                n3 = ((Number)bigDecimal).intValue();
                if (n3.doubleValue() != ((Number)bigDecimal).doubleValue()) {
                    Logging.debug.finest(Logging.format("[%s] Coerce episode number [%s] to [%s]", seriesInfo, bigDecimal, n3));
                }
            }
        } else if (sortOrder == SortOrder.Absolute && n2 != null && n2 > 0) {
            n4 = null;
            n3 = n2;
        } else if (sortOrder == SortOrder.Date && simpleDate != null) {
            n4 = null;
            n3 = simpleDate.getYear() * 10000 + simpleDate.getMonth() * 100 + simpleDate.getDay();
        }
        if (n4 == null || n4 > 0) {
            return new Episode(seriesInfo.getName(), n4, n3, string, n2, null, simpleDate, null, n, null, new SeriesInfo(seriesInfo));
        }
        return new Episode(seriesInfo.getName(), null, null, string, n2, n3, simpleDate, null, n, null, new SeriesInfo(seriesInfo));
    }

    @Override
    public URI getEpisodeListLink(SearchResult searchResult) {
        return URI.create(Link.TheTVDB.getURL(searchResult));
    }

    @Override
    public EpisodeDetails getEpisodeInfo(Episode episode, Locale locale) throws Exception {
        return this.getEpisodeInfo(episode.getId(), locale);
    }

    @Override
    public List<Artwork> getArtwork(int n, Locale locale) throws Exception {
        return this.getArtwork(n, "poster", locale);
    }

    @Override
    public List<Artwork> getArtwork(int n, String string, Locale locale) throws Exception {
        Object object = this.requestJson("series/" + n + "/images/query?keyType=" + string, locale, Cache.ONE_MONTH);
        List<Artwork> list = JsonUtilities.streamJsonObjects(object, "data").map(map -> {
            String string2 = JsonUtilities.getString(map, "subKey");
            String string3 = JsonUtilities.getString(map, "resolution");
            URL uRL = JsonUtilities.getStringValue(map, "fileName", this::resolveImage);
            Double d = JsonUtilities.getDouble(JsonUtilities.getMap(map, "ratingsInfo"), "average");
            if (uRL == null) {
                Logging.debug.warning(Logging.message("Bad artwork response", JsonUtilities.json(map)));
                return null;
            }
            return new Artwork(uRL, locale, d, "tvdb", string, string2, string3);
        }).filter(Objects::nonNull).sorted(Artwork.relevanceOrder(locale, this.getDefaultLocale())).collect(Collectors.toList());
        if (list.isEmpty() && !this.isDefaultLocale(locale)) {
            return this.getArtwork(n, string, this.getDefaultLocale());
        }
        return list;
    }

    protected URL resolveImage(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        try {
            return WebRequest.newURL(TheTVDBClientV2.defaultBannerMirror(), string);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Bad Image URL", string, exception));
            return null;
        }
    }

    public List<String> getLanguages() throws Exception {
        Object object = this.requestJson("languages", Locale.ROOT, Cache.NEVER);
        return JsonUtilities.streamJsonObjects(object, "data").map(map -> JsonUtilities.getString(map, "abbreviation")).collect(Collectors.toList());
    }

    public List<Person> getActors(int n, Locale locale) throws Exception {
        Object object = this.requestJson("series/" + n + "/actors", locale, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "data").map(map -> {
            Integer id = JsonUtilities.getInteger(map, "id");
            String string = JsonUtilities.getString(map, "name");
            String string2 = JsonUtilities.getString(map, "role");
            Integer n2 = JsonUtilities.getInteger(map, "sortOrder");
            URL uRL = JsonUtilities.getStringValue(map, "image", this::resolveImage);
            return new Person(id, string, string2, "Actor", null, n2, uRL);
        }).sorted(Person.CREDIT_ORDER).collect(Collectors.toList());
    }

    public Object requestEpisodeInfo(int n, Locale locale) throws Exception {
        Object object = this.requestJson("episodes/" + n, locale, Cache.ONE_MONTH);
        return JsonUtilities.getMap(object, "data");
    }

    public EpisodeDetails getEpisodeInfo(int n, Locale locale) throws Exception {
        Object object = this.requestEpisodeInfo(n, locale);
        Integer n2 = JsonUtilities.getInteger(object, "seriesId");
        String string = JsonUtilities.getString(object, "productionCode");
        String string2 = JsonUtilities.getString(object, "overview");
        Double d = JsonUtilities.getDouble(object, "siteRating");
        Integer n3 = JsonUtilities.getInteger(object, "siteRatingCount");
        ArrayList<Person> arrayList = new ArrayList<Person>();
        for (Object object2 : JsonUtilities.getArray(object, "directors")) {
            arrayList.add(new Person(object2.toString(), "Director"));
        }
        for (Object object2 : JsonUtilities.getArray(object, "writers")) {
            arrayList.add(new Person(object2.toString(), "Writer"));
        }
        for (Object object2 : JsonUtilities.getArray(object, "guestStars")) {
            arrayList.add(new Person(object2.toString(), "Guest Star"));
        }
        SeriesInfo seriesInfo = new SeriesInfo(this, SortOrder.Airdate, locale, n2, "TV Series");
        Episode episode = this.getEpisodeValue(object, SortOrder.Airdate, seriesInfo);
        return new EpisodeDetails(episode, string, string2, d, n3, null, null, arrayList);
    }

    public Map<Integer, Instant> updates(Instant instant3, Instant instant4) throws Exception {
        if (!instant3.isBefore(instant4)) {
            return Collections.emptyMap();
        }
        if (Duration.between(instant3, instant4).compareTo(UPDATE_LIMIT) > 0) {
            HashMap<Integer, Instant> hashMap = new HashMap<Integer, Instant>();
            Instant instant5 = instant3;
            while (instant5.isBefore(instant4)) {
                hashMap.putAll(this.updates(instant5, instant5.plus(UPDATE_LIMIT)));
                instant5 = instant5.plus(UPDATE_LIMIT);
            }
            return hashMap;
        }
        LinkedHashMap<String, Long> linkedHashMap = new LinkedHashMap<String, Long>(2);
        linkedHashMap.put("fromTime", instant3.getEpochSecond());
        linkedHashMap.put("toTime", instant4.getEpochSecond());
        Object object = this.requestJson("/updated/query?" + WebRequest.encodeParameters(linkedHashMap), Locale.ROOT, Cache.NEVER);
        return JsonUtilities.streamJsonObjects(object, "data").collect(Collectors.toMap(map -> JsonUtilities.getInteger(map, "id"), map -> JsonUtilities.getEpochTime(map, "lastUpdated"), (instant, instant2) -> instant, LinkedHashMap::new));
    }

    private static /* synthetic */ Instant lambda$getSeriesInfo$7(String string) {
        return Instant.ofEpochSecond(Long.parseLong(string));
    }
}

