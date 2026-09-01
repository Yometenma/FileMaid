package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UndoServiceTest {
    @TempDir Path tempDir;

    private static final class InMemoryHistory implements OperationHistoryRepository {
        private final List<OperationRecord> records = new ArrayList<>();
        private long nextId = 1;

        @Override public List<OperationRecord> append(List<OperationResult> results) {
            List<OperationRecord> appended = new ArrayList<>();
            for (OperationResult result : results) {
                OperationRecord record = new OperationRecord(nextId++, result.source(), result.target(),
                        result.type(), result.success(), result.error(), Instant.now());
                records.add(record);
                appended.add(record);
            }
            return appended;
        }
        @Override public List<OperationRecord> findAll() { return List.copyOf(records); }
        @Override public Optional<OperationRecord> findById(long id) {
            return records.stream().filter(record -> record.id() == id).findFirst();
        }
    }

    private record Fixture(ExecuteRenamePlanService executor, UndoService undo, OperationHistoryRepository history) {}

    private Fixture fixture() {
        var history = new InMemoryHistory();
        var roots = List.of(new StorageRoot("media", tempDir, true));
        var executor = new ExecuteRenamePlanService(roots, new StoragePathPolicy(), history);
        var undo = new UndoService(roots, new StoragePathPolicy(), history);
        return new Fixture(executor, undo, history);
    }

    @Test
    void undoesMoveByMovingBack() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var fixture = fixture();
        var results = fixture.executor().execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE)));
        assertTrue(results.get(0).success());
        long id = fixture.history().findAll().get(0).id();

        var undoResult = fixture.undo().undo("media", id);
        assertTrue(undoResult.success());
        assertTrue(Files.exists(tempDir.resolve("a.mkv")));
        assertFalse(Files.exists(tempDir.resolve("b.mkv")));
    }

    @Test
    void undoesCopyByDeletingTarget() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var fixture = fixture();
        fixture.executor().execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.COPY)));
        long id = fixture.history().findAll().get(0).id();

        var undoResult = fixture.undo().undo("media", id);
        assertTrue(undoResult.success());
        assertTrue(Files.exists(tempDir.resolve("a.mkv")));
        assertFalse(Files.exists(tempDir.resolve("b.mkv")));
    }

    @Test
    void refusesUndoWhenTargetAlreadyGone() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var fixture = fixture();
        fixture.executor().execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE)));
        long id = fixture.history().findAll().get(0).id();
        Files.delete(tempDir.resolve("b.mkv"));

        var undoResult = fixture.undo().undo("media", id);
        assertFalse(undoResult.success());
    }
}
