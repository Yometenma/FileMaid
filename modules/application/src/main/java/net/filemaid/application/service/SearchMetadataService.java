package net.filemaid.application.service;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public final class SearchMetadataService {
    private final MetadataProvider provider;

    public SearchMetadataService(MetadataProvider provider) {
        this.provider = provider;
    }

    public ProviderStatus status() {
        return new ProviderStatus(provider.id(), provider.available(), provider.status());
    }

    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search query must not be blank");
        if (!provider.available()) throw new MetadataProviderUnavailableException(provider.status());
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return provider.search(query.trim(), type, locale == null ? Locale.SIMPLIFIED_CHINESE : locale, safeLimit);
    }

    public record ProviderStatus(String id, boolean available, String message) {}

    public static final class MetadataProviderUnavailableException extends IllegalStateException {
        public MetadataProviderUnavailableException(String message) { super(message); }
    }
}
