package net.filemaid.format;

import groovy.json.JsonSlurper;
import groovy.xml.XmlSlurper;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.InvalidInputException;
import net.filemaid.MemoryCache;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ZipUtilities;
import org.jsoup.Jsoup;

public abstract class DataResource {
    private static final MemoryCache<Object, DataResource> cache = MemoryCache.forMinutes();

    public abstract Object getResource();

    public abstract boolean isStale();

    public abstract byte[] bytes() throws Exception;

    public Map<Object, Object> csv() throws Exception {
        LinkedHashMap<Object, Object> linkedHashMap = new LinkedHashMap<Object, Object>();
        List<Pattern> list = Arrays.asList(RegularExpressions.TAB, RegularExpressions.EQUALS, RegularExpressions.SEMICOLON, RegularExpressions.PIPE, RegularExpressions.COLON, RegularExpressions.COMMA);
        block0: for (String string : this.lines()) {
            if (string.startsWith("#")) continue;
            for (Pattern pattern : list) {
                String[] stringArray = pattern.split(string, 2);
                if (stringArray.length < 2) continue;
                linkedHashMap.put(stringArray[0], stringArray[1]);
                list = Collections.singletonList(pattern);
                continue block0;
            }
        }
        return linkedHashMap;
    }

    public String text() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(this.bytes());
        if (ZipUtilities.isZipFile(byteBuffer)) {
            throw new InvalidInputException(this + " is a ZIP archive and not a plain/text file");
        }
        return FileUtilities.decodeTextContent(byteBuffer, true, StandardCharsets.UTF_8);
    }

    public List<String> lines() throws Exception {
        return Arrays.asList(RegularExpressions.NEWLINE.split(this.text()));
    }

    public Object xml() throws Exception {
        return new XmlSlurper().parseText(this.text());
    }

    public Object json() throws Exception {
        return new JsonSlurper().parseText(this.text());
    }

    public Object html() throws Exception {
        return Jsoup.parse((String)this.text());
    }

    public static DataResource local(File file) throws Exception {
        return DataResource.getDataResource(file, file2 -> {
            if (Settings.isMacSandbox()) {
                MacAppUtilities.askUnlockFolders(null, Collections.singleton(file));
            }
            return new Memoized(new Local((File)file2));
        });
    }

    public static DataResource remote(URI uRI2) throws Exception {
        return DataResource.getDataResource(uRI2, uRI -> new Memoized(new Remote((URI)uRI)));
    }

    private static <T> DataResource getDataResource(T t, Function<T, DataResource> function) throws Exception {
        DataResource dataResource = cache.getIfPresent(t);
        if (dataResource == null || dataResource.isStale()) {
            dataResource = function.apply(t);
            cache.put(t, dataResource);
        }
        return dataResource;
    }

    private static class Memoized
    extends DataResource {
        private final DataResource resource;
        private final Resource<Map<Object, Object>> csv;
        private final Resource<List<String>> lines;
        private final Resource<String> text;
        private final Resource<Object> xml;
        private final Resource<Object> json;
        private final Resource<Object> html;

        public Memoized(DataResource dataResource) {
            this.resource = dataResource;
            this.csv = Resource.lazy(() -> new LookupMap(dataResource.csv()));
            this.lines = Resource.lazy(() -> new LookupList(dataResource.lines()));
            this.text = Resource.lazy(dataResource::text);
            this.xml = Resource.lazy(dataResource::xml);
            this.json = Resource.lazy(dataResource::json);
            this.html = Resource.lazy(dataResource::html);
        }

        @Override
        public Object getResource() {
            return this.resource.getResource();
        }

        @Override
        public boolean isStale() {
            return this.resource.isStale();
        }

        @Override
        public byte[] bytes() throws Exception {
            return this.resource.bytes();
        }

        @Override
        public Map<Object, Object> csv() throws Exception {
            return this.csv.get();
        }

        @Override
        public List<String> lines() throws Exception {
            return this.lines.get();
        }

        @Override
        public String text() throws Exception {
            return this.text.get();
        }

        @Override
        public Object xml() throws Exception {
            return this.xml.get();
        }

        @Override
        public Object json() throws Exception {
            return this.json.get();
        }

        @Override
        public Object html() throws Exception {
            return this.html.get();
        }
    }

    private static class Remote
    extends DataResource {
        private final URI url;

        public Remote(URI uRI) {
            this.url = uRI;
        }

        @Override
        public Object getResource() {
            return this.url;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public byte[] bytes() throws Exception {
            return Cache.getConcurrentCache("url", CacheType.Monthly).url(this.url.toURL()).get();
        }
    }

    private static class Local
    extends DataResource {
        private final File file;
        private final boolean directory;
        private final long lastModified;

        public Local(File file) {
            this.file = file;
            this.directory = file.isDirectory();
            this.lastModified = file.lastModified();
        }

        @Override
        public Object getResource() {
            return this.file;
        }

        @Override
        public boolean isStale() {
            return this.lastModified != this.file.lastModified();
        }

        @Override
        public byte[] bytes() throws Exception {
            if (!this.file.isFile()) {
                throw new InvalidInputException("File not found: " + this.file);
            }
            if (this.file.length() > 1000000000L) {
                throw new InvalidInputException("File is too large and probably not a plain/text file: " + this.file);
            }
            return FileUtilities.readFile(this.file);
        }

        @Override
        public Map<Object, Object> csv() throws Exception {
            if (this.directory) {
                return this.getMediaIndex();
            }
            return super.csv();
        }

        @Override
        public List<String> lines() throws Exception {
            if (this.directory) {
                return this.getDirectoryIndex();
            }
            return super.lines();
        }

        public List<String> getDirectoryIndex() {
            return Arrays.asList(this.file.list());
        }

        public Map<Object, Object> getMediaIndex() {
            LinkedHashMap<Object, Object> linkedHashMap = new LinkedHashMap<Object, Object>();
            for (File file : FileUtilities.listFiles(this.file, FileUtilities.NOT_HIDDEN)) {
                Object object = XattrMetaInfo.xattr.getMetaInfo(file);
                if (object == null) continue;
                linkedHashMap.put(file, object);
            }
            return linkedHashMap;
        }
    }

    protected static class LookupList
    extends AbstractList<String>
    implements RandomAccess {
        private final List<String> values;
        private Set<String> lookup;

        public LookupList(List<String> list) {
            this.values = list;
        }

        @Override
        public String get(int n) {
            return this.values.get(n);
        }

        @Override
        public int size() {
            return this.values.size();
        }

        private String definingKey(Object object) {
            return Normalization.normalizePunctuation(object.toString()).toLowerCase(Locale.ROOT);
        }

        private Set<String> getLookup() {
            if (this.lookup == null) {
                this.lookup = this.values.stream().map(this::definingKey).collect(Collectors.toSet());
            }
            return this.lookup;
        }

        public Set<String> keySet() {
            return Collections.unmodifiableSet(this.getLookup());
        }

        @Override
        public boolean contains(Object object) {
            if (object == null) {
                return false;
            }
            return this.getLookup().contains(this.definingKey(object));
        }
    }

    protected static class LookupMap
    extends AbstractMap<Object, Object> {
        private final Map<Object, Object> values;
        private Map<String, Object> lookup;

        public LookupMap(Map<Object, Object> map) {
            this.values = map;
        }

        private String definingKey(Object object) {
            return Normalization.normalizePunctuation(object.toString()).toLowerCase(Locale.ROOT);
        }

        private Map<String, Object> getLookup() {
            if (this.lookup == null) {
                this.lookup = this.values.entrySet().stream().collect(Collectors.toMap(entry -> this.definingKey(entry.getKey()), entry -> entry.getValue(), (object, object2) -> object, LinkedHashMap::new));
            }
            return this.lookup;
        }

        @Override
        public Object get(Object object) {
            return this.getLookup().get(this.definingKey(object));
        }

        @Override
        public boolean containsKey(Object object) {
            return this.getLookup().containsKey(this.definingKey(object));
        }

        @Override
        public Set<Object> keySet() {
            return Collections.unmodifiableSet(this.getLookup().keySet());
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            return Collections.unmodifiableSet(this.values.entrySet());
        }
    }

    public static class Post
    extends DataResource {
        private final URI url;
        private final String postData;
        private final String contentType;
        private final Map<String, String> requestHeader;

        public Post(URI uRI, String string, String string2, Map<String, String> map) {
            this.url = uRI;
            this.postData = string;
            this.contentType = string2;
            this.requestHeader = map;
        }

        @Override
        public Object getResource() {
            return this.url;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public byte[] bytes() throws Exception {
            return Cache.getConcurrentCache("url", CacheType.Monthly).bytes(this.url + " " + this.postData, string -> this.url.toURL()).fetch(CachedResource.post(() -> this.postData.getBytes(StandardCharsets.UTF_8), () -> this.contentType, () -> this.requestHeader)).get();
        }
    }
}

