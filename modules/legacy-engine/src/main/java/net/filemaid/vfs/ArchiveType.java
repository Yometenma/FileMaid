package net.filemaid.vfs;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.logging.Level;
import net.filemaid.Logging;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.vfs.ZipArchive;

public enum ArchiveType {
    ZIP{

        @Override
        public Iterable<MemoryFile> fromData(ByteBuffer byteBuffer) {
            return new ZipArchive(byteBuffer);
        }
    }
    ,
    UNDEFINED{

        @Override
        public Iterable<MemoryFile> fromData(ByteBuffer byteBuffer) {
            for (ArchiveType archiveType : EnumSet.of(ZIP)) {
                try {
                    Iterable<MemoryFile> iterable = archiveType.fromData(byteBuffer);
                    if (!iterable.iterator().hasNext()) continue;
                    return iterable;
                }
                catch (Exception exception) {
                    Logging.debug.log(Level.WARNING, exception, exception::toString);
                }
            }
            return Collections.emptySet();
        }
    }
    ,
    UNKOWN{

        @Override
        public Iterable<MemoryFile> fromData(ByteBuffer byteBuffer) {
            return Collections.emptySet();
        }
    };


    public abstract Iterable<MemoryFile> fromData(ByteBuffer var1);

    public static ArchiveType forName(String string) {
        if (string == null) {
            return UNDEFINED;
        }
        if ("zip".equalsIgnoreCase(string)) {
            return ZIP;
        }
        return UNKOWN;
    }
}

