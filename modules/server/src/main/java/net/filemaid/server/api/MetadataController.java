package net.filemaid.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.service.MatchMetadataService;
import net.filemaid.application.service.SearchMetadataService;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {
    private final SearchMetadataService service;
    private final MatchMetadataService matchService;

    public MetadataController(SearchMetadataService service, MatchMetadataService matchService) {
        this.service = service;
        this.matchService = matchService;
    }

    @GetMapping("/providers")
    List<SearchMetadataService.ProviderStatus> providers() { return service.statuses(); }

    @GetMapping("/search")
    List<MetadataCandidate> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "SERIES") MetadataType type,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(defaultValue = "10") int limit) throws Exception {
        return service.search(query, type, Locale.forLanguageTag(locale), limit);
    }

    @PostMapping("/match")
    List<MatchMetadataService.MatchResult> match(@Valid @RequestBody MatchRequest request) throws Exception {
        Locale locale = Locale.forLanguageTag(request.locale() == null || request.locale().isBlank() ? "zh-CN" : request.locale());
        int limit = request.limit() == null ? 5 : Math.max(1, Math.min(request.limit(), 20));
        List<MatchMetadataService.MatchResult> results = new ArrayList<>();
        for (String name : request.names()) {
            results.add(matchService.match(name, locale, limit));
        }
        return results;
    }

    public record MatchRequest(String locale, Integer limit, @NotEmpty List<String> names) {}
}
