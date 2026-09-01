package net.filemaid.infrastructure.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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
}
