package net.filemaid.postprocess;

import java.io.File;
import net.filemaid.MetaAttributeView;
import net.filemaid.media.MetaAttributes;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.PlainFileXattrView;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

public class XattrFolder
implements ApplyMetadata {
    private final File store;

    public XattrFolder(String string) {
        this.store = new File(string);
    }

    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        this.setMetadata(file, file2, movie, feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        this.setMetadata(file, file2, episode, feedback);
    }

    public void setMetadata(File file, File file2, Object object, Feedback feedback) throws Exception {
        PlainFileXattrView plainFileXattrView = new PlainFileXattrView(file2, this.store);
        feedback.info(object, plainFileXattrView);
        MetaAttributes metaAttributes = new MetaAttributes(new MetaAttributeView(plainFileXattrView), null);
        metaAttributes.setObject(object);
        metaAttributes.setOriginalName(file.getName());
    }
}

