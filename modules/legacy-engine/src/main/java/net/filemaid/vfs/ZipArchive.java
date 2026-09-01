package net.filemaid.vfs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.vfs.MemoryFile;

public class ZipArchive
implements Iterable<MemoryFile> {
    private final ByteBuffer data;

    public ZipArchive(ByteBuffer byteBuffer) {
        this.data = byteBuffer.duplicate();
    }

    @Override
    public Iterator<MemoryFile> iterator() {
        try {
            return this.extract().iterator();
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
    }

    public List<MemoryFile> extract() throws IOException {
        ArrayList<MemoryFile> arrayList = new ArrayList<MemoryFile>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteBufferInputStream(this.data.duplicate()));){
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) continue;
                ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(zipEntry.getSize());
                byteBufferOutputStream.transferFully(zipInputStream);
                arrayList.add(new MemoryFile(zipEntry.getName(), byteBufferOutputStream.getByteBuffer()));
            }
        }
        return arrayList;
    }
}

