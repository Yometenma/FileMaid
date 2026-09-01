package net.filemaid.server.api;

import java.util.List;
import java.util.Map;
import net.filemaid.server.BackupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
