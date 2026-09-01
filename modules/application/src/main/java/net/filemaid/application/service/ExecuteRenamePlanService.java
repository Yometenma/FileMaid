package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;

/**
 * Executes a rename plan's write operations (MOVE / COPY / HARDLINK). It never
 * overwrites an existing target, refuses read-only storage roots, re-checks
 * source/target right before each operation, returns a per-file result, and
 * records the results in the operation history. When {@code files.conflictPolicy}
 * is SKIP an existing target is skipped instead of failing, and when
 * {@code postprocess.cleanEmptyDirectories} is enabled emptied source
 * directories are removed.
 */
public final class ExecuteRenamePlanService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final OperationHistoryRepository history;
    private final SettingsService settings;

    public ExecuteRenamePlanService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, OperationHistoryRepository history) {
        this(roots, pathPolicy, history, null);
    }

    public ExecuteRenamePlanService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, OperationHistoryRepository history, SettingsService settings) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
        this.history = history;
        this.settings = settings;
    }

    public List<OperationResult> execute(String rootId, List<RenameOperation> operations) {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        if (!root.writable()) throw new IllegalStateException("Storage root is read-only: " + rootId);
        boolean cleanEmptyDirectories = cleanEmptyDirectories();
        List<OperationResult> results = new ArrayList<>();
        Set<Path> emptiedParents = new LinkedHashSet<>();
        for (RenameOperation operation : operations) {
            OperationResult result = executeOne(root, operation);
            results.add(result);
            if (cleanEmptyDirectories && result.success() && operation.type() == RenameOperation.OperationType.MOVE) {
                Path source = pathPolicy.resolve(root, operation.source().toString().replace('\\', '/'));
                if (source.getParent() != null) emptiedParents.add(source.getParent());
            }
        }
        cleanupEmptyDirectories(root, emptiedParents);
        if (history != null && !results.isEmpty()) history.append(results);
        cleanupHistory();
        return results;
    }

    private OperationResult executeOne(StorageRoot root, RenameOperation operation) {
        String sourceText = operation.source().toString().replace('\\', '/');
        String targetText = operation.target().toString().replace('\\', '/');
        try {
            Path source = pathPolicy.resolve(root, sourceText);
            Path target = pathPolicy.resolve(root, targetText);
            if (!Files.exists(source)) return failure(operation, "源文件不存在: " + sourceText);
            if (!source.equals(target) && Files.exists(target)) {
                if (skipConflicts()) return new OperationResult(sourceText, sourceText, operation.type(), true, null);
                return failure(operation, "目标已存在（不覆盖）: " + targetText);
            }
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

    private boolean skipConflicts() {
        return settings != null && "SKIP".equals(settings.value("files.conflictPolicy", "FAIL"));
    }

    private boolean cleanEmptyDirectories() {
        return settings != null && "true".equals(settings.value("postprocess.cleanEmptyDirectories", "false"));
    }

    private void cleanupEmptyDirectories(StorageRoot root, Set<Path> parents) {
        Path rootPath = root.path();
        parents.stream()
                .sorted(Comparator.comparingInt((Path p) -> p.getNameCount()).reversed())
                .forEach(parent -> deleteEmptyUpTo(parent, rootPath));
    }

    private void deleteEmptyUpTo(Path dir, Path rootPath) {
        Path current = dir;
        while (current != null && current.startsWith(rootPath) && !current.equals(rootPath)) {
            if (!isEmptyDirectory(current)) return;
            try { Files.delete(current); } catch (IOException ignored) { return; }
            current = current.getParent();
        }
    }

    private boolean isEmptyDirectory(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) { return entries.findAny().isEmpty(); }
        catch (IOException ignored) { return false; }
    }

    private void cleanupHistory() {
        int days = settings == null ? 0 : OperationHistoryService.parseDays(settings.value("files.historyRetentionDays", "90"));
        if (history != null && days > 0) history.deleteOlderThan(Instant.now().minus(days, ChronoUnit.DAYS));
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
