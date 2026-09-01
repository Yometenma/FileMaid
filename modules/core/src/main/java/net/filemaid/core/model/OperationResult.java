package net.filemaid.core.model;

/** Per-file outcome of executing one rename operation. */
public record OperationResult(
        String source,
        String target,
        RenameOperation.OperationType type,
        boolean success,
        String error) {
    public OperationResult {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (target == null || target.isBlank()) throw new IllegalArgumentException("target must not be blank");
        if (type == null) throw new IllegalArgumentException("type must not be null");
    }
}
