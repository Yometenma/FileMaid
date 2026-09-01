package net.filemaid.web;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.FloodLimit;
import net.filemaid.web.Link;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.Person;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.WebRequest;

public class OMDbClient
implements MovieLookupService {
    private static final FloodLimit REQUEST_LIMIT = new FloodLimit(2, 1L, TimeUnit.SECONDS);
    private String apikey;

    public OMDbClient(String string) {
        this.apikey = string;
    }

    @Override
    public String getIdentifier() {
        return "OMDb";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.omdb");
    }

    public List<Movie> getIndex() throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<Movie> searchMovie(String string, Locale locale) throws Exception {
        Matcher matcher = Pattern.compile("(.+)\\b(19\\d{2}|20\\d{2})$").matcher(string);
        if (matcher.matches()) {
            return this.searchMovie(matcher.group(1).trim(), Integer.parseInt(matcher.group(2)), locale);
        }
        return this.searchMovie(string, -1, locale);
    }

    public List<Movie> searchMovie(String string, int n, Locale locale) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("s", string);
        linkedHashMap.put("y", n > 0 ? Integer.valueOf(n) : null);
        Object object = this.request(linkedHashMap);
        return JsonUtilities.streamJsonObjects(object, "Search").filter(map -> "movie".equals(JsonUtilities.getString(map, "Type"))).map(map -> this.parseMovie(map, locale)).filter(movie -> movie.getYear() >= 1930).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Movie getMovieDescriptor(Movie movie, Locale locale) throws Exception {
        if (movie.getImdbId() > 0) {
            Movie movie2 = this.parseMovie(this.getMovieInfo(movie.getImdbId(), null, null, false), locale);
            if (movie2 != null) {
                return movie2;
            }
            throw new LookupException((Object)"IMDB ID not found", movie.getImdbId());
        }
        throw new LookupException((Object)"OMDb lookup not supported", movie.getTmdbId());
    }

    private Movie parseMovie(Object object, Locale locale) {
        String string = JsonUtilities.getString(object, "Title");
        Integer n = JsonUtilities.getInteger(object, "Year");
        Integer n2 = JsonUtilities.getStringValue(object, "imdbID", Link.IMDb::parseID);
        if (string == null || n == null || n2 == null) {
            Logging.debug.warning(Logging.message("Invalid movie properties", object));
            return null;
        }
        return new Movie(n2, string, null, n, n2, 0, locale);
    }

    public Object request(Map<String, Object> map) throws Exception {
        Cache cache = Cache.getConcurrentCache(this.getName(), CacheType.Monthly);
        return cache.json(WebRequest.encodeParameters(map), string -> this.getResource("?" + string + "&apikey=" + this.apikey)).fetch(CachedResource.withPermit(CachedResource.fetchIfModified(), uRL -> REQUEST_LIMIT.acquirePermit())).expire(Cache.ONE_MONTH).get();
    }

    public URL getResource(String string) throws Exception {
        return WebRequest.newURL("https://private.omdbapi.com/" + string);
    }

    public Object getMovieInfo(Integer n, String string, String string2, boolean bl) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        if (n != null) {
            linkedHashMap.put("i", Link.IMDb.getID(n));
        }
        if (string != null) {
            linkedHashMap.put("t", string);
        }
        if (string2 != null) {
            linkedHashMap.put("y", string2);
        }
        linkedHashMap.put("tomatoes", String.valueOf(bl));
        return this.request(linkedHashMap);
    }

    public MovieDetails getMovieInfo(Movie movie) throws Exception {
        Object object;
        Object object2 = object = movie.getImdbId() > 0 ? this.getMovieInfo(movie.getImdbId(), null, null, false) : this.getMovieInfo(null, movie.getName(), String.valueOf(movie.getYear()), false);
        if (!Boolean.parseBoolean(JsonUtilities.getString(object, "Response"))) {
            if (movie.getImdbId() > 0) {
                throw new LookupException((Object)"IMDB ID not found", movie.getImdbId());
            }
            throw new LookupException(object, movie);
        }
        EnumMap<MovieDetails.Property, String> enumMap = new EnumMap<MovieDetails.Property, String>(MovieDetails.Property.class);
        enumMap.put(MovieDetails.Property.title, JsonUtilities.getString(object, "Title"));
        enumMap.put(MovieDetails.Property.certification, JsonUtilities.getString(object, "Rated"));
        enumMap.put(MovieDetails.Property.runtime, JsonUtilities.getStringValue(object, "Runtime", this::parseRuntimeMinutes));
        enumMap.put(MovieDetails.Property.tagline, JsonUtilities.getString(object, "Plot"));
        enumMap.put(MovieDetails.Property.vote_average, JsonUtilities.getStringValue(object, "imdbRating", this::parseVoteAverage));
        enumMap.put(MovieDetails.Property.vote_count, JsonUtilities.getStringValue(object, "imdbVotes", this::parseVoteCount));
        enumMap.put(MovieDetails.Property.imdb_id, JsonUtilities.getString(object, "imdbID"));
        enumMap.put(MovieDetails.Property.poster_path, JsonUtilities.getString(object, "Poster"));
        enumMap.put(MovieDetails.Property.release_date, JsonUtilities.getStringValue(object, "Released", this::parseReleaseDate));
        List<String> list = this.split(JsonUtilities.getString(object, "Genre"), Object::toString);
        List<String> list2 = this.split(JsonUtilities.getString(object, "Language"), Object::toString);
        List<String> list3 = this.split(JsonUtilities.getString(object, "Country"), Object::toString);
        ArrayList<Person> arrayList = new ArrayList<Person>();
        arrayList.addAll(this.split(JsonUtilities.getString(object, "Actors"), string -> new Person((String)string, "Actor")));
        arrayList.addAll(this.split(JsonUtilities.getString(object, "Director"), string -> new Person((String)string, "Director")));
        arrayList.addAll(this.split(JsonUtilities.getString(object, "Writer"), string -> new Person((String)string, "Writer")));
        return new MovieDetails(enumMap, Collections.emptyList(), list, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), list2, list3, Collections.emptyList(), arrayList);
    }

    private boolean isNA(String string) {
        return string == null || string.isEmpty() || string.equals("N/A");
    }

    private String parseVoteAverage(String string) {
        return this.isNA(string) ? null : string;
    }

    private String parseVoteCount(String string) {
        return this.isNA(string) ? null : RegularExpressions.NON_DIGIT.matcher(string).replaceAll("");
    }

    private String parseRuntimeMinutes(String string) {
        List<Integer> list = StringUtilities.matchIntegers(string);
        switch (list.size()) {
            case 0: {
                return null;
            }
            case 1: {
                return Integer.toString(list.get(0));
            }
        }
        return Integer.toString(list.get(0) * 60 + list.get(1));
    }

    private String parseReleaseDate(String string) {
        return this.isNA(string) ? null : (String)Stream.of("d MMM yyyy", "yyyy").map(string2 -> this.parsePartialDate(string, (String)string2)).filter(Objects::nonNull).map(Objects::toString).findFirst().orElse(null);
    }

    private SimpleDate parsePartialDate(String string, String string2) {
        if (string != null && !string.isEmpty()) {
            try {
                TemporalAccessor temporalAccessor = DateTimeFormatter.ofPattern(string2, Locale.ENGLISH).parse(string);
                int n = this.get(temporalAccessor, ChronoField.YEAR, 0);
                int n2 = this.get(temporalAccessor, ChronoField.MONTH_OF_YEAR, 1);
                int n3 = this.get(temporalAccessor, ChronoField.DAY_OF_MONTH, 1);
                if (n > 0) {
                    return SimpleDate.of(n, n2, n3);
                }
            }
            catch (DateTimeParseException dateTimeParseException) {
                Logging.debug.warning(Logging.format("Bad date: %s =~ %s => %s", string, string2, dateTimeParseException));
            }
        }
        return null;
    }

    private int get(TemporalAccessor temporalAccessor, ChronoField chronoField, int n) {
        return temporalAccessor.isSupported(chronoField) ? temporalAccessor.get(chronoField) : n;
    }

    private <T> List<T> split(String string2, Function<String, T> function) {
        if (string2 == null || string2.isEmpty()) {
            return Collections.emptyList();
        }
        String[] stringArray = string2.split(",");
        return Stream.of(stringArray).map(String::trim).filter(string -> !string.isEmpty() && !string.equals("N/A")).map(function).collect(Collectors.toList());
    }
}

