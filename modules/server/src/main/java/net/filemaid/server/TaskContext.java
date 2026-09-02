package net.filemaid.server;

import java.util.concurrent.atomic.AtomicBoolean;

/** Handle passed to a running task so it can report progress and observe cancellation. */
public final class TaskContext {
    private final BackgroundTaskService owner;
    private final String taskId;
    private final AtomicBoolean cancelled;

    TaskContext(BackgroundTaskService owner, String taskId, AtomicBoolean cancelled) {
        this.owner = owner;
        this.taskId = taskId;
        this.cancelled = cancelled;
    }

    public void progress(int percent, String message) {
        owner.reportProgress(taskId, percent, message);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void checkCancelled() throws InterruptedException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedException("任务已取消");
    }

    public String taskId() { return taskId; }
}
