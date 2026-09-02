package net.filemaid.core.model;

import java.time.Instant;
import java.util.Objects;

/** A persisted entry in the operation history, enabling undo. */
public record OperationRecord(
        long id,
        String batchId,
        String source,
        String target,
        RenameOperation.OperationType type,
        boolean success,
        String error,
        Instant timestamp) {
    public OperationRecord {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (target == null || target.isBlank()) throw new IllegalArgumentException("target must not be blank");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public OperationRecord(long id, String source, String target, RenameOperation.OperationType type,
                           boolean success, String error, Instant timestamp) {
        this(id, null, source, target, type, success, error, timestamp);
    }
}
