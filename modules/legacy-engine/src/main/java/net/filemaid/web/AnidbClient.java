package net.filemaid.web;

import java.net.URI;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.ResourceManager;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.XPathUtilities;
import net.filemaid.web.AbstractEpisodeListProvider;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.FloodLimit;
import net.filemaid.web.Link;
import net.filemaid.web.LocalSearch;
import net.filemaid.web.LookupException;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;
import net.filemaid.web.XDB;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class AnidbClient
extends AbstractEpisodeListProvider {
    private static final FloodLimit SOFT_LIMIT = new FloodLimit(5, 60L, TimeUnit.SECONDS);
    private static final FloodLimit HARD_LIMIT = new FloodLimit(200, 24L, TimeUnit.HOURS);
    private static final AtomicBoolean BANNED = new AtomicBoolean(false);
    private final String client;
    private final int clientver;
    private final URI api;
    private final boolean movie;
    private static final EnumSet<SortOrder> SUPPORTED_ORDER = EnumSet.of(SortOrder.Absolute, SortOrder.Date);
    private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> new LocalSearch<SearchResult>(this.getIndex(), SearchResult::getEffectiveNames));

    private static URI defaultEndpoint() {
        return URI.create("http://api.anidb.net:9001/");
    }

    private static boolean defaultMovieEnabled() {
        return Boolean.parseBoolean(System.getProperty("net.filemaid.AniDB.movie"));
    }

    public AnidbClient(String string, int n) {
        this(string, n, AnidbClient.defaultEndpoint(), AnidbClient.defaultMovieEnabled());
    }

    public AnidbClient(String string, int n, URI uRI, boolean bl) {
        this.client = string;
        this.clientver = n;
        this.api = uRI;
        this.movie = bl;
    }

    @Override
    public String getIdentifier() {
        return "AniDB";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.anidb");
    }

    @Override
    public boolean hasSeasonSupport() {
        return false;
    }

    @Override
    public SortOrder vetoRequestParameter(SortOrder sortOrder) {
        return SUPPORTED_ORDER.contains((Object)sortOrder) ? sortOrder : SortOrder.Absolute;
    }

    @Override
    public List<SearchResult> search(String string, Locale locale) throws Exception {
        return this.fetchSearchResult(string, locale);
    }

    @Override
    public SearchResult id(Series series) {
        Integer n = series.getExternalId(XDB.AniDB);
        return n == null ? null : new SearchResult((int)n, series.getName(), series.getAliasNames());
    }

    @Override
    protected List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
        LocalSearch<SearchResult> localSearch = this.localIndex.get();
        if (localSearch.size() > 0) {
            return localSearch.search(string);
        }
        throw new IllegalStateException("AniDB search index is empty");
    }

    @Override
    protected AbstractEpisodeListProvider.SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        Document document = this.getXmlResource(searchResult.getId());
        String string = XPathUtilities.selectString("//type", document);
        SeriesDetails seriesDetails = new SeriesDetails(this, sortOrder, locale, searchResult.getId(), string != null ? string : "Anime");
        if (searchResult.getName() != null && !this.movie && ("Movie".equals(string) || "Music Video".equals(string) || "Other".equals(string) || "unknown".equals(string))) {
            Logging.debug.warning(Logging.format("Ignore type: %s: %s (%s)", string, searchResult, searchResult.getId()));
            return new AbstractEpisodeListProvider.SeriesData(seriesDetails, Collections.emptyList());
        }
        String string3 = XPathUtilities.selectString("anime/titles/title[@type='main']", document);
        if (string3 == null || string3.isEmpty()) {
            Logging.debug.warning(Logging.format("Ignore empty main title: %s (%s)", searchResult, searchResult.getId()));
            return new AbstractEpisodeListProvider.SeriesData(seriesDetails, Collections.emptyList());
        }
        seriesDetails.setName(string3);
        seriesDetails.setOriginalName(string3);
        seriesDetails.setAliasNames((String[])Stream.concat(XPathUtilities.selectStrings("anime/titles/title", document).stream(), Stream.of(searchResult.getAliasNames())).filter(string2 -> !string2.equals(string3)).distinct().toArray(String[]::new));
        seriesDetails.setRating(XPathUtilities.getDecimal(XPathUtilities.selectString("anime/ratings/permanent", document)));
        seriesDetails.setRatingCount(StringUtilities.matchInteger(XPathUtilities.getTextContent("anime/ratings/permanent/@count", document)));
        seriesDetails.setStartDate(SimpleDate.parse(XPathUtilities.selectString("anime/startdate", document)));
        seriesDetails.setEndDate(SimpleDate.parse(XPathUtilities.selectString("anime/enddate", document)));
        seriesDetails.setOverview(XPathUtilities.selectString("anime/description", document));
        seriesDetails.setPoster(this.resolveImage(XPathUtilities.selectString("anime/picture", document)));
        seriesDetails.setGenres((String[])XPathUtilities.streamNodes("anime/categories/category", document).map(node -> {
            String name = XPathUtilities.getTextContent("name", node);
            Integer n = StringUtilities.matchInteger(XPathUtilities.getAttribute("weight", node));
            return new AbstractMap.SimpleImmutableEntry<String, Integer>(name, n);
        }).filter(simpleImmutableEntry -> simpleImmutableEntry.getKey() != null && simpleImmutableEntry.getValue() != null && ((String)simpleImmutableEntry.getKey()).length() > 0 && (Integer)simpleImmutableEntry.getValue() >= 400).sorted((simpleImmutableEntry, simpleImmutableEntry2) -> ((Integer)simpleImmutableEntry2.getValue()).compareTo((Integer)simpleImmutableEntry.getValue())).map(simpleImmutableEntry -> (String)simpleImmutableEntry.getKey()).limit(5L).toArray(String[]::new));
        String string4 = XPathUtilities.selectString("anime/titles/title[@type='official' and @lang='" + this.getLanguageCode(locale) + "']", document);
        if (string4 == null || string4.isEmpty()) {
            string4 = seriesDetails.getName();
        }
        ArrayList<Episode> arrayList = new ArrayList<Episode>(25);
        for (Node node2 : XPathUtilities.selectNodes("anime/episodes/episode", document)) {
            Node node3 = XPathUtilities.getChild("epno", node2);
            int n = Integer.parseInt(XPathUtilities.getTextContent(node3).replaceAll("\\D", ""));
            int n2 = Integer.parseInt(XPathUtilities.getAttribute("type", node3));
            if (n2 != 1 && n2 != 2) continue;
            Integer n3 = Integer.parseInt(XPathUtilities.getAttribute("id", node2));
            SimpleDate simpleDate = SimpleDate.parse(XPathUtilities.getTextContent("airdate", node2));
            Integer n4 = StringUtilities.matchInteger(XPathUtilities.getTextContent("length", node2));
            String string5 = XPathUtilities.selectString(".//title[@lang='" + this.getLanguageCode(locale) + "']", node2);
            if (string5.isEmpty()) {
                string5 = XPathUtilities.selectString(".//title[@lang='en']", node2);
            }
            if (n2 == 1) {
                if (sortOrder == SortOrder.Date && simpleDate != null) {
                    n = simpleDate.getYear() * 10000 + simpleDate.getMonth() * 100 + simpleDate.getDay();
                }
                arrayList.add(new Episode(string4, null, n, string5, n, null, simpleDate, n4, n3, null, new SeriesInfo(seriesDetails)));
                continue;
            }
            arrayList.add(new Episode(string4, null, null, string5, null, n, simpleDate, n4, n3, null, new SeriesInfo(seriesDetails)));
        }
        arrayList.sort(EpisodeUtilities.episodeComparator());
        if (arrayList.isEmpty()) {
            Logging.debug.fine(Logging.format("No episode data: %s (%s) => %s", searchResult, searchResult.getId(), this.getResource(searchResult.getId())));
        }
        return new AbstractEpisodeListProvider.SeriesData(seriesDetails, arrayList);
    }

    private Document getXmlResource(int n) throws Exception {
        AtomicBoolean atomicBoolean = BANNED;
        synchronized (atomicBoolean) {
            Document document;
            String string;
            CachedResource<Integer, Document> cachedResource = this.getCache().xml(n, this::getResource).fetch(CachedResource.withPermit(CachedResource.fetchIfModified(), uRL -> {
                if (BANNED.get()) {
                    throw new IllegalStateException("AniDB has already banned your IP. Please stop hitting AniDB for at least 24 hours.");
                }
                if (HARD_LIMIT.availablePermits() == 0) {
                    Logging.log.warning("AniDB HTTP API daily limit has been exceeded. Processing will continue in 24 hours...");
                    Cache.DISK_STORE.flush();
                    TimeUnit.HOURS.sleep(24L);
                }
                Logging.debug.finest(Logging.message("AniDB daily limit", HARD_LIMIT));
                HARD_LIMIT.acquirePermit();
                Logging.debug.finest(Logging.message("AniDB minutely limit", SOFT_LIMIT));
                SOFT_LIMIT.acquirePermit();
            })).expire(Cache.ONE_WEEK);
            if ((BANNED.get() || HARD_LIMIT.availablePermits() < 100) && cachedResource.available()) {
                Logging.log.config(Logging.message("AniDB cache expiration temporarily disabled due to heavy usage", n, HARD_LIMIT));
                cachedResource.expire(Cache.NEVER);
            }
            if ((string = XPathUtilities.selectString("/error", document = cachedResource.get())) != null && !string.isEmpty()) {
                if ("anime not found".equalsIgnoreCase(string)) {
                    throw new LookupException((Object)"ANIDB ID not found", n);
                }
                cachedResource.invalidate();
                if ("banned".equalsIgnoreCase(string)) {
                    BANNED.set(true);
                    throw new InvalidResponseException("AniDB has banned your IP due to excessive usage. Please try again in 24 hours or later.");
                }
                throw new InvalidResponseException(string);
            }
            return document;
        }
    }

    private URL getResource(int n) throws Exception {
        return WebRequest.newURL(this.api, "httpapi?request=anime&client=" + this.client + "&clientver=" + this.clientver + "&protover=1&aid=" + n);
    }

    public Cache getCache() {
        return Cache.getCache(this.getName(), CacheType.Persistent);
    }

    @Override
    public URI getEpisodeListLink(SearchResult searchResult) {
        return URI.create(Link.AniDB.getURL(searchResult));
    }

    public List<SearchResult> getIndex() throws Exception {
        String string2 = this.getCache().text("anime-titles.dat.gz", string -> WebRequest.newURL("https://anidb.net/api/" + string)).expire(Cache.ONE_MONTH).get();
        Pattern pattern = Pattern.compile("^(?!#)(\\d+)[|](\\d)[|]([\\w-]+)[|](.+)$");
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("x-jat");
        arrayList.add("x-zht");
        arrayList.add("en");
        arrayList.add("ja");
        ArrayList<String> arrayList2 = new ArrayList<String>();
        arrayList2.add("1");
        arrayList2.add("4");
        arrayList2.add("2");
        arrayList2.add("3");
        HashMap<Integer, ArrayList<Object[]>> hashMap = new HashMap<>(65536);
        RegularExpressions.NEWLINE.splitAsStream(string2).forEach(string -> {
            Matcher matcher = pattern.matcher((CharSequence)string);
            if (matcher.matches()) {
                int n2 = Integer.parseInt(matcher.group(1));
                String type = matcher.group(2);
                String string3 = matcher.group(3);
                String string4 = matcher.group(4);
                if (n2 > 0 && string4.length() > 0 && arrayList2.contains(type) && arrayList.contains(string3)) {
                    string4 = Jsoup.parse((String)string4).text();
                    if (type.equals("3") && string4.length() < 4) {
                        return;
                    }
                    hashMap.computeIfAbsent(n2, n -> new ArrayList<>()).add(new Object[]{arrayList2.indexOf(type), arrayList.indexOf(string3), string4});
                }
            }
        });
        return hashMap.entrySet().stream().map(entry -> {
            List<String> list = entry.getValue().stream().sorted((objectArray, objectArray2) -> {
                for (int i = 0; i < objectArray.length; ++i) {
                    if (objectArray[i].equals(objectArray2[i])) continue;
                    return ((Comparable)objectArray[i]).compareTo(objectArray2[i]);
                }
                return 0;
            }).map(objectArray -> objectArray[2].toString()).collect(Collectors.toList());
            String string = list.get(0);
            List<String> list2 = list.subList(1, list.size());
            return new SearchResult(entry.getKey(), string, list2);
        }).collect(Collectors.toList());
    }

    @Override
    protected String getLanguageCode(Locale locale) {
        if ("ja".equals(locale.getLanguage()) && "Latn".equals(locale.getScript())) {
            return "x-jat";
        }
        return super.getLanguageCode(locale);
    }

    protected URL resolveImage(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        return WebRequest.parseURL("https://cdn.anidb.net/images/main/" + string);
    }
}

