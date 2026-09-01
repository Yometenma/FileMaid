package net.filemaid.application.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;

/**
 * Executes a rename plan's write operations (MOVE / COPY / HARDLINK). It never
 * overwrites an existing target, refuses read-only storage roots, re-checks
 * source/target right before each operation, returns a per-file result, and
 * records the results in the operation history.
 */
public final class ExecuteRenamePlanService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final OperationHistoryRepository history;

    public ExecuteRenamePlanService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, OperationHistoryRepository history) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
        this.history = history;
    }

    public List<OperationResult> execute(String rootId, List<RenameOperation> operations) {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        if (!root.writable()) throw new IllegalStateException("Storage root is read-only: " + rootId);
        List<OperationResult> results = new ArrayList<>();
        for (RenameOperation operation : operations) {
            results.add(executeOne(root, operation));
        }
        if (history != null && !results.isEmpty()) history.append(results);
        return results;
    }

    private OperationResult executeOne(StorageRoot root, RenameOperation operation) {
        String sourceText = operation.source().toString().replace('\\', '/');
        String targetText = operation.target().toString().replace('\\', '/');
        try {
            Path source = pathPolicy.resolve(root, sourceText);
            Path target = pathPolicy.resolve(root, targetText);
            if (!Files.exists(source)) return failure(operation, "源文件不存在: " + sourceText);
            if (!source.equals(target) && Files.exists(target)) return failure(operation, "目标已存在（不覆盖）: " + targetText);
            Path targetParent = target.getParent();
            if (targetParent != null) Files.createDirectories(targetParent);
            switch (operation.type()) {
                case MOVE -> Files.move(source, target);
                case COPY -> Files.copy(source, target);
                case HARDLINK -> Files.createLink(target, source);
                case NFO, ARTWORK -> throw new IllegalArgumentException("后处理操作必须通过后处理服务执行");
            }
            return new OperationResult(sourceText, targetText, operation.type(), true, null);
        } catch (Exception failure) {
            return failure(operation, failure.getMessage());
        }
    }

    private OperationResult failure(RenameOperation operation, String message) {
        return new OperationResult(
                operation.source().toString().replace('\\', '/'),
                operation.target().toString().replace('\\', '/'),
                operation.type(),
                false,
                message);
    }
}
