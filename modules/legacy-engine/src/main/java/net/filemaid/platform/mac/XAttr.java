package net.filemaid.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.nio.ByteBuffer;

public interface XAttr
extends Library {
    public static final XAttr INSTANCE = (XAttr)Native.load(null, XAttr.class);
    public static final int XATTR_FOLLOW = 0;
    public static final int XATTR_NOFOLLOW = 1;
    public static final int XATTR_CREATE = 2;
    public static final int XATTR_REPLACE = 4;
    public static final int XATTR_NOSECURITY = 8;
    public static final int XATTR_NODEFAULT = 16;
    public static final int XATTR_SHOWCOMPRESSION = 32;
    public static final int XATTR_MAXNAMELEN = 127;
    public static final String XATTR_FINDERINFO_NAME = "com.apple.FinderInfo";
    public static final String XATTR_RESOURCEFORK_NAME = "com.apple.ResourceFork";

    public long getxattr(String var1, String var2, ByteBuffer var3, long var4, int var6, int var7);

    public int setxattr(String var1, String var2, ByteBuffer var3, long var4, int var6, int var7);

    public int removexattr(String var1, String var2, int var3);

    public long listxattr(String var1, ByteBuffer var2, long var3, int var5);
}

