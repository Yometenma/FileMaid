package net.filemaid.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.Extra;
import net.filemaid.web.LookupException;
import net.filemaid.web.WebRequest;

public class TMDbCore {
    private final URI api;
    private final String apikey;
    private final boolean adult;
    public final Resource<URI> imageBaseUrl = Resource.lazy(() -> JsonUtilities.getStringValue(JsonUtilities.getMap(this.getConfiguration(), "images"), "secure_base_url", URI::create));

    private static URI defaultEndpoint() {
        return URI.create("https://api.themoviedb.org/3/");
    }

    private static boolean defaultAdultEnabled() {
        return Boolean.parseBoolean(System.getProperty("net.filemaid.TheMovieDB.adult"));
    }

    public TMDbCore(URI uRI, String string, boolean bl) {
        this.api = uRI;
        this.apikey = string;
        this.adult = bl;
    }

    public TMDbCore(String string) {
        this(TMDbCore.defaultEndpoint(), string, TMDbCore.defaultAdultEnabled());
    }

    public boolean isAdultEnabled() {
        return this.adult;
    }

    public Cache getCache(String string, CacheType cacheType) {
        if (string == null || string.isEmpty()) {
            return Cache.getConcurrentCache("tmdb", cacheType);
        }
        return Cache.getConcurrentCache("tmdb_" + string, cacheType);
    }

    public Object request(String string, Map<String, Object> map, Locale locale, Duration duration) throws Exception {
        String string3 = map.isEmpty() ? string : string + "?" + WebRequest.encodeParameters(map);
        String string4 = this.getLanguageCode(locale);
        Cache cache = this.getCache(string4, CacheType.Monthly);
        Object object = cache.json(string3, string2 -> this.getResource((String)string2, string4)).fetch(CachedResource.fetchIfNoneMatch(string3, cache)).expire(duration).get();
        if (JsonUtilities.asMap(object).isEmpty()) {
            throw new LookupException((Object)("TMDB ID not found: " + JsonUtilities.json(object)), string3);
        }
        return object;
    }

    public URL getResource(String string, String string2) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.append(string.lastIndexOf(63) < 0 ? (char)'?' : '&');
        if (string2 != null) {
            stringBuilder.append("language=").append(string2).append('&');
        }
        stringBuilder.append("api_key=").append(this.apikey);
        return WebRequest.newURL(this.api, stringBuilder.toString());
    }

    public <R> R requestExport(String string2, Function<Stream<?>, R> function) throws Exception {
        Cache cache = this.getCache("exports", CacheType.Persistent);
        try {
            Object object = cache.stream(string2, string -> {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM_dd_YYYY");
                LocalDate localDate = LocalDate.now().minusWeeks(1L).withDayOfMonth(1);
                String string3 = string + "_" + dateTimeFormatter.format(localDate) + ".json.gz";
                return WebRequest.newURL("https://files.tmdb.org/p/exports/" + string3);
            }, GZIPInputStream::new, inputStream -> {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)inputStream, StandardCharsets.UTF_8));
                return function.apply(bufferedReader.lines().map(string -> JsonUtilities.readJson(string)));
            }).expire(Cache.ONE_MONTH).get();
            return (R)object;
        }
        finally {
            cache.flush();
        }
    }

    public List<Artwork> requestImages(String string, int n, Locale locale) throws Exception {
        Object object3 = this.request(string + "/" + n + "/images", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        ArrayList<Artwork> arrayList = new ArrayList<Artwork>();
        JsonUtilities.asMap(object3).forEach((object, object2) -> {
            for (Object item : JsonUtilities.asArray(object2)) {
                URL uRL = JsonUtilities.getStringValue(item, "file_path", this::resolveImage);
                Integer width = JsonUtilities.getInteger(item, "width");
                Integer n2 = JsonUtilities.getInteger(item, "height");
                Locale locale2 = JsonUtilities.getStringValue(item, "iso_639_1", Locale::forLanguageTag);
                Double d = JsonUtilities.getDouble(item, "vote_average");
                Integer n3 = JsonUtilities.getInteger(item, "vote_count");
                double d2 = Math.round(d * (double)n3.intValue() * (double)n2.intValue());
                if (uRL == null) {
                    Logging.debug.warning(Logging.message("Bad artwork response", JsonUtilities.json(item)));
                    continue;
                }
                arrayList.add(new Artwork(uRL, locale2, d2, "tmdb", object, width + "x" + n2));
            }
        });
        arrayList.sort(Artwork.relevanceOrder(locale, Locale.ENGLISH));
        return arrayList;
    }

    public List<Extra> requestVideos(String string, int n) throws Exception {
        Object object = this.request(string + "/" + n + "/videos", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            String string2 = JsonUtilities.getString(map, "type");
            String string3 = JsonUtilities.getString(map, "name");
            String string4 = JsonUtilities.getString(map, "site");
            String string5 = JsonUtilities.getString(map, "key");
            String string6 = Stream.of("iso_639_1", "iso_3166_1").map(field -> JsonUtilities.getString(map, field)).filter(Objects::nonNull).collect(Collectors.joining("_"));
            Long l = JsonUtilities.getStringValue(map, "size", Long::parseLong);
            Boolean bl = JsonUtilities.getStringValue(map, "official", Boolean::parseBoolean);
            Instant instant = JsonUtilities.getStringValue(map, "published_at", Instant::parse);
            return new Extra(string2, string3, string4, string5, string6, l, bl, instant);
        }).collect(Collectors.toList());
    }

    public Object getConfiguration() throws Exception {
        return this.request("configuration", Collections.emptyMap(), Locale.ROOT, Cache.ONE_MONTH);
    }

    public URL resolveImage(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        try {
            return WebRequest.newURL(this.imageBaseUrl.get(), "original" + string);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Bad Image URL", string, exception));
            return null;
        }
    }

    public String getLanguageCode(Locale locale) {
        String string;
        if (locale == null) {
            return null;
        }
        switch (string = locale.getLanguage()) {
            case "iw": {
                return "he-IL";
            }
            case "in": {
                return "id-ID";
            }
            case "": {
                return null;
            }
        }
        String string2 = locale.getCountry();
        if (string2.length() > 0) {
            return string + "-" + string2;
        }
        return string;
    }
}

