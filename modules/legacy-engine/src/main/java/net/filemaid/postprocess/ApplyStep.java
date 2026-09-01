package net.filemaid.postprocess;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.RenameAction;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public interface ApplyStep
extends Apply {
    @Override
    default public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) {
        List<Map.Entry> list = map.entrySet().stream().filter(entry -> this.accept((File)entry.getKey(), ((Match)entry.getValue()).getCandidate())).collect(Collectors.toList());
        int n = 0;
        int n2 = list.size();
        for (Map.Entry entry2 : list) {
            File file = (File)((Match)entry2.getValue()).getValue();
            File file2 = (File)entry2.getKey();
            Object Candidate = ((Match)entry2.getValue()).getCandidate();
            if (feedback.isCancelled()) {
                throw new CancellationException();
            }
            feedback.progress(n++, n2);
            try {
                this.apply(file, file2, Candidate, feedback);
            }
            catch (CancellationException cancellationException) {
                throw cancellationException;
            }
            catch (Exception exception) {
                feedback.warning(Logging.cause(exception), file2);
            }
        }
    }

    public boolean accept(File var1, Object var2);

    public void apply(File var1, File var2, Object var3, Feedback var4) throws Exception;
}

