package net.filemaid.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import net.filemaid.util.FileUtilities;

public class APFS {
    public static void copyFile(String string, String string2, int n) throws IOException {
        int n2 = LibC.INSTANCE.copyfile(string, string2, 0, n);
        if (n2 != 0) {
            throw new IOException(APFS.getErrorMessage(Native.getLastError()));
        }
    }

    public static void cloneFile(String string, String string2) throws IOException {
        int n = LibC.INSTANCE.clonefile(string, string2, 0);
        if (n != 0) {
            throw new IOException(APFS.getErrorMessage(Native.getLastError()));
        }
    }

    public static String getErrorMessage(int n) {
        switch (n) {
            case 13: {
                return "Permission denied";
            }
            case 45: {
                return "Operation not supported";
            }
            case 17: {
                return "File exists";
            }
            case 18: {
                return "Cross-device link";
            }
            case 22: {
                return "Invalid argument";
            }
            case 28: {
                return "No space left on device";
            }
            case 5: {
                return "Input/output error";
            }
            case 1: {
                return "Operation not permitted";
            }
            case 62: {
                return "Too many levels of symbolic links";
            }
            case 30: {
                return "Read-only file system";
            }
            case 63: {
                return "File name too long";
            }
            case 2: {
                return "No such file or directory";
            }
            case 20: {
                return "Not a directory";
            }
        }
        return "Error " + n;
    }

    public static File copy(File file, File file2) throws IOException {
        APFS.copyFile(file.getPath(), file2.getPath(), 151027724);
        return file2;
    }

    public static File clone(File file, File file2) throws IOException {
        APFS.cloneFile(file.getPath(), file2.getPath());
        return file2;
    }

    public static boolean cloneable(File file, File file2) {
        FileStore fileStore = FileUtilities.getFileStore(file);
        if (fileStore == null) {
            return false;
        }
        String string = fileStore.type();
        if ("apfs".equals(string)) {
            FileStore fileStore2 = FileUtilities.getFileStore(file2);
            if (fileStore2 == null) {
                return false;
            }
            return fileStore.equals(fileStore2);
        }
        return false;
    }

    public static interface LibC
    extends Library {
        public static final LibC INSTANCE = (LibC)Native.load(null, LibC.class);
        public static final int EPERM = 1;
        public static final int ENOENT = 2;
        public static final int EIO = 5;
        public static final int EACCES = 13;
        public static final int ENOTDIR = 20;
        public static final int EEXIST = 17;
        public static final int EXDEV = 18;
        public static final int EINVAL = 22;
        public static final int ENOSPC = 28;
        public static final int EROFS = 30;
        public static final int ENOTSUP = 45;
        public static final int ELOOP = 62;
        public static final int ENAMETOOLONG = 63;
        public static final int CLONE_NOFOLLOW = 1;
        public static final int CLONE_NOOWNERCOPY = 2;
        public static final int COPYFILE_ACL = 1;
        public static final int COPYFILE_STAT = 2;
        public static final int COPYFILE_XATTR = 4;
        public static final int COPYFILE_DATA = 8;
        public static final int COPYFILE_RECURSIVE = 32768;
        public static final int COPYFILE_EXCL = 131072;
        public static final int COPYFILE_NOFOLLOW_SRC = 262144;
        public static final int COPYFILE_NOFOLLOW_DST = 524288;
        public static final int COPYFILE_MOVE = 0x100000;
        public static final int COPYFILE_UNLINK = 0x200000;
        public static final int COPYFILE_CLONE = 0x1000000;
        public static final int COPYFILE_CLONE_FORCE = 0x2000000;
        public static final int COPYFILE_DATA_SPARSE = 0x8000000;

        public int clonefile(String var1, String var2, int var3);

        public int copyfile(String var1, String var2, int var3, int var4);
    }
}

