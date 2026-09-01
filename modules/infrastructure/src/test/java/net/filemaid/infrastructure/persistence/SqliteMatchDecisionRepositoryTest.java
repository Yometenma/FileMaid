package net.filemaid.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMatchDecisionRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void savesAndFindsDecision() {
        var repository = new SqliteMatchDecisionRepository(tempDir.resolve("test.db").toString());
        repository.save(new MetadataSelection("show/Example.S01E01.mkv", "tmdb", "123", MetadataType.SERIES, "Example", 2024));

        var found = repository.findBySource("show/Example.S01E01.mkv").orElseThrow();
        assertEquals("tmdb", found.provider());
        assertEquals("123", found.id());
        assertEquals("Example", found.title());
        assertEquals(2024, found.year());
    }

    @Test
    void upsertsOnSameSource() {
        var repository = new SqliteMatchDecisionRepository(tempDir.resolve("test.db").toString());
        repository.save(new MetadataSelection("a.mkv", "tmdb", "1", MetadataType.SERIES, "First", 2020));
        repository.save(new MetadataSelection("a.mkv", "tvdb", "2", MetadataType.SERIES, "Second", 2021));

        var found = repository.findBySource("a.mkv").orElseThrow();
        assertEquals("tvdb", found.provider());
        assertEquals("Second", found.title());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void returnsEmptyForUnknownSource() {
        var repository = new SqliteMatchDecisionRepository(tempDir.resolve("test.db").toString());
        assertTrue(repository.findBySource("missing.mkv").isEmpty());
    }
}
