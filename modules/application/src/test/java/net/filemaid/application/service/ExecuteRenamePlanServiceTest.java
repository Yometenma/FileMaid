package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecuteRenamePlanServiceTest {
    @TempDir Path tempDir;

    private ExecuteRenamePlanService service(boolean writable) {
        return new ExecuteRenamePlanService(
                List.of(new StorageRoot("media", tempDir, writable)),
                new StoragePathPolicy());
    }

    @Test
    void movesFile() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var results = service(true).execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE)));
        assertTrue(results.get(0).success());
        assertTrue(Files.exists(tempDir.resolve("b.mkv")));
        assertFalse(Files.exists(tempDir.resolve("a.mkv")));
    }

    @Test
    void copiesFile() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var results = service(true).execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.COPY)));
        assertTrue(results.get(0).success());
        assertTrue(Files.exists(tempDir.resolve("a.mkv")));
        assertTrue(Files.exists(tempDir.resolve("b.mkv")));
    }

    @Test
    void createsHardlink() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        var results = service(true).execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.HARDLINK)));
        assertTrue(results.get(0).success());
        assertEquals("x", Files.readString(tempDir.resolve("b.mkv")));
    }

    @Test
    void refusesReadOnlyRoot() {
        assertThrows(IllegalStateException.class, () -> service(false).execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE))));
    }

    @Test
    void doesNotOverwriteExistingTarget() throws Exception {
        Files.writeString(tempDir.resolve("a.mkv"), "x");
        Files.writeString(tempDir.resolve("b.mkv"), "y");
        var results = service(true).execute("media", List.of(
                new RenameOperation(Path.of("a.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE)));
        assertFalse(results.get(0).success());
        assertEquals("x", Files.readString(tempDir.resolve("a.mkv")));
        assertEquals("y", Files.readString(tempDir.resolve("b.mkv")));
    }

    @Test
    void reportsMissingSource() {
        var results = service(true).execute("media", List.of(
                new RenameOperation(Path.of("missing.mkv"), Path.of("b.mkv"), RenameOperation.OperationType.MOVE)));
        assertFalse(results.get(0).success());
        assertTrue(results.get(0).error().contains("源文件不存在"));
    }
}
