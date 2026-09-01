package net.filemaid.platform.windows;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface Kernel32
extends com.sun.jna.platform.win32.Kernel32 {
    public static final Kernel32 INSTANCE = (Kernel32)Native.load((String)"kernel32", Kernel32.class, (Map)W32APIOptions.DEFAULT_OPTIONS);

    public String GetCommandLineW();

    public boolean SetConsoleCtrlHandler(Callback var1, boolean var2);
}

