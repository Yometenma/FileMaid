package net.filemaid.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.LongSummaryStatistics;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;

public class ZipUtilities {
    public static ByteBuffer zip(File ... fileArray) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream((OutputStream)byteBufferOutputStream, StandardCharsets.UTF_8);){
            for (File file : fileArray) {
                ZipUtilities.zip(file, "", zipOutputStream, FileUtilities.NOT_HIDDEN);
            }
        }
        return byteBufferOutputStream.getByteBuffer();
    }

    public static void zip(Iterable<File> iterable, FileFilter fileFilter, File file) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream((OutputStream)new BufferedOutputStream(new FileOutputStream(file), 0x400000), StandardCharsets.UTF_8);){
            for (File file2 : iterable) {
                ZipUtilities.zip(file2, "", zipOutputStream, fileFilter);
            }
        }
    }

    private static void zip(File file, String string, ZipOutputStream zipOutputStream, FileFilter fileFilter) throws IOException {
        if (file.isDirectory()) {
            for (File file2 : FileUtilities.getChildren(file)) {
                ZipUtilities.zip(file2, string + file.getName() + "/", zipOutputStream, fileFilter);
            }
        } else if (fileFilter.accept(file)) {
            ZipEntry zipEntry = new ZipEntry(string + file.getName());
            zipEntry.setSize(file.length());
            zipEntry.setTime(file.lastModified());
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(FileUtilities.readFile(file));
            zipOutputStream.closeEntry();
        }
    }

    public static void unzip(File file, BiConsumer<ZipEntry, ByteBuffer> biConsumer) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream((InputStream)new FileInputStream(file), StandardCharsets.UTF_8);){
            ZipUtilities.unzip(zipInputStream, biConsumer);
        }
    }

    public static void unzip(ZipInputStream zipInputStream, BiConsumer<ZipEntry, ByteBuffer> biConsumer) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(zipEntry.getSize());
            byteBufferOutputStream.transferFully(zipInputStream);
            biConsumer.accept(zipEntry, byteBufferOutputStream.getByteBuffer());
            zipEntry = zipInputStream.getNextEntry();
        }
    }

    public static String summaryStatistics(File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file);){
            LongSummaryStatistics longSummaryStatistics = zipFile.stream().mapToLong(ZipEntry::getSize).summaryStatistics();
            String string = String.format(Locale.ROOT, "%s (%s files, %s compressed, %s uncompressed)", file, longSummaryStatistics.getCount(), FileUtilities.formatSize(file.length()), FileUtilities.formatSize(longSummaryStatistics.getSum()));
            return string;
        }
    }

    public static boolean isZipFile(ByteBuffer byteBuffer) {
        return byteBuffer.remaining() >= 4 && byteBuffer.get(byteBuffer.position()) == 80 && byteBuffer.get(byteBuffer.position() + 1) == 75 && byteBuffer.get(byteBuffer.position() + 2) == 3 && byteBuffer.get(byteBuffer.position() + 3) == 4;
    }
}

