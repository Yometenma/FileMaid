package net.filemaid.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteBufferOutputStream
extends OutputStream {
    private static final int DEFAULT_INITIAL_CAPACITY = 64;
    private static final float DEFAULT_LOAD_FACTOR = 1.0f;
    private ByteBuffer buffer;
    private final float loadFactor;

    public ByteBufferOutputStream(long l) {
        this((int)l, 1.0f);
    }

    public ByteBufferOutputStream(int n) {
        this(n, 1.0f);
    }

    public ByteBufferOutputStream(int n, float f) {
        this.buffer = ByteBuffer.allocate(n > 0 ? n + 1 : 64);
        this.loadFactor = f > 0.0f ? f : 1.0f;
    }

    @Override
    public void write(int n) throws IOException {
        this.ensureCapacity(this.buffer.position() + 1);
        this.buffer.put((byte)n);
    }

    @Override
    public void write(byte[] byArray) throws IOException {
        this.ensureCapacity(this.buffer.position() + byArray.length);
        this.buffer.put(byArray);
    }

    public void write(ByteBuffer byteBuffer) throws IOException {
        this.ensureCapacity(this.buffer.position() + byteBuffer.remaining());
        this.buffer.put(byteBuffer);
    }

    @Override
    public void write(byte[] byArray, int n, int n2) throws IOException {
        this.ensureCapacity(this.buffer.position() + n2);
        this.buffer.put(byArray, n, n2);
    }

    public void ensureCapacity(int n) {
        if (n <= this.buffer.capacity()) {
            return;
        }
        int n2 = (int)((float)this.buffer.capacity() * (1.0f + this.loadFactor));
        if (n2 < n) {
            n2 = n;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(n2);
        this.buffer.flip();
        byteBuffer.put(this.buffer);
        this.buffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer byteBuffer = this.buffer.duplicate();
        byteBuffer.flip();
        return byteBuffer;
    }

    public byte[] getByteArray() {
        ByteBuffer byteBuffer = this.getByteBuffer();
        byte[] byArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(byArray);
        return byArray;
    }

    public int transferFrom(InputStream inputStream) throws IOException {
        int n = this.buffer.position();
        int n2 = 1 + inputStream.available();
        this.ensureCapacity(n + n2);
        int n3 = inputStream.read(this.buffer.array(), this.buffer.arrayOffset() + n, n2);
        if (n3 > 0) {
            this.buffer.position(n + n3);
        }
        return n3;
    }

    public int transferFully(InputStream inputStream) throws IOException {
        int n = 0;
        int n2 = 0;
        while ((n2 = this.transferFrom(inputStream)) >= 0) {
            n += n2;
        }
        return n;
    }

    public int transferFrom(ReadableByteChannel readableByteChannel) throws IOException {
        this.ensureCapacity(this.buffer.position() + 1);
        return readableByteChannel.read(this.buffer);
    }

    public int transferFully(ReadableByteChannel readableByteChannel) throws IOException {
        int n = 0;
        int n2 = 0;
        while ((n2 = this.transferFrom(readableByteChannel)) >= 0) {
            n += n2;
        }
        return n;
    }

    public int position() {
        return this.buffer.position();
    }

    public int capacity() {
        return this.buffer.capacity();
    }

    public void rewind() {
        this.buffer.rewind();
    }
}

