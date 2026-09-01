package net.filemaid.infrastructure.metadata;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public final class PreferredTmdbMetadataProvider implements MetadataProvider {
    private final MetadataProvider delegate;

    public PreferredTmdbMetadataProvider(String apiKey) {
        MetadataProvider legacy = new LegacyTmdbMetadataProvider(apiKey);
        delegate = legacy.available() ? legacy : new TmdbHttpMetadataProvider(apiKey);
    }

    @Override public String id() { return delegate.id(); }
    @Override public boolean available() { return delegate.available(); }
    @Override public String status() { return delegate.status(); }
    @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        return delegate.search(query, type, locale, limit);
    }
}
