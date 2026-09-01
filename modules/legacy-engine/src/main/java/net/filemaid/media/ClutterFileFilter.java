package net.filemaid.media;

import java.io.File;
import java.io.FileFilter;
import java.time.Duration;
import java.util.regex.Pattern;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.util.FileUtilities;

public class ClutterFileFilter
implements FileFilter {
    private final Pattern namePattern;
    private final long maxFileSize;
    private final long maxVideoLength;

    public ClutterFileFilter(Pattern pattern, long l, Duration duration) {
        this.namePattern = pattern;
        this.maxFileSize = l;
        this.maxVideoLength = duration.toMillis();
    }

    private boolean find(String string) {
        return string != null && this.namePattern.matcher(string).find();
    }

    @Override
    public boolean accept(File file) {
        String string = file.getName();
        if (FileUtilities.getExtension(string) == null) {
            return file.isFile() && !file.isHidden();
        }
        if (this.find(FileUtilities.getNameWithoutExtension(string)) || this.find(file.getParent())) {
            return !(!file.isFile() || this.maxFileSize > 0L && file.length() >= this.maxFileSize || this.maxVideoLength > 0L && CachedMediaCharacteristics.getMediaCharacteristics(file, mediaCharacteristics -> mediaCharacteristics.getDuration().toMillis() < this.maxVideoLength).orElse(true) == false);
        }
        return false;
    }
}

