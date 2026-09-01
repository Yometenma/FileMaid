package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import org.junit.jupiter.api.Test;

class RenamePreviewServiceTest {
    private final RenamePreviewService service = new RenamePreviewService(name ->
            new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1, List.of(2), ".mkv", 0.9, "test"),
            media -> "TV Shows/" + media.title() + "/Season 01/" + media.title() + " - S01E02.mkv");

    @Test
    void createsEpisodeTargetWithoutChangingFiles() {
        var preview = service.preview(List.of("incoming/Example.Show.S01E02.mkv")).get(0);
        assertEquals("TV Shows/Example Show/Season 01/Example Show - S01E02.mkv", preview.target());
    }

    @Test
    void rejectsEscapingPath() {
        assertThrows(IllegalArgumentException.class, () -> service.preview(List.of("../outside.mkv")));
    }

    @Test
    void confirmedMetadataReplacesParsedTitleButKeepsEpisodeNumbers() {
        String source = "incoming/Example.Show.S01E02.mkv";
        var selection = new MetadataSelection(source, "tmdb", "123", MetadataType.SERIES, "正式剧名", 2024);
        var preview = service.preview(List.of(source), List.of(selection)).get(0);
        assertEquals("TV Shows/正式剧名/Season 01/正式剧名 - S01E02.mkv", preview.target());
        assertEquals("confirmed:tmdb:123", preview.media().parser());
        assertEquals("123", preview.metadata().id());
    }
}
