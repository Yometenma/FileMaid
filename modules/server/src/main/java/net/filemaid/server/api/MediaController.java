package net.filemaid.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.core.model.MediaGroup;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.MetadataSelection;
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

    public MediaController(ParseMediaNameService parseService, RenamePreviewService previewService, AnalyzeMediaGroupsService groupService) {
        this.parseService = parseService;
        this.previewService = previewService;
        this.groupService = groupService;
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
        return previewService.preview(request.paths(), request.selections());
    }

    public record ParseRequest(@NotEmpty List<String> names) {}
    public record PreviewRequest(@NotEmpty List<String> paths, List<MetadataSelection> selections) {}
}
