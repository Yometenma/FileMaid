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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import net.filemaid.application.port.MetadataProvider;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {
    private final SearchMetadataService service;
    private final MatchMetadataService matchService;
    private final List<MetadataProvider> providers;

    public MetadataController(SearchMetadataService service, MatchMetadataService matchService, List<MetadataProvider> providers) {
        this.service = service;
        this.matchService = matchService;
        this.providers = List.copyOf(providers);
    }

    @GetMapping("/providers")
    List<SearchMetadataService.ProviderStatus> providers() { return service.statuses(); }

    @GetMapping("/search")
    List<MetadataCandidate> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "SERIES") MetadataType type,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "10") int limit) throws Exception {
        return service.search(query, type, locale == null || locale.isBlank() ? null : Locale.forLanguageTag(locale), limit);
    }

    @PostMapping("/match")
    List<MatchMetadataService.MatchResult> match(@Valid @RequestBody MatchRequest request) throws Exception {
        Locale locale = request.locale() == null || request.locale().isBlank() ? null : Locale.forLanguageTag(request.locale());
        int limit = request.limit() == null ? 5 : Math.max(1, Math.min(request.limit(), 20));
        List<MatchMetadataService.MatchResult> results = new ArrayList<>();
        for (String name : request.names()) {
            results.add(matchService.match(name, locale, limit));
        }
        return results;
    }

    @PostMapping("/providers/{providerId}/test")
    ProviderTest testProvider(@PathVariable String providerId) {
        MetadataProvider provider = providers.stream().filter(item -> item.id().equals(providerId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知元数据源: " + providerId));
        if (!provider.available()) return new ProviderTest(false, provider.status());
        try {
            provider.search("Avatar", MetadataType.SERIES, Locale.ENGLISH, 1);
            return new ProviderTest(true, "连接成功");
        } catch (Exception failure) {
            return new ProviderTest(false, failure.getMessage() == null ? "连接失败" : failure.getMessage());
        }
    }

    public record MatchRequest(String locale, Integer limit, @NotEmpty List<String> names) {}
    public record ProviderTest(boolean success, String message) { }
}
