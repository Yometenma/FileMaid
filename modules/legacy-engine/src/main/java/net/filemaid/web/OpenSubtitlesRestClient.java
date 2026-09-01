package net.filemaid.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.FloodLimit;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.OpenSubtitlesHasher;
import net.filemaid.web.OpenSubtitlesRestSubtitleDescriptor;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.SubtitleSearchResult;
import net.filemaid.web.WebRequest;

public class OpenSubtitlesRestClient
implements SubtitleProvider,
SubtitleLookupService,
MovieLookupService {
    private static final String DEFAULT_HOST = "api.opensubtitles.com";
    private static final FloodLimit REQUEST_LIMIT = new FloodLimit(5, 1L, TimeUnit.SECONDS);
    public final String useragent;
    public final String apikey;
    private String username = "";
    private String password = "";

    private static URL getEndpoint(String string, String string2) throws Exception {
        return WebRequest.newURL("https://" + string + "/api/v1/" + string2);
    }

    public OpenSubtitlesRestClient(String string, String string2) {
        this.useragent = string;
        this.apikey = string2;
    }

    public synchronized void login(String string, String string2) {
        this.username = string;
        this.password = string2;
    }

    @Override
    public synchronized boolean requireLogin() {
        return this.username == null || this.username.isEmpty();
    }

    @Override
    public String getIdentifier() {
        return "OpenSubtitles";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.opensubtitles");
    }

    @Override
    public URI getLink() {
        return URI.create("https://www.opensubtitles.com/");
    }

    public String getLanguageCode(Locale locale) {
        String string;
        if (locale == null) {
            return null;
        }
        switch (string = locale.getLanguage()) {
            case "iw": {
                return "he";
            }
            case "in": {
                return "id";
            }
            case "zh": {
                return locale.getCountry().equals("TW") ? "zh-tw" : "zh-cn";
            }
            case "pt": {
                return locale.getCountry().equals("BR") ? "pt-br" : "pt-pt";
            }
            case "": {
                return null;
            }
        }
        return string;
    }

    public String originalName(File file) throws IOException {
        String string = XattrMetaInfo.xattr.getOriginalName(file);
        if (string == null || string.isEmpty()) {
            string = file.getName();
        }
        return string.toLowerCase(Locale.ROOT);
    }

    protected Cache getCache() {
        return Cache.getCache(this.getName(), CacheType.Monthly);
    }

    protected Object postJson(URL uRL, Object object, Map<String, String> map) throws Exception {
        ByteBuffer byteBuffer = WebRequest.post(uRL, JsonUtilities.json(object).getBytes(StandardCharsets.UTF_8), "application/json", map);
        Logging.debug.finest(WebRequest.log(byteBuffer));
        return JsonUtilities.readJson(StandardCharsets.UTF_8.decode(byteBuffer));
    }

    protected Object requestJson(String object, String ... stringArray) throws Exception {
        if (stringArray.length > 0) {
            object = (String)object + "?" + WebRequest.encodeParameters(new TreeMap<String, String>(WebRequest.mapParameters(stringArray)));
        }
        AuthorizationToken authorizationToken = this.getAuthorizationToken();
        return this.getCache().json(object, authorizationToken::getURL).fetch(CachedResource.withPermit(CachedResource.fetchIfModified(() -> this.getRequestHeader(authorizationToken)), uRL -> REQUEST_LIMIT.acquirePermit())).expire(Cache.ONE_WEEK).get();
    }

    protected Map<String, String> getRequestHeader(AuthorizationToken authorizationToken) throws Exception {
        return WebRequest.mapParameters("Authorization", authorizationToken.toString(), "Api-Key", this.apikey, "User-Agent", this.useragent, "Accept", "application/json");
    }

    protected synchronized AuthorizationToken getAuthorizationToken() throws Exception {
        return (AuthorizationToken)this.getCache().computeIf(this.username, Cache.isStale(Cache.ONE_MONTH), element -> {
            Object object = this.postJson(OpenSubtitlesRestClient.getEndpoint(DEFAULT_HOST, "login"), WebRequest.mapParameters("username", this.username, "password", this.password), WebRequest.mapParameters("Accept", "application/json", "Api-Key", this.apikey, "User-Agent", this.useragent));
            Integer n = JsonUtilities.getInteger(object, "status");
            if (n == null || n != 200) {
                throw new InvalidResponseException(n + " Login Failed", object);
            }
            return new AuthorizationToken(JsonUtilities.getString(object, "token"), JsonUtilities.getString(object, "base_url"));
        });
    }

    public synchronized void logout() {
        if (this.getCache().contains(this.username)) {
            try {
                AuthorizationToken authorizationToken = this.getAuthorizationToken();
                WebRequest.status("DELETE", authorizationToken.getURL("logout"), this.getRequestHeader(authorizationToken));
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(this.getName(), "logout", exception));
            }
        }
        this.getCache().clear();
    }

    public Map<?, ?> getServerInfo() throws Exception {
        this.getCache().clear();
        Object object = this.requestJson("infos/user", new String[0]);
        return JsonUtilities.getMap(object, "data");
    }

    @Override
    public List<SubtitleSearchResult> getIndex() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SubtitleSearchResult> search(String string) throws Exception {
        Object object = this.requestJson("features", "query", string);
        return this.parseFeatureList(object).collect(Collectors.toList());
    }

    @Override
    public List<Movie> searchMovie(String string, Locale locale) throws Exception {
        Object object = this.requestJson("features", "query", string);
        return this.parseFeatureList(object).filter(SubtitleSearchResult::isMovie).collect(Collectors.toList());
    }

    @Override
    public List<Movie> lookupMovie(File file, Locale locale) throws Exception {
        if (file.length() < 65536L) {
            return null;
        }
        String string = OpenSubtitlesHasher.computeHash(file);
        String string2 = this.originalName(file);
        Object object = this.requestJson("subtitles", "languages", this.getLanguageCode(locale), "moviehash", string, "moviehash_match", "only", "query", string2, "type", "movie");
        return this.parseFeatureDetails(object).filter(SubtitleSearchResult::isMovie).collect(Collectors.toList());
    }

    @Override
    public List<SubtitleSearchResult> guess(String string) throws Exception {
        Object object = this.requestJson("subtitles", "query", string);
        return this.parseFeatureDetails(object).collect(Collectors.toList());
    }

    @Override
    public List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult subtitleSearchResult, int[][] nArray2, Locale locale) throws Exception {
        if (nArray2 == null || nArray2.length == 0) {
            return this.getSubtitleList(subtitleSearchResult, null, null, locale);
        }
        int[] nArray3 = Stream.of(nArray2).mapToInt(nArray -> nArray[0]).filter(n -> n >= 0).sorted().distinct().toArray();
        int[] nArray4 = Stream.of(nArray2).mapToInt(nArray -> nArray[1]).filter(n -> n >= 0).sorted().distinct().toArray();
        if (nArray3.length == 0 && nArray4.length == 0) {
            return this.getSubtitleList(subtitleSearchResult, null, null, locale);
        }
        if (nArray3.length == 1 && nArray4.length == 1) {
            return this.getSubtitleList(subtitleSearchResult, nArray3[0], nArray4[0], locale);
        }
        ArrayList<SubtitleDescriptor> arrayList = new ArrayList<SubtitleDescriptor>();
        for (int[] nArray5 : nArray2) {
            arrayList.addAll(this.getSubtitleList(subtitleSearchResult, nArray5[0], nArray5[1], locale));
        }
        return arrayList;
    }

    protected List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult subtitleSearchResult, Integer n, Integer n2, Locale locale) throws Exception {
        if (n == null || n2 == null) {
            Object object = this.requestJson("subtitles", "languages", this.getLanguageCode(locale), subtitleSearchResult.getKind() == SubtitleSearchResult.Kind.Series ? "parent_feature_id" : "id", Integer.toString(subtitleSearchResult.getFeatureId()));
            return this.parseSubtitleList(object).collect(Collectors.toList());
        }
        Object object = this.requestJson("subtitles", "languages", this.getLanguageCode(locale), subtitleSearchResult.getKind() == SubtitleSearchResult.Kind.Series ? "parent_feature_id" : "id", Integer.toString(subtitleSearchResult.getFeatureId()), "season_number", n.toString(), "episode_number", n2.toString());
        return this.parseSubtitleList(object).collect(Collectors.toList());
    }

    @Override
    public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] fileArray, Locale locale) throws Exception {
        LinkedHashMap<File, List<SubtitleDescriptor>> linkedHashMap = new LinkedHashMap<File, List<SubtitleDescriptor>>(fileArray.length);
        for (File file : fileArray) {
            List<SubtitleDescriptor> list = this.getSubtitleList(file, locale);
            if (list == null || list.isEmpty()) continue;
            linkedHashMap.put(file, list);
        }
        return linkedHashMap;
    }

    public List<SubtitleDescriptor> getSubtitleList(File file, Locale locale) throws Exception {
        if (file.length() < 65536L) {
            return null;
        }
        String string = OpenSubtitlesHasher.computeHash(file);
        String string2 = this.originalName(file);
        Object object = this.requestJson("subtitles", "languages", this.getLanguageCode(locale), "moviehash", string, "query", string2);
        return this.parseSubtitleList(object).collect(Collectors.toList());
    }

    protected Stream<SubtitleDescriptor> parseSubtitleList(Object object) throws Exception {
        return JsonUtilities.streamJsonObjects(object, "data").flatMap(map -> {
            Map<Object, Object> map2 = JsonUtilities.getMap(map, "attributes");
            EnumMap enumMap = JsonUtilities.getEnumMap(map2, OpenSubtitlesRestSubtitleDescriptor.SubtitleProperty.class, Function.identity());
            EnumMap enumMap2 = JsonUtilities.getEnumMap(JsonUtilities.getMap(map2, "feature_details"), OpenSubtitlesRestSubtitleDescriptor.FeatureProperty.class, Function.identity());
            return JsonUtilities.streamJsonObjects(map2, "files").map(map3 -> {
                Integer n = JsonUtilities.getInteger(map3, "file_id");
                Integer n2 = JsonUtilities.getInteger(map3, "cd_number");
                String string = JsonUtilities.getString(map3, "file_name");
                return new OpenSubtitlesRestSubtitleDescriptor(enumMap, enumMap2, n, n2, string, this);
            });
        });
    }

    protected Stream<SubtitleSearchResult> parseFeatureDetails(Object object) throws Exception {
        return JsonUtilities.streamJsonObjects(object, "data").map(map -> {
            Map<Object, Object> map2 = JsonUtilities.getMap(JsonUtilities.getMap(map, "attributes"), "feature_details");
            SubtitleSearchResult.Kind kind = JsonUtilities.getStringValue(map2, "feature_type", SubtitleSearchResult.Kind::forName);
            if (kind == null) {
                return null;
            }
            Integer n = JsonUtilities.getInteger(map2, kind == SubtitleSearchResult.Kind.Episode ? "parent_feature_id" : "feature_id");
            String string = JsonUtilities.getString(map2, kind == SubtitleSearchResult.Kind.Episode ? "parent_title" : "title");
            Integer n2 = JsonUtilities.getInteger(map2, "year");
            Integer n3 = JsonUtilities.getInteger(map2, kind == SubtitleSearchResult.Kind.Episode ? "parent_imdb_id" : "imdb_id");
            Integer n4 = JsonUtilities.getInteger(map2, kind == SubtitleSearchResult.Kind.Episode ? "parent_tmdb_id" : "tmdb_id");
            if (n == null || string == null || n2 == null || n3 == null || n4 == null) {
                return null;
            }
            return new SubtitleSearchResult(n, string, null, n2, n3, n4, Locale.ENGLISH, kind == SubtitleSearchResult.Kind.Episode ? SubtitleSearchResult.Kind.Series : kind, 0);
        }).filter(Objects::nonNull).distinct();
    }

    protected Stream<SubtitleSearchResult> parseFeatureList(Object object) throws Exception {
        return JsonUtilities.streamJsonObjects(object, "data").map(map -> {
            Map<Object, Object> map2 = JsonUtilities.getMap(map, "attributes");
            Integer n = JsonUtilities.getInteger(map2, "feature_id");
            String string = JsonUtilities.getString(map2, "original_title", "title");
            Integer n2 = JsonUtilities.getInteger(map2, "year");
            Integer n3 = JsonUtilities.getInteger(map2, "imdb_id");
            Integer n4 = JsonUtilities.getInteger(map2, "tmdb_id");
            String[] stringArray = JsonUtilities.getStringArray(map2, "title_aka");
            SubtitleSearchResult.Kind kind = JsonUtilities.getStringValue(map2, "feature_type", SubtitleSearchResult.Kind::forName);
            Integer n5 = JsonUtilities.getInteger(map2, "subtitles_count");
            if (n == null || string == null || n2 == null || n3 == null || n4 == null || kind == null || n5 == null) {
                return null;
            }
            return new SubtitleSearchResult(n, string, stringArray, n2, n3, n4, Locale.ENGLISH, kind, n5);
        }).filter(Objects::nonNull);
    }

    public ByteBuffer download(int n, String string) throws Exception {
        try {
            AuthorizationToken authorizationToken = this.getAuthorizationToken();
            Object object = this.postJson(authorizationToken.getURL("download"), WebRequest.mapParameters("file_id", Integer.toString(n), "sub_format", string), this.getRequestHeader(authorizationToken));
            return WebRequest.fetch(WebRequest.newURL(JsonUtilities.getString(object, "link")));
        }
        catch (HttpClientError httpClientError) {
            if (httpClientError.isErrorResponse()) {
                String string2 = httpClientError.getResponseContent();
                Logging.debug.finest(string2);
                try {
                    Object object = JsonUtilities.readJson(string2);
                    Integer n2 = JsonUtilities.getInteger(object, "requests");
                    Instant instant = JsonUtilities.getStringValue(object, "reset_time_utc", Instant::parse);
                    if (n2 != null && instant != null) {
                        string2 = String.format(Locale.ROOT, "Quota (%s) has been exceeded and will reset at %tc.", n2, instant.toEpochMilli());
                    }
                }
                catch (Throwable throwable) {
                    Logging.debug.finest(Logging.cause(throwable));
                }
                throw new IllegalStateException(this.getName() + ": " + string2);
            }
            throw httpClientError;
        }
    }

    @Override
    public SubtitleLookupService.CheckResult checkSubtitle(File file, File file2) throws Exception {
        throw new UnsupportedOperationException("OpenSubtitles REST API does not support subtitle upload");
    }

    @Override
    public void uploadSubtitle(Object object, Locale locale, File[] fileArray, File[] fileArray2) throws Exception {
        throw new UnsupportedOperationException("OpenSubtitles REST API does not support subtitle upload");
    }

    @Override
    public Movie getMovieDescriptor(Movie movie, Locale locale) throws Exception {
        if (movie.getTmdbId() > 0) {
            Object object = this.requestJson("features", "tmdb_id", Integer.toString(movie.getTmdbId()), "type", "movie");
            return this.parseFeatureList(object).filter(SubtitleSearchResult::isMovie).findFirst().orElse(null);
        }
        if (movie.getImdbId() > 0) {
            Object object = this.requestJson("features", "imdb_id", Integer.toString(movie.getImdbId()), "type", "movie");
            return this.parseFeatureList(object).filter(SubtitleSearchResult::isMovie).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public URI getSubtitleListLink(SubtitleSearchResult subtitleSearchResult, Locale locale) {
        return URI.create("https://www.opensubtitles.com/en/features/redirect/" + subtitleSearchResult.getFeatureId());
    }

    protected static class AuthorizationToken
    implements Serializable {
        public String token;
        public String host;

        public AuthorizationToken(String string, String string2) {
            this.token = string;
            this.host = string2;
        }

        public String getToken() {
            return this.token;
        }

        public String getHost() {
            return this.host;
        }

        public URL getURL(String string) throws Exception {
            return OpenSubtitlesRestClient.getEndpoint(this.host, string);
        }

        public String toString() {
            return "Bearer " + this.token;
        }
    }
}

