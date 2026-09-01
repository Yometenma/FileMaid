package net.filemaid.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.filemaid.core.model.Task;

/** Stores background task state. Implementations may be in-memory or durable. */
public interface TaskRepository {
    void save(Task task);

    Optional<Task> findById(String id);

    List<Task> findAll();

    int removeOlderThan(Instant cutoff);
}
