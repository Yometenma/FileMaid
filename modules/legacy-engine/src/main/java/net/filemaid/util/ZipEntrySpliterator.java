package net.filemaid.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipEntrySpliterator
extends Spliterators.AbstractSpliterator<ZipEntry> {
    private final ZipInputStream in;
    private ZipEntry directoryEntry;
    private static final Pattern SYSTEM_FILES = Pattern.compile("[\\p{Upper}\\p{Punct}]+");

    public ZipEntrySpliterator(ZipInputStream zipInputStream) {
        super(Long.MAX_VALUE, 1297);
        this.in = zipInputStream;
    }

    @Override
    public boolean tryAdvance(Consumer<? super ZipEntry> consumer) {
        try {
            ZipEntry zipEntry = this.in.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.isDirectory()) {
                    this.directoryEntry = zipEntry;
                } else if (!(SYSTEM_FILES.matcher(zipEntry.getName()).matches() || this.directoryEntry == null || zipEntry.getSize() == 0L || zipEntry.getSize() >= 0L && zipEntry.getCompressedSize() >= zipEntry.getSize() && this.directoryEntry.getTime() >= zipEntry.getTime() && zipEntry.getExtra() == null)) {
                    consumer.accept(zipEntry);
                    return true;
                }
                zipEntry = this.in.getNextEntry();
            }
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
        return false;
    }

    public static Stream<ZipEntry> stream(ZipInputStream zipInputStream) {
        return StreamSupport.stream(new ZipEntrySpliterator(zipInputStream), false);
    }

    public static Stream<ZipEntry> stream(URL uRL) {
        try (ZipInputStream zipInputStream = new ZipInputStream(uRL.openStream(), StandardCharsets.UTF_8)) {
            return ZipEntrySpliterator.stream(zipInputStream).collect(Collectors.toList()).stream();
        } catch (Exception exception) {
            return Stream.empty();
        }
    }
}

