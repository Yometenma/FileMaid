package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OmdbHttpMetadataProviderTest {
    @Test
    void availableRequiresKey() {
        assertFalse(new OmdbHttpMetadataProvider(null).available());
        assertTrue(new OmdbHttpMetadataProvider("key").available());
        assertEquals("omdb", new OmdbHttpMetadataProvider("key").id());
    }
}
