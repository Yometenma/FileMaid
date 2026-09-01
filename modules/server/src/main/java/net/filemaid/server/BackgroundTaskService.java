package net.filemaid.server;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import net.filemaid.application.port.TaskRepository;
import net.filemaid.core.model.Task;
import org.springframework.stereotype.Service;

/**
 * Submits work to a small worker pool and tracks each task's status, progress,
 * result and cancellation. Tasks live in a repository keyed by id so the UI can
 * reconnect after a refresh and keep polling a running task.
 */
@Service
public class BackgroundTaskService {
    private final TaskRepository repository;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();

    public BackgroundTaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public String submit(String type, String initialMessage, TaskWork work) {
        repository.removeOlderThan(Instant.now().minus(Duration.ofHours(1)));
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        repository.save(new Task(id, type, Task.Status.PENDING, 0, initialMessage, null, null, now, now));
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancellations.put(id, cancelled);
        Future<?> future = executor.submit(() -> run(id, work, cancelled));
        futures.put(id, future);
        return id;
    }

    public Task get(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
    }

    public List<Task> list() {
        return repository.findAll();
    }

    public boolean cancel(String id) {
        AtomicBoolean cancelled = cancellations.get(id);
        if (cancelled == null) return false;
        cancelled.set(true);
        Future<?> future = futures.get(id);
        if (future != null) future.cancel(true);
        return true;
    }

    void reportProgress(String id, int percent, String message) {
        update(id, null, percent, message, null, null);
    }

    private void run(String id, TaskWork work, AtomicBoolean cancelled) {
        update(id, Task.Status.RUNNING, 5, "运行中", null, null);
        try {
            Object result = work.run(new TaskContext(this, id, cancelled));
            if (cancelled.get()) {
                update(id, Task.Status.CANCELLED, 100, "已取消", null, null);
            } else {
                update(id, Task.Status.COMPLETED, 100, "完成", result, null);
            }
        } catch (Exception failure) {
            if (cancelled.get()) {
                update(id, Task.Status.CANCELLED, 100, "已取消", null, null);
            } else {
                update(id, Task.Status.FAILED, 100, "失败", null, failure.getMessage());
            }
        } finally {
            futures.remove(id);
            cancellations.remove(id);
        }
    }

    private void update(String id, Task.Status status, int progress, String message, Object result, String error) {
        repository.findById(id).ifPresent(current -> {
            Task.Status nextStatus = status == null ? current.status() : status;
            int nextProgress = progress < 0 ? current.progress() : Math.max(0, Math.min(100, progress));
            String nextMessage = message == null ? current.message() : message;
            repository.save(new Task(current.id(), current.type(), nextStatus, nextProgress,
                    nextMessage, result, error, current.createdAt(), Instant.now()));
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
