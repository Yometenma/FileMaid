package net.filemaid.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.application.service.BuildRenamePlanService;
import net.filemaid.application.service.ExecuteRenamePlanService;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.ValidateRenamePlanService;
import net.filemaid.core.model.MediaGroup;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.RenamePlan;
import net.filemaid.core.model.RenamePreview;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MediaController {
    private final ParseMediaNameService parseService;
    private final RenamePreviewService previewService;
    private final AnalyzeMediaGroupsService groupService;
    private final BuildRenamePlanService buildPlanService;
    private final ValidateRenamePlanService validatePlanService;
    private final ExecuteRenamePlanService executePlanService;

    public MediaController(ParseMediaNameService parseService, RenamePreviewService previewService,
            AnalyzeMediaGroupsService groupService, BuildRenamePlanService buildPlanService,
            ValidateRenamePlanService validatePlanService, ExecuteRenamePlanService executePlanService) {
        this.parseService = parseService;
        this.previewService = previewService;
        this.groupService = groupService;
        this.buildPlanService = buildPlanService;
        this.validatePlanService = validatePlanService;
        this.executePlanService = executePlanService;
    }

    @PostMapping("/media/groups/analyze")
    List<MediaGroup> analyzeGroups(@Valid @RequestBody PreviewRequest request) {
        return groupService.analyze(request.paths());
    }

    @PostMapping("/media/parse")
    List<ParsedMediaName> parse(@Valid @RequestBody ParseRequest request) {
        return parseService.parse(request.names());
    }

    @PostMapping("/rename-plans/preview")
    List<RenamePreview> preview(@Valid @RequestBody PreviewRequest request) {
        if (request.rootId() == null || request.rootId().isBlank()) {
            return previewService.preview(request.paths(), request.selections());
        }
        return previewService.preview(request.rootId(), request.paths(), request.selections());
    }

    @PostMapping("/rename-plans")
    RenamePlan buildPlan(@Valid @RequestBody PlanRequest request) {
        RenameOperation.OperationType type = request.type() == null ? RenameOperation.OperationType.MOVE : request.type();
        return buildPlanService.build(request.rootId(), request.paths(), request.selections(), type);
    }

    @PostMapping("/rename-plans/validate")
    ValidateRenamePlanService.PlanValidation validatePlan(@Valid @RequestBody ValidateRequest request) {
        return validatePlanService.validate(request.rootId(), request.operations());
    }

    @PostMapping("/rename-plans/execute")
    List<OperationResult> executePlan(@Valid @RequestBody ValidateRequest request) {
        return executePlanService.execute(request.rootId(), request.operations());
    }

    public record ParseRequest(@NotEmpty List<String> names) {}
    public record PreviewRequest(String rootId, @NotEmpty List<String> paths, List<MetadataSelection> selections) {}
    public record PlanRequest(String rootId, @NotEmpty List<String> paths, List<MetadataSelection> selections, RenameOperation.OperationType type) {}
    public record ValidateRequest(@NotEmpty String rootId, @NotEmpty List<RenameOperation> operations) {}
}
