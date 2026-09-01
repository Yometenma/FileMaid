package net.filemaid.server.api;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.service.SearchMetadataService;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {
    private final SearchMetadataService service;

    public MetadataController(SearchMetadataService service) { this.service = service; }

    @GetMapping("/providers")
    List<SearchMetadataService.ProviderStatus> providers() { return List.of(service.status()); }

    @GetMapping("/search")
    List<MetadataCandidate> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "SERIES") MetadataType type,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(defaultValue = "10") int limit) throws Exception {
        return service.search(query, type, Locale.forLanguageTag(locale), limit);
    }
}
