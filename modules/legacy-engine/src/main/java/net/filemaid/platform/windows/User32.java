package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface User32
extends com.sun.jna.platform.win32.User32 {
    public static final User32 INSTANCE = (User32)Native.load((String)"user32", User32.class, (Map)W32APIOptions.DEFAULT_OPTIONS);
    public static final int GCLP_HBRBACKGROUND = -10;

    public WinDef.ULONG SetClassLongPtr(Pointer var1, int var2, Pointer var3);
}

