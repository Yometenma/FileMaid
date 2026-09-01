package net.filemaid.media;

import java.io.File;
import net.filemaid.MediaTypes;
import net.filemaid.media.FFProbe;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.util.SystemProperty;

public enum MediaCharacteristicsParser {
    mediainfo{

        @Override
        public MediaCharacteristics open(File file) throws Exception {
            return MediaInfoTable.read(file);
        }
    }
    ,
    ffprobe{

        @Override
        public MediaCharacteristics open(File file) throws Exception {
            return FFProbe.read(file);
        }
    }
    ,
    none{

        @Override
        public MediaCharacteristics open(File file) throws Exception {
            throw new UnsupportedOperationException("MediaCharacteristicsParser::open");
        }

        @Override
        public boolean acceptVideoFile(File file) {
            return false;
        }

        @Override
        public boolean canRead() {
            return false;
        }
    };

    private static final MediaCharacteristicsParser INSTANCE;

    public abstract MediaCharacteristics open(File var1) throws Exception;

    public boolean acceptVideoFile(File file) {
        return MediaTypes.VIDEO_FILES.accept(file) && file.length() > 1000000L;
    }

    public boolean canRead() {
        return true;
    }

    public static MediaCharacteristicsParser getDefault() {
        return INSTANCE;
    }

    static {
        INSTANCE = SystemProperty.get("net.filemaid.media.parser", MediaCharacteristicsParser::valueOf, mediainfo);
    }
}

