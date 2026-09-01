package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;

class SearchMetadataServiceTest {
    private static MetadataProvider provider(String id, boolean available, List<MetadataCandidate> results) {
        return new MetadataProvider() {
            @Override public String id() { return id; }
            @Override public boolean available() { return available; }
            @Override public String status() { return available ? "ok" : "off"; }
            @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) {
                return results;
            }
        };
    }

    @Test
    void aggregatesResultsAcrossProviders() throws Exception {
        var a = provider("a", true, List.of(candidate("a", "A")));
        var b = provider("b", true, List.of(candidate("b", "B")));
        var service = new SearchMetadataService(List.of(a, b));
        assertEquals(2, service.search("q", MetadataType.SERIES, Locale.ROOT, 10).size());
    }

    @Test
    void oneFailingProviderDoesNotBreakOthers() throws Exception {
        MetadataProvider failing = new MetadataProvider() {
            @Override public String id() { return "failing"; }
            @Override public boolean available() { return true; }
            @Override public String status() { return "ok"; }
            @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) {
                throw new IllegalStateException("boom");
            }
        };
        var good = provider("good", true, List.of(candidate("g", "G")));
        var service = new SearchMetadataService(List.of(failing, good));
        assertEquals(1, service.search("q", MetadataType.SERIES, Locale.ROOT, 10).size());
    }

    @Test
    void throwsWhenNoProviderAvailable() {
        var service = new SearchMetadataService(List.of(provider("off", false, List.of())));
        assertThrows(SearchMetadataService.MetadataProviderUnavailableException.class,
                () -> service.search("q", MetadataType.SERIES, Locale.ROOT, 10));
    }

    @Test
    void statusesReflectsAllProviders() {
        var service = new SearchMetadataService(List.of(
                provider("a", true, List.of()),
                provider("b", false, List.of())));
        assertEquals(2, service.statuses().size());
    }

    private static MetadataCandidate candidate(String id, String title) {
        return new MetadataCandidate("test", id, MetadataType.SERIES, title, List.of(), null, null);
    }
}
