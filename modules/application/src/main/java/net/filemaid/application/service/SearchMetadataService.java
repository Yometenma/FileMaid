package net.filemaid.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public final class SearchMetadataService {
    private final List<MetadataProvider> providers;

    public SearchMetadataService(List<MetadataProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    public List<ProviderStatus> statuses() {
        return providers.stream().map(p -> new ProviderStatus(p.id(), p.available(), p.status())).toList();
    }

    /** Aggregates results across every available provider; one provider failing does not break the others. */
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search query must not be blank");
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Locale safeLocale = locale == null ? Locale.SIMPLIFIED_CHINESE : locale;
        List<MetadataCandidate> results = new ArrayList<>();
        boolean anyAvailable = false;
        for (MetadataProvider provider : providers) {
            if (!provider.available()) continue;
            anyAvailable = true;
            try {
                results.addAll(provider.search(query.trim(), type, safeLocale, safeLimit));
            } catch (Exception ignored) {
                // 单个提供器失败不影响其它
            }
        }
        if (!anyAvailable) throw new MetadataProviderUnavailableException("没有可用的元数据提供器");
        return results;
    }

    public record ProviderStatus(String id, boolean available, String message) {}

    public static final class MetadataProviderUnavailableException extends IllegalStateException {
        public MetadataProviderUnavailableException(String message) { super(message); }
    }
}
