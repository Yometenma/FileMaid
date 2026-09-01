package net.filemaid.postprocess;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;
import groovy.xml.XmlSlurper;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.Execute;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionFormatFunctions;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.Builder;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.web.Datasource;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.WebRequest;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public abstract class ScriptBaseClass
extends Script {
    public void alert(Object ... objectArray) throws Exception {
        String string = Logging.message(objectArray).toString();
        this.println("ALERT " + string);
        Logging.log.warning(string);
    }

    public void system(String string, Object ... objectArray) throws Exception {
        this.system(Collections.EMPTY_MAP, string, objectArray);
    }

    public void system(Map map, String string, Object ... objectArray) throws Exception {
        Collection<Object> collection = DefaultGroovyMethods.flatten((Object[])objectArray);
        List<String> list = collection.stream().filter(Objects::nonNull).map(Objects::toString).collect(Collectors.toList());
        this.println("EXECUTE " + string + " " + list);
        Execute.system(string, list, null, ScriptBaseClass.toStringMap(map));
    }

    public void trash(Object ... objectArray) throws Exception {
        for (File file : FileUtilities.asFileList(objectArray)) {
            this.println("TRASH " + file);
            UserFiles.trash(file);
        }
    }

    public void move(Object ... objectArray) throws Exception {
        Iterator<File> iterator = FileUtilities.asFileList(objectArray).iterator();
        while (iterator.hasNext()) {
            File file;
            File file2 = iterator.next();
            if (!StandardRenameAction.MOVE.canRename(file2, file = StandardRenameAction.MOVE.resolve(file2, iterator.next()))) continue;
            if (file2.exists() && file.exists()) {
                this.trash(file);
            }
            this.println("MOVE " + file2 + " to " + file);
            StandardRenameAction.MOVE.rename(file2, file);
        }
    }

    public void reveal(Object ... objectArray) throws Exception {
        for (File file : FileUtilities.asFileList(objectArray)) {
            this.println("REVEAL " + file);
            UserInteraction.reveal(file);
        }
    }

    public File curl(Object object, File file) throws Exception {
        if (file.exists()) {
            this.println("SKIP " + file);
            return null;
        }
        this.println("GET " + object + " (" + file + ")");
        ByteBuffer byteBuffer = WebRequest.fetch(ScriptBaseClass.url(object));
        FileUtilities.createFolders(file.getParentFile());
        return FileUtilities.writeFile(byteBuffer, file);
    }

    public Response curl(Object object) throws Exception {
        return this.request(object, Collections.EMPTY_MAP, null);
    }

    public Response curl(Map map, Object object) throws Exception {
        return this.request(object, map, null);
    }

    public Response curl(Object object, Map map) throws Exception {
        return this.request(object, Collections.EMPTY_MAP, Post.of("POST", map, Collections.EMPTY_MAP));
    }

    public Response curl(Object object, String string) throws Exception {
        return this.request(object, Collections.EMPTY_MAP, Post.of("POST", string, Collections.EMPTY_MAP));
    }

    public Response submit(Object object, Map map) throws Exception {
        return this.request(object, Collections.EMPTY_MAP, Post.of("POST", map, Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded")));
    }

    public Response curl(Map map, Object object, Object object2) throws Exception {
        return this.request(object, map, Post.of("POST", object2, map));
    }

    public Response http(String string, Object object, Object object2) throws Exception {
        return this.request(object, Collections.EMPTY_MAP, Post.of(string, object2, Collections.EMPTY_MAP));
    }

    public Response http(Map map, String string, Object object, Object object2) throws Exception {
        return this.request(object, map, Post.of(string, object2, map));
    }

    private Response request(Object object, Map map, Post post) throws Exception {
        URL uRL = ScriptBaseClass.url(object);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        if (post == null) {
            this.println("GET " + uRL);
            ByteBuffer byteBuffer = WebRequest.fetch(uRL, 0L, null, ScriptBaseClass.toStringMap(map), linkedHashMap::put);
            return new Response(byteBuffer, linkedHashMap);
        }
        this.println(post.getRequestMethod() + " " + uRL + " " + post.toString());
        ByteBuffer byteBuffer = WebRequest.post(post.getRequestMethod(), uRL, post.toByteArray(), post.getContentType(), ScriptBaseClass.toStringMap(map), linkedHashMap::put);
        return new Response(byteBuffer, linkedHashMap);
    }

    public List<File> getSubtitles(File file, String ... stringArray) throws Exception {
        return this.getSubtitles(Collections.EMPTY_MAP, file, stringArray);
    }

    public List<File> getSubtitles(Map map, File file, String ... stringArray) throws Exception {
        ArrayList<File> arrayList = new ArrayList<File>(stringArray.length);
        boolean bl = ScriptBaseClass.optional(map, "strict", ScriptBaseClass::flag, true);
        Predicate<SubtitleDescriptor> predicate = ScriptBaseClass.optional(map, "filter", ScriptBaseClass::selector, ScriptBaseClass.selectAll());
        SubtitleFormat subtitleFormat = SubtitleFormat.SubRip;
        if (MediaTypes.VIDEO_FILES.accept(file) && file.length() > 1000000L) {
            for (String string : stringArray) {
                File file2 = new File(file.getParentFile(), SubtitleUtilities.formatSubtitle(FileUtilities.getName(file), string, subtitleFormat.getFilter().extension()));
                if (file2.exists()) continue;
                this.println("FIND " + file2.getName());
                SubtitleDescriptor subtitleDescriptor = this.findSubtitles(file, string, bl, predicate);
                if (subtitleDescriptor == null) continue;
                this.println("FETCH " + subtitleDescriptor);
                MemoryFile memoryFile = SubtitleUtilities.fetchSubtitle(subtitleDescriptor);
                ByteBuffer byteBuffer = SubtitleUtilities.exportSubtitles(memoryFile, subtitleFormat, StandardCharsets.UTF_8);
                arrayList.add(FileUtilities.writeFile(byteBuffer, file2));
            }
        }
        return arrayList;
    }

    public SubtitleDescriptor findSubtitles(File file, String string, boolean bl, Predicate<SubtitleDescriptor> predicate) throws Exception {
        SubtitleDescriptor subtitleDescriptor;
        Locale locale = Language.forName(string).getLocale();
        for (SubtitleLookupService datasource : WebServices.getSubtitleLookupServices(locale)) {
            subtitleDescriptor = SubtitleUtilities.lookupSubtitlesByHash(datasource, Collections.singleton(file), locale, true, bl).values().stream().flatMap(Collection::stream).filter(predicate).findFirst().orElse(null);
            if (subtitleDescriptor == null) continue;
            return subtitleDescriptor;
        }
        if (bl) {
            return null;
        }
        for (SubtitleProvider datasource : WebServices.getSubtitleProviders(locale)) {
            subtitleDescriptor = SubtitleUtilities.findSubtitlesByName(datasource, Collections.singleton(file), locale, null, true, bl).values().stream().flatMap(Collection::stream).filter(predicate).findFirst().orElse(null);
            if (subtitleDescriptor == null) continue;
            return subtitleDescriptor;
        }
        return null;
    }

    private static <T> Predicate<T> selectAll() {
        return object -> true;
    }

    private static <T> Predicate<T> selector(Object object2) {
        if (object2 instanceof Closure) {
            Closure closure = (Closure)object2;
            return object -> DefaultTypeTransformation.castToBoolean((Object)closure.call(object));
        }
        if (object2 instanceof Pattern) {
            Pattern pattern = (Pattern)object2;
            return object -> pattern.matcher(object.toString()).find();
        }
        return null;
    }

    private static boolean flag(Object object) {
        if (object instanceof Boolean) {
            return (Boolean)object;
        }
        if (object instanceof String) {
            return Boolean.parseBoolean((String)object);
        }
        return DefaultTypeTransformation.castToBoolean((Object)object);
    }

    private static URL url(Object object) throws Exception {
        if (object instanceof URL) {
            return (URL)object;
        }
        if (object instanceof URI) {
            return ((URI)object).toURL();
        }
        return WebRequest.newURL(object.toString());
    }

    private static <T> T optional(Map map, String string, Function<Object, T> function, T t) {
        return Optional.ofNullable(map.get(string)).map(function).orElse(t);
    }

    public void NEWLINE(File file, Object object2, Object ... objectArray) throws Exception {
        if (file.length() > 0x1400000L) {
            this.alert(Logging.format("NEWLINE file exists already: %s (%s)", file, FileUtilities.formatSize(file.length())));
            return;
        }
        List list = file.exists() ? Files.readAllLines(file.toPath(), StandardCharsets.UTF_8) : Collections.emptyList();
        List list2 = ExpressionFormatFunctions.list(this, object2, objectArray).stream().filter(object -> !ExpressionFormatFunctions.isEmptyValue(this, object)).map(Object::toString).flatMap(RegularExpressions.NEWLINE::splitAsStream).map(String::trim).distinct().filter(string -> !string.isEmpty() && !list.contains(string)).peek(string -> this.println(Logging.format("NEWLINE %s (%s)", string, file))).collect(Collectors.toList());
        if (list2.size() > 0) {
            Files.write(file.toPath(), list2, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    public File XML(File file, Closure closure) throws Exception {
        return this.write(file, Builder.XML, closure);
    }

    public String XML(Closure closure) {
        return Builder.XML.toString(closure);
    }

    public File INI(File file, Closure closure) throws Exception {
        return this.write(file, Builder.INI, closure);
    }

    public String INI(Closure closure) throws Exception {
        return Builder.INI.toString(closure);
    }

    public File JSON(File file, Closure closure) throws Exception {
        return this.write(file, Builder.JSON, closure);
    }

    public String JSON(Closure closure) {
        return Builder.JSON.toString(closure);
    }

    private File write(File file, Builder builder, Closure closure) throws Exception {
        if (file.length() > 0x1400000L) {
            this.alert(Logging.format("%s file exists already: %s (%s)", new Object[]{builder, file, FileUtilities.formatSize(file.length())}));
            return null;
        }
        byte[] byArray = builder.toString(closure).getBytes(StandardCharsets.UTF_8);
        this.println(Logging.format("%s %s (%s)", new Object[]{builder, file, FileUtilities.formatSize(byArray.length)}));
        FileUtilities.createFolders(file.getParentFile());
        return FileUtilities.writeFile(byArray, file);
    }

    private static Map<String, String> toStringMap(Map map) {
        if (map.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(map.size());
        map.forEach((object, object2) -> {
            if (object != null && object2 != null) {
                linkedHashMap.put(object.toString(), object2.toString());
            }
        });
        return linkedHashMap;
    }

    private static class Post {
        private final String requestMethod;
        private final Object content;
        private final String contentType;

        private Post(String string, Object object, String string2) {
            this.requestMethod = string;
            this.content = object;
            this.contentType = string2;
        }

        public String getRequestMethod() {
            return this.requestMethod;
        }

        public String getContentType() {
            return this.contentType;
        }

        public byte[] toByteArray() {
            if (this.content instanceof byte[]) {
                return (byte[])this.content;
            }
            return this.content.toString().getBytes(StandardCharsets.UTF_8);
        }

        public String toString() {
            if (this.content instanceof byte[]) {
                byte[] byArray = (byte[])this.content;
                return "[" + byArray.length + " bytes]";
            }
            return this.content.toString();
        }

        public static Post of(String string, Object object, Map map) {
            if (object instanceof Map && "application/x-www-form-urlencoded".equals(map.get("Content-Type"))) {
                return new Post(string, WebRequest.encodeParameters((Map)object), "application/x-www-form-urlencoded");
            }
            if (object instanceof CharSequence) {
                return new Post(string, object.toString(), "text/plain");
            }
            if (object instanceof byte[]) {
                return new Post(string, (byte[])object, "application/octet-stream");
            }
            if (object == null) {
                return new Post(string, new byte[0], "application/octet-stream");
            }
            return new Post(string, JsonOutput.toJson((Object)object), "application/json");
        }
    }

    public static class Response
    extends GroovyObjectSupport
    implements Iterable<Object> {
        private final ByteBuffer content;
        private final Map<String, String> header;
        private Object object;

        public Response(ByteBuffer byteBuffer, Map<String, String> map) {
            this.content = byteBuffer;
            this.header = map;
        }

        public ByteBuffer content() {
            return this.content.duplicate();
        }

        public Map<String, String> header() {
            return this.header;
        }

        public String header(String string) {
            return this.header().getOrDefault(string.toLowerCase(Locale.ROOT), "");
        }

        public synchronized Object object() {
            if (this.object == null) {
                this.object = ContentType.forContentType(this.header("content-type")).parse(this.content());
            }
            return this.object;
        }

        public Object invokeMethod(String string, Object object) {
            return InvokerHelper.invokeMethod((Object)this.object(), (String)string, (Object)object);
        }

        public Object getProperty(String string) {
            return InvokerHelper.getProperty((Object)this.object(), (String)string);
        }

        @Override
        public Iterator<Object> iterator() {
            return DefaultGroovyMethods.iterator((Object)this.object());
        }

        public boolean asBoolean() {
            return DefaultTypeTransformation.castToBoolean((Object)this.object());
        }

        public String toString() {
            return StandardCharsets.UTF_8.decode(this.content()).toString();
        }
    }

    public static enum ContentType {
        JSON,
        XML,
        TEXT,
        BYTES;


        public Object parse(ByteBuffer byteBuffer) {
            try {
                switch (this) {
                    case JSON: {
                        return new JsonSlurper().parse((InputStream)new ByteBufferInputStream(byteBuffer));
                    }
                    case XML: {
                        return new XmlSlurper().parse((InputStream)new ByteBufferInputStream(byteBuffer));
                    }
                    case TEXT: {
                        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
                    }
                }
                return byteBuffer;
            }
            catch (Exception exception) {
                throw new IllegalStateException("Invalid " + this, exception);
            }
        }

        public static ContentType forContentType(String string) {
            if (string.contains("json")) {
                return JSON;
            }
            if (string.contains("xml")) {
                return XML;
            }
            if (string.contains("text")) {
                return TEXT;
            }
            return BYTES;
        }
    }
}

