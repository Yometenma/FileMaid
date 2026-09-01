package net.filemaid.server.api;

import java.util.Map;
import net.filemaid.application.port.NamingTemplateEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/naming")
public class NamingController {
    private final NamingTemplateEngine engine;
    public NamingController(NamingTemplateEngine engine) { this.engine = engine; }

    @GetMapping("/templates")
    Map<String, String> templates() {
        return engine.templates();
    }
}
