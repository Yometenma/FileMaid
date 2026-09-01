package net.filemaid.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.MediaKind;
import net.filemaid.core.model.StorageRoot;

public final class LocalMediaScanner implements MediaScanner {
    private static final Set<String> VIDEO = Set.of("mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "m2ts");
    private static final Set<String> SUBTITLE = Set.of("srt", "ass", "ssa", "sub", "vtt");
    private static final Set<String> AUDIO = Set.of("flac", "mp3", "m4a", "aac", "ogg", "opus", "wav");
    private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "webp", "avif");

    @Override
    public List<MediaFile> scan(StorageRoot root, Path start, int maxDepth, int maxFiles) throws IOException {
        if (!Files.isDirectory(start)) throw new IllegalArgumentException("Scan path is not a directory: " + root.path().relativize(start));
        int safeDepth = Math.max(0, Math.min(maxDepth, 64));
        int safeLimit = Math.max(1, Math.min(maxFiles, 100_000));
        try (Stream<Path> paths = Files.walk(start, safeDepth)) {
            return paths.filter(Files::isRegularFile).limit(safeLimit).map(path -> describe(root, path)).toList();
        }
    }

    private MediaFile describe(StorageRoot root, Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return new MediaFile(root.id(), root.path().relativize(path), detectKind(path), attributes.size(), attributes.lastModifiedTime().toInstant());
        } catch (IOException exception) {
            throw new UnreadableMediaFileException(path, exception);
        }
    }

    private MediaKind detectKind(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (VIDEO.contains(extension)) return MediaKind.VIDEO;
        if (SUBTITLE.contains(extension)) return MediaKind.SUBTITLE;
        if (AUDIO.contains(extension)) return MediaKind.AUDIO;
        if (IMAGE.contains(extension)) return MediaKind.IMAGE;
        return MediaKind.OTHER;
    }

    private static final class UnreadableMediaFileException extends RuntimeException {
        private UnreadableMediaFileException(Path path, IOException cause) {
            super("Unable to read media file attributes: " + path, cause);
        }
    }
}
