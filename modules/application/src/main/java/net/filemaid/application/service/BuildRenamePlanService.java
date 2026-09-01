package net.filemaid.application.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.RenamePlan;
import net.filemaid.core.model.RenamePreview;

/**
 * Turns a rename preview into an immutable execution plan carrying a
 * confirmation token (a UUID). The plan itself does not touch the filesystem;
 * execution happens later, after revalidation.
 */
public final class BuildRenamePlanService {
    private final RenamePreviewService previewService;

    public BuildRenamePlanService(RenamePreviewService previewService) {
        this.previewService = previewService;
    }

    public RenamePlan build(String rootId, List<String> paths, List<MetadataSelection> selections, RenameOperation.OperationType type) {
        List<RenamePreview> previews = previewService.preview(rootId, paths, selections);
        List<RenameOperation> operations = previews.stream()
                .map(preview -> new RenameOperation(Path.of(preview.source()), Path.of(preview.target()), type))
                .toList();
        List<String> warnings = previews.stream()
                .flatMap(preview -> preview.warnings().stream())
                .distinct()
                .toList();
        return new RenamePlan(UUID.randomUUID(), Instant.now(), operations, warnings);
    }
}
