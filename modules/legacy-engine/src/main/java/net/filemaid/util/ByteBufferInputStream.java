package net.filemaid.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream
extends InputStream {
    private final ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer byteBuffer) {
        this.buffer = byteBuffer;
    }

    @Override
    public int read() throws IOException {
        return this.buffer.position() < this.buffer.limit() ? this.buffer.get() & 0xFF : -1;
    }

    @Override
    public int read(byte[] byArray, int n, int n2) throws IOException {
        if (byArray == null) {
            throw new NullPointerException();
        }
        if (n < 0 || n2 < 0 || n2 > byArray.length - n) {
            throw new IndexOutOfBoundsException();
        }
        if (this.buffer.position() >= this.buffer.limit()) {
            return -1;
        }
        if (n2 > this.buffer.remaining()) {
            n2 = this.buffer.remaining();
        }
        if (n2 <= 0) {
            return 0;
        }
        this.buffer.get(byArray, n, n2);
        return n2;
    }

    @Override
    public int available() throws IOException {
        return this.buffer.remaining();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int n) {
        this.buffer.mark();
    }

    @Override
    public void reset() throws IOException {
        this.buffer.reset();
    }
}

