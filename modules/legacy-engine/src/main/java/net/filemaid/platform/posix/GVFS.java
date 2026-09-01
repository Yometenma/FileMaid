package net.filemaid.platform.posix;

import java.io.File;
import net.filemaid.platform.posix.NativeGVFS;
import net.filemaid.platform.posix.PlatformGVFS;
import net.filemaid.util.SystemProperty;

public interface GVFS {
    public File getPathForURI(String var1);

    public static GVFS getDefaultVFS() {
        return SystemProperty.optional("net.filemaid.gio.GVFS", string -> (GVFS)new PlatformGVFS(new File((String)string))).orElseGet(NativeGVFS::new);
    }
}

