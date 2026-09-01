package net.filemaid;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.MemoryCache;
import net.filemaid.Parallelism;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.ThumbnailServices;
import net.filemaid.UserData;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaDetection;
import net.filemaid.similarity.MetricAvg;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.SystemProperty;
import net.filemaid.web.AcoustIDClient;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Datasource;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.FanartTVClient;
import net.filemaid.web.GoogleMapsClient;
import net.filemaid.web.ID3;
import net.filemaid.web.Link;
import net.filemaid.web.LocalSearch;
import net.filemaid.web.Manami;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.Person;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.ShooterSubtitles;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.SubtitleSearchResult;
import net.filemaid.web.TMDbCore;
import net.filemaid.web.TVMazeClient;
import net.filemaid.web.ThumbnailProvider;
import net.filemaid.web.XDB;

public final class WebServices {
    public static final boolean LEGACY_TVDB_CLIENT = Boolean.parseBoolean(System.getProperty("net.filemaid.WebServices.TheTVDB.v2"));
    public static final boolean LEGACY_OSDB_CLIENT = Boolean.parseBoolean(System.getProperty("net.filemaid.WebServices.OpenSubtitles.v1"));
    private static final TMDbCore TMDbCore = new TMDbCore(WebServices.getApiKey("TheMovieDB", ""));
    public static final net.filemaid.web.TMDbMovieClient TheMovieDB = new TMDbMovieClient(TMDbCore);
    public static final net.filemaid.web.OMDbClient OMDb = new OMDbClient(WebServices.getApiKey("OMDb", ""));
    public static final net.filemaid.web.TMDbTVClient TheMovieDB_TV = new TMDbTVClient(TMDbCore);
    public static final TheTVDBClient TheTVDBv2 = new TheTVDBClientV2(WebServices.getApiKey("TheTVDB", ""));
    public static final TheTVDBClient TheTVDBv4 = new TheTVDBClientV4(WebServices.getApiKey("TheTVDBv4", ""));
    public static final TheTVDBClient TheTVDB = LEGACY_TVDB_CLIENT ? TheTVDBv2 : TheTVDBv4;
    public static final net.filemaid.web.AnidbClient AniDB = new AnidbClient(Settings.getApplicationName().toLowerCase(Locale.ROOT), 8);
    public static final TVMazeClient TVmaze = new TVMazeClient();
    public static final OpenSubtitlesClient OSDBv1 = new OpenSubtitlesXmlRpcClient(Settings.getApplicationName() + " v" + Settings.getApplicationVersion());
    public static final OpenSubtitlesClient OSDBv2 = new OpenSubtitlesRestClient(Settings.getApplicationName() + " v" + Settings.getApplicationVersion(), "");
    public static final OpenSubtitlesClient OpenSubtitles = LEGACY_OSDB_CLIENT ? OSDBv1 : OSDBv2;
    public static final ShooterSubtitles Shooter = new ShooterSubtitles();
    public static final net.filemaid.web.AnimeLists AnimeList = new AnimeLists();
    public static final FanartTVClient FanartTV = new FanartTVClient(WebServices.getApiKey("FanartTV", ""));
    public static final AcoustIDClient AcoustID = new AcoustIDClient(WebServices.getApiKey("AcoustID", ""));
    public static final ID3 MediaInfoID3 = new ID3();
    public static final GoogleMapsClient GoogleMaps = new GoogleMapsClient(WebServices.getApiKey("GoogleMaps", ""));
    private static final Parallelism requestPool = new Parallelism("Request", 10);
    private static final String LOGIN_SEPARATOR = "\t";

    public static MovieLookupService getDefaultMovieDB() {
        return WebServices.getDefaultService("movie", WebServices::getMovieLookupService, TheMovieDB);
    }

    public static EpisodeListProvider getDefaultSeriesDB() {
        return WebServices.getDefaultService("series", WebServices::getEpisodeListProvider, TheMovieDB_TV);
    }

    public static EpisodeListProvider getDefaultAnimeDB() {
        return WebServices.getDefaultService("anime", WebServices::getEpisodeListProvider, TheMovieDB_TV);
    }

