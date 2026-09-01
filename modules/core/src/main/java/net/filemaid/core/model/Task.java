package net.filemaid.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A background task's observable state. Immutable; each progress update
 * replaces the whole record. The result is an untyped payload whose concrete
 * type depends on the task type and is serialized by the HTTP layer.
 */
public record Task(
        String id,
        String type,
        Status status,
        int progress,
        String message,
        Object result,
        String error,
        Instant createdAt,
        Instant updatedAt) {

    public Task {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        status = status == null ? Status.PENDING : status;
        progress = Math.max(0, Math.min(100, progress));
        message = message == null ? "" : message;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public boolean finished() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
}
