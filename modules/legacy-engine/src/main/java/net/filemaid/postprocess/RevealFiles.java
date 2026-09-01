package net.filemaid.postprocess;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Map;
import net.filemaid.RenameAction;
import net.filemaid.UserInteraction;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public class RevealFiles
implements Apply {
    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        UserInteraction.revealFiles(map.keySet());
    }
}

