package net.filemaid.vfs;

import java.io.File;
import java.util.Objects;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.FileInfo;

public class SimpleFileInfo
implements FileInfo,
Comparable<FileInfo> {
    protected String path;
    protected long length;

    public SimpleFileInfo() {
    }

    public SimpleFileInfo(File file) {
        this.path = file.getPath();
        this.length = file.length();
    }

    public SimpleFileInfo(String string, long l) {
        this.path = string;
        this.length = l;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getName() {
        return FileUtilities.getNameWithoutExtension(new File(this.path).getName());
    }

    @Override
    public String getType() {
        return FileUtilities.getExtension(this.path);
    }

    @Override
    public long getLength() {
        return this.length;
    }

    public int hashCode() {
        return Objects.hash(this.getPath(), this.getLength());
    }

    public boolean equals(Object object) {
        if (object instanceof FileInfo) {
            FileInfo fileInfo = (FileInfo)object;
            return fileInfo.getLength() == this.getLength() && fileInfo.getPath().equals(this.getPath());
        }
        return false;
    }

    @Override
    public int compareTo(FileInfo fileInfo) {
        return this.getPath().compareTo(fileInfo.getPath());
    }

    public String toString() {
        return this.getPath();
    }

    @Override
    public File toFile() {
        return new File(this.path);
    }
}

