package net.filemaid.util;

import java.io.File;
import java.util.Locale;

public class FileKey {
    private final File file;
    private final long lastModified;

    public FileKey(File file, long l) {
        this.file = file;
        this.lastModified = l;
    }

    public File getFile() {
        return this.file;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public boolean equals(Object object) {
        if (object instanceof FileKey) {
            FileKey fileKey = (FileKey)object;
            return this.lastModified == fileKey.lastModified && this.file.equals(fileKey.file);
        }
        return false;
    }

    public int hashCode() {
        return this.file.hashCode();
    }

    public String toString() {
        return String.format(Locale.ROOT, "%s [Last-Modified: %tc]", this.file, this.lastModified);
    }

    public static FileKey of(File file) {
        return new FileKey(file, file.lastModified());
    }
}

