package net.filemaid.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.filemaid.application.port.TaskRepository;
import net.filemaid.core.model.Task;

/**
 * In-memory task store. Survives browser disconnects (the server keeps running)
 * but not process restarts; durable task persistence can be added once the task
 * model stabilizes.
 */
public final class InMemoryTaskRepository implements TaskRepository {
    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();

    @Override public void save(Task task) { tasks.put(task.id(), task); }

    @Override public Optional<Task> findById(String id) { return Optional.ofNullable(tasks.get(id)); }

    @Override public List<Task> findAll() {
        return new ArrayList<>(tasks.values()).stream()
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    @Override public int removeOlderThan(Instant cutoff) {
        int before = tasks.size();
        tasks.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
        return before - tasks.size();
    }
}
