package net.filemaid.web;

import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.ResourceManager;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.AbstractEpisodeListProvider;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.Person;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;
import net.filemaid.web.XDB;

public class TheTVDBClientV4
extends AbstractEpisodeListProvider
implements ArtworkProvider {
    private final URI endpoint;
    private final String apikey;
    private final String pin;
    private final String defaultLanguage;
    private static final int ID_FLOOR = 70000;
    private final Resource<Map<Integer, ArtworkType>> artworkTypes = Resource.lazy(this::getArtworkTypes);

    private static URI defaultEndpoint() {
        return URI.create("https://api4.thetvdb.com/v4/");
    }

    private static String defaultLanguage() {
        return System.getProperty("net.filemaid.TheTVDB.language", "eng");
    }

    public TheTVDBClientV4(URI uRI, String string, String string2, String string3) {
        this.endpoint = uRI;
        this.apikey = string;
        this.pin = string2;
        this.defaultLanguage = string3;
    }

    public TheTVDBClientV4(String string) {
        this(TheTVDBClientV4.defaultEndpoint(), string, null, TheTVDBClientV4.defaultLanguage());
    }

    @Override
    public String getIdentifier() {
        return "TheTVDB";
    }

    @Override
    public String getName() {
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

    protected Object requestJson(String string, Duration duration) throws Exception {
        Cache cache = Cache.getConcurrentCache(this.getName(), CacheType.Monthly);
        return cache.json(string, this::getEndpoint).fetch(CachedResource.fetchIfModified(this::getRequestHeader)).expire(duration).get();
    }

    protected URL getEndpoint(String string) throws Exception {
        return WebRequest.newURL(this.endpoint, string);
    }

    private Map<String, String> getRequestHeader() throws Exception {
        return WebRequest.mapParameters("Authorization", "Bearer " + this.getAuthorizationToken(), "Accept", "application/json");
    }

    private synchronized String getAuthorizationToken() throws Exception {
        Cache cache = Cache.getConcurrentCache(this.getName(), CacheType.Monthly);
        return (String)cache.computeIf("token", Cache.isStale(Cache.ONE_MONTH), element -> {
            Object object = this.postJson("login", WebRequest.mapParameters("apikey", this.apikey, "pin", this.pin));
            return JsonUtilities.getString(JsonUtilities.getMap(object, "data"), "token");
        });
    }

    @Override
    protected List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
        Movie movie = Movie.matchNameYear(string);
        if (movie == null) {
            return this.fetchSearchResult(string, null, locale);
        }
        List<SearchResult> list = this.fetchSearchResult(movie.getName(), movie.getYear(), locale);
        if (list.stream().anyMatch(searchResult -> movie.getName().equalsIgnoreCase(searchResult.getName()))) {
            return list;
        }
        List<SearchResult> list2 = this.fetchSearchResult(string, null, locale);
        return Stream.of(list, list2).flatMap(Collection::stream).distinct().collect(Collectors.toList());
    }

    protected List<SearchResult> fetchSearchResult(String string, Integer n, Locale locale) throws Exception {
        Object object = this.requestJson("search?" + WebRequest.encodeParameters("q", string, "year", n == null ? null : n.toString(), "type", "series"), Cache.ONE_WEEK);
        return JsonUtilities.streamJsonObjects(object, "data").<SearchResult>map(var2_2 -> {
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
             *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment.rewriteExpressions(StructuredAssignment.java:146)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
             *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
             *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1050)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
             *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
             *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
             *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
             *     at org.benf.cfr.reader.Main.main(Main.java:54)
             */
            throw new IllegalStateException("Decompilation failed");
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected Optional<String> getLocalizedString(Object object, String string, Locale locale) {
        return Optional.ofNullable(JsonUtilities.getMap(object, string).get(this.getLanguageCode(locale))).map(Object::toString);
    }

    @Override
    public SeriesDetails getSeriesInfo(int n, Locale locale) throws Exception {
        return this.getSeriesInfo(new SearchResult(n), locale);
    }

    public Object requestSeriesInfo(int n, Locale locale) throws Exception {
        Object object = this.requestJson("series/" + n + "/extended", Cache.ONE_MONTH);
        if (object == null) {
            throw new LookupException((Object)"TVDB ID not found", n);
        }
        return JsonUtilities.getMap(object, "data");
    }

    public Object requestSeriesTranslations(int n, String string) throws Exception {
        Object object = this.requestJson("series/" + n + "/translations/" + string, Cache.ONE_MONTH);
        return JsonUtilities.getMap(object, "data");
    }

    private Object requestEpisodePage(int n, String string, String string2, int n2, Duration duration) throws Exception {
        if (string2 == null) {
            return this.requestJson("series/" + n + "/episodes/" + string + "?page=" + n2, duration);
        }
        return this.requestJson("series/" + n + "/episodes/" + string + "/" + string2 + "?page=" + n2, duration);
    }

    private List<Episode> requestEpisodeList(SeriesInfo seriesInfo, SortOrder sortOrder, String string, Duration duration) throws Exception {
        ArrayList<Episode> arrayList = new ArrayList<Episode>();
        ArrayList<Episode> arrayList2 = new ArrayList<Episode>();
        int n = 1;
        for (int i = 0; i < n; ++i) {
            Object object = this.requestEpisodePage(seriesInfo.getId(), this.getSeasonType(sortOrder), string, i, duration);
            Object[] objectArray = JsonUtilities.getArray(JsonUtilities.getMap(object, "data"), "episodes");
            Map<Object, Object> map = JsonUtilities.getMap(object, "links");
            for (Object object2 : objectArray) {
                boolean bl;
                Integer n2 = JsonUtilities.getInteger(object2, "id");
                String string2 = JsonUtilities.getString(object2, "name");
                SimpleDate simpleDate = JsonUtilities.getStringValue(object2, "aired", SimpleDate::parse);
                Integer n3 = JsonUtilities.getInteger(object2, "runtime");
                Integer n4 = JsonUtilities.getInteger(object2, "number");
                Integer n5 = JsonUtilities.getInteger(object2, "seasonNumber");
                String string3 = JsonUtilities.getString(object2, "seasonName");
                Integer n6 = JsonUtilities.getInteger(object2, "absoluteNumber");
                boolean bl2 = bl = n5 == null || n5 > 0;
                if (n6 == null || n6 <= 0) {
                    n6 = null;
                }
                if (sortOrder == SortOrder.Absolute) {
                    n5 = null;
                    n6 = n4;
                }
                if (sortOrder == SortOrder.Date && simpleDate != null) {
                    n5 = null;
                    n4 = simpleDate.getYear() * 10000 + simpleDate.getMonth() * 100 + simpleDate.getDay();
                }
                if (bl) {
                    arrayList.add(new Episode(seriesInfo.getName(), n5, n4, string2, n6, null, simpleDate, n3, n2, string3, new SeriesInfo(seriesInfo)));
                    continue;
                }
                arrayList2.add(new Episode(seriesInfo.getName(), null, null, string2, n6, n4, simpleDate, n3, n2, string3, new SeriesInfo(seriesInfo)));
            }
            if (objectArray.length == 0 || JsonUtilities.getString(map, "next") == null) break;
            int n7 = JsonUtilities.getInteger(map, "total_items");
            int n8 = JsonUtilities.getInteger(map, "page_size");
            n = n7 / n8 + 1;
        }
        arrayList.addAll(arrayList2);
        return arrayList;
    }

    private int[] getSeasons(SearchResult searchResult, Locale locale, SortOrder sortOrder) throws Exception {
        Object object = this.requestSeriesInfo(searchResult.getId(), locale);
        return JsonUtilities.streamJsonObjects(object, "seasons").mapToInt(map -> {
            Integer n = JsonUtilities.getInteger(map, "id");
            String string = JsonUtilities.getString(JsonUtilities.getMap(map, "type"), "type");
            return this.getSeasonType(sortOrder).equals(string) ? n : 0;
        }).filter(n -> n > 0).toArray();
    }

    private List<Episode> requestEpisodeListBySeason(int[] nArray, SeriesInfo seriesInfo, SortOrder sortOrder, String string, Duration duration) throws Exception {
        ArrayList<Episode> arrayList = new ArrayList<Episode>();
        for (int n : nArray) {
            Object object = this.requestJson("seasons/" + n + "/extended", duration);
            for (Object object2 : JsonUtilities.getArray(JsonUtilities.getMap(object, "data"), "episodes")) {
                boolean bl;
                Integer n2 = JsonUtilities.getInteger(object2, "id");
                String string2 = JsonUtilities.getString(object2, "name");
                SimpleDate simpleDate = JsonUtilities.getStringValue(object2, "aired", SimpleDate::parse);
                Integer n3 = JsonUtilities.getInteger(object2, "runtime");
                Integer n4 = JsonUtilities.getInteger(object2, "number");
                Integer n5 = JsonUtilities.getInteger(object2, "seasonNumber");
                String string3 = JsonUtilities.getString(object2, "seasonName");
                Integer n6 = JsonUtilities.getInteger(object2, "absoluteNumber");
                boolean bl2 = bl = n5 == null || n5 > 0;
                if (n6 == null || n6 <= 0) {
                    n6 = null;
                }
                if (bl) {
                    arrayList.add(new Episode(seriesInfo.getName(), n5, n4, string2, n6, null, simpleDate, n3, n2, string3, new SeriesInfo(seriesInfo)));
                    continue;
                }
                arrayList.add(new Episode(seriesInfo.getName(), null, null, string2, n6, n4, simpleDate, n3, n2, string3, new SeriesInfo(seriesInfo)));
            }
        }
        arrayList.sort(EpisodeUtilities.EPISODE_NUMBERS_COMPARATOR);
        return arrayList;
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

    @Override
    protected AbstractEpisodeListProvider.SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        String[] stringArray;
        boolean bl;
        SeriesDetails seriesDetails = this.getSeriesInfo(searchResult, locale);
        seriesDetails.setOrder(sortOrder.name());
        String string = this.getLanguageCode(locale);
        boolean bl2 = seriesDetails.isContinuing() && seriesDetails.isAiringToday();
        boolean bl3 = bl = !seriesDetails.isContinuing() && !seriesDetails.isRecent();
        Duration duration = bl ? Cache.ONE_MONTH : (bl2 ? Cache.ONE_DAY : Cache.ONE_WEEK);
        List<Episode> list = this.requestEpisodeList(seriesDetails, sortOrder, string, duration);
        if (list.isEmpty() && sortOrder == SortOrder.Story) {
            list = this.requestEpisodeListBySeason(this.getSeasons(searchResult, locale, sortOrder), seriesDetails, sortOrder, string, duration);
        }
        if (list.isEmpty() && sortOrder != SortOrder.Airdate) {
            list = this.requestEpisodeList(seriesDetails, SortOrder.Airdate, string, duration);
        }
        for (String string3 : stringArray = (String[])Stream.of(this.defaultLanguage, seriesDetails.getOriginalLanguage()).filter(string2 -> string2 != null && !string2.isEmpty() && !string2.equals(string)).distinct().toArray(String[]::new)) {
            if (list.stream().allMatch(episode -> episode.getTitle() != null)) break;
            list = this.mergeEpisodes(list, this.requestEpisodeList(seriesDetails, sortOrder, string3, duration));
        }
        return new AbstractEpisodeListProvider.SeriesData(seriesDetails, list);
    }

    private List<Episode> mergeEpisodes(List<Episode> list, List<Episode> list2) {
        Map<Integer, Episode> map = list2.stream().collect(Collectors.toMap(episode -> episode.getId(), episode -> episode, (episode, episode2) -> episode));
        return list.stream().map(episode -> {
            Episode episode2 = (Episode)map.get(episode.getId());
            if (episode2 != null) {
                if (episode.getAbsolute() == null && episode2.getAbsolute() != null) {
                    episode = episode.absolute(episode2.getAbsolute());
                }
                if (episode.getTitle() == null && episode2.getTitle() != null) {
                    episode = episode.title(episode2.getTitle());
                }
                if (episode.getGroup() == null && episode2.getGroup() != null) {
                    episode = episode.group(episode2.getGroup());
                }
            }
            return episode;
        }).collect(Collectors.toList());
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
    public List<Artwork> getArtwork(int n3, Locale locale) throws Exception {
        Object object = this.requestSeriesInfo(n3, locale);
        Map<Integer, ArtworkType> map2 = this.artworkTypes.get();
        Map map4 = JsonUtilities.streamJsonObjects(object, "seasons").collect(Collectors.toMap(map -> JsonUtilities.getInteger(map, "id"), map -> JsonUtilities.getInteger(map, "number"), (n, n2) -> n, HashMap::new));
        return JsonUtilities.streamJsonObjects(object, "artworks").map(map3 -> {
            Integer n;
            ArtworkType artworkType = (ArtworkType)map2.get(JsonUtilities.getInteger(map3, "type"));
            URL uRL = JsonUtilities.getStringValue(map3, "image", string -> WebRequest.parseURL(string));
            Locale language = JsonUtilities.getStringValue(map3, "language", this::parseLanguageCode);
            Double d = JsonUtilities.getDouble(map3, "score");
            Integer n2 = JsonUtilities.getInteger(map3, "seasonId");
            n = n2 == null ? null : (Integer)map4.get(n2);
            if (artworkType == null || uRL == null) {
                Logging.debug.warning(Logging.message("Bad artwork response", JsonUtilities.json(map3)));
                return null;
            }
            return new Artwork(uRL, language, d, "tvdb", artworkType.type, n, artworkType.slug, artworkType.size);
        }).filter(Objects::nonNull).sorted(Artwork.relevanceOrder(locale, Locale.ENGLISH)).collect(Collectors.toList());
    }

    protected String getSeasonType(SortOrder sortOrder) {
        switch (sortOrder) {
            case Absolute: {
                return "absolute";
            }
            case DVD: {
                return "dvd";
            }
            case Digital: {
                return "alternate";
            }
            case Story: {
                return "altdvd";
            }
            case Production: {
                return "regional";
            }
            case Official: {
                return "official";
            }
        }
        return "default";
    }

    @Override
    protected String getLanguageCode(Locale locale) {
        if (locale == null) {
            return null;
        }
        switch (locale.getCountry()) {
            case "BR": {
                return "pt";
            }
            case "TW": {
                return "zhtw";
            }
        }
        return locale.getISO3Language();
    }

    protected Locale parseLanguageCode(String string) {
        switch (string) {
            case "pt": {
                return Locale.forLanguageTag("pt-BR");
            }
            case "zhtw": {
                return Locale.forLanguageTag("zh-TW");
            }
        }
        return Locale.forLanguageTag(string);
    }

    public Map<String, String> getLanguages() throws Exception {
        Object object = this.requestJson("languages", Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "data").collect(Collectors.toMap(map -> JsonUtilities.getString(map, "id"), map -> JsonUtilities.getString(map, "name")));
    }

    public Object requestEpisodeInfo(int n, Locale locale) throws Exception {
        LinkedHashMap<Object, Object> linkedHashMap = new LinkedHashMap<Object, Object>();
        Object object = this.requestJson("episodes/" + n + "/extended", Cache.ONE_MONTH);
        linkedHashMap.putAll(JsonUtilities.getMap(object, "data"));
        String string2 = this.getLanguageCode(locale);
        List list = Stream.of("nameTranslations", "overviewTranslations").flatMap(string -> Stream.of(JsonUtilities.getStringArray(linkedHashMap, string))).collect(Collectors.toList());
        if (list.contains(string2)) {
            Object object2 = this.requestJson("episodes/" + n + "/translations/" + string2, Cache.ONE_MONTH);
            linkedHashMap.putAll(JsonUtilities.getMap(object2, "data"));
        }
        return linkedHashMap;
    }

    public EpisodeDetails getEpisodeInfo(int n, Locale locale) throws Exception {
        Object object = this.requestEpisodeInfo(n, locale);
        Integer n2 = JsonUtilities.getInteger(object, "id");
        Integer n3 = JsonUtilities.getInteger(object, "seriesId");
        Integer n4 = JsonUtilities.getInteger(object, "seasonNumber");
        Integer n5 = JsonUtilities.getInteger(object, "number");
        String string2 = JsonUtilities.getString(object, "name");
        String string3 = JsonUtilities.getString(object, "productionCode");
        String string4 = JsonUtilities.getString(object, "overview");
        Integer n6 = JsonUtilities.getInteger(object, "runtime");
        SimpleDate simpleDate = JsonUtilities.getStringValue(object, "aired", SimpleDate::parse);
        URL uRL = JsonUtilities.getStringValue(object, "image", string -> WebRequest.parseURL(string));
        String string5 = "https://thetvdb.com/series/" + n3 + "/episodes/" + n2;
        List<Person> list = JsonUtilities.streamJsonObjects(object, "characters").map(map -> {
            Integer personId = JsonUtilities.getInteger(map, "peopleId");
            String personName = JsonUtilities.getString(map, "personName");
            String characterName = JsonUtilities.getString(map, "name");
            String peopleType = JsonUtilities.getString(map, "peopleType");
            Integer sort = JsonUtilities.getInteger(map, "sort");
            URL image = JsonUtilities.getStringValue(map, "image", string -> WebRequest.parseURL(string));
            return new Person(personId, personName, characterName, peopleType, null, sort == null || sort == 0 ? null : sort, image);
        }).sorted(Person.CREDIT_ORDER).collect(Collectors.toList());
        SeriesInfo seriesInfo = new SeriesInfo(this, SortOrder.Airdate, locale, n3, "TV Series");
        Episode episode = new Episode(null, n4, n5, string2, null, null, simpleDate, n6, n2, null, seriesInfo);
        return new EpisodeDetails(episode, string3, string4, null, null, string5, uRL, list);
    }

    public List<Person> getCharacters(int n, Locale locale) throws Exception {
        Object object = this.requestSeriesInfo(n, locale);
        return JsonUtilities.streamJsonObjects(object, "characters").map(map -> {
            Integer personId = JsonUtilities.getInteger(map, "peopleId");
            String personName = JsonUtilities.getString(map, "personName");
            String characterName = JsonUtilities.getString(map, "name");
            String peopleType = JsonUtilities.getString(map, "peopleType");
            Integer sort = JsonUtilities.getInteger(map, "sort");
            URL image = JsonUtilities.getStringValue(map, "image", string -> WebRequest.parseURL(string));
            return new Person(personId, personName, characterName, peopleType, null, sort == null || sort == 0 ? null : sort, image);
        }).sorted(Person.CREDIT_ORDER).collect(Collectors.toList());
    }

    public Map<String, String> getExternalIds(int n) throws Exception {
        Object object = this.requestSeriesInfo(n, Locale.ROOT);
        return JsonUtilities.streamJsonObjects(object, "remoteIds").collect(Collectors.toMap(map -> JsonUtilities.getString(map, "sourceName"), map -> JsonUtilities.getString(map, "id"), (string, string2) -> string, LinkedHashMap::new));
    }

    public Series getExternalSeries(int n) throws Exception {
        Object object = this.requestSeriesInfo(n, Locale.ROOT);
        HashMap hashMap = JsonUtilities.streamJsonObjects(object, "remoteIds").collect(Collectors.toMap(map -> JsonUtilities.getString(map, "sourceName"), map -> JsonUtilities.getString(map, "id"), (string, string2) -> string, HashMap::new));
        String string3 = JsonUtilities.getString(object, "name");
        SimpleDate simpleDate = JsonUtilities.getStringValue(object, "firstAired", SimpleDate::parse);
        Integer n2 = JsonUtilities.getInteger(hashMap, "TheMovieDB.com");
        Integer n3 = JsonUtilities.getStringValue(hashMap, "IMDB", Link.IMDb::parseID);
        return Series.XDB(string3, simpleDate != null ? Integer.valueOf(simpleDate.getYear()) : null, -1, Series.XID(n2, n, n3));
    }

    private Map<Integer, ArtworkType> getArtworkTypes() throws Exception {
        Object object = this.requestJson("artwork/types", Cache.NEVER);
        HashMap<Integer, ArtworkType> hashMap = new HashMap<Integer, ArtworkType>(32);
        JsonUtilities.streamJsonObjects(object, "data").forEach(map2 -> {
            Integer n = JsonUtilities.getInteger(map2, "id");
            String string = JsonUtilities.getString(map2, "recordType");
            String string2 = JsonUtilities.getString(map2, "slug");
            String string3 = JsonUtilities.getString(map2, "width");
            String string4 = JsonUtilities.getString(map2, "height");
            hashMap.put(n, new ArtworkType(string, string2, string3 + "x" + string4));
        });
        return hashMap;
    }

    private static /* synthetic */ String lambda$getSeriesInfo$20(Map.Entry entry) {
        return entry.getKey().toString();
    }

    private static /* synthetic */ boolean lambda$getSeriesInfo$19(Map.Entry entry) {
        return Boolean.TRUE.equals(entry.getValue());
    }

    private static class ArtworkType {
        public final String type;
        public final String slug;
        public final String size;

        public ArtworkType(String string, String string2, String string3) {
            this.type = string;
            this.slug = string2;
            this.size = string3;
        }

        public String toString() {
            return "[" + this.type + " | " + this.slug + " | " + this.size + "]";
        }
    }
}

