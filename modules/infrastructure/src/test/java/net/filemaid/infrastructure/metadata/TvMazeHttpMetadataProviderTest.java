package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TvMazeHttpMetadataProviderTest {
    @Test
    void availableReflectsEnabledFlag() {
        assertTrue(new TvMazeHttpMetadataProvider(true).available());
        assertFalse(new TvMazeHttpMetadataProvider(false).available());
        assertEquals("tvmaze", new TvMazeHttpMetadataProvider(true).id());
    }
}
