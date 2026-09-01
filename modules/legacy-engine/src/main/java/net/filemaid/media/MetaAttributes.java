package net.filemaid.media;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.MetaAttributeView;
import net.filemaid.vfs.SimpleFileInfo;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Episode;
import net.filemaid.web.MappedEpisode;
import net.filemaid.web.Movie;
import net.filemaid.web.MoviePart;
import net.filemaid.web.MultiEpisode;

public class MetaAttributes {
    public static final String FILENAME_KEY = "net.filemaid.filename";
    public static final String METADATA_KEY = "net.filemaid.metadata";
    public static final Map<String, String> JSON_TYPE_MAP = Collections.unmodifiableMap(Stream.of(Episode.class, MultiEpisode.class, MappedEpisode.class, Movie.class, MoviePart.class, AudioTrack.class, SimpleFileInfo.class, File.class).collect(Collectors.toMap(Class::getName, Class::getSimpleName)));
    private BasicFileAttributeView fileAttributeView;
    private MetaAttributeView metaAttributeView;

    public MetaAttributes(File file) throws IOException {
        this.metaAttributeView = new MetaAttributeView(file);
        this.fileAttributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class, new LinkOption[0]);
    }

    public MetaAttributes(MetaAttributeView metaAttributeView, BasicFileAttributeView basicFileAttributeView) {
        this.metaAttributeView = metaAttributeView;
        this.fileAttributeView = basicFileAttributeView;
    }

    public void setCreationDate(long l) throws IOException {
        FileTime fileTime = FileTime.fromMillis(l);
        Logging.debug.finest(Logging.format("Write [basic:creationTime] %s", fileTime));
        this.fileAttributeView.setTimes(null, null, fileTime);
    }

    public long getCreationDate() {
        try {
            return this.fileAttributeView.readAttributes().creationTime().toMillis();
        }
        catch (Exception exception) {
            return 0L;
        }
    }

    public void setOriginalName(String string) {
        Logging.debug.finest(Logging.format("Write [xattr:%s] %s", FILENAME_KEY, string));
        this.metaAttributeView.put(FILENAME_KEY, string);
    }

    public String getOriginalName() {
        return this.metaAttributeView.get(FILENAME_KEY);
    }

    public void setObject(Object object) {
        String string = MetaAttributes.toJson(object, false);
        Logging.debug.finest(Logging.format("Write [xattr:%s] %s", METADATA_KEY, string));
        this.metaAttributeView.put(METADATA_KEY, string);
    }

    public Object getObject() {
        String string = this.metaAttributeView.get(METADATA_KEY);
        if (string != null && !string.isEmpty()) {
            return MetaAttributes.toObject(string);
        }
        return null;
    }

    public void clear() {
        for (String string : this.metaAttributeView.list()) {
            if (!string.equals(FILENAME_KEY) && !string.equals(METADATA_KEY)) continue;
            try {
                Logging.debug.finest(Logging.format("Delete [xattr:%s]", string));
                this.metaAttributeView.delete(string);
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to clear xattr", string, throwable));
            }
        }
    }

    public static String toJson(Object object, boolean bl) {
        if (object == null) {
            return null;
        }
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("TYPE_NAME_MAP", JSON_TYPE_MAP);
        hashMap.put("SKIP_NULL", true);
        hashMap.put("PRETTY_PRINT", bl);
        return JsonWriter.objectToJson((Object)object, hashMap);
    }

    public static Object toObject(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("TYPE_NAME_MAP", JSON_TYPE_MAP);
        return JsonReader.jsonToJava((String)string, hashMap);
    }
}

