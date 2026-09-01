package net.filemaid.media;

import java.io.File;
import java.io.IOException;
import net.filemaid.Execute;
import net.filemaid.Logging;
import net.filemaid.mediainfo.MediaInfo;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.SystemProperty;

public abstract class MediaInfoTool {
    protected final String binary;
    public static final MediaInfoTool INSTANCE = SystemProperty.get("net.filemaid.mediainfo", Command::new, new Library());

    public abstract String raw(File var1) throws IOException;

    public abstract String version() throws IOException;

    public MediaInfoTool(String string) {
        this.binary = string;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(this.binary);
        try {
            stringBuilder.append(" ").append(this.version().replaceAll("[^.0-9]", ""));
        }
        catch (Throwable throwable) {
            stringBuilder.append(" [").append(Logging.cause(throwable)).append("]");
        }
        return stringBuilder.toString();
    }

    public static class Library
    extends MediaInfoTool {
        public Library() {
            super("libmediainfo");
        }

        @Override
        public String raw(File file) throws IOException {
            try (MediaInfo mediaInfo = new MediaInfo();){
                long l = mediaInfo.read(file, 65536);
                if (l <= 0L) {
                    String string = "";
                    return string;
                }
                Logging.debug.finest(() -> "Read media file: " + file + " (" + FileUtilities.formatSize(l) + " of " + FileUtilities.formatSize(file.length()) + ")");
                String string = mediaInfo.raw();
                return string;
            }
        }

        @Override
        public String version() {
            return MediaInfo.version();
        }
    }

    public static class Command
    extends MediaInfoTool {
        public Command(String string) {
            super(string);
        }

        @Override
        public String raw(File file) throws IOException {
            return Execute.execute(this.binary, "--Full", "--Language=raw", "--", file.getAbsolutePath()).toString();
        }

        @Override
        public String version() throws IOException {
            return Execute.execute(this.binary, "--Version").toString();
        }
    }
}

