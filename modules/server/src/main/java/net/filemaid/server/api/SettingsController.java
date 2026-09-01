package net.filemaid.server.api;

import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.application.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {
    private static final String MASK = "********";
    private final SettingsService service;
    public SettingsController(SettingsService service) { this.service = service; }

    @GetMapping Map<String, String> get() {
        Map<String, String> result = new LinkedHashMap<>(service.findAll());
        service.secretKeys().forEach(key -> {
            if (!result.getOrDefault(key, "").isBlank()) result.put(key, MASK);
        });
        return result;
    }

    @GetMapping("/schema")
    java.util.List<SettingsService.Definition> schema() { return service.definitions(); }

    @PutMapping Map<String, String> update(@RequestBody Map<String, String> values) {
        Map<String, String> safe = new LinkedHashMap<>(values == null ? Map.of() : values);
        service.secretKeys().forEach(key -> {
            if (MASK.equals(safe.get(key))) safe.remove(key);
        });
        service.update(safe);
        return get();
    }
}
