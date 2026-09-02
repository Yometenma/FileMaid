package net.filemaid.server.api;

import java.util.List;
import java.util.Map;
import net.filemaid.server.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class BackupController {
    private final BackupService service;
    public BackupController(BackupService service) { this.service = service; }

    @PostMapping("/backup")
    Map<String, Object> backup() {
        String file = service.createBackup();
        return Map.of("success", true, "file", file);
    }

    @GetMapping("/backups")
    List<BackupService.BackupEntry> backups() { return service.listBackups(); }

    @GetMapping("/backups/{name}")
    ResponseEntity<FileSystemResource> download(@PathVariable String name) {
        var file = service.backupFile(name);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(new FileSystemResource(file));
    }
}
