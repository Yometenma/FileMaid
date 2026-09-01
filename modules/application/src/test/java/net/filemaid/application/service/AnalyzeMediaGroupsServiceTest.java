package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.filemaid.core.model.MediaKind;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;
import org.junit.jupiter.api.Test;

class AnalyzeMediaGroupsServiceTest {
    private final AnalyzeMediaGroupsService service = new AnalyzeMediaGroupsService(AnalyzeMediaGroupsServiceTest::parse);

    private static ParsedMediaName parse(String name) {
        String base = name.substring(0, Math.max(0, name.lastIndexOf('.')));
        String extension = name.substring(name.lastIndexOf('.'));
        if (base.matches(".*S\\d{1,2}E\\d{1,3}.*")) {
            boolean second = base.contains("E02");
            return new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1,
                    List.of(second ? 2 : 1), extension, 0.9, "test");
        }
        String title = base.replaceAll("[._-]+", " ").trim();
        return new ParsedMediaName(name, title.isEmpty() ? "Unknown" : title, MediaType.UNKNOWN, null, null, List.of(), extension, 0.3, "test");
    }

    @Test
    void groupsEpisodesAndAssociatesSidecarSubtitle() {
        var groups = service.analyze(List.of(
                "show/Example.Show.S01E01.mkv",
                "show/Example.Show.S01E01.zh.ass",
                "show/Example.Show.S01E02.mkv"));
        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).members().size());
        var subtitle = groups.get(0).members().stream().filter(item -> item.kind() == MediaKind.SUBTITLE).findFirst().orElseThrow();
        assertEquals("show/Example.Show.S01E01.mkv", subtitle.companionOf());
    }

    @Test
    void reportsOrphanSubtitle() {
        var group = service.analyze(List.of("subs/Unrelated.zh.ass")).get(0);
        assertTrue(group.warnings().get(0).contains("字幕"));
    }

    @Test
    void associatesArtworkAndNfoAsCompanionFiles() {
        var groups = service.analyze(List.of(
                "show/Example.Show.S01E01.mkv",
                "show/Example.Show.S01E01.jpg",
                "show/Example.Show.S01E01.nfo"));
        assertEquals(1, groups.size());
        var members = groups.get(0).members();
        assertEquals(3, members.size());
        var artwork = members.stream().filter(m -> m.kind() == MediaKind.IMAGE).findFirst().orElseThrow();
        assertEquals("show/Example.Show.S01E01.mkv", artwork.companionOf());
        var nfo = members.stream().filter(m -> m.kind() == MediaKind.NFO).findFirst().orElseThrow();
        assertEquals("show/Example.Show.S01E01.mkv", nfo.companionOf());
    }

    @Test
    void associatesDirectoryLevelArtwork() {
        var groups = service.analyze(List.of(
                "show/Example.Show.S01E01.mkv",
                "show/poster.jpg"));
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).members().size());
        var artwork = groups.get(0).members().stream().filter(m -> m.kind() == MediaKind.IMAGE).findFirst().orElseThrow();
        assertEquals("show/Example.Show.S01E01.mkv", artwork.companionOf());
    }
}
