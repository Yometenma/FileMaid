package net.filemaid.postprocess;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import net.filemaid.RenameAction;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public class SetLastModified
implements Apply {
    private final TemporalUnit granularity;

    public SetLastModified(TemporalUnit temporalUnit) {
        this.granularity = temporalUnit;
    }

    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        Instant instant = Instant.now().truncatedTo(this.granularity);
        MediaFileUtilities.walkStructureTree(map.keySet()).forEach(file -> this.touch((File)file, instant, feedback));
    }

    private void touch(File file, Instant instant, Feedback feedback) {
        feedback.info(instant, file);
        try {
            Files.setLastModifiedTime(file.toPath(), FileTime.from(instant));
        }
        catch (Exception exception) {
            feedback.warning(exception, file);
        }
    }
}

