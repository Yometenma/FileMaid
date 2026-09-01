package net.filemaid.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMetadataCacheRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void persistsAndPrunesCandidates() {
        var repository = new SqliteMetadataCacheRepository(tempDir.resolve("test.db").toString());
        var createdAt = Instant.parse("2026-09-02T00:00:00Z");
        var candidate = new MetadataCandidate(
                "tmdb", "42", MetadataType.MOVIE, "Example", List.of("示例"), 2026,
                "Overview", "https://image.example/poster.jpg", "https://image.example/fanart.jpg");

        repository.save("tmdb:movie:example", "tmdb", List.of(candidate), createdAt, 3600);

        var entry = repository.find("tmdb:movie:example").orElseThrow();
        assertEquals(createdAt, entry.createdAt());
        assertEquals(3600, entry.ttlSeconds());
        assertEquals(candidate, entry.candidates().get(0));
        assertEquals(1, repository.deleteOlderThan(createdAt.plusSeconds(1)));
        assertTrue(repository.find("tmdb:movie:example").isEmpty());
        assertTrue(repository.tryConsumeDaily("omdb", "hash", LocalDate.of(2026, 9, 2), 1));
        assertTrue(!repository.tryConsumeDaily("omdb", "hash", LocalDate.of(2026, 9, 2), 1));
    }
}
