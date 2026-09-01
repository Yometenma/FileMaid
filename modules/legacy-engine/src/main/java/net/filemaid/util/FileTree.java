package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.util.FileUtilities;

public class FileTree
extends AbstractSet<Path> {
    private static final int ROOT_LEVEL = -1;
    private final Map<Path, FileTree> folders = new HashMap<Path, FileTree>(4, 2.0f);
    private final Set<Path> files = new HashSet<Path>(4, 2.0f);

    private boolean add(Path path2, int n) {
        if (path2.getNameCount() - 1 == n) {
            return this.files.add(path2.getFileName());
        }
        return this.folders.computeIfAbsent(n == -1 ? path2.getRoot() : path2.getName(n), path -> new FileTree()).add(path2, n + 1);
    }

    @Override
    public boolean add(Path path) {
        return path != null && this.add(path, -1);
    }

    public boolean add(File file) {
        return this.add(this.getPath(file));
    }

    public boolean add(String string) {
        return this.add(this.getPath(string));
    }

    private boolean contains(Path path, int n) {
        FileTree fileTree;
        if (path.getNameCount() - 1 == n) {
            return this.files.contains(path.getFileName());
        }
        if (path.getNameCount() - 1 > n && (fileTree = this.folders.get(n == -1 ? path.getRoot() : path.getName(n))) != null) {
            return fileTree.contains(path, n + 1);
        }
        return false;
    }

    public boolean contains(Path path) {
        return path != null && this.contains(path, -1);
    }

    @Override
    public boolean contains(Object object) {
        return this.contains(this.getPath(object));
    }

    private Path getPath(Object object) {
        try {
            if (object instanceof Path) {
                return (Path)object;
            }
            if (object instanceof File) {
                return ((File)object).toPath();
            }
            if (object instanceof String) {
                return Paths.get((String)object, new String[0]);
            }
            if (object instanceof URI) {
                return Paths.get((URI)object);
            }
            return Paths.get(object.toString(), new String[0]);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.message("Invalid file path", object));
            return null;
        }
    }

    public Map<Path, List<Path>> getRoots() {
        if (this.folders.size() != 1 || this.files.size() > 0) {
            return Collections.emptyMap();
        }
        Map.Entry<Path, FileTree> entry2 = this.folders.entrySet().iterator().next();
        Path path = entry2.getKey();
        Map<Path, List<Path>> map = entry2.getValue().getRoots();
        if (map.size() > 0) {
            return map.entrySet().stream().collect(Collectors.toMap(entry -> path.resolve((Path)entry.getKey()), entry -> (List)entry.getValue()));
        }
        return this.folders.entrySet().stream().collect(Collectors.toMap(entry -> (Path)entry.getKey(), entry -> ((FileTree)entry.getValue()).stream().collect(Collectors.toList())));
    }

    @Override
    public int size() {
        return this.folders.values().stream().mapToInt(fileTree -> fileTree.size()).sum() + this.files.size();
    }

    @Override
    public Stream<Path> stream() {
        Stream stream = this.folders.entrySet().stream().flatMap(entry -> ((FileTree)entry.getValue()).stream().map(path -> ((Path)entry.getKey()).resolve((Path)path)));
        return Stream.concat(stream, this.files.stream());
    }

    @Override
    public Spliterator<Path> spliterator() {
        return this.stream().spliterator();
    }

    @Override
    public Iterator<Path> iterator() {
        return this.stream().iterator();
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        this.folders.values().forEach(FileTree::clear);
        this.folders.clear();
        this.files.clear();
    }

    public void load(File file) throws IOException {
        FileUtilities.readLines(file).forEach(this::add);
    }

    public void append(File file, Collection<?> ... collectionArray) throws IOException {
        List list = Stream.of(collectionArray).flatMap(Collection::stream).map(this::getPath).filter(path -> path != null && !this.contains((Path)path)).map(Path::toString).collect(Collectors.toList());
        Files.write(file.toPath(), list, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
}

