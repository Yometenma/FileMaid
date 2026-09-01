package net.filemaid.platform.posix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import net.filemaid.util.FileUtilities;

public class BTRFS {
    public static int rename(File file, File file2) throws IOException {
        int n = LibC.INSTANCE.rename(file.getPath(), file2.getPath());
        if (n == 0) {
            return 0;
        }
        int n2 = Native.getLastError();
        if (n2 == 18) {
            return 18;
        }
        throw new IOException(BTRFS.getErrorMessage(n2));
    }

    public static void moveFile(File file, File file2) throws IOException {
        BTRFS.cloneFile(file, file2);
        int n = LibC.INSTANCE.unlink(file.getPath());
        if (n != 0) {
            throw new IOException(BTRFS.getErrorMessage(Native.getLastError()));
        }
    }

    public static void cloneFile(File file, File file2) throws IOException {
        int n = LibC.INSTANCE.open(file.getPath(), 0);
        int n2 = LibC.INSTANCE.open(file2.getPath(), 577, 420);
        try {
            int n3 = LibC.INSTANCE.ioctl(n2, LibC.FICLONE, n);
            if (n3 != 0) {
                throw new IOException(BTRFS.getErrorMessage(Native.getLastError()));
            }
        }
        catch (IOException iOException) {
            if (LibC.INSTANCE.lseek(n2, 0L, 2) == 0L) {
                LibC.INSTANCE.unlink(file2.getPath());
            }
            throw iOException;
        }
        finally {
            LibC.INSTANCE.close(n2);
            LibC.INSTANCE.close(n);
        }
    }

    public static String getErrorMessage(int n) {
        switch (n) {
            case 18: {
                return "Cross-device link";
            }
            case 13: {
                return "Permission denied";
            }
            case 1: {
                return "Operation not permitted";
            }
            case 95: {
                return "Operation not supported";
            }
            case 9: {
                return "Bad file number";
            }
            case 22: {
                return "Invalid argument";
            }
            case 21: {
                return "Is a directory";
            }
        }
        return "Error " + n;
    }

    public static File clone(File file3, File file4) throws IOException {
        return FileUtilities.mirror(file3, file4, (file, file2) -> {
            BTRFS.cloneFile(file, file2);
            return file2;
        });
    }

    public static File move(File file3, File file4) throws IOException {
        int n = BTRFS.rename(file3, file4);
        if (n == 0) {
            return file4;
        }
        if (n == 18) {
            file4 = FileUtilities.mirror(file3, file4, (file, file2) -> {
                BTRFS.moveFile(file, file2);
                return file2;
            });
            if (file3.isDirectory()) {
                try {
                    FileUtilities.delete(file3);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            return file4;
        }
        throw new IOException(BTRFS.getErrorMessage(18));
    }

    public static boolean cloneable(File file, File file2) {
        FileStore fileStore = FileUtilities.getFileStore(file);
        if (fileStore == null) {
            return false;
        }
        String string = fileStore.type();
        if ("btrfs".equals(string) || "xfs".equals(string) || "bcachefs".equals(string)) {
            FileStore fileStore2 = FileUtilities.getFileStore(file2);
            if (fileStore2 == null) {
                return false;
            }
            return string.equals(fileStore2.type());
        }
        return false;
    }

    public static interface LibC
    extends Library {
        public static final LibC INSTANCE = (LibC)Native.load(null, LibC.class);
        public static final int SEEK_SET = 0;
        public static final int SEEK_CUR = 1;
        public static final int SEEK_END = 2;
        public static final int _IOC_NRBITS = 8;
        public static final int _IOC_TYPEBITS = 8;
        public static final int _IOC_SIZEBITS = 14;
        public static final int _IOC_NRSHIFT = 0;
        public static final int _IOC_TYPESHIFT = 8;
        public static final int _IOC_SIZESHIFT = 16;
        public static final int _IOC_DIRSHIFT = 30;
        public static final int _IOC_WRITE = 1;
        public static final int FICLONE = LibC._IOW(148, 9, Integer.TYPE);

        public int rename(String var1, String var2);

        public int ioctl(int var1, int var2, int var3);

        public int open(String var1, int var2);

        public int open(String var1, int var2, int var3);

        public long lseek(int var1, long var2, int var4);

        public int unlink(String var1);

        public int close(int var1);

        public static int _IOC(int n, int n2, int n3, int n4) {
            return n << 30 | n2 << 8 | n3 << 0 | n4 << 16;
        }

        public static int _IOW(int n, int n2, Class<?> clazz) {
            return LibC._IOC(1, n, n2, Native.getNativeSize(clazz));
        }
    }
}

