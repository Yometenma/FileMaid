package net.filemaid.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;

class RuntimeMetadataProviderTest {
    @Test
    void usesTmdbOriginalTitleAndKeepsLocalizedTitleAsAlias() {
        var candidate = new MetadataCandidate("tmdb", "1", MetadataType.MOVIE, "本地标题",
                List.of("Original Title", "Alias"), 2024, "overview");

        var result = RuntimeMetadataProvider.applyTitlePreference(candidate, "ORIGINAL");

        assertEquals("Original Title", result.title());
        assertEquals(List.of("本地标题", "Alias"), result.alternativeTitles());
    }

    @Test
    void leavesNonTmdbAndLocalizedCandidatesUntouched() {
        var candidate = new MetadataCandidate("tvmaze", "1", MetadataType.SERIES, "Title",
                List.of("Original"), 2024, null);

        assertSame(candidate, RuntimeMetadataProvider.applyTitlePreference(candidate, "ORIGINAL"));
        assertSame(candidate, RuntimeMetadataProvider.applyTitlePreference(candidate, "LOCALIZED"));
    }
}
