package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.filemaid.core.model.MediaKind;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;
import org.junit.jupiter.api.Test;

class AnalyzeMediaGroupsServiceTest {
    private final AnalyzeMediaGroupsService service = new AnalyzeMediaGroupsService(name -> {
        boolean second = name.contains("E02");
        return new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1,
                List.of(second ? 2 : 1), name.substring(name.lastIndexOf('.')), 0.9, "test");
    });

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
}
