package net.filemaid.postprocess;

import java.io.File;
import java.util.Map;
import net.filemaid.RenameAction;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public interface Apply {
    public void apply(Map<File, Match<File, ?>> var1, RenameAction var2, Feedback var3) throws Exception;
}

