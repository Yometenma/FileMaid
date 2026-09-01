package net.filemaid.postprocess;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.HistorySpooler;
import net.filemaid.RenameAction;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public interface ApplyHistory
extends Apply {
    public void applyHistory(Map<File, File> var1, RenameAction var2, Feedback var3) throws Exception;

    @Override
    default public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>(map.size());
        map.forEach((file, match) -> {
            File file2 = (File)match.getValue();
            if (file2 == null || !file2.isAbsolute()) {
                file2 = HistorySpooler.HISTORY.getOriginalPath((File)file);
            }
            if (file2 == null) {
                feedback.trace("No original location", file);
                return;
            }
            linkedHashMap.put((File)file, file2);
        });
        this.applyHistory(linkedHashMap, renameAction, feedback);
    }
}

