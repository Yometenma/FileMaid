package net.filemaid.infrastructure.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import net.filemaid.core.model.MediaKind;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalMediaScannerTest {
    private final Path directory = Path.of("build/test-media-scanner").toAbsolutePath();

    @BeforeEach
    void createDirectory() throws Exception {
        Files.createDirectories(directory);
    }

    @AfterEach
    void removeDirectory() throws Exception {
        Files.deleteIfExists(directory.resolve("Show.S01E01.mkv"));
        Files.deleteIfExists(directory.resolve("Show.S01E01.zh-CN.srt"));
        Files.deleteIfExists(directory.resolve("notes.txt"));
        Files.deleteIfExists(directory);
    }

    @Test
    void classifiesMediaWithoutChangingFiles() throws Exception {
        Files.writeString(directory.resolve("Show.S01E01.mkv"), "video");
        Files.writeString(directory.resolve("Show.S01E01.zh-CN.srt"), "subtitle");
        Files.writeString(directory.resolve("notes.txt"), "notes");

        var files = new LocalMediaScanner().scan(new StorageRoot("media", directory, false), directory, 4, 100);

        assertEquals(3, files.size());
        assertEquals(1, files.stream().filter(file -> file.kind() == MediaKind.VIDEO).count());
        assertEquals(1, files.stream().filter(file -> file.kind() == MediaKind.SUBTITLE).count());
        assertEquals(1, files.stream().filter(file -> file.kind() == MediaKind.OTHER).count());
    }
}
