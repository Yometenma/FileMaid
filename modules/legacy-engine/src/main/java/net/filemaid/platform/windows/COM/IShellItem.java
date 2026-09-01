package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface IShellItem
extends IUnknown {
    public static final Guid.IID IID_ISHELLITEM = new Guid.IID("{43826d1e-e718-42ee-bc55-a1e261c37bfe}");
    public static final Guid.CLSID CLSID_SHELLITEM = new Guid.CLSID("{9ac9fbe1-e0a2-4ad6-b4ee-e212013ea917}");

    public WinNT.HRESULT BindToHandler(Pointer var1, Guid.GUID.ByReference var2, Guid.REFIID var3, PointerByReference var4);

    public WinNT.HRESULT GetParent(PointerByReference var1);

    public WinNT.HRESULT GetDisplayName(int var1, PointerByReference var2);

    public WinNT.HRESULT GetAttributes(int var1, IntByReference var2);

    public WinNT.HRESULT Compare(Pointer var1, int var2, IntByReference var3);
}

