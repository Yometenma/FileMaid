package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenamePreviewServiceTest {
    private final RenamePreviewService service = new RenamePreviewService(name ->
            new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1, List.of(2), ".mkv", 0.9, "test"),
            media -> "TV Shows/" + media.title() + "/Season 01/" + media.title() + " - S01E02.mkv",
            null);

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

    @Test
    void marksTargetConflictsWhenTwoFilesShareATarget() {
        var conflicting = new RenamePreviewService(name ->
                new ParsedMediaName(name, "Same", MediaType.MOVIE, 2024, null, List.of(), ".mkv", 0.9, "test"),
                media -> "Movies/Same (2024).mkv",
                null);
        var previews = conflicting.preview(List.of("a.mkv", "b.mkv"));
        assertEquals(2, previews.size());
        assertTrue(previews.get(0).warnings().stream().anyMatch(w -> w.contains("冲突")));
        assertTrue(previews.get(1).warnings().stream().anyMatch(w -> w.contains("冲突")));
    }

    @Test
    void probesMediaInfoAndFeedsNamingWhenRootProvided(@TempDir Path root) throws Exception {
        Files.createFile(root.resolve("Show.S01E02.mkv"));
        StorageRoot storageRoot = new StorageRoot("media", root, false);
        MediaInfoProvider provider = new MediaInfoProvider() {
            @Override public String id() { return "fake"; }
            @Override public boolean available() { return true; }
            @Override public Optional<MediaInfo> probe(Path absolutePath) {
                return Optional.of(new MediaInfo("Show.S01E02.mkv", 100L, "h264", "High", 1920, 1080, 23.976, "aac", "jpn", null, null, 3600.0, 1000.0, "Show"));
            }
        };
        var probeService = new ProbeMediaInfoService(List.of(storageRoot), new StoragePathPolicy(), provider);
        NamingTemplateEngine naming = new NamingTemplateEngine() {
            @Override public String format(ParsedMediaName media) { return format(media, null); }
            @Override public String format(ParsedMediaName media, MediaInfo info) {
                return "TV Shows/" + media.title() + "/" + media.title() + " - [" + (info == null ? "?" : info.resolution()) + "].mkv";
            }
        };
        var probingService = new RenamePreviewService(name ->
                new ParsedMediaName(name, "Show", MediaType.EPISODE, null, 1, List.of(2), ".mkv", 0.9, "test"),
                naming, probeService);

        var preview = probingService.preview("media", List.of("Show.S01E02.mkv"), List.of()).get(0);

        assertEquals("TV Shows/Show/Show - [1080p].mkv", preview.target());
        assertEquals("1080p", preview.mediaInfo().resolution());
    }
}
