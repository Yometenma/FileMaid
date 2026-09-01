package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface GDI32
extends com.sun.jna.platform.win32.GDI32 {
    public static final GDI32 INSTANCE = (GDI32)Native.load((String)"gdi32", GDI32.class, (Map)W32APIOptions.DEFAULT_OPTIONS);

    public Pointer CreateSolidBrush(int var1);
}

