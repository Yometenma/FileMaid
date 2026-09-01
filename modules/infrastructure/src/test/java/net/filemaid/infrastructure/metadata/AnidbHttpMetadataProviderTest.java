package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnidbHttpMetadataProviderTest {
    @Test
    void parsesTitlesFromTsv() {
        String tsv = """
                # comment
                1|1|en|Full Metal Alchemist
                1|4|ja|Hagane no Renkinjutsushi
                2|1|en|Another Anime
                3|2|en|Official Title
                9|5|en|Skipped Type
                """;
        var index = AnidbHttpMetadataProvider.parseTitles(tsv);
        assertEquals(3, index.size());
        assertEquals("anidb", index.get(0).provider());
        assertEquals("1", index.get(0).id());
        assertEquals("Full Metal Alchemist", index.get(0).title());
        assertEquals(List.of("Hagane no Renkinjutsushi"), index.get(0).alternativeTitles());
    }

    @Test
    void availableReflectsEnabledFlag() {
        assertTrue(new AnidbHttpMetadataProvider(true).available());
        assertFalse(new AnidbHttpMetadataProvider(false).available());
    }
}
