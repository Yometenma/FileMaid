package net.filemaid.server.api;

import java.io.IOException;
import java.util.List;
import net.filemaid.application.service.ProbeMediaInfoService;
import net.filemaid.application.service.ScanMediaService;
import net.filemaid.application.service.BrowseStorageService;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.server.BackgroundTaskService;
import net.filemaid.server.FileMaidProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roots")
public class StorageController {
    private final ScanMediaService scanService;
    private final ProbeMediaInfoService probeService;
    private final FileMaidProperties properties;
    private final BrowseStorageService browseService;
    private final SettingsService settings;
    private final BackgroundTaskService taskService;

    public StorageController(ScanMediaService scanService, ProbeMediaInfoService probeService, FileMaidProperties properties, BrowseStorageService browseService, SettingsService settings, BackgroundTaskService taskService) {
        this.scanService = scanService;
        this.probeService = probeService;
        this.properties = properties;
        this.browseService = browseService;
        this.settings = settings;
        this.taskService = taskService;
    }

    @GetMapping("/{rootId}/directories")
    BrowseStorageService.DirectoryListing directories(@PathVariable String rootId,
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "100") int limit) throws IOException {
        return browseService.browse(rootId, path, query, limit);
    }

    @GetMapping
    List<RootResponse> roots() {
        return scanService.roots().stream().map(root -> new RootResponse(root.id(), root.writable())).toList();
    }

    @GetMapping("/{rootId}/scan")
    List<MediaFileResponse> scan(@PathVariable String rootId, @RequestParam(defaultValue = "") String path) throws IOException {
        int maxDepth = integer("scan.maxDepth", properties.scan().maxDepth());
        int maxFiles = integer("scan.maxFiles", properties.scan().maxFiles());
        return scanService.scan(rootId, path, maxDepth, maxFiles).stream()
                .map(MediaFileResponse::from)
                .toList();
    }

    @PostMapping("/{rootId}/scan")
    ScanTaskResponse startScan(@PathVariable String rootId, @RequestBody ScanRequest request) {
        int maxDepth = integer("scan.maxDepth", properties.scan().maxDepth());
        int maxFiles = integer("scan.maxFiles", properties.scan().maxFiles());
        String path = request.path() == null ? "" : request.path();
        String taskId = taskService.submit("SCAN", "正在扫描…", context -> {
            context.progress(10, "扫描目录");
            List<MediaFile> files = scanService.scan(rootId, path, maxDepth, maxFiles);
            context.progress(90, "扫描完成，整理结果");
            return files.stream().map(MediaFileResponse::from).toList();
        });
        return new ScanTaskResponse(taskId);
    }

    private int integer(String key, int fallback) {
        try { return Integer.parseInt(settings.value(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    @GetMapping("/{rootId}/probe")
    ResponseEntity<MediaInfoResponse> probe(@PathVariable String rootId, @RequestParam(defaultValue = "") String path) throws IOException {
        return probeService.probe(rootId, path)
                .map(info -> ResponseEntity.ok(MediaInfoResponse.from(info)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record RootResponse(String id, boolean writable) {}
    record ScanRequest(String path) {}
    record ScanTaskResponse(String taskId) {}

    public record MediaFileResponse(String rootId, String path, String kind, long size, String modifiedAt) {
        static MediaFileResponse from(MediaFile file) {
            String relativePath = file.relativePath().toString().replace('\\', '/');
            return new MediaFileResponse(
                    file.storageRootId(),
                    relativePath,
                    file.kind().name(),
                    file.size(),
                    file.modifiedAt().toString());
        }
    }

    record MediaInfoResponse(String fileName, Long fileSize, String videoCodec, String videoProfile,
            Integer width, Integer height, String resolution, Double frameRate,
            String audioCodec, String audioLanguage, String subtitleCodec, String subtitleLanguage,
            Double durationSeconds, Double bitRate, String title) {
        static MediaInfoResponse from(MediaInfo info) {
            return new MediaInfoResponse(
                    info.fileName(),
                    info.fileSize(),
                    info.videoCodec(),
                    info.videoProfile(),
                    info.width(),
                    info.height(),
                    info.resolution(),
                    info.frameRate(),
                    info.audioCodec(),
                    info.audioLanguage(),
                    info.subtitleCodec(),
                    info.subtitleLanguage(),
                    info.durationSeconds(),
                    info.bitRate(),
                    info.title());
        }
    }
}
