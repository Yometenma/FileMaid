package net.filemaid.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.StandardOpenOption;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ReadOnlyFile;

public class FileSet
extends AbstractSet<File> {
    private final Set<Object> keys = new HashSet<Object>(64, 4.0f);

    @Override
    public boolean add(File file) {
        Object object = this.getKey(file);
        if (object != null) {
            return this.keys.add(object);
        }
        return false;
    }

    @Override
    public boolean contains(Object object) {
        Object object2 = this.getKey(this.getFile(object));
        if (object2 != null) {
            return this.keys.contains(object2);
        }
        return false;
    }

    private Object getKey(File file) {
        if (ReadOnlyFile.HEURISTICS) {
            return file;
        }
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            file = file.getCanonicalFile();
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(exception));
        }
        try {
            Object object = FileUtilities.getFileKey(file);
            if (object != null) {
                return object;
            }
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(exception));
        }
        return file;
    }

    private File getFile(Object object) {
        if (object instanceof File) {
            return (File)object;
        }
        if (object == null) {
            return null;
        }
        return new File(object.toString());
    }

    @Override
    public int size() {
        return this.keys.size();
    }

    @Override
    public void clear() {
        this.keys.clear();
    }

    @Override
    public Iterator<File> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return this.keys.toString();
    }

    public void load(File file) throws IOException {
        FileUtilities.readLines(file).forEach(string -> this.add(new File((String)string)));
    }

    public void append(File file2, Collection<?> ... collectionArray) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (Closeable closeable = new PrintStream((OutputStream)byteBufferOutputStream, false, "UTF-8");){
            Stream.of(collectionArray).flatMap(Collection::stream).map(this::getFile).filter(file -> file != null && !this.contains(file)).forEach(((PrintStream)closeable)::println);
        }
        Closeable closeable2 = FileChannel.open(file2.toPath(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        try {
            ((FileChannel)closeable2).write(byteBufferOutputStream.getByteBuffer());
        }
        finally {
            if (closeable2 != null) {
                ((AbstractInterruptibleChannel)closeable2).close();
            }
        }
    }
}

