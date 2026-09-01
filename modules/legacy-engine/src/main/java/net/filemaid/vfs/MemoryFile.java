package net.filemaid.vfs;

import java.nio.ByteBuffer;

public class MemoryFile {
    private final String path;
    private final ByteBuffer data;
    public static final long LARGE_FILE_SIZE = 0x1400000L;

    public MemoryFile(String string, ByteBuffer byteBuffer) {
        this.path = string.replace('\\', '/');
        this.data = byteBuffer;
    }

    public String getName() {
        return this.path.substring(this.path.lastIndexOf(47) + 1);
    }

    public String getPath() {
        return this.path;
    }

    public int size() {
        return this.data.remaining();
    }

    public ByteBuffer getData() {
        return this.data.duplicate();
    }

    public String toString() {
        return this.path;
    }
}

