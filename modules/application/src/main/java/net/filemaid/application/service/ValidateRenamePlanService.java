package net.filemaid.application.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;

/**
 * Revalidates a rename plan against the current filesystem state before
 * execution. The plan may have been produced earlier; by the time it runs the
 * directory may have changed, so this checks that every source still exists,
 * every target is still free, and no path escapes its storage root. It performs
 * no writes.
 */
public final class ValidateRenamePlanService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;

    public ValidateRenamePlanService(List<StorageRoot> roots, StoragePathPolicy pathPolicy) {
        this.roots = roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity()));
        this.pathPolicy = pathPolicy;
    }

    public PlanValidation validate(String rootId, List<RenameOperation> operations) {
        StorageRoot root = roots.get(rootId);
        if (root == null) throw new IllegalArgumentException("Unknown storage root: " + rootId);
        List<String> problems = new ArrayList<>();
        for (RenameOperation operation : operations) {
            try {
                Path source = pathPolicy.resolve(root, operation.source().toString().replace('\\', '/'));
                Path target = pathPolicy.resolve(root, operation.target().toString().replace('\\', '/'));
                if (!Files.exists(source)) problems.add("源文件不存在: " + operation.source());
                if (!source.equals(target) && Files.exists(target)) problems.add("目标已存在（冲突）: " + operation.target());
            } catch (IllegalArgumentException problem) {
                problems.add(problem.getMessage());
            }
        }
        return new PlanValidation(problems.isEmpty(), problems);
    }

    public record PlanValidation(boolean valid, List<String> problems) {
        public PlanValidation {
            problems = problems == null ? List.of() : List.copyOf(problems);
        }
    }
}
