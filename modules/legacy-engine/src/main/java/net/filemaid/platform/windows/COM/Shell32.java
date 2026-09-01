package net.filemaid.platform.windows.COM;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface Shell32
extends com.sun.jna.platform.win32.Shell32 {
    public static final Shell32 INSTANCE = (Shell32)Native.load((String)"shell32", Shell32.class, (Map)W32APIOptions.DEFAULT_OPTIONS);
    public static final int FOFX_PREFERHARDLINK = 131072;
    public static final int FOFX_NOCOPYHOOKS = 0x800000;
    public static final int FOFX_NOMINIMIZEBOX = 0x1000000;

    public WinNT.HRESULT SHCreateItemFromParsingName(WString var1, Pointer var2, Guid.REFIID var3, PointerByReference var4);

    public WinNT.HRESULT SHParseDisplayName(WString var1, Pointer var2, PointerByReference var3, WinDef.ULONG var4, Pointer var5);

    public WinNT.HRESULT SHOpenFolderAndSelectItems(Pointer var1, int var2, Pointer[] var3, int var4);

    public Pointer ILCreateFromPath(String var1);

    public void ILFree(Pointer var1);
}

