package net.filemaid.server.api;

import java.io.IOException;
import java.util.List;
import net.filemaid.application.service.ProbeMediaInfoService;
import net.filemaid.application.service.ScanMediaService;
import net.filemaid.core.model.MediaFile;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.server.FileMaidProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roots")
public class StorageController {
    private final ScanMediaService scanService;
    private final ProbeMediaInfoService probeService;
    private final FileMaidProperties properties;

    public StorageController(ScanMediaService scanService, ProbeMediaInfoService probeService, FileMaidProperties properties) {
        this.scanService = scanService;
        this.probeService = probeService;
        this.properties = properties;
    }

    @GetMapping
    List<RootResponse> roots() {
        return scanService.roots().stream().map(root -> new RootResponse(root.id(), root.writable())).toList();
    }

    @GetMapping("/{rootId}/scan")
    List<MediaFileResponse> scan(@PathVariable String rootId, @RequestParam(defaultValue = "") String path) throws IOException {
        return scanService.scan(rootId, path, properties.scan().maxDepth(), properties.scan().maxFiles()).stream()
                .map(MediaFileResponse::from)
                .toList();
    }

    @GetMapping("/{rootId}/probe")
    ResponseEntity<MediaInfoResponse> probe(@PathVariable String rootId, @RequestParam(defaultValue = "") String path) throws IOException {
        return probeService.probe(rootId, path)
                .map(info -> ResponseEntity.ok(MediaInfoResponse.from(info)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record RootResponse(String id, boolean writable) {}

    record MediaFileResponse(String rootId, String path, String kind, long size, String modifiedAt) {
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
