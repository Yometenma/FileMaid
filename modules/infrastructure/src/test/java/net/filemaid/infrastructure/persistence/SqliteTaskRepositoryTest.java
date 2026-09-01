package net.filemaid.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import net.filemaid.core.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteTaskRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void persistsCompletedTaskAcrossRepositoryInstances() {
        String db = tempDir.resolve("tasks.db").toString();
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        var first = new SqliteTaskRepository(db);
        first.save(new Task("task-1", "SCAN", Task.Status.COMPLETED, 100, "完成",
                java.util.Map.of("count", 2), null, now, now));

        Task restored = new SqliteTaskRepository(db).findById("task-1").orElseThrow();
        assertEquals(Task.Status.COMPLETED, restored.status());
        assertEquals(100, restored.progress());
        assertEquals(2, ((Number) ((java.util.Map<?, ?>) restored.result()).get("count")).intValue());
    }

    @Test
    void marksInterruptedTaskFailedOnRestart() {
        String db = tempDir.resolve("restart.db").toString();
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        var first = new SqliteTaskRepository(db);
        first.save(new Task("task-2", "EXECUTE", Task.Status.RUNNING, 50, "运行中", null, null, now, now));

        Task restored = new SqliteTaskRepository(db).findById("task-2").orElseThrow();
        assertEquals(Task.Status.FAILED, restored.status());
        assertEquals("服务重启，任务未能继续执行", restored.error());
    }
}
