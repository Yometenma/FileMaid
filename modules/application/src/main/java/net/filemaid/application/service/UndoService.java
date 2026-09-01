package net.filemaid.application.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.StorageRoot;

/**
 * Undoes a previously executed operation: MOVE is moved back to its source,
 * COPY and HARDLINK delete the created target. It re-checks that the target
 * still exists and the source is still free before undoing, and never
 * overwrites.
 */
public final class UndoService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final OperationHistoryRepository history;

    public UndoService(java.util.List<StorageRoot> roots, StoragePathPolicy pathPolicy, OperationHistoryRepository history) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
        this.history = history;
    }

    public OperationResult undo(String rootId, long operationId) {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        if (!root.writable()) throw new IllegalStateException("Storage root is read-only: " + rootId);
        OperationRecord record = history.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown operation: " + operationId));
        if (!record.success()) return failure(record, "该操作原本未成功，无法撤销");
        try {
            Path source = pathPolicy.resolve(root, record.source());
            Path target = pathPolicy.resolve(root, record.target());
            if (!Files.exists(target)) return failure(record, "目标已不存在，无法撤销");
            switch (record.type()) {
                case MOVE -> {
                    if (Files.exists(source)) return failure(record, "源路径已存在（不覆盖），无法撤销");
                    Files.move(target, source);
                }
                case COPY, HARDLINK -> Files.delete(target);
            }
            return new OperationResult(record.target(), record.source(), record.type(), true, null);
        } catch (Exception failure) {
            return failure(record, failure.getMessage());
        }
    }

    private OperationResult failure(OperationRecord record, String message) {
        return new OperationResult(record.target(), record.source(), record.type(), false, message);
    }
}
