package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface Dwmapi
extends StdCallLibrary {
    public static final Dwmapi INSTANCE = (Dwmapi)Native.load((String)"dwmapi", Dwmapi.class, (Map)W32APIOptions.DEFAULT_OPTIONS);
    public static final int DWMWA_TRANSITIONS_FORCEDISABLED = 3;
    public static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    public static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    public static final int DWMWA_BORDER_COLOR = 34;
    public static final int DWMWA_CAPTION_COLOR = 35;
    public static final int DWMWA_TEXT_COLOR = 36;
    public static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    public static final int DWMWCP_DEFAULT = 0;
    public static final int DWMWCP_DONOTROUND = 1;
    public static final int DWMWCP_ROUND = 2;
    public static final int DWMWCP_ROUNDSMALL = 3;
    public static final int DWMWA_COLOR_DEFAULT = -1;
    public static final int DWMWA_COLOR_NONE = -2;
    public static final int DWMSBT_AUTO = 0;
    public static final int DWMSBT_NONE = 1;
    public static final int DWMSBT_MAINWINDOW = 2;
    public static final int DWMSBT_TRANSIENTWINDOW = 3;
    public static final int DWMSBT_TABBEDWINDOW = 4;
    public static final int DWM_BB_ENABLE = 1;

    public int DwmSetWindowAttribute(Pointer var1, int var2, PointerType var3, int var4);

    public int DwmExtendFrameIntoClientArea(Pointer var1, MARGINS var2);

    public boolean DwmEnableBlurBehindWindow(Pointer var1, DWM_BLURBEHIND var2);

    @Structure.FieldOrder(value={"dwFlags", "fEnable", "hRgnBlur", "fTransitionOnMaximized"})
    public static class DWM_BLURBEHIND
    extends Structure {
        public int dwFlags;
        public boolean fEnable;
        public IntByReference hRgnBlur;
        public boolean fTransitionOnMaximized;
    }

    @Structure.FieldOrder(value={"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
    public static class MARGINS
    extends Structure {
        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight;
        public int cyBottomHeight;
    }
}

