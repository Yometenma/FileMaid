package net.filemaid.infrastructure.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnidbHttpMetadataProviderTest {
    @TempDir Path temporaryDirectory;
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

    @Test
    void reusesFreshDiskIndexWithoutDownloading() throws Exception {
        Path cache = temporaryDirectory.resolve("anime-titles.dat");
        Files.writeString(cache, "1|1|en|Cached Anime\n");
        var provider = new AnidbHttpMetadataProvider(true, "https://invalid.example/index.gz",
                HttpClient.newHttpClient(), Duration.ofMillis(50), cache);

        var results = provider.search("cached", net.filemaid.core.model.MetadataType.SERIES, java.util.Locale.ENGLISH, 5);

        assertEquals(1, results.size());
        assertEquals("Cached Anime", results.get(0).title());
    }
}
