package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TvdbHttpMetadataProviderTest {
    @Test
    void unavailableWithoutKey() {
        var provider = new TvdbHttpMetadataProvider(null, null);
        assertFalse(provider.available());
        assertEquals("tvdb", provider.id());
    }

    @Test
    void availableWithKey() {
        assertTrue(new TvdbHttpMetadataProvider("key", null).available());
    }
}
