package net.filemaid.web;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.util.IntSet;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.AbstractEpisodeListProvider;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.Extra;
import net.filemaid.web.Link;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.Person;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.TMDbCore;
import net.filemaid.web.XDB;

public class TMDbTVClient
extends AbstractEpisodeListProvider
implements ArtworkProvider {
    private final TMDbCore core;

    public TMDbTVClient(TMDbCore tMDbCore) {
        this.core = tMDbCore;
    }

    @Override
    public String getIdentifier() {
        return "TheMovieDB::TV";
    }

    @Override
    public String getName() {
        return "TheMovieDB";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.tmdb");
    }

    @Override
    public boolean hasSeasonSupport() {
        return true;
    }

    @Override
    public URI getEpisodeListLink(SearchResult searchResult) {
        return URI.create("https://www.themoviedb.org/tv/" + searchResult.getId());
    }

    @Override
    protected String getLanguageCode(Locale locale) {
        return this.core.getLanguageCode(locale);
    }

    @Override
    protected Cache getCache(String string) {
        return Cache.getCache("tmdb_tv_" + string, CacheType.Daily);
    }

    @Override
    public SearchResult id(Series series) {
        Integer n = series.getExternalId(XDB.TheMovieDB);
        return n == null ? null : new SearchResult((int)n, series.getName(), series.getAliasNames());
    }

    @Override
    protected List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
        Movie movie = Movie.matchNameYear(string);
        if (movie == null) {
            return this.search(string.trim(), -1, locale);
        }
        List<SearchResult> list = this.search(movie.getName(), movie.getYear(), locale);
        if (list.size() > 0) {
            return list;
        }
        return this.search(movie.getName(), -1, locale);
    }

    protected List<SearchResult> search(String string, int n, Locale locale) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("query", string);
        if (n > 0) {
            linkedHashMap.put("first_air_date_year", n);
        }
        if (this.core.isAdultEnabled()) {
            linkedHashMap.put("include_adult", "true");
        }
        Object object = this.core.request("search/tv", linkedHashMap, locale, Cache.ONE_WEEK);
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            try {
                String[] stringArray;
                int id = JsonUtilities.getInteger(map, "id");
                String originalName = JsonUtilities.getString(map, "original_name");
                String string2 = JsonUtilities.optionalString(map, "name").orElse(originalName);
                if (string2 == null) {
                    return null;
                }
                String string3 = JsonUtilities.getString(map, "original_language");
                String[] stringArray2 = JsonUtilities.getStringArray(map, "origin_country");
                SimpleDate simpleDate = JsonUtilities.getStringValue(map, "first_air_date", SimpleDate::parse);
                String string4 = JsonUtilities.getString(map, "overview");
                Double d = JsonUtilities.getDouble(map, "popularity");
                if (originalName == null || string2.equals(originalName)) {
                    stringArray = null;
                } else {
                    String[] stringArray3 = new String[1];
                    stringArray = stringArray3;
                    stringArray3[0] = originalName;
                }
                String[] stringArray4 = stringArray;
                return new SearchResultDetails(id, string2, stringArray4, simpleDate, string4, originalName, string3, stringArray2, d, null, null, null);
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(map, exception));
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public SeriesDetails getSeriesInfo(int n, Locale locale) throws Exception {
        return this.getSeriesInfo(new SearchResult(n), locale, Cache.ONE_MONTH);
    }

    @Override
    public SeriesDetails getSeriesInfo(SearchResult searchResult, Locale locale) throws Exception {
        return this.getSeriesInfo(searchResult, locale, Cache.ONE_MONTH);
    }

    public Object requestSeriesInfo(int n, Locale locale, Duration duration) throws Exception {
        return this.core.request("tv/" + n, Collections.singletonMap("append_to_response", "alternative_titles,content_ratings,keywords"), locale, duration);
    }

    public Object requestEpisodeList(int n, int n2, Locale locale, Duration duration) throws Exception {
        try {
            return this.core.request("tv/" + n + "/season/" + n2, Collections.emptyMap(), locale, duration);
        }
        catch (LookupException lookupException) {
            return null;
        }
    }

    public SeriesDetails getSeriesInfo(SearchResult searchResult, Locale locale, Duration duration) throws Exception {
        Object object = this.requestSeriesInfo(searchResult.getId(), locale, duration);
        String string = JsonUtilities.optionalString(object, "name").orElseGet(searchResult::getName);
        SeriesDetails seriesDetails = new SeriesDetails(this, SortOrder.Airdate, locale, searchResult.getId(), "TV Series");
        seriesDetails.setName(string);
        seriesDetails.setAliasNames(this.mergeAliasNames(searchResult, object, string2 -> !string2.equals(string)));
        seriesDetails.setStatus(JsonUtilities.getString(object, "status"));
        seriesDetails.setStartDate(JsonUtilities.getStringValue(object, "first_air_date", SimpleDate::parse));
        seriesDetails.setRating(JsonUtilities.getStringValue(object, "vote_average", Double::parseDouble));
        seriesDetails.setRatingCount(JsonUtilities.getStringValue(object, "vote_count", Integer::parseInt));
        seriesDetails.setRuntime(Arrays.stream(JsonUtilities.getArray(object, "episode_run_time")).map(Object::toString).map(Integer::parseInt).findFirst().orElse(null));
        seriesDetails.setGenres((String[])JsonUtilities.streamJsonObjects(object, "genres").map(map -> JsonUtilities.getString(map, "name")).toArray(String[]::new));
        seriesDetails.setSpokenLanguages(JsonUtilities.getStringArray(object, "languages"));
        seriesDetails.setNetwork(JsonUtilities.streamJsonObjects(object, "networks").map(map -> JsonUtilities.getString(map, "name")).findFirst().orElse(null));
        seriesDetails.setType(JsonUtilities.optionalString(object, "type").orElse("TV Series"));
        seriesDetails.setOriginalName(JsonUtilities.getString(object, "original_name"));
        seriesDetails.setOriginalLanguage(JsonUtilities.getString(object, "original_language"));
        seriesDetails.setSeasons(JsonUtilities.streamJsonObjects(object, "seasons").mapToInt(map -> JsonUtilities.getInteger(map, "season_number")).filter(n -> n >= 0).toArray());
        seriesDetails.setEndDate(JsonUtilities.getStringValue(object, "last_air_date", SimpleDate::parse));
        seriesDetails.setPopularity(JsonUtilities.getStringValue(object, "popularity", Double::parseDouble));
        seriesDetails.setCountry(JsonUtilities.getStringArray(object, "origin_country"));
        seriesDetails.setOverview(JsonUtilities.getString(object, "overview"));
        seriesDetails.setPoster(JsonUtilities.getStringValue(object, "poster_path", this.core::resolveImage));
        seriesDetails.setKeywords((String[])JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object, "keywords"), "results").map(map -> JsonUtilities.getString(map, "name")).toArray(String[]::new));
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Map<Object, Object> map2 : JsonUtilities.getMapArray(JsonUtilities.getMap(object, "content_ratings"), "results")) {
            String string3 = JsonUtilities.getString(map2, "iso_3166_1");
            String string4 = JsonUtilities.getString(map2, "rating");
            if (string3 == null || string4 == null) continue;
            linkedHashMap.put(string3, string4);
        }
        seriesDetails.setCertifications(linkedHashMap);
        seriesDetails.setCertification(Stream.concat(Stream.of(locale, Locale.US).map(Locale::getCountry), seriesDetails.getCountry().stream()).map(linkedHashMap::get).filter(Objects::nonNull).findFirst().orElse(null));
        return seriesDetails;
    }

    public int[] getSeasonNumbers(int n2, Locale locale, Duration duration) throws Exception {
        Object object = this.requestSeriesInfo(n2, locale, duration);
        return JsonUtilities.streamJsonObjects(object, "seasons").mapToInt(map -> JsonUtilities.getInteger(map, "season_number")).filter(n -> n >= 0).toArray();
    }

    private String[] mergeAliasNames(SearchResult searchResult, Object object, Predicate<String> predicate) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(JsonUtilities.getString(object, "original_name"));
        arrayList.addAll(searchResult.getEffectiveNames());
        JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object, "alternative_titles"), "results").map(map -> JsonUtilities.getString(map, "title")).forEach(arrayList::add);
        return (String[])arrayList.stream().filter(Objects::nonNull).distinct().filter(predicate).toArray(String[]::new);
    }

    @Override
    protected AbstractEpisodeListProvider.SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        Object object;
        List<Episode> list;
        SeriesDetails seriesDetails = this.getSeriesInfo(searchResult, locale);
        seriesDetails.setOrder(sortOrder.name());
        if (sortOrder == SortOrder.Absolute && (list = this.getInferredAbsoluteOrder(seriesDetails, this.getEpisodeList(searchResult, SortOrder.Airdate, locale))) != null) {
            return new AbstractEpisodeListProvider.SeriesData(seriesDetails, list);
        }
        searchResult = new SearchResult(searchResult.getId(), seriesDetails.getName());
        boolean bl = seriesDetails.isRecent();
        boolean bl2 = !bl && seriesDetails.hasEnded();
        Integer n = this.getEpisodeGroupType(sortOrder);
        if (n != null && (object = (Group)this.getEpisodeGroups(searchResult.getId()).stream().filter(group -> n.equals(group.type)).findFirst().orElse(null)) != null) {
            List<Episode> list2 = this.fetchEpisodeGroup((Group)object, searchResult, seriesDetails, sortOrder, locale, bl2 ? Cache.ONE_MONTH : Cache.ONE_WEEK);
            return new AbstractEpisodeListProvider.SeriesData(seriesDetails, list2);
        }
        IntSet intSet = IntSet.of(bl2 ? seriesDetails.getSeasons() : this.getSeasonNumbers(seriesDetails.getId(), locale, Cache.ONE_WEEK));
        List<Episode> list3 = this.fetchSeason(searchResult, seriesDetails, sortOrder, locale, intSet, arg_0 -> seasonCacheDuration(bl2, bl, intSet, arg_0));
        return new AbstractEpisodeListProvider.SeriesData(seriesDetails, list3);
    }

    protected List<Episode> fetchSeason(SearchResult searchResult, SeriesInfo seriesInfo, SortOrder sortOrder, Locale locale, Set<Integer> set, Function<Integer, Duration> function) throws Exception {
        ArrayList<Episode> arrayList = new ArrayList<Episode>();
        ArrayList arrayList2 = new ArrayList();
        for (int n : set) {
            Object object = this.requestEpisodeList(searchResult.getId(), n, locale, function.apply(n));
            String string = JsonUtilities.getString(object, "name");
            JsonUtilities.streamJsonObjects(object, "episodes").forEach(map -> {
                Integer index = arrayList.size() + 1;
                Integer n2 = JsonUtilities.getInteger(map, "id");
                Integer n3 = JsonUtilities.getInteger(map, "episode_number");
                Integer n4 = JsonUtilities.getInteger(map, "season_number");
                String string2 = JsonUtilities.getString(map, "name");
                SimpleDate simpleDate = JsonUtilities.getStringValue(map, "air_date", SimpleDate::parse);
                Integer n5 = JsonUtilities.getInteger(map, "runtime");
                if (sortOrder == SortOrder.Date && simpleDate != null) {
                    n4 = null;
                    n3 = simpleDate.getYear() * 10000 + simpleDate.getMonth() * 100 + simpleDate.getDay();
                }
                if (n4 == null || n4 > 0) {
                    arrayList.add(new Episode(searchResult.getName(), n4, n3, string2, index, null, simpleDate, n5, n2, string, new SeriesInfo(seriesInfo)));
                } else {
                    arrayList2.add(new Episode(searchResult.getName(), null, null, string2, null, n3, simpleDate, n5, n2, string, new SeriesInfo(seriesInfo)));
                }
            });
        }
        arrayList.addAll(arrayList2);
        return arrayList;
    }

    protected List<Group> getEpisodeGroups(int n) throws Exception {
        Object object = this.core.request("tv/" + n + "/episode_groups", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            String string = JsonUtilities.getString(map, "id");
            String string2 = JsonUtilities.getString(map, "name");
            String string3 = JsonUtilities.getString(map, "description");
            Integer type = JsonUtilities.getInteger(map, "type");
            Integer n2 = JsonUtilities.getInteger(map, "episode_count");
            Integer n3 = JsonUtilities.getInteger(map, "group_count");
            if (n2 == null || n2 == 0) {
                return null;
            }
            return new Group(string, string2, string3, type, n2, n3);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected List<Episode> fetchEpisodeGroup(Group group, SearchResult searchResult, SeriesDetails seriesDetails, SortOrder sortOrder, Locale locale, Duration duration) throws Exception {
        List<Episode> list;
        Object object = this.core.request("tv/episode_group/" + group.id, Collections.emptyMap(), locale, duration);
        ArrayList<Episode> arrayList = new ArrayList<Episode>();
        ArrayList arrayList2 = new ArrayList();
        JsonUtilities.streamJsonObjects(object, "groups").forEach(map -> JsonUtilities.streamJsonObjects(map, "episodes").forEach(map2 -> {
            Integer n = JsonUtilities.getInteger(map, "order");
            String string = JsonUtilities.getString(map, "name");
            Integer n2 = JsonUtilities.getInteger(map2, "id");
            Integer n3 = JsonUtilities.getInteger(map2, "order") + 1;
            String string2 = JsonUtilities.getString(map2, "name");
            SimpleDate simpleDate = JsonUtilities.getStringValue(map2, "air_date", SimpleDate::parse);
            Integer n4 = JsonUtilities.getInteger(map2, "runtime");
            Integer n5 = arrayList.size() + 1;
            if (n == null || n > 0) {
                arrayList.add(new Episode(searchResult.getName(), n, n3, string2, n5, null, simpleDate, n4, n2, string, new SeriesInfo(seriesDetails)));
            } else {
                arrayList2.add(new Episode(searchResult.getName(), null, null, string2, null, n3, simpleDate, n4, n2, string, new SeriesInfo(seriesDetails)));
            }
        }));
        arrayList.addAll(arrayList2);
        if (sortOrder == SortOrder.Absolute && (list = this.getInferredAbsoluteOrder(seriesDetails, arrayList)) != null) {
            return list;
        }
        return arrayList;
    }

    protected List<Episode> getInferredAbsoluteOrder(SeriesInfo seriesInfo, List<Episode> list) {
        ArrayList<Episode> arrayList = new ArrayList<Episode>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            Episode episode = list.get(i);
            Integer n = episode.getEpisode();
            if (n == null) continue;
            if (n != i + 1) {
                return null;
            }
            arrayList.add(new Episode(episode.getSeriesName(), null, n, episode.getTitle(), n, null, episode.getAirdate(), episode.getRuntime(), episode.getId(), episode.getGroup(), new SeriesInfo(seriesInfo)));
        }
        return arrayList;
    }

    protected Integer getEpisodeGroupType(SortOrder sortOrder) {
        switch (sortOrder) {
            case Absolute: {
                return 2;
            }
            case DVD: {
                return 3;
            }
            case Digital: {
                return 4;
            }
            case Story: {
                return 5;
            }
            case Production: {
                return 6;
            }
            case Official: {
                return 7;
            }
        }
        return null;
    }

    public List<Episode> getEpisodeGroup(int n, Predicate<Group> predicate, Locale locale) throws Exception {
        Group group = this.getEpisodeGroups(n).stream().filter(predicate).findFirst().orElse(null);
        if (group != null) {
            SeriesDetails seriesDetails = this.getSeriesInfo(n, locale);
            return this.fetchEpisodeGroup(group, new SearchResult(n, seriesDetails.getName()), seriesDetails, SortOrder.Airdate, locale, Cache.ONE_WEEK);
        }
        return null;
    }

    public Map<String, List<String>> getAlternativeTitles(int n) throws Exception {
        Object object = this.core.request("tv/" + n + "/alternative_titles", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "results").collect(Collectors.groupingBy(map -> JsonUtilities.getString(map, "iso_3166_1"), LinkedHashMap::new, Collectors.mapping(map -> JsonUtilities.getString(map, "title"), Collectors.toList())));
    }

    public Map<String, String> getTranslations(int n) throws Exception {
        Object object = this.core.request("tv/" + n + "/translations", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        JsonUtilities.streamJsonObjects(object, "translations").forEach(map2 -> {
            String string = JsonUtilities.getString(map2, "iso_3166_1");
            String string2 = JsonUtilities.getString(JsonUtilities.getMap(map2, "data"), "name");
            if (string != null && string2 != null) {
                linkedHashMap.put(string, string2);
            }
        });
        return linkedHashMap;
    }

    public Map<String, String> getExternalSeriesID(int n) throws Exception {
        Object object3 = this.core.request("tv/" + n + "/external_ids", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        JsonUtilities.asMap(object3).forEach((object, object2) -> {
            if (object2 != null) {
                linkedHashMap.put(object.toString(), object2.toString());
            }
        });
        return linkedHashMap;
    }

    public Map<String, String> getExternalEpisodeID(int n, int n2, int n3) throws Exception {
        Object object3 = this.core.request("tv/" + n + "/season/" + n2 + "/episode/" + n3 + "/external_ids", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        JsonUtilities.asMap(object3).forEach((object, object2) -> {
            if (object2 != null) {
                linkedHashMap.put(object.toString(), object2.toString());
            }
        });
        return linkedHashMap;
    }

    public SeriesDetails lookupByExternalId(XDB xDB, int n, Locale locale) throws Exception {
        switch (xDB) {
            case TheMovieDB: {
                return this.getSeriesInfo(n, locale);
            }
            case TheTVDB: {
                return this.lookupByExternalId(Link.TheTVDB.getID(n), "tvdb_id", locale);
            }
            case IMDb: {
                return this.lookupByExternalId(Link.IMDb.getID(n), "imdb_id", locale);
            }
        }
        return null;
    }

    public SeriesDetails lookupByExternalId(String string, String string2, Locale locale) throws Exception {
        Object object = this.core.request("find/" + string, Collections.singletonMap("external_source", string2), locale, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "tv_results").map(map -> {
            Integer n = JsonUtilities.getInteger(map, "id");
            SeriesDetails seriesDetails = new SeriesDetails(this, SortOrder.Airdate, locale, n, "TV Series");
            seriesDetails.setName(JsonUtilities.getString(map, "name"));
            seriesDetails.setStartDate(JsonUtilities.getStringValue(map, "first_air_date", SimpleDate::parse));
            seriesDetails.setRating(JsonUtilities.getStringValue(map, "vote_average", Double::parseDouble));
            seriesDetails.setRatingCount(JsonUtilities.getStringValue(map, "vote_count", Integer::parseInt));
            seriesDetails.setOriginalName(JsonUtilities.getString(map, "original_name"));
            seriesDetails.setOriginalLanguage(JsonUtilities.getString(map, "original_language"));
            seriesDetails.setPopularity(JsonUtilities.getStringValue(map, "popularity", Double::parseDouble));
            seriesDetails.setCountry(JsonUtilities.getStringArray(map, "origin_country"));
            seriesDetails.setOverview(JsonUtilities.getString(map, "overview"));
            return seriesDetails;
        }).findFirst().orElse(null);
    }

    public Series getExternalSeries(XDB xDB, int n) throws Exception {
        SeriesDetails seriesDetails = this.lookupByExternalId(xDB, n, Locale.US);
        if (seriesDetails == null) {
            return null;
        }
        Integer n2 = seriesDetails.getStartDate() != null ? Integer.valueOf(seriesDetails.getStartDate().getYear()) : null;
        Double d = seriesDetails.getPopularity();
        Map<String, String> map = this.getExternalSeriesID(seriesDetails.getId());
        Integer n3 = JsonUtilities.getInteger(map, "tvdb_id");
        Integer n4 = JsonUtilities.getStringValue(map, "imdb_id", Link.IMDb::parseID);
        return Series.XDB(seriesDetails.getName(), n2, d, Series.XID(seriesDetails.getId(), n3, n4));
    }

    public List<Person> getCredits(int n, Locale locale) throws Exception {
        Object object = this.core.request("tv/" + n + "/credits", Collections.emptyMap(), locale, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "cast", "crew").map(map -> {
            Integer id = JsonUtilities.getInteger(map, "id");
            String name = JsonUtilities.getString(map, "name");
            String character = JsonUtilities.getString(map, "character");
            String department = JsonUtilities.getString(map, "known_for_department", "department");
            String job = JsonUtilities.getString(map, "job");
            Integer order = JsonUtilities.getInteger(map, "order");
            URL profile = JsonUtilities.getStringValue(map, "profile_path", this.core::resolveImage);
            return new Person(id, name, character, job, department, order, profile);
        }).sorted(Person.CREDIT_ORDER).collect(Collectors.toList());
    }

    public Object requestEpisodeInfo(int n, int n2, int n3, Locale locale) throws Exception {
        Object object = this.requestEpisodeList(n, n2, locale, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "episodes").filter(map -> {
            Integer episodeNumber = JsonUtilities.getInteger(map, "episode_number");
            return episodeNumber != null && n3 == episodeNumber;
        }).findFirst().orElseThrow(() -> new LookupException((Object)"Episode not found", n + "/" + n2 + "/" + n3));
    }

    public EpisodeDetails getEpisodeInfo(int n, int n2, int n3, Locale locale) throws Exception {
        Object object = this.requestEpisodeInfo(n, n2, n3, locale);
        Integer n4 = JsonUtilities.getInteger(object, "id");
        Integer n5 = JsonUtilities.getInteger(object, "season_number");
        Integer n6 = JsonUtilities.getInteger(object, "episode_number");
        String string = JsonUtilities.getString(object, "name");
        String string2 = JsonUtilities.getString(object, "production_code");
        String string3 = JsonUtilities.getString(object, "overview");
        Integer n7 = JsonUtilities.getInteger(object, "runtime");
        SimpleDate simpleDate = JsonUtilities.getStringValue(object, "air_date", SimpleDate::parse);
        Double d = JsonUtilities.getDouble(object, "vote_average");
        Integer n8 = JsonUtilities.getInteger(object, "vote_count");
        URL uRL = JsonUtilities.getStringValue(object, "still_path", this.core::resolveImage);
        String string4 = "https://www.themoviedb.org/tv/" + n + "/season/" + n2 + "/episode/" + n3;
        List<Person> list = JsonUtilities.streamJsonObjects(object, "crew", "guest_stars").map(map -> {
            Integer id = JsonUtilities.getInteger(map, "id");
            String name = JsonUtilities.getString(map, "name");
            String character = JsonUtilities.getString(map, "character");
            String department = JsonUtilities.getString(map, "known_for_department", "department");
            String job = JsonUtilities.getString(map, "job");
            Integer order = JsonUtilities.getInteger(map, "order");
            URL profile = JsonUtilities.getStringValue(map, "profile_path", this.core::resolveImage);
            return new Person(id, name, character, job, department, order, profile);
        }).sorted(Person.CREDIT_ORDER).collect(Collectors.toList());
        SeriesInfo seriesInfo = new SeriesInfo(this, SortOrder.Airdate, locale, n, "TV Series");
        Episode episode = new Episode(null, n5, n6, string, null, null, simpleDate, n7, n4, null, seriesInfo);
        return new EpisodeDetails(episode, string2, string3, d, n8, string4, uRL, list);
    }

    public Episode defaultOrder(Episode episode) throws Exception {
        if (SortOrder.Airdate.equals(episode.getSeriesInfo().getOrder())) {
            return episode;
        }
        return this.getEpisodeList(episode.getSeriesInfo().getId(), SortOrder.Airdate, episode.getSeriesInfo().getLanguage()).stream().filter(episode::equals).findFirst().orElse(null);
    }

    @Override
    public EpisodeDetails getEpisodeInfo(Episode episode, Locale locale) throws Exception {
        int n = (episode = this.defaultOrder(episode)).getSpecial() == null ? episode.getSeason() : 0;
        int n2 = episode.getSpecial() == null ? episode.getEpisode() : episode.getSpecial();
        return this.getEpisodeInfo(episode.getSeriesInfo().getId(), n, n2, locale);
    }

    @Override
    public List<Artwork> getArtwork(int n, Locale locale) throws Exception {
        return this.core.requestImages("tv", n, locale);
    }

    public List<Extra> getExtras(int n) throws Exception {
        return this.core.requestVideos("tv", n);
    }

    public List<Series> getIndex() throws Exception {
        return this.core.requestExport("tv_series_ids", stream -> stream.map(object -> {
            Integer n = JsonUtilities.getInteger(object, "id");
            String string = JsonUtilities.getString(object, "original_name");
            Double d = JsonUtilities.getDouble(object, "popularity");
            if (n == null || string == null || d == null || d < 4.0) {
                return null;
            }
            return Series.TMDB(n, string, d);
        }).filter(Objects::nonNull).sorted(Comparator.comparingInt(Series::getScore).reversed()).collect(Collectors.toList()));
    }

    private static Duration seasonCacheDuration(boolean ended, boolean recent, IntSet seasons, Integer n) {
        if (ended) {
            return Cache.ONE_MONTH;
        }
        if (recent && n.intValue() == seasons.max()) {
            return Cache.ONE_DAY;
        }
        return Cache.ONE_WEEK;
    }

    public static class Group {
        public final String id;
        public final String name;
        public final String description;
        public final Integer type;
        public final Integer episodeCount;
        public final Integer groupCount;

        public Group(String string, String string2, String string3, Integer n, Integer n2, Integer n3) {
            this.id = string;
            this.name = string2;
            this.description = string3;
            this.type = n;
            this.episodeCount = n2;
            this.groupCount = n3;
        }

        public String toString() {
            return this.name + " (" + this.groupCount + " groups, " + this.episodeCount + " episodes) [Type " + this.type + "]";
        }
    }
}

