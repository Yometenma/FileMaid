package net.filemaid.infrastructure.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.filemaid.core.model.MediaType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class LegacyMediaNameParserAdapterTest {
    private final LegacyMediaNameParserAdapter parser = new LegacyMediaNameParserAdapter();

    @Test
    void stripsFormatInfoFromEpisodeTitleWhenLegacyEnginePresent() {
        Assumptions.assumeTrue(parser.available(), "legacy engine not on classpath");
        var parsed = parser.parse("[Group] The.Show.S02E03.1080p.BluRay.mkv");
        assertEquals(MediaType.EPISODE, parsed.type());
        assertEquals("The Show", parsed.title());
        assertEquals(2, parsed.season());
        assertEquals(List.of(3), parsed.episodes());
    }
}
