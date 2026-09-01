package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MetadataHttpExceptionTest {
    @Test
    void parsesRetryAfterSeconds() {
        assertEquals(Duration.ofSeconds(12), MetadataHttpException.parseRetryAfter(Optional.of("12")));
    }

    @Test
    void ignoresInvalidRetryAfter() {
        assertTrue(MetadataHttpException.parseRetryAfter(Optional.of("not-a-date")) == null);
    }
}
