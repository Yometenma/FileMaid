package net.filemaid.platform.posix;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.File;
import net.filemaid.platform.posix.GVFS;
import net.filemaid.platform.posix.LibGIO;

public class NativeGVFS
implements GVFS {
    private static final LibGIO lib_gio = (LibGIO)Native.loadLibrary((String)"gio-2.0", LibGIO.class);
    private static final Pointer gvfs = lib_gio.g_vfs_get_default();

    @Override
    public File getPathForURI(String string) {
        Pointer pointer = lib_gio.g_vfs_get_file_for_uri(gvfs, string);
        Pointer pointer2 = lib_gio.g_file_get_path(pointer);
        String string2 = pointer2 == null ? null : pointer2.getString(0L);
        lib_gio.g_object_unref(pointer);
        lib_gio.g_free(pointer2);
        if (string2 == null || string2.isEmpty()) {
            throw new IllegalArgumentException("Failed to locate local path: " + string);
        }
        return new File(string2);
    }

    public String toString() {
        return this.getClass().getSimpleName() + "[" + lib_gio + "]";
    }
}