    public static MusicLookupService[] getDefaultMusicDB() {
        return new MusicLookupService[]{MediaInfoID3, AcoustID};
    }

    private static final String getApiKey(String string, String string2) {
        return SystemProperty.get(string + ".key", String::toString, string2);
    }

    private static final <T> T getDefaultService(String string, Function<String, T> function, T t) {
        return SystemProperty.get(string + ".db", function, t);
    }

    public static Datasource[] getServices() {
        return new Datasource[]{TheMovieDB, OMDb, TheTVDB, AniDB, TheMovieDB_TV, TVmaze, AcoustID, MediaInfoID3, LocalDatasource.EXIF, LocalDatasource.XATTR, LocalDatasource.FILE, OpenSubtitles, Shooter, AnimeList, FanartTV};
    }

    public static MovieLookupService[] getMovieLookupServices() {
        return new MovieLookupService[]{TheMovieDB, OMDb};
    }

    public static EpisodeListProvider[] getEpisodeListProviders() {
        return new EpisodeListProvider[]{TheMovieDB_TV, AniDB, TheTVDB, TVmaze};
    }

    public static MusicLookupService[] getMusicLookupServices() {
        return new MusicLookupService[]{AcoustID, MediaInfoID3};
    }

    public static LocalDatasource[] getLocalDatasources() {
        return new LocalDatasource[]{LocalDatasource.EXIF, LocalDatasource.XATTR, LocalDatasource.FILE};
    }

    public static SubtitleProvider[] getSubtitleProviders(Locale locale) {
        return new SubtitleProvider[]{OpenSubtitles};
    }

    public static SubtitleLookupService[] getSubtitleLookupServices(Locale locale) {
        switch (locale.getLanguage()) {
            case "zh": {
                return new SubtitleLookupService[]{OpenSubtitles, Shooter};
            }
        }
        return new SubtitleLookupService[]{OpenSubtitles};
    }

    public static Datasource getService(String string) {
        return WebServices.getService((String)string, (Datasource[])WebServices.getServices());
    }

    public static EpisodeListProvider getEpisodeListProvider(String string) {
        return (EpisodeListProvider)WebServices.getService((String)string, (Datasource[])WebServices.getEpisodeListProviders());
    }

    public static MovieLookupService getMovieLookupService(String string) {
        return (MovieLookupService)WebServices.getService((String)string, (Datasource[])WebServices.getMovieLookupServices());
    }

    public static MusicLookupService getMusicLookupService(String string) {
        return (MusicLookupService)WebServices.getService((String)string, (Datasource[])WebServices.getMusicLookupServices());
    }

    public static <T extends Datasource> T getService(String string, T ... TArray) {
        for (T t : TArray) {
            if (!t.getIdentifier().equalsIgnoreCase(string)) continue;
            return t;
        }
        throw new IllegalArgumentException(string + " not in " + Arrays.stream(TArray).map(Datasource::getIdentifier).collect(Collectors.toList()));
    }

    public static Parallelism requestPool() {
        return requestPool;
    }

    private WebServices() {
        throw new UnsupportedOperationException();
    }

    public static void login() {
        String[] stringArray = WebServices.getLogin(OpenSubtitles);
        if (stringArray != null && stringArray.length == 2) {
            OpenSubtitles.login(stringArray[0], stringArray[1]);
        }
    }

    private static PreferencesMap.PreferencesEntry<String> persistentLogin(Datasource datasource) {
        return UserData.root().node("login").entry(datasource.getIdentifier());
    }

    public static String[] getLogin(Datasource datasource) {
        String string = WebServices.persistentLogin(datasource).getValue();
        if (string != null) {
            return string.split(LOGIN_SEPARATOR, 2);
        }
        return null;
    }

