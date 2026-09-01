package net.filemaid.server.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @GetMapping("/health")
    Map<String, String> health() { return Map.of("status", "UP", "service", "filemaid"); }
}
