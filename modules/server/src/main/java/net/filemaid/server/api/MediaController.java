package net.filemaid.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.application.service.BuildRenamePlanService;
import net.filemaid.application.service.ExecuteRenamePlanService;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.PostProcessMediaService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.ValidateRenamePlanService;
import net.filemaid.core.model.MediaGroup;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.PostProcessPlan;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.RenamePlan;
import net.filemaid.core.model.RenamePreview;
import net.filemaid.server.BackgroundTaskService;
import net.filemaid.server.ConfirmedPlanRegistry;
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
    private final PostProcessMediaService postProcessService;
    private final ConfirmedPlanRegistry confirmedPlans;
    private final BackgroundTaskService taskService;

    public MediaController(ParseMediaNameService parseService, RenamePreviewService previewService,
            AnalyzeMediaGroupsService groupService, BuildRenamePlanService buildPlanService,
            ValidateRenamePlanService validatePlanService, ExecuteRenamePlanService executePlanService,
            PostProcessMediaService postProcessService, ConfirmedPlanRegistry confirmedPlans,
            BackgroundTaskService taskService) {
        this.parseService = parseService;
        this.previewService = previewService;
        this.groupService = groupService;
        this.buildPlanService = buildPlanService;
        this.validatePlanService = validatePlanService;
        this.executePlanService = executePlanService;
        this.postProcessService = postProcessService;
        this.confirmedPlans = confirmedPlans;
        this.taskService = taskService;
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
        requireFileOperation(type);
        return buildPlanService.build(request.rootId(), request.paths(), request.selections(), type);
    }

    @PostMapping("/rename-plans/validate")
    ConfirmedValidation validatePlan(@Valid @RequestBody ValidateRequest request) {
        request.operations().forEach(operation -> requireFileOperation(operation.type()));
        var validation = validatePlanService.validate(request.rootId(), request.operations());
        UUID token = validation.valid() ? confirmedPlans.store(request.rootId(), request.operations(), request.postProcess()) : null;
        return new ConfirmedValidation(validation.valid(), validation.problems(), token);
    }

    @PostMapping("/rename-plans/execute")
    ExecuteTaskResponse executePlan(@Valid @RequestBody ExecuteRequest request) {
        var plan = confirmedPlans.consume(request.confirmationToken());
        var validation = validatePlanService.validate(plan.rootId(), plan.operations());
        if (!validation.valid()) throw new IllegalArgumentException(String.join("；", validation.problems()));
        String taskId = taskService.submit("EXECUTE", "正在整理…", context -> executeConfirmed(plan, context));
        return new ExecuteTaskResponse(taskId);
    }

    private List<OperationResult> executeConfirmed(ConfirmedPlanRegistry.ConfirmedPlan plan, net.filemaid.server.TaskContext context) {
        List<OperationResult> results = new ArrayList<>(executePlanService.execute(plan.rootId(), plan.operations()));
        context.progress(70, "文件操作完成，执行后处理");
        PostProcessPlan postProcess = plan.postProcess();
        if (postProcess != null && postProcess.enabled()) {
            Map<String, String> targetBySource = results.stream()
                    .filter(OperationResult::success)
                    .collect(Collectors.toMap(OperationResult::source, OperationResult::target, (a, b) -> a));
            List<PostProcessMediaService.Item> items = postProcess.items().stream()
                    .filter(item -> targetBySource.containsKey(item.source()))
                    .map(item -> new PostProcessMediaService.Item(targetBySource.get(item.source()), item.metadata(), item.artworkUrl()))
                    .toList();
            if (!items.isEmpty()) {
                results.addAll(postProcessService.process(plan.rootId(), items, postProcess.generateNfo(), postProcess.downloadArtwork(), postProcess.artworkType()));
            }
        }
        return results;
    }

    public record ParseRequest(@NotEmpty List<String> names) {}
    public record PreviewRequest(String rootId, @NotEmpty List<String> paths, List<MetadataSelection> selections) {}
    public record PlanRequest(String rootId, @NotEmpty List<String> paths, List<MetadataSelection> selections, RenameOperation.OperationType type) {}
    public record ValidateRequest(@NotEmpty String rootId, @NotEmpty List<RenameOperation> operations, PostProcessPlan postProcess) {}
    public record ExecuteRequest(UUID confirmationToken) {
        public ExecuteRequest { if (confirmationToken == null) throw new IllegalArgumentException("缺少确认令牌"); }
    }
    public record ExecuteTaskResponse(String taskId) { }
    public record ConfirmedValidation(boolean valid, List<String> problems, UUID confirmationToken) { }
    private void requireFileOperation(RenameOperation.OperationType type) {
        if (type == RenameOperation.OperationType.NFO || type == RenameOperation.OperationType.ARTWORK) throw new IllegalArgumentException("后处理操作不能进入文件整理计划");
    }
}
