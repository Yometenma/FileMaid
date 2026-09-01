package net.filemaid.postprocess;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import net.filemaid.UserFiles;
import net.filemaid.postprocess.ApplyStep;
import net.filemaid.postprocess.Feedback;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.FileUtilities;

public class TranscodeSubtitles
implements ApplyStep {
    private final SubtitleFormat format;
    private final Charset encoding;

    public TranscodeSubtitles(SubtitleFormat subtitleFormat, Charset charset) {
        this.format = subtitleFormat;
        this.encoding = charset;
    }

    @Override
    public boolean accept(File file, Object object) {
        for (SubtitleFormat subtitleFormat : SubtitleFormat.values()) {
            if (!subtitleFormat.getFilter().accept(file)) continue;
            return true;
        }
        return false;
    }

    @Override
    public void apply(File file, File file2, Object object, Feedback feedback) throws Exception {
        String string = FileUtilities.getNameWithoutExtension(file2.getName());
        File file3 = new File(file2.getParentFile(), string + "." + this.format.getFilter().extension());
        ByteBuffer byteBuffer = SubtitleUtilities.exportSubtitles(SubtitleUtilities.readSubtitleFile(file2), this.format, this.encoding);
        if (file3.exists()) {
            UserFiles.trash(file3);
        }
        feedback.file("Transcode " + file2.getName(), file3);
        FileUtilities.writeFile(byteBuffer, file3);
    }
}

