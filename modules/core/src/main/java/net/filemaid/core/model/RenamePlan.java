package net.filemaid.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RenamePlan(UUID id, Instant createdAt, List<RenameOperation> operations, List<String> warnings) {
    public RenamePlan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        operations = List.copyOf(operations);
        warnings = List.copyOf(warnings);
    }
}
