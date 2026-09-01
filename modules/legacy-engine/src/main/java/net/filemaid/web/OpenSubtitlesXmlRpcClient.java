package net.filemaid.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.util.Digest;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.Timer;
import net.filemaid.web.FloodLimit;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.OpenSubtitlesHasher;
import net.filemaid.web.OpenSubtitlesXmlRpc;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.SubtitleSearchResult;
import redstone.xmlrpc.XmlRpcFault;

public class OpenSubtitlesXmlRpcClient
implements SubtitleProvider,
SubtitleLookupService,
MovieLookupService {
    public final OpenSubtitlesXmlRpc xmlrpc;
    private String username = "";
    private String password = "";
    protected final Timer logoutTimer = new Timer(this::logout);

    public OpenSubtitlesXmlRpcClient(String string) {
        this.xmlrpc = new OpenSubtitlesXmlRpcWithRetryAndFloodLimit(string);
    }

    public String getUserAgent() {
        return this.xmlrpc.getUserAgent();
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
        return URI.create("https://www.opensubtitles.org/");
    }

    public String hash(File file) throws IOException {
        if (file.length() < 65536L) {
            throw new IllegalArgumentException("File size is too small: " + FileUtilities.formatSize(file.length()));
        }
        return OpenSubtitlesHasher.computeHash(file);
    }

    public String originalName(File file) throws IOException {
        String string = XattrMetaInfo.xattr.getOriginalName(file);
        if (string != null && !string.isEmpty()) {
            return string;
        }
        return file.getName();
    }

    public synchronized void login(String string, String string2) {
        this.logout();
        this.username = string;
        this.password = string2;
    }

    @Override
    public synchronized boolean requireLogin() {
        return false;
    }

    @Override
    public List<SubtitleSearchResult> getIndex() throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<SubtitleSearchResult> search(String string) throws Exception {
        throw new UnsupportedOperationException("XMLRPC::SearchMoviesOnIMDB");
    }

    @Override
    public List<Movie> searchMovie(String string, Locale locale) throws Exception {
        throw new UnsupportedOperationException("XMLRPC::SearchMoviesOnIMDB");
    }

    @Override
    public List<Movie> lookupMovie(File file, Locale locale) throws Exception {
        return this.getMovieDescriptors(Collections.singleton(file), locale).values().stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public synchronized List<SubtitleSearchResult> guess(String string) throws Exception {
        return this.getSearchCache("tag").computeIfAbsent(string, element -> {
            this.login();
            return this.xmlrpc.guessMovie(Collections.singleton(string)).getOrDefault(string, Collections.emptyList());
        });
    }

    public synchronized List<SubtitleSearchResult> searchIMDB(String string) throws Exception {
        return this.getSearchCache("query").computeIfAbsent(string, element -> {
            this.login();
            return this.xmlrpc.searchMoviesOnIMDB(string);
        });
    }

    public synchronized List<SubtitleDescriptor> getSubtitleList(OpenSubtitlesXmlRpc.Query query) throws Exception {
        return this.getSubtitlesCache().computeIfAbsent(query, element -> {
            this.login();
            return this.xmlrpc.searchSubtitles(Collections.singleton(query));
        });
    }

    @Override
    public List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult subtitleSearchResult, int[][] nArray2, Locale locale) throws Exception {
        if (nArray2 == null || nArray2.length == 0) {
            return this.getSubtitleList(OpenSubtitlesXmlRpc.Query.forImdbId(subtitleSearchResult.getImdbId(), -1, -1, this.getLanguageFilter(locale)));
        }
        int[] nArray3 = Arrays.stream(nArray2).mapToInt(nArray -> nArray[0]).filter(n -> n >= 0).sorted().distinct().toArray();
        int[] nArray4 = Arrays.stream(nArray2).mapToInt(nArray -> nArray[1]).filter(n -> n >= 0).sorted().distinct().toArray();
        if (nArray3.length == 0 && nArray4.length == 0) {
            return this.getSubtitleList(OpenSubtitlesXmlRpc.Query.forImdbId(subtitleSearchResult.getImdbId(), -1, -1, this.getLanguageFilter(locale)));
        }
        if (nArray3.length == 1 && nArray4.length == 1) {
            return this.getSubtitleList(OpenSubtitlesXmlRpc.Query.forImdbId(subtitleSearchResult.getImdbId(), nArray3[0], nArray4[0], this.getLanguageFilter(locale)));
        }
        ArrayList<SubtitleDescriptor> arrayList = new ArrayList<SubtitleDescriptor>();
        for (int[] nArray5 : nArray2) {
            arrayList.addAll(this.getSubtitleList(OpenSubtitlesXmlRpc.Query.forImdbId(subtitleSearchResult.getImdbId(), nArray5[0], nArray5[1], this.getLanguageFilter(locale))));
        }
        return arrayList;
    }

    @Override
    public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] fileArray, Locale locale) throws Exception {
        LinkedHashMap<File, List<SubtitleDescriptor>> linkedHashMap = new LinkedHashMap<File, List<SubtitleDescriptor>>(fileArray.length);
        for (File file : fileArray) {
            List<SubtitleDescriptor> list = this.getSubtitleListByHash(file, locale);
            if (list == null || list.isEmpty()) {
                list = this.getSubtitleListByTag(file, locale);
            }
            linkedHashMap.put(file, list);
        }
        return linkedHashMap;
    }

    public List<SubtitleDescriptor> getSubtitleListByHash(File file, Locale locale) throws Exception {
        OpenSubtitlesXmlRpc.Query query;
        try {
            query = OpenSubtitlesXmlRpc.Query.forHash(this.hash(file), file.length(), this.getLanguageFilter(locale));
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause("Failed to compute hash", file, exception));
            return Collections.emptyList();
        }
        return this.getSubtitleList(query);
    }

    public List<SubtitleDescriptor> getSubtitleListByTag(File file, Locale locale) throws Exception {
        String string = FileUtilities.getNameWithoutExtension(this.originalName(file));
        if (string == null || string.isEmpty()) {
            return Collections.emptyList();
        }
        OpenSubtitlesXmlRpc.Query query = OpenSubtitlesXmlRpc.Query.forTag(string, this.getLanguageFilter(locale));
        return this.getSubtitleList(query);
    }

    @Override
    public synchronized SubtitleLookupService.CheckResult checkSubtitle(File file, File file2) throws Exception {
        this.login();
        OpenSubtitlesXmlRpc.SubFile subFile = this.getSubFile(file, file2, false);
        OpenSubtitlesXmlRpc.TryUploadResponse tryUploadResponse = this.xmlrpc.tryUploadSubtitles(subFile);
        boolean bl = !tryUploadResponse.isUploadRequired();
        Movie movie = null;
        Locale locale = null;
        if (tryUploadResponse.getSubtitleData().size() > 0) {
            try {
                Map<String, String> map = tryUploadResponse.getSubtitleData().get(0);
                String string = map.get("SubLanguageID");
                locale = Locale.forLanguageTag(string);
                String string2 = map.get("IDMovieImdb");
                String string3 = map.get("MovieName");
                String string4 = map.get("MovieYear");
                movie = Movie.IMDB(string3, Integer.parseInt(string4), Integer.parseInt(string2));
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to upload subtitles", exception));
            }
        }
        return new SubtitleLookupService.CheckResult(bl, movie, locale);
    }

    @Override
    public synchronized void uploadSubtitle(Object object, Locale locale, File[] fileArray, File[] fileArray2) throws Exception {
        int n = -1;
        if (object instanceof Movie) {
            n = ((Movie)object).getImdbId();
        }
        if (n <= 0) {
            throw new IllegalArgumentException("Illegal Movie ID: " + object);
        }
        String string = this.getSubLanguageID(locale);
        OpenSubtitlesXmlRpc.BaseInfo baseInfo = new OpenSubtitlesXmlRpc.BaseInfo();
        baseInfo.setIDMovieImdb(n);
        baseInfo.setSubLanguageID(string);
        OpenSubtitlesXmlRpc.SubFile[] subFileArray = new OpenSubtitlesXmlRpc.SubFile[fileArray.length];
        for (int i = 0; i < subFileArray.length; ++i) {
            subFileArray[i] = this.getSubFile(fileArray[i], fileArray2[i], true);
        }
        this.login();
        this.xmlrpc.uploadSubtitles(baseInfo, subFileArray);
    }

    protected OpenSubtitlesXmlRpc.SubFile getSubFile(File file, File file2, boolean bl) throws IOException {
        OpenSubtitlesXmlRpc.SubFile subFile = new OpenSubtitlesXmlRpc.SubFile();
        subFile.setSubHash(Digest.md5(FileUtilities.readFile(file2)));
        subFile.setSubFileName(file2.getName());
        subFile.setMovieHash(this.hash(file));
        subFile.setMovieByteSize(file.length());
        subFile.setMovieFileName(this.originalName(file));
        if (bl) {
            subFile.setSubContent(FileUtilities.readFile(file2));
        }
        CachedMediaCharacteristics.applyMediaCharacteristics(file, mediaCharacteristics -> {
            Optional.of(mediaCharacteristics).map(MediaCharacteristics::getFrameRate).ifPresent(subFile::setMovieFPS);
            Optional.of(mediaCharacteristics).map(MediaCharacteristics::getDuration).ifPresent(subFile::setMovieTimeMS);
        });
        return subFile;
    }

    @Override
    public synchronized Movie getMovieDescriptor(Movie movie, Locale locale) throws Exception {
        if (movie.getImdbId() <= 0) {
            throw new LookupException((Object)"IMDB ID not found", movie.getImdbId());
        }
        return this.getLookupCache(locale).computeIfAbsent(movie.getImdbId(), element -> {
            this.login();
            return this.xmlrpc.getIMDBMovieDetails(movie.getImdbId());
        });
    }

    public synchronized Map<File, Movie> getMovieDescriptors(Collection<File> collection, Locale locale) throws Exception {
        HashMap<File, Movie> hashMap = new HashMap<File, Movie>();
        int n = 20;
        for (File file : collection) {
            try {
                String string = this.hash(file);
                Movie movie = this.getLookupCache(locale).computeIfAbsent(string, element -> {
                    this.login();
                    return this.xmlrpc.checkMovieHash(Collections.singleton(string), n).get(string);
                });
                hashMap.put(file, movie);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to lookup movie hash", file, exception));
            }
        }
        return hashMap;
    }

    @Override
    public URI getSubtitleListLink(SubtitleSearchResult subtitleSearchResult, Locale locale) {
        return URI.create(String.format("http://www.opensubtitles.org/en/search/imdbid-%s/sublanguageid-%s", subtitleSearchResult.getImdbId(), this.getSubLanguageID(locale)));
    }

    public synchronized Locale detectLanguage(byte[] byArray) throws Exception {
        if (byArray.length < 256) {
            throw new IllegalArgumentException("Data is too small: " + byArray.length);
        }
        List<String> list = this.getCache("detect").castList(String.class).computeIfAbsent(Digest.md5(byArray), element -> {
            this.login();
            return this.xmlrpc.detectLanguage(byArray);
        });
        return list.size() > 0 ? Locale.forLanguageTag(list.get(0)) : Locale.ROOT;
    }

    public synchronized void login() throws Exception {
        if (!this.xmlrpc.isLoggedOn()) {
            this.xmlrpc.login(this.username, this.password, "en");
        }
        this.logoutTimer.set(120L, TimeUnit.MINUTES, false);
    }

    public synchronized void logout() {
        if (this.xmlrpc.isLoggedOn()) {
            try {
                this.logoutTimer.cancel();
                this.xmlrpc.logout();
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(this.getName(), "logout", exception));
            }
        }
    }

    public synchronized Map<?, ?> getServerInfo() throws Exception {
        this.login();
        return this.xmlrpc.getServerInfo();
    }

    public Map<?, ?> getDownloadLimits() throws Exception {
        return (Map)this.getServerInfo().get("download_limits");
    }

    protected synchronized Map<String, String> getSubLanguageMap() throws Exception {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        Cache cache = Cache.getCache(this.getName() + "_languages", CacheType.Persistent);
        Map map = (Map)cache.computeIfAbsent("subLanguageMap", element -> this.xmlrpc.getSubLanguages());
        Map<String, Locale> map2 = MediaDetection.releaseInfo.getLanguageMap(Locale.ENGLISH);
        map.forEach((object, object2) -> {
            String string = object.toString().toLowerCase(Locale.ROOT);
            String string2 = object2.toString().toLowerCase(Locale.ROOT);
            hashMap.put(string2, string);
            hashMap.put(string, string);
            for (String string3 : new String[]{string, string2}) {
                Locale locale = (Locale)map2.get(string3);
                if (locale == null) continue;
                for (String string4 : Arrays.asList(locale.getLanguage(), locale.getISO3Language(), locale.getDisplayLanguage(Locale.ENGLISH))) {
                    if (string4 == null || string4.length() <= 0 || hashMap.containsKey(string4.toLowerCase(Locale.ROOT))) continue;
                    hashMap.put(string4.toLowerCase(Locale.ROOT), string);
                }
            }
        });
        return hashMap;
    }

    protected String getSubLanguageID(Locale locale) {
        Map<String, String> map;
        if (locale == null || locale.equals(Locale.ROOT)) {
            return "all";
        }
        switch (locale.toString()) {
            case "en_US": {
                return "eng";
            }
            case "pt_BR": {
                return "pob";
            }
            case "zh_CN": {
                return "chi";
            }
            case "zh_TW": {
                return "zht";
            }
            case "iw_IL": {
                return "heb";
            }
        }
        try {
            map = this.getSubLanguageMap();
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to retrieve subtitle language map", exception);
        }
        String string = map.get(locale.getLanguage());
        if (string == null) {
            throw new IllegalArgumentException("SubLanguageID not found: " + locale);
        }
        return string;
    }

    protected String[] getLanguageFilter(Locale locale) {
        String[] stringArray;
        if (locale == null || locale.getLanguage().isEmpty()) {
            stringArray = new String[]{};
        } else {
            String[] stringArray2 = new String[1];
            stringArray = stringArray2;
            stringArray2[0] = this.getSubLanguageID(locale);
        }
        return stringArray;
    }

    public Cache getCache(String string) {
        return Cache.getCache(this.getName() + "_" + string, CacheType.Daily);
    }

    protected Cache.TypedCache<List<SubtitleSearchResult>> getSearchCache(String string) {
        return this.getCache("search_" + string).castList(SubtitleSearchResult.class);
    }

    protected Cache.TypedCache<List<SubtitleDescriptor>> getSubtitlesCache() {
        return this.getCache("data").castList(SubtitleDescriptor.class);
    }

    protected Cache.TypedCache<Movie> getLookupCache(Locale locale) {
        return this.getCache("lookup_" + locale).cast(Movie.class);
    }

    protected static class OpenSubtitlesXmlRpcWithRetryAndFloodLimit
    extends OpenSubtitlesXmlRpc {
        public static final int DEFAULT_RETRY_LIMIT = CachedResource.DEFAULT_RETRY_LIMIT;
        public static final Duration DEFAULT_RETRY_DELAY = CachedResource.DEFAULT_RETRY_DELAY.plus(10L, ChronoUnit.SECONDS);
        public static final int DEFAULT_RETRY_MULTIPLIER = CachedResource.DEFAULT_RETRY_MULTIPLIER;
        private static final FloodLimit REQUEST_LIMIT = new FloodLimit(35, 10L, TimeUnit.SECONDS);

        public OpenSubtitlesXmlRpcWithRetryAndFloodLimit(String string) {
            super(string);
        }

        @Override
        protected Map<?, ?> invoke(String string, Object ... objectArray) throws Exception {
            return this.retry(() -> {
                REQUEST_LIMIT.acquirePermit();
                Logging.debug.finest(Logging.message("OpenSubtitles rate limit", REQUEST_LIMIT));
                OpenSubtitlesXmlRpcWithRetryAndFloodLimit openSubtitlesXmlRpcWithRetryAndFloodLimit = this;
                synchronized (openSubtitlesXmlRpcWithRetryAndFloodLimit) {
                    return super.invoke(string, objectArray);
                }
            }, DEFAULT_RETRY_LIMIT, DEFAULT_RETRY_DELAY);
        }

        protected <T> T retry(Callable<T> callable, int n, Duration duration) throws Exception {
            try {
                return callable.call();
            }
            catch (FileNotFoundException | InterruptedException | CancellationException | XmlRpcFault throwable) {
                throw throwable;
            }
            catch (Exception exception) {
                if (n <= 0) {
                    throw exception;
                }
                Logging.debug.warning(Logging.format("Request failed: Try again in %s seconds (%s more) => %s", duration.getSeconds(), n, exception));
                Thread.sleep(duration.toMillis());
                return this.retry(callable, n - 1, duration.multipliedBy(DEFAULT_RETRY_MULTIPLIER));
            }
        }
    }
}

