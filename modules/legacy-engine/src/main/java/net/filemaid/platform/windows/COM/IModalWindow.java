package net.filemaid.platform.windows.COM;

import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

public interface IModalWindow
extends IUnknown {
    public static final Guid.IID IID_IMODALWINDOW = new Guid.IID("{b4db1657-70d7-485e-8e3e-6fcb5a5c1802}");

    public WinNT.HRESULT Show(WinDef.HWND var1);
}

