package net.filemaid.server.api;

import net.filemaid.server.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @PostMapping("/test-notification")
    NotificationService.NotificationResult test() { return service.sendTest(); }
}
