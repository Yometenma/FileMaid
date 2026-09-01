package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidateRenamePlanServiceTest {
    @TempDir Path root;

    private ValidateRenamePlanService service() {
        return new ValidateRenamePlanService(List.of(new StorageRoot("media", root, false)), new StoragePathPolicy());
    }

    @Test
    void rejectsMissingSourceAndConflictingTarget() throws Exception {
        Files.createFile(root.resolve("existing.mkv"));
        Files.createFile(root.resolve("conflict.mkv"));
        var result = service().validate("media", List.of(
                new RenameOperation(Path.of("missing.mkv"), Path.of("new.mkv"), RenameOperation.OperationType.MOVE),
                new RenameOperation(Path.of("existing.mkv"), Path.of("conflict.mkv"), RenameOperation.OperationType.MOVE)));
        assertFalse(result.valid());
        assertEquals(2, result.problems().size());
    }

    @Test
    void acceptsCleanPlan() throws Exception {
        Files.createFile(root.resolve("existing.mkv"));
        var result = service().validate("media", List.of(
                new RenameOperation(Path.of("existing.mkv"), Path.of("renamed.mkv"), RenameOperation.OperationType.MOVE)));
        assertTrue(result.valid());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void rejectsEscapingTargetPath() {
        var result = service().validate("media", List.of(
                new RenameOperation(Path.of("existing.mkv"), Path.of("../outside.mkv"), RenameOperation.OperationType.MOVE)));
        assertFalse(result.valid());
    }
}
