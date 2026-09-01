package net.filemaid.core.model;

import java.nio.file.Path;
import java.util.Objects;

public record RenameOperation(Path source, Path target, OperationType type) {
    public RenameOperation {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(type, "type");
    }

    public enum OperationType { MOVE, COPY, HARDLINK, NFO, ARTWORK }
}