    public static void setLogin(Datasource datasource, String string, String string2) {
        string = string == null || string.isEmpty() || string.contains(LOGIN_SEPARATOR) ? "" : string;
        String string3 = string2 = string2 == null || string2.isEmpty() || string2.contains(LOGIN_SEPARATOR) ? "" : string2;
        if (datasource == OpenSubtitles) {
            OpenSubtitles.login(string, string2);
        }
        if (string.isEmpty() && string2.isEmpty()) {
            WebServices.persistentLogin(datasource).remove();
        } else {
            WebServices.persistentLogin(datasource).setValue(String.join((CharSequence)LOGIN_SEPARATOR, string, string2));
        }
    }

    public static interface TheTVDBClient
    extends EpisodeListProvider,
    ArtworkProvider {
        @Override
        public SeriesDetails getSeriesInfo(SearchResult var1, Locale var2) throws Exception;

        @Override
        public SeriesDetails getSeriesInfo(int var1, Locale var2) throws Exception;

        public EpisodeDetails getEpisodeInfo(int var1, Locale var2) throws Exception;

        public Map<String, String> getExternalIds(int var1) throws Exception;

        public Series getExternalSeries(int var1) throws Exception;

        public List<Person> getCharacters(int var1, Locale var2) throws Exception;

        public Object requestSeriesInfo(int var1, Locale var2) throws Exception;

        public Object requestEpisodeInfo(int var1, Locale var2) throws Exception;
    }

    public static interface OpenSubtitlesClient
    extends SubtitleProvider,
    SubtitleLookupService,
    MovieLookupService,
    ThumbnailProvider {
        public void login(String var1, String var2);

        public void logout();

        public Map<?, ?> getServerInfo() throws Exception;

        public List<SubtitleSearchResult> searchIMDB(String var1) throws Exception;

        public Locale detectLanguage(byte[] var1) throws Exception;

        public OpenSubtitlesClient newInstance();

        public URI getAccountLink(String var1);
    }

