package net.filemaid.util.prefs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.filemaid.util.ByteBufferOutputStream;

public class PropertyFileBackingStore {
    private final File backingStore;
    private long lastModified = 0L;
    private int modCount = 0;
    private final Map<String, Map<String, String>> nodes = new HashMap<String, Map<String, String>>();
    private static final char nodeSeparatorChar = '/';
    private static final char nodeSeparatorEscapeChar = '\\';
    private static final Pattern nodeSeparatorPattern = Pattern.compile(String.valueOf('/'), 16);

    public PropertyFileBackingStore(File file) {
        this.backingStore = file;
    }

    public synchronized Collection<String> getKeys(String string) {
        Map<String, String> map = this.nodes.get(string);
        if (map != null) {
            return map.keySet();
        }
        return Collections.emptySet();
    }

    public synchronized String getValue(String string, String string2) {
        Map<String, String> map = this.nodes.get(string);
        if (map != null) {
            return map.get(string2);
        }
        return null;
    }

    public synchronized String setValue(String string2, String string3, String string4) {
        ++this.modCount;
        return this.nodes.computeIfAbsent(string2, string -> new HashMap<String, String>()).put(string3, string4);
    }

    public synchronized void removeValue(String string, String string2) {
        Map<String, String> map = this.nodes.get(string);
        if (map != null) {
            ++this.modCount;
            map.remove(string2);
        }
    }

    public synchronized void removeNode(String string) {
        ++this.modCount;
        this.nodes.remove(string);
    }

    public synchronized Collection<String> getChildren(String string) {
        HashSet<String> hashSet = new HashSet<String>();
        String[] stringArray = PropertyFileBackingStore.nodePath(string);
        for (String string2 : this.nodes.keySet()) {
            String[] stringArray2 = PropertyFileBackingStore.nodePath(string2);
            if (stringArray2.length <= stringArray.length || stringArray.length != 0 && !IntStream.range(0, stringArray.length).allMatch(n -> stringArray2[n].equals(stringArray[n]))) continue;
            hashSet.add(stringArray2[stringArray.length]);
        }
        return hashSet;
    }

    public synchronized void sync() throws IOException {
        if (this.backingStore.lastModified() <= this.lastModified) {
            return;
        }
        Properties properties = new Properties();
        try (FileChannel fileChannel = FileChannel.open(this.backingStore.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock fileLock = fileChannel.lock();){
            properties.load(Channels.newReader((ReadableByteChannel)fileChannel, "UTF-8"));
        }
        this.lastModified = this.backingStore.lastModified();
        PropertyFileBackingStore.toNodes(properties).forEach((string, map3) -> this.nodes.merge((String)string, (Map<String, String>)map3, (map, map2) -> {
            map.putAll(map2);
            return map;
        }));
    }

    public synchronized void flush() throws IOException {
        if (this.modCount == 0) {
            return;
        }
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (Closeable writer = new OutputStreamWriter((OutputStream)byteBufferOutputStream, StandardCharsets.UTF_8);){
            PropertyFileBackingStore.toProperties(this.nodes).store((Writer)writer, null);
        }
        FileChannel fileChannel = FileChannel.open(this.backingStore.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        try (FileLock fileLock = fileChannel.lock();){
            fileChannel.write(byteBufferOutputStream.getByteBuffer());
            fileChannel.truncate(fileChannel.position());
        }
        finally {
            fileChannel.close();
        }
        this.lastModified = this.backingStore.lastModified();
        this.modCount = 0;
    }

    public String toString() {
        return this.backingStore.getPath();
    }

    private static String escapeKey(String string) {
        return string.replace('/', '\\');
    }

    private static String unescapeKey(String string) {
        return string.replace('\\', '/');
    }

    private static String[] nodePath(String string) {
        return string.isEmpty() ? new String[]{} : nodeSeparatorPattern.split(string);
    }

    private static Properties toProperties(Map<String, Map<String, String>> map2) {
        Properties properties = new Properties();
        map2.forEach((string, map) -> map.forEach((string2, string3) -> properties.put(string + "/" + PropertyFileBackingStore.escapeKey(string2), string3)));
        return properties;
    }

    private static Map<String, Map<String, String>> toNodes(Properties properties) {
        HashMap<String, Map<String, String>> hashMap = new HashMap<String, Map<String, String>>();
        properties.forEach((BiConsumer<? super Object, ? super Object>)((BiConsumer<Object, Object>)(object, object2) -> {
            String string2 = object.toString();
            int n = string2.lastIndexOf(47) + 1;
            String string3 = string2.substring(0, n - 1);
            String string4 = PropertyFileBackingStore.unescapeKey(string2.substring(n));
            hashMap.computeIfAbsent(string3, string -> new HashMap()).put(string4, object2.toString());
        }));
        return hashMap;
    }
}

