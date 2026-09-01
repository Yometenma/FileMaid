package net.filemaid.web;

import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Extra;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieCollection;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.Person;
import net.filemaid.web.Score;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.TMDbCore;

public class TMDbMovieClient
implements MovieLookupService,
ArtworkProvider {
    private final TMDbCore core;

    public TMDbMovieClient(TMDbCore tMDbCore) {
        this.core = tMDbCore;
    }

    @Override
    public String getIdentifier() {
        return "TheMovieDB";
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
    public Movie grepMovie(String string) {
        if (RegularExpressions.DIGIT.matcher(string).matches()) {
            return Movie.TMDB(Integer.parseInt(string));
        }
        return MovieLookupService.super.grepMovie(string);
    }

    @Override
    public List<Movie> searchMovie(String string, Locale locale) throws Exception {
        return this.getSearchCache(locale).computeIfAbsent(string.toLowerCase(locale), element -> {
            Movie movie = Movie.matchNameYear(string);
            if (movie == null) {
                return this.fetchSearchResult(string.trim(), -1, locale);
            }
            List<Movie> list = this.fetchSearchResult(movie.getName(), movie.getYear(), locale);
            if (list.size() > 0) {
                return list;
            }
            return this.fetchSearchResult(movie.getName(), -1, locale);
        });
    }

    protected List<Movie> fetchSearchResult(String string, int n, Locale locale) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("query", string);
        if (n > 0) {
            linkedHashMap.put("year", n);
        }
        if (this.core.isAdultEnabled()) {
            linkedHashMap.put("include_adult", "true");
        }
        Object object = this.core.request("search/movie", linkedHashMap, locale, Cache.ONE_WEEK);
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            try {
                String[] stringArray;
                Integer movieId = JsonUtilities.getInteger(map, "id");
                SimpleDate simpleDate = JsonUtilities.getStringValue(map, "release_date", SimpleDate::parse);
                if (simpleDate == null) {
                    Logging.debug.warning(Logging.message("Missing data", "release_date", JsonUtilities.json(map)));
                    return null;
                }
                String originalTitle = JsonUtilities.getString(map, "original_title");
                String string2 = JsonUtilities.optionalString(map, "title").orElse(originalTitle);
                if (string2 == null) {
                    Logging.debug.warning(Logging.message("Missing data", "title", JsonUtilities.json(map)));
                    return null;
                }
                if (string2.equals(originalTitle)) {
                    stringArray = null;
                } else {
                    String[] stringArray2 = new String[1];
                    stringArray = stringArray2;
                    stringArray2[0] = originalTitle;
                }
                String[] stringArray3 = stringArray;
                return new Movie(movieId, string2, stringArray3, simpleDate.getYear(), 0, movieId, locale);
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(map, exception));
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected Cache.TypedCache<List<Movie>> getSearchCache(Locale locale) {
        Object object = this.core.getLanguageCode(locale);
        if (this.core.isAdultEnabled()) {
            object = (String)object + "_adult";
        }
        return Cache.getCache("tmdb_search_" + (String)object, CacheType.Daily).castList(Movie.class);
    }

    public Movie lookupMovieDescriptor(Movie movie2) throws Exception {
        int n = movie2.getTmdbId();
        if (n > 0) {
            return this.getIndex().stream().filter(movie -> n == movie.getTmdbId()).findFirst().orElse(null);
        }
        int n2 = movie2.getImdbId();
        if (n2 > 0) {
            return this.getIndex().stream().filter(movie -> n2 == movie.getImdbId()).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public Movie getMovieDescriptor(Movie movie, Locale locale) throws Exception {
        MovieDetails movieDetails;
        if ((movie.getTmdbId() > 0 || movie.getImdbId() > 0) && (movieDetails = this.getMovieInfo(movie, locale, false)) != null) {
            String string = Optional.ofNullable(movieDetails.getName()).orElseGet(movieDetails::getOriginalName);
            if (string == null) {
                Logging.debug.warning(Logging.message("Missing data", "title", movieDetails.properties()));
                return null;
            }
            String[] stringArray = (String[])Stream.of(movieDetails.getOriginalName()).filter(string2 -> string2 != null && !string2.isEmpty() && !string2.equals(string)).toArray(String[]::new);
            int n = movieDetails.getReleased() != null ? movieDetails.getReleased().getYear() : movie.getYear();
            int n2 = movieDetails.getId();
            int n3 = movieDetails.getImdbId() != null ? movieDetails.getImdbId() : 0;
            return new Movie(n2, string, stringArray, n, n3, n2, locale);
        }
        return null;
    }

    public MovieDetails getMovieInfo(Movie movie, Locale locale, boolean bl) throws Exception {
        try {
            if (movie.getTmdbId() > 0) {
                return this.getMovieInfo(Integer.toString(movie.getTmdbId()), locale != null ? locale : Locale.US, bl);
            }
            if (movie.getImdbId() > 0) {
                return this.getMovieInfo(Link.IMDb.getID(movie), locale != null ? locale : Locale.US, bl);
            }
        }
        catch (FileNotFoundException fileNotFoundException) {
            Logging.debug.finest(Logging.format("Movie data not found: %s [%s / %s] => %s", movie, Link.TheMovieDB.getID(movie), Link.IMDb.getID(movie), fileNotFoundException));
        }
        return null;
    }

    protected MovieDetails getMovieInfo(String string2, Locale locale, boolean bl) throws Exception {
        Object object;
        Object object2;
        Object object3 = this.core.request("movie/" + string2, bl ? Collections.singletonMap("append_to_response", "translations,alternative_titles,release_dates,keywords,credits") : Collections.emptyMap(), locale, Cache.ONE_MONTH);
        EnumMap<MovieDetails.Property, String> enumMap = JsonUtilities.getEnumMap(object3, MovieDetails.Property.class);
        Stream.of(MovieDetails.Property.poster_path, MovieDetails.Property.backdrop_path).forEach(property -> enumMap.computeIfPresent((MovieDetails.Property)((Object)property), (property2, string) -> {
            if (bl) {
                try {
                    return this.core.resolveImage((String)string).toExternalForm();
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.message("Bad data", property, object3));
                }
            }
            return null;
        }));
        try {
            object2 = JsonUtilities.getMap(object3, "belongs_to_collection");
            enumMap.put(MovieDetails.Property.collection, JsonUtilities.getString(object2, "name"));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "belongs_to_collection", object3));
        }
        object2 = new ArrayList();
        try {
            JsonUtilities.streamJsonObjects(object3, "genres").map(map -> JsonUtilities.getString(map, "name")).filter(Objects::nonNull).forEach(((List)object2)::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "genres", object3));
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        try {
            JsonUtilities.streamJsonObjects(object3, "spoken_languages").map(map -> JsonUtilities.getString(map, "iso_639_1")).filter(Objects::nonNull).forEach(arrayList::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "spoken_languages", object3));
        }
        ArrayList<String> arrayList2 = new ArrayList<String>();
        try {
            JsonUtilities.streamJsonObjects(object3, "production_countries").map(map -> JsonUtilities.getString(map, "iso_3166_1")).filter(Objects::nonNull).forEach(arrayList2::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "production_countries", object3));
        }
        ArrayList<String> arrayList3 = new ArrayList<String>();
        try {
            JsonUtilities.streamJsonObjects(object3, "production_companies").map(map -> JsonUtilities.getString(map, "name")).filter(Objects::nonNull).forEach(arrayList3::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "production_companies", object3));
        }
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        try {
            JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object3, "translations"), "translations").forEach(map2 -> {
                String string = JsonUtilities.getString(map2, "iso_3166_1");
                String title = JsonUtilities.getString(JsonUtilities.getMap(map2, "data"), "title");
                if (string != null && title != null) {
                    linkedHashMap.put(string, title);
                }
            });
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "translations", object3));
        }
        ArrayList<String> arrayList4 = new ArrayList<String>();
        try {
            JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object3, "alternative_titles"), "titles").map(map -> JsonUtilities.getString(map, "title")).filter(Objects::nonNull).forEach(arrayList4::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "alternative_titles", object3));
        }
        ArrayList<String> arrayList5 = new ArrayList<String>();
        try {
            JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object3, "keywords"), "keywords").map(map -> JsonUtilities.getString(map, "name")).filter(Objects::nonNull).forEach(arrayList5::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "keywords", object3));
        }
        LinkedHashMap<String, String> linkedHashMap2 = new LinkedHashMap<String, String>();
        try {
            JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object3, "release_dates"), "results").forEach(map3 -> {
                String string = JsonUtilities.getString(map3, "iso_3166_1");
                if (string != null) {
                    JsonUtilities.streamJsonObjects(map3, "release_dates").map(map -> JsonUtilities.getString(map, "certification")).filter(Objects::nonNull).findFirst().ifPresent(certification -> linkedHashMap2.put(string, (String)certification));
                }
            });
            object = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
        }
        catch (Exception exception) {
            try {
                Logging.debug.warning(Logging.message("Bad data", "certification", object3));
                object = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
            }
            catch (Throwable throwable) {
                String string3 = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
                enumMap.put(MovieDetails.Property.certification, (String)linkedHashMap2.get(string3));
                throw throwable;
            }
            enumMap.put(MovieDetails.Property.certification, (String)linkedHashMap2.get(object));
        }
        enumMap.put(MovieDetails.Property.certification, (String)linkedHashMap2.get(object));
        object = new ArrayList();
        try {
            Function<String, String> function = string -> Normalization.replaceSpace(string, " ").trim();
            Stream.of("cast", "crew").flatMap(string -> JsonUtilities.streamJsonObjects(JsonUtilities.getMap(object3, "credits"), string)).map(map -> {
                Integer n = JsonUtilities.getInteger(map, "id");
                String string = (String)JsonUtilities.getStringValue(map, "name", function);
                String character = (String)JsonUtilities.getStringValue(map, "character", function);
                String string3 = (String)JsonUtilities.getStringValue(map, "job", function);
                String string4 = (String)JsonUtilities.getStringValue(map, "department", function);
                Integer n2 = JsonUtilities.getInteger(map, "order");
                URL uRL = JsonUtilities.getStringValue(map, "profile_path", this.core::resolveImage);
                if (string4 == null) {
                    string4 = (String)JsonUtilities.getStringValue(map, "known_for_department", function);
                }
                return new Person(n, string, character, string3, string4, n2, uRL);
            }).sorted(Person.CREDIT_ORDER).forEach(((List)object)::add);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Bad data", "casts", object3));
        }
        return new MovieDetails(enumMap, arrayList4, (List<String>)object2, arrayList5, linkedHashMap2, linkedHashMap, arrayList, arrayList2, arrayList3, (List<Person>)object);
    }

    protected Integer getCollectionId(int n, Locale locale) throws Exception {
        Object object = this.core.request("movie/" + n, Collections.emptyMap(), locale, Cache.ONE_MONTH);
        Map<Object, Object> map = JsonUtilities.getMap(object, "belongs_to_collection");
        return JsonUtilities.getInteger(map, "id");
    }

    public MovieCollection getCollection(int n, Locale locale) throws Exception {
        Integer n2 = this.getCollectionId(n, locale);
        if (n2 == null) {
            return null;
        }
        Object object = this.core.request("collection/" + n2, Collections.emptyMap(), locale, Cache.ONE_MONTH);
        List<Movie> list = JsonUtilities.streamJsonObjects(object, "parts").filter(map -> null != JsonUtilities.getString(map, "release_date")).sorted(Comparator.comparing(map -> JsonUtilities.getString(map, "release_date"))).map(map -> {
            int movieId = JsonUtilities.getInteger(map, "id");
            String string = JsonUtilities.getString(map, "title");
            Integer releaseYear = StringUtilities.matchInteger(JsonUtilities.getString(map, "release_date"));
            return new Movie(movieId, string, null, releaseYear, 0, movieId, locale);
        }).collect(Collectors.toList());
        String string = JsonUtilities.getString(object, "name");
        String string2 = JsonUtilities.getString(object, "overview");
        URL uRL = JsonUtilities.getStringValue(object, "poster_path", this.core::resolveImage);
        URL uRL2 = JsonUtilities.getStringValue(object, "backdrop_path", this.core::resolveImage);
        return new MovieCollection(list, n2, string, string2, uRL, uRL2);
    }

    @Override
    public List<Artwork> getArtwork(int n, Locale locale) throws Exception {
        return this.core.requestImages("movie", n, locale);
    }

    public List<Extra> getExtras(int n) throws Exception {
        return this.core.requestVideos("movie", n);
    }

    public Map<String, List<String>> getAlternativeTitles(int n) throws Exception {
        Object object = this.core.request("movie/" + n + "/alternative_titles", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "titles").collect(Collectors.groupingBy(map -> JsonUtilities.getString(map, "iso_3166_1"), LinkedHashMap::new, Collectors.mapping(map -> JsonUtilities.getString(map, "title"), Collectors.toList())));
    }

    public Map<String, String> getTranslations(int n) throws Exception {
        Object object = this.core.request("movie/" + n + "/translations", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        JsonUtilities.streamJsonObjects(object, "translations").forEach(map2 -> {
            String string = JsonUtilities.getString(map2, "iso_3166_1");
            String string2 = JsonUtilities.getString(JsonUtilities.getMap(map2, "data"), "title");
            if (string != null && string2 != null) {
                linkedHashMap.put(string, string2);
            }
        });
        return linkedHashMap;
    }

    public List<String> getKeywords(int n) throws Exception {
        Object object = this.core.request("movie/" + n + "/keywords", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "keywords").map(map -> JsonUtilities.getString(map, "name")).collect(Collectors.toList());
    }

    public List<Movie> discover(LocalDate localDate, LocalDate localDate2, Locale locale) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(3);
        linkedHashMap.put("primary_release_date.gte", localDate);
        linkedHashMap.put("primary_release_date.lte", localDate2);
        linkedHashMap.put("sort_by", "popularity.desc");
        return this.discover(linkedHashMap, locale);
    }

    public List<Movie> discover(int n, Locale locale) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("primary_release_year", n);
        linkedHashMap.put("sort_by", "vote_count.desc");
        return this.discover(linkedHashMap, locale);
    }

    public List<Movie> discover(Map<String, Object> map2, Locale locale) throws Exception {
        Object object = this.core.request("discover/movie", map2, locale, Cache.NEVER);
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            String string = JsonUtilities.getString(map, "title");
            int n = JsonUtilities.getStringValue(map, "release_date", SimpleDate::parse).getYear();
            int n2 = JsonUtilities.getInteger(map, "id");
            return new Movie(n2, string, null, n, 0, n2, locale);
        }).collect(Collectors.toList());
    }

    public List<Movie> getIndex() throws Exception {
        return this.core.requestExport("movie_ids", stream -> stream.map(object -> {
            boolean bl = JsonUtilities.getStringValue(object, "adult", Boolean::parseBoolean);
            boolean bl2 = JsonUtilities.getStringValue(object, "video", Boolean::parseBoolean);
            if (bl || bl2) {
                return null;
            }
            Integer n = JsonUtilities.getInteger(object, "id");
            String string = JsonUtilities.getString(object, "original_title");
            Double d = JsonUtilities.getDouble(object, "popularity");
            if (n == null || string == null || d == null || d < 5.0) {
                return null;
            }
            return Score.of(new Movie(n, string, null, 0, 0, n, null), d.intValue());
        }).filter(Objects::nonNull).sorted(Score.descending()).map(Score::getValue).collect(Collectors.toList()));
    }
}