    private static class TMDbMovieClient
    extends net.filemaid.web.TMDbMovieClient
    implements ThumbnailProvider {
        private final MemoryCache<Integer, LocalSearch<Movie>> localIndexPerYear = MemoryCache.forMinutes();

        public TMDbMovieClient(TMDbCore tMDbCore) {
            super(tMDbCore);
        }

        private LocalSearch<Movie> computeLocalIndex(int n) throws Exception {
            if (n > 0) {
                List list = this.getIndex().stream().filter(movie -> n == movie.getYear()).collect(Collectors.toList());
                return new LocalSearch<Movie>(list, Movie::getEffectiveNamesWithoutYear);
            }
            return new LocalSearch<Movie>(this.getIndex(), movie -> Collections.singleton(movie.getName()));
        }

        private LocalSearch<Movie> getLocalIndex(int n2) throws Exception {
            return this.localIndexPerYear.get(n2, n -> {
                try {
                    return this.computeLocalIndex((int)n);
                }
                catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }

        private Movie merge(Locale locale, List<Movie> list) {
            String string = list.stream().map(SearchResult::getName).filter(Objects::nonNull).findFirst().orElse(null);
            String[] stringArray = (String[])list.stream().flatMap(movie -> Arrays.stream(movie.getAliasNames())).filter(string2 -> !string2.equals(string)).distinct().toArray(String[]::new);
            int n2 = list.stream().mapToInt(Movie::getYear).filter(n -> n > 0).findFirst().orElse(0);
            int n3 = list.stream().mapToInt(Movie::getImdbId).filter(n -> n > 0).findFirst().orElse(0);
            int n4 = list.stream().mapToInt(Movie::getTmdbId).filter(n -> n > 0).findFirst().orElse(0);
            Locale locale2 = list.stream().map(Movie::getLanguage).filter(Objects::nonNull).findFirst().orElse(locale);
            return new Movie(n4, string, stringArray, n2, n3, n4, locale2);
        }

        private List<Movie> mergeResultSet(Locale locale, List<Movie> ... listArray) {
            return Arrays.stream(listArray).flatMap(Collection::stream).collect(Collectors.groupingBy(SearchResult::getId, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), list -> this.merge(locale, (List<Movie>)list)))).values().stream().collect(Collectors.toList());
        }

        @Override
        protected List<Movie> fetchSearchResult(String string, int n, Locale locale) throws Exception {
            List<Movie> list = super.fetchSearchResult(string, n, locale);
            if (n > 0) {
                List<Movie> list2 = this.getLocalIndex(n).search(string);
                List<Movie> list3 = this.getLocalIndex(n - 1).search(string);
                List<Movie> list4 = this.getLocalIndex(n + 1).search(string);
                return this.mergeResultSet(locale, list, list2, list3, list4);
            }
            LocalSearch<Movie> localSearch = this.getLocalIndex(0);
            List<Movie> list5 = localSearch.search(string);
            List list6 = list.stream().map(movie -> list5.contains(movie) ? null : (Movie)localSearch.stream().filter(movie::equals).findFirst().orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
            return this.mergeResultSet(locale, list, list6, list5);
        }

        @Override
        public List<Movie> getIndex() throws Exception {
            return MediaDetection.releaseInfo.getMovieIndex();
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            return ThumbnailServices.TheMovieDB.getThumbnail(n, resolutionVariant);
        }
    }

    private static class OMDbClient
    extends net.filemaid.web.OMDbClient
    implements ThumbnailProvider {
        public OMDbClient(String string) {
            super(string);
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            Movie movie = TheMovieDB.lookupMovieDescriptor(Movie.IMDB(n));
            return movie == null ? null : ThumbnailServices.TheMovieDB.getThumbnail(movie.getTmdbId(), resolutionVariant);
        }
    }

    private static class TMDbTVClient
    extends net.filemaid.web.TMDbTVClient
    implements ThumbnailProvider {
        private final Resource<LocalSearch<Series>> localIndex = Resource.lazy(() -> new LocalSearch<Series>(this.getIndex(), SearchResult::getEffectiveNames));

        public TMDbTVClient(TMDbCore tMDbCore) {
            super(tMDbCore);
        }

        private SearchResult merge(List<SearchResult> list) {
            SearchResult searchResult2 = list.get(0);
            if (list.size() == 1) {
                return searchResult2;
            }
            int n = searchResult2.getId();
            String string = searchResult2.getName();
            String[] stringArray = (String[])list.stream().flatMap(searchResult -> Arrays.stream(searchResult.getAliasNames())).filter(string2 -> !string2.equals(string)).distinct().toArray(String[]::new);
            if (searchResult2 instanceof SearchResultDetails) {
                return new SearchResultDetails(new SearchResult(n, string, stringArray), (SearchResultDetails)searchResult2);
            }
            return new SearchResult(n, string, stringArray);
        }

        @Override
        public List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
            List<SearchResult> list = super.fetchSearchResult(string, locale);
            List<Series> list2 = this.localIndex.get().search(string);
            Map map = Stream.of(list, list2).flatMap(Collection::stream).collect(Collectors.groupingBy(SearchResult::getId, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), this::merge)));
            return MediaDetection.sortBySeriesMatchSimilarity(map.values(), string);
        }

        @Override
        public List<Series> getIndex() throws Exception {
            return MediaDetection.releaseInfo.getSeriesIndex();
        }

        @Override
        public SearchResult id(String string) throws Exception {
            Integer n = Link.IMDb.parseID(string);
            if (n != null) {
                Integer n2 = XDB.IMDb.getExternalId(n, XDB.TheMovieDB);
                if (n2 != null) {
                    return new SearchResult(n2);
                }
                return null;
            }
            return super.id(string);
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            return ThumbnailServices.TheMovieDB_TV.getThumbnail(n, resolutionVariant);
        }
    }

    private static class TheTVDBClientV2
    extends net.filemaid.web.TheTVDBClientV2
    implements ThumbnailProvider,
    TheTVDBClient {
        private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> new LocalSearch<SearchResult>(this.getIndex(), SearchResult::getEffectiveNames));

        public TheTVDBClientV2(String string) {
            super(string);
        }

        private SearchResult merge(List<SearchResult> list) {
            SearchResult searchResult2 = list.get(0);
            if (list.size() == 1) {
                return searchResult2;
            }
            int n = searchResult2.getId();
            String string = searchResult2.getName();
            String[] stringArray = (String[])list.stream().flatMap(searchResult -> Arrays.stream(searchResult.getAliasNames())).filter(string2 -> !string2.equals(string)).distinct().toArray(String[]::new);
            if (searchResult2 instanceof SearchResultDetails) {
                return new SearchResultDetails(new SearchResult(n, string, stringArray), (SearchResultDetails)searchResult2);
            }
            return new SearchResult(n, string, stringArray);
        }

        @Override
        public List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
            List<SearchResult> list = super.fetchSearchResult(string, locale);
            List<SearchResult> list2 = this.localIndex.get().search(string);
            Map map = Stream.of(list, list2).flatMap(Collection::stream).collect(Collectors.groupingBy(SearchResult::getId, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), this::merge)));
            return MediaDetection.sortBySeriesMatchSimilarity(map.values(), string);
        }

        public List<SearchResult> getIndex() throws Exception {
            return MediaDetection.releaseInfo.getSeriesIndex().stream().map(series -> {
                Integer n = series.getExternalId(XDB.TheTVDB);
                if (n == null) {
                    return null;
                }
                return new SearchResult((int)n, series.getName(), series.getAliasNames());
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public SearchResult id(String string) throws Exception {
            Integer n = Link.IMDb.parseID(string);
            if (n != null) {
                Integer n2 = XDB.IMDb.getExternalId(n, XDB.TheTVDB);
                if (n2 != null) {
                    return new SearchResult(n2);
                }
                return null;
            }
            return super.id(string);
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            return ThumbnailServices.TheTVDB.getThumbnail(n, resolutionVariant);
        }

        @Override
        public Map<String, String> getExternalIds(int n) throws Exception {
            return Collections.emptyMap();
        }

        @Override
        public Series getExternalSeries(int n) throws Exception {
            return null;
        }

        @Override
        public List<Person> getCharacters(int n, Locale locale) throws Exception {
            return this.getActors(n, locale);
        }
    }

    private static class TheTVDBClientV4
    extends net.filemaid.web.TheTVDBClientV4
    implements ThumbnailProvider,
    TheTVDBClient {
        private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> new LocalSearch<SearchResult>(this.getIndex(), SearchResult::getEffectiveNames));

        public TheTVDBClientV4(String string) {
            super(string);
        }

        private SearchResult merge(List<SearchResult> list) {
            SearchResult searchResult2 = list.get(0);
            if (list.size() == 1) {
                return searchResult2;
            }
            int n = searchResult2.getId();
            String string = searchResult2.getName();
            String[] stringArray = (String[])list.stream().flatMap(searchResult -> Arrays.stream(searchResult.getAliasNames())).filter(string2 -> !string2.equals(string)).distinct().toArray(String[]::new);
            if (searchResult2 instanceof SearchResultDetails) {
                return new SearchResultDetails(new SearchResult(n, string, stringArray), (SearchResultDetails)searchResult2);
            }
            return new SearchResult(n, string, stringArray);
        }

        @Override
        public List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
            List<SearchResult> list = super.fetchSearchResult(string, locale);
            List<SearchResult> list2 = this.localIndex.get().search(string);
            Map map = Stream.of(list, list2).flatMap(Collection::stream).collect(Collectors.groupingBy(SearchResult::getId, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), this::merge)));
            return MediaDetection.sortBySeriesMatchSimilarity(map.values(), string);
        }

        public List<SearchResult> getIndex() throws Exception {
            return MediaDetection.releaseInfo.getSeriesIndex().stream().map(series -> {
                Integer n = series.getExternalId(XDB.TheTVDB);
                if (n == null) {
                    return null;
                }
                return new SearchResult((int)n, series.getName(), series.getAliasNames());
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public SearchResult id(String string) throws Exception {
            Integer n = Link.IMDb.parseID(string);
            if (n != null) {
                Integer n2 = XDB.IMDb.getExternalId(n, XDB.TheTVDB);
                if (n2 != null) {
                    return new SearchResult(n2);
                }
                return null;
            }
            return super.id(string);
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            return ThumbnailServices.TheTVDB.getThumbnail(n, resolutionVariant);
        }
    }

    private static class AnidbClient
    extends net.filemaid.web.AnidbClient
    implements ArtworkProvider,
    ThumbnailProvider {
        private final Resource<Map<Integer, Artwork>> artwork = Resource.lazy(Manami.AniDB::getArtwork);

        public AnidbClient(String string, int n) {
            super(string, n);
        }

        @Override
        public List<SearchResult> getIndex() throws Exception {
            return MediaDetection.releaseInfo.getAnidbIndex();
        }

        @Override
        public List<Artwork> getArtwork(int n, Locale locale) throws Exception {
            Artwork artwork = this.artwork.get().get(n);
            return this.artwork == null ? Collections.emptyList() : Collections.singletonList(artwork);
        }

        @Override
        public SearchResult id(String string) throws Exception {
            Integer n = Link.IMDb.parseID(string);
            if (n != null) {
                int n2;
                Integer n3 = XDB.IMDb.getExternalId(n, XDB.TheTVDB);
                if (n3 != null && (n2 = AnimeList.map(n3, 1, 1, AnimeLists.DB.TheTVDB, AnimeLists.DB.AniDB)) > 0) {
                    return new SearchResult(n2);
                }
                return null;
            }
            return super.id(string);
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            return ThumbnailServices.AniDB.getThumbnail(n, resolutionVariant);
        }
    }

    private static class OpenSubtitlesXmlRpcClient
    extends net.filemaid.web.OpenSubtitlesXmlRpcClient
    implements OpenSubtitlesClient {
        private final Resource<LocalSearch<SubtitleSearchResult>> localIndex = Resource.lazy(() -> {
            Stream<SubtitleSearchResult> stream = MediaDetection.releaseInfo.getSeriesIndex().stream().map(series -> {
                Integer n = series.getExternalId(XDB.IMDb);
                Integer n2 = series.getExternalId(XDB.TheMovieDB);
                if (n == null || n2 == null) {
                    return null;
                }
                return new SubtitleSearchResult(n, series.getName(), series.getAliasNames(), series.getYear(), n, n2, Locale.ENGLISH, SubtitleSearchResult.Kind.Series, series.getScore());
            });
            Stream<SubtitleSearchResult> stream2 = MediaDetection.releaseInfo.getMovieIndex().stream().filter(movie -> movie.getImdbId() > 0).map(movie -> new SubtitleSearchResult(movie.getImdbId(), movie.getName(), movie.getAliasNames(), movie.getYear(), movie.getImdbId(), movie.getTmdbId(), Locale.ENGLISH, SubtitleSearchResult.Kind.Movie, -1));
            return new LocalSearch<SubtitleSearchResult>(Stream.concat(stream, stream2).filter(Objects::nonNull).collect(Collectors.toList()), SearchResult::getEffectiveNames);
        });

        public OpenSubtitlesXmlRpcClient(String string) {
            super(string);
        }

        @Override
        public OpenSubtitlesClient newInstance() {
            return new OpenSubtitlesXmlRpcClient(this.xmlrpc.getUserAgent());
        }

        @Override
        public List<SubtitleSearchResult> getIndex() throws Exception {
            return this.localIndex.get().stream().collect(Collectors.toList());
        }

        @Override
        public List<SubtitleSearchResult> search(String string) throws Exception {
            return MediaDetection.sortBySimilarity(this.localIndex.get().search(string), Collections.singleton(string), new MetricAvg(MediaDetection.getSeriesMatchMetric(), MediaDetection.getMovieMatchMetric()), null);
        }

        @Override
        public List<Movie> searchMovie(String string, Locale locale) throws Exception {
            return MediaDetection.sortBySimilarity(this.localIndex.get().search(string), Collections.singleton(string), MediaDetection.getMovieMatchMetric(), null).stream().map(Movie::new).collect(Collectors.toList());
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            SubtitleSearchResult subtitleSearchResult2 = this.localIndex.get().stream().filter(subtitleSearchResult -> n == subtitleSearchResult.getImdbId()).findFirst().orElse(null);
            if (subtitleSearchResult2 != null) {
                if (subtitleSearchResult2.isMovie()) {
                    return ThumbnailServices.TheMovieDB.getThumbnail(subtitleSearchResult2.getTmdbId(), resolutionVariant);
                }
                if (subtitleSearchResult2.isSeries()) {
                    return ThumbnailServices.TheMovieDB_TV.getThumbnail(subtitleSearchResult2.getTmdbId(), resolutionVariant);
                }
            }
            return null;
        }

        @Override
        public URI getAccountLink(String string) {
            if (string.isEmpty()) {
                return URI.create("https://www.opensubtitles.org/en/newuser");
            }
            return URI.create("https://www.opensubtitles.org/en/support");
        }
    }

    private static class OpenSubtitlesRestClient
    extends net.filemaid.web.OpenSubtitlesRestClient
    implements OpenSubtitlesClient {
        private final Resource<LocalSearch<SubtitleSearchResult>> localIndex = Resource.lazy(() -> {
            Stream<SubtitleSearchResult> stream = MediaDetection.releaseInfo.getSeriesIndex().stream().map(series -> {
                Integer n = series.getExternalId(XDB.IMDb);
                Integer n2 = series.getExternalId(XDB.TheMovieDB);
                if (n == null || n2 == null) {
                    return null;
                }
                return new SubtitleSearchResult(n, series.getName(), series.getAliasNames(), series.getYear(), n, n2, Locale.ENGLISH, SubtitleSearchResult.Kind.Series, series.getScore());
            });
            Stream<SubtitleSearchResult> stream2 = MediaDetection.releaseInfo.getMovieIndex().stream().filter(movie -> movie.getImdbId() > 0).map(movie -> new SubtitleSearchResult(movie.getImdbId(), movie.getName(), movie.getAliasNames(), movie.getYear(), movie.getImdbId(), movie.getTmdbId(), Locale.ENGLISH, SubtitleSearchResult.Kind.Movie, -1));
            return new LocalSearch<SubtitleSearchResult>(Stream.concat(stream, stream2).filter(Objects::nonNull).collect(Collectors.toList()), SearchResult::getEffectiveNames);
        });

        public OpenSubtitlesRestClient(String string, String string2) {
            super(string, string2);
        }

        @Override
        public OpenSubtitlesClient newInstance() {
            return new OpenSubtitlesRestClient(this.useragent, this.apikey);
        }

        @Override
        public List<SubtitleSearchResult> searchIMDB(String string) throws Exception {
            return null;
        }

        @Override
        public Locale detectLanguage(byte[] byArray) throws Exception {
            return null;
        }

        @Override
        public List<SubtitleSearchResult> getIndex() throws Exception {
            return this.localIndex.get().stream().collect(Collectors.toList());
        }

        @Override
        public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
            SubtitleSearchResult subtitleSearchResult2 = this.localIndex.get().stream().filter(subtitleSearchResult -> n == subtitleSearchResult.getImdbId()).findFirst().orElse(null);
            if (subtitleSearchResult2 != null) {
                if (subtitleSearchResult2.isMovie()) {
                    return ThumbnailServices.TheMovieDB.getThumbnail(subtitleSearchResult2.getTmdbId(), resolutionVariant);
                }
                if (subtitleSearchResult2.isSeries()) {
                    return ThumbnailServices.TheMovieDB_TV.getThumbnail(subtitleSearchResult2.getTmdbId(), resolutionVariant);
                }
            }
            return null;
        }

        @Override
        public URI getAccountLink(String string) {
            if (string.isEmpty()) {
                return URI.create("https://www.opensubtitles.com/en/users/profile");
            }
            return URI.create("https://www.opensubtitles.com/en/users/vip");
        }
    }

    private static class AnimeLists
    extends net.filemaid.web.AnimeLists {
        private AnimeLists() {
        }

        @Override
        public AnimeLists.Model getModel() throws Exception {
            return MediaDetection.releaseInfo.getAnimeListModel();
        }
    }
}

