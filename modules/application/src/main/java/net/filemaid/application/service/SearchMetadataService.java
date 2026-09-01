package net.filemaid.application.service;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public final class SearchMetadataService {
    private final List<MetadataProvider> providers;
    private final Supplier<List<Locale>> languagePriority;

    public SearchMetadataService(List<MetadataProvider> providers) {
        this(providers, () -> List.of(Locale.SIMPLIFIED_CHINESE));
    }

    public SearchMetadataService(List<MetadataProvider> providers, Supplier<List<Locale>> languagePriority) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
        this.languagePriority = languagePriority;
    }

    public List<ProviderStatus> statuses() {
        return providers.stream().map(p -> new ProviderStatus(p.id(), p.available(), p.status())).toList();
    }

    /** Aggregates results across every available provider; one provider failing does not break the others. */
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search query must not be blank");
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Locale> locales = locale == null ? languagePriority.get() : List.of(locale);
        Map<String, MetadataCandidate> results = new LinkedHashMap<>();
        boolean anyAvailable = false;
        for (MetadataProvider provider : providers) {
            if (!provider.available()) continue;
            anyAvailable = true;
            try {
                for (Locale candidateLocale : locales) {
                    for (MetadataCandidate candidate : provider.search(query.trim(), type, candidateLocale, safeLimit)) {
                        results.putIfAbsent(candidate.provider() + ":" + candidate.id(), candidate);
                    }
                    if (results.size() >= safeLimit) break;
                }
            } catch (Exception ignored) {
                // 单个提供器失败不影响其它
            }
        }
        if (!anyAvailable) throw new MetadataProviderUnavailableException("没有可用的元数据提供器");
        return results.values().stream().limit(safeLimit).toList();
    }

    public record ProviderStatus(String id, boolean available, String message) {}

    public static final class MetadataProviderUnavailableException extends IllegalStateException {
        public MetadataProviderUnavailableException(String message) { super(message); }
    }
}
