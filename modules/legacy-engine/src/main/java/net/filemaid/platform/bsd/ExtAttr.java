package net.filemaid.platform.bsd;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI;
import java.nio.ByteBuffer;

public interface ExtAttr
extends Library {
    public static final ExtAttr INSTANCE = (ExtAttr)Native.load(null, ExtAttr.class);
    public static final int EXTATTR_NAMESPACE_USER = 1;

    public LibCAPI.ssize_t extattr_get_file(String var1, int var2, String var3, ByteBuffer var4, LibCAPI.size_t var5);

    public LibCAPI.ssize_t extattr_set_file(String var1, int var2, String var3, ByteBuffer var4, LibCAPI.size_t var5);

    public int extattr_delete_file(String var1, int var2, String var3);

    public LibCAPI.ssize_t extattr_list_file(String var1, int var2, ByteBuffer var3, LibCAPI.size_t var4);
}

