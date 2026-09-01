package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.MediaPostProcessor;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PostProcessMediaServiceTest {
    @TempDir Path tempDir;

    private static final class RecordingProcessor implements MediaPostProcessor {
        final List<String> downloads = new ArrayList<>();
        @Override public Path writeKodiNfo(Path mediaFile, MetadataSelection metadata) {
            return mediaFile.resolveSibling("episode.nfo");
        }
        @Override public Path downloadArtwork(Path mediaFile, String artworkUrl, String artworkType) {
            downloads.add(artworkType + ":" + artworkUrl);
            String fileName = "FANART".equalsIgnoreCase(artworkType) ? "fanart.jpg" : "poster.jpg";
            return mediaFile.getParent().resolve(fileName);
        }
    }

    private static final class InMemoryHistory implements OperationHistoryRepository {
        private final List<OperationRecord> records = new ArrayList<>();
        private long nextId = 1;
        @Override public List<OperationRecord> append(List<OperationResult> results) {
            for (OperationResult result : results) {
                records.add(new OperationRecord(nextId++, result.source(), result.target(),
                        result.type(), result.success(), result.error(), Instant.now()));
            }
            return List.copyOf(records);
        }
        @Override public List<OperationRecord> findAll() { return List.copyOf(records); }
        @Override public Optional<OperationRecord> findById(long id) {
            return records.stream().filter(r -> r.id() == id).findFirst();
        }
    }

    private static MetadataSelection selection(String source) {
        return new MetadataSelection(source, "tmdb", "99", MetadataType.SERIES, "Example Show", 2024);
    }

    private PostProcessMediaService service(RecordingProcessor processor) {
        return new PostProcessMediaService(
                List.of(new StorageRoot("media", tempDir, true)), new StoragePathPolicy(), processor, new InMemoryHistory());
    }

    @Test
    void bothGeneratesPosterAndFanartFiles() throws Exception {
        Files.writeString(tempDir.resolve("episode.mkv"), "video");
        RecordingProcessor processor = new RecordingProcessor();
        var service = service(processor);
        List<OperationResult> results = service.process("media",
                List.of(new PostProcessMediaService.Item("episode.mkv", selection("episode.mkv"),
                        "https://image.tmdb.org/t/p/w500/poster.jpg", "https://image.tmdb.org/t/p/original/fanart.jpg")),
                false, true, "BOTH");
        List<OperationResult> artwork = results.stream()
                .filter(r -> r.type() == RenameOperation.OperationType.ARTWORK).toList();
        assertEquals(2, artwork.size());
        assertTrue(artwork.stream().allMatch(OperationResult::success));
        assertTrue(artwork.stream().anyMatch(r -> r.target().equals("poster.jpg")));
        assertTrue(artwork.stream().anyMatch(r -> r.target().equals("fanart.jpg")));
        assertEquals(List.of(
                "POSTER:https://image.tmdb.org/t/p/w500/poster.jpg",
                "FANART:https://image.tmdb.org/t/p/original/fanart.jpg"), processor.downloads);
    }

    @Test
    void bothFallsBackToPosterUrlWhenFanartNull() throws Exception {
        Files.writeString(tempDir.resolve("episode.mkv"), "video");
        RecordingProcessor processor = new RecordingProcessor();
        var service = service(processor);
        List<OperationResult> results = service.process("media",
                List.of(new PostProcessMediaService.Item("episode.mkv", selection("episode.mkv"),
                        "https://image.tmdb.org/t/p/w500/poster.jpg", null)),
                false, true, "BOTH");
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(OperationResult::success));
        assertEquals(List.of(
                "POSTER:https://image.tmdb.org/t/p/w500/poster.jpg",
                "FANART:https://image.tmdb.org/t/p/w500/poster.jpg"), processor.downloads);
    }

    @Test
    void fanartUsesFanartUrlWhenAvailable() throws Exception {
        Files.writeString(tempDir.resolve("episode.mkv"), "video");
        RecordingProcessor processor = new RecordingProcessor();
        var service = service(processor);
        service.process("media",
                List.of(new PostProcessMediaService.Item("episode.mkv", selection("episode.mkv"),
                        "https://image.tmdb.org/t/p/w500/poster.jpg", "https://image.tmdb.org/t/p/original/fanart.jpg")),
                false, true, "FANART");
        assertEquals(List.of("FANART:https://image.tmdb.org/t/p/original/fanart.jpg"), processor.downloads);
    }
}
