package net.filemaid.platform.posix;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

interface LibGIO
extends Library {
    public void g_type_init();

    public Pointer g_vfs_get_default();

    public Pointer g_vfs_get_file_for_uri(Pointer var1, String var2);

    public Pointer g_file_get_path(Pointer var1);

    public void g_free(Pointer var1);

    public void g_object_unref(Pointer var1);
}

