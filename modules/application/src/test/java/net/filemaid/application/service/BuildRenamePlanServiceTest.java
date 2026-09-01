package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RenameOperation;
import org.junit.jupiter.api.Test;

class BuildRenamePlanServiceTest {
    @Test
    void buildsImmutablePlanWithConfirmationToken() {
        var previewService = new RenamePreviewService(
                name -> new ParsedMediaName(name, "Example Show", MediaType.EPISODE, null, 1, List.of(2), ".mkv", 0.9, "test"),
                media -> "TV Shows/" + media.title() + "/Season 01/" + media.title() + " - S01E02.mkv",
                null);
        var service = new BuildRenamePlanService(previewService);

        var plan = service.build(null, List.of("incoming/Example.Show.S01E02.mkv"), List.of(), RenameOperation.OperationType.MOVE);

        assertNotNull(plan.id());
        assertNotNull(plan.createdAt());
        assertEquals(1, plan.operations().size());
        assertEquals(RenameOperation.OperationType.MOVE, plan.operations().get(0).type());
        assertEquals(Path.of("incoming/Example.Show.S01E02.mkv").toString(), plan.operations().get(0).source().toString());
        assertEquals(Path.of("TV Shows/Example Show/Season 01/Example Show - S01E02.mkv").toString(), plan.operations().get(0).target().toString());
    }
}
