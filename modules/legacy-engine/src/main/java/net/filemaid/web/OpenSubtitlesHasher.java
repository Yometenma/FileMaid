package net.filemaid.web;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class OpenSubtitlesHasher {
    public static final int HASH_CHUNK_SIZE = 65536;

    public static String computeHashNIO(File file) throws IOException {
        long l = file.length();
        long l2 = Math.min(65536L, l);
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);){
            long l3 = OpenSubtitlesHasher.computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, l2));
            long l4 = OpenSubtitlesHasher.computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, Math.max(l - 65536L, 0L), l2));
            String string = String.format(Locale.ROOT, "%016x", l + l3 + l4);
            return string;
        }
    }

    public static String computeHash(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file);){
            String string = OpenSubtitlesHasher.computeHash(fileInputStream, file.length());
            return string;
        }
    }

    public static String computeHash(InputStream inputStream, long l) throws IOException {
        int n = (int)Math.min(65536L, l);
        byte[] byArray = new byte[(int)Math.min(131072L, l)];
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readFully(byArray, 0, n);
        long l2 = n;
        long l3 = l - (long)n;
        while (l2 < l3 && (l2 += dataInputStream.skip(l3 - l2)) >= 0L) {
        }
        dataInputStream.readFully(byArray, n, byArray.length - n);
        long l4 = OpenSubtitlesHasher.computeHashForChunk(ByteBuffer.wrap(byArray, 0, n));
        long l5 = OpenSubtitlesHasher.computeHashForChunk(ByteBuffer.wrap(byArray, byArray.length - n, n));
        return String.format(Locale.ROOT, "%016x", l + l4 + l5);
    }

    private static long computeHashForChunk(ByteBuffer byteBuffer) {
        LongBuffer longBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        long l = 0L;
        while (longBuffer.hasRemaining()) {
            l += longBuffer.get();
        }
        return l;
    }
}

