package net.filemaid.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteOperationHistoryRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void appendsAndFinds() {
        var repository = new SqliteOperationHistoryRepository(tempDir.resolve("test.db").toString());
        var records = repository.append(List.of(
                new OperationResult("a.mkv", "b.mkv", RenameOperation.OperationType.MOVE, true, null),
                new OperationResult("c.mkv", "d.mkv", RenameOperation.OperationType.COPY, false, "boom")));

        assertEquals(2, records.size());
        assertTrue(records.get(0).id() > 0);
        assertEquals(2, repository.findAll().size());

        var found = repository.findById(records.get(0).id()).orElseThrow();
        assertEquals("a.mkv", found.source());
        assertEquals("b.mkv", found.target());
        assertTrue(found.success());
    }
}
