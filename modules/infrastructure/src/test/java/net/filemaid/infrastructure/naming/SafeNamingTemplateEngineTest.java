package net.filemaid.infrastructure.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;
import org.junit.jupiter.api.Test;

class SafeNamingTemplateEngineTest {
    @Test
    void formatsLegacyStyleVariablesWithoutExecutingScripts() {
        var engine = new SafeNamingTemplateEngine("Series/{title}/S{season:02}/{episodes}{extension}", null, null);
        var media = new ParsedMediaName("Show.S1E2.mkv", "Show", MediaType.EPISODE, 2024, 1, List.of(2, 3), ".mkv", 1, "test");
        assertEquals("Series/Show/S01/E02E03.mkv", engine.format(media));
    }

    @Test
    void rejectsTraversalAndUnknownOrScriptTokens() {
        assertThrows(IllegalArgumentException.class, () -> new SafeNamingTemplateEngine("../{title}", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SafeNamingTemplateEngine("{System.exit(0)}", null, null));
    }

    @Test
    void formatsMediaInfoTokens() {
        var engine = new SafeNamingTemplateEngine(null, "Movies/{title} ({year}) [{resolution} {videoCodec}]{extension}", null);
        var media = new ParsedMediaName("Movie.2024.1080p.mkv", "Movie", MediaType.MOVIE, 2024, null, List.of(), ".mkv", 1, "test");
        var info = new MediaInfo("Movie.2024.1080p.mkv", 123L, "h264", "High", 1920, 1080, 23.976, "aac", "jpn", null, null, 5400.0, 5000.0, "Movie");
        assertEquals("Movies/Movie (2024) [1080p h264].mkv", engine.format(media, info));
    }
}
