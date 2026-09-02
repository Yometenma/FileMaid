package net.filemaid.server.api;

import java.util.List;
import net.filemaid.server.WebLogBuffer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public final class LogController {
    private final WebLogBuffer logs;

    public LogController(WebLogBuffer logs) { this.logs = logs; }

    @GetMapping
    List<WebLogBuffer.Entry> list(@RequestParam(defaultValue = "0") long after,
            @RequestParam(defaultValue = "ALL") String level,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "300") int limit) {
        return logs.find(Math.max(0, after), level, query, limit);
    }
}
