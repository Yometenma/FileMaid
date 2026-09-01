package net.filemaid.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.stream.IntStream;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

public class XZ {
    public static final int XZ_HEADER_SIZE = 32;

    public static ByteBuffer xz(ByteBuffer byteBuffer) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(byteBuffer.remaining());
        try (WritableByteChannel writableByteChannel = Channels.newChannel((OutputStream)new XZOutputStream((OutputStream)byteBufferOutputStream, (FilterOptions)new LZMA2Options(6)));){
            writableByteChannel.write(byteBuffer);
        }
        return byteBufferOutputStream.getByteBuffer();
    }

    public static ByteBuffer unxz(ByteBuffer byteBuffer) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(byteBuffer.remaining());
        try (XZInputStream xZInputStream = new XZInputStream((InputStream)new ByteBufferInputStream(byteBuffer));){
            byteBufferOutputStream.transferFully((InputStream)xZInputStream);
        }
        return byteBufferOutputStream.getByteBuffer();
    }

    public static boolean isXZ(ByteBuffer byteBuffer) {
        return byteBuffer.remaining() >= 32 && IntStream.range(0, org.tukaani.xz.XZ.HEADER_MAGIC.length).allMatch(n -> byteBuffer.get(n) == org.tukaani.xz.XZ.HEADER_MAGIC[n]);
    }
}

