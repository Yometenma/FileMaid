package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

public interface IFileOperation
extends IUnknown {
    public static final Guid.IID IID_IFILEOPERATION = new Guid.IID("{947aab5f-0a5c-4c13-b4d6-4bf7836fc9f8}");
    public static final Guid.CLSID CLSID_FILEOPERATION = new Guid.CLSID("{3ad05575-8857-4850-9277-11b85bdb8e09}");

    public WinNT.HRESULT Advise(Pointer var1, IntByReference var2);

    public WinNT.HRESULT Unadvise(int var1);

    public WinNT.HRESULT SetOperationFlags(int var1);

    public WinNT.HRESULT SetProgressMessage(WString var1);

    public WinNT.HRESULT SetProgressDialog(Pointer var1);

    public WinNT.HRESULT SetProperties(Pointer var1);

    public WinNT.HRESULT SetOwnerWindow(WinDef.HWND var1);

    public WinNT.HRESULT ApplyPropertiesToItem(Pointer var1);

    public WinNT.HRESULT ApplyPropertiesToItems(Pointer var1);

    public WinNT.HRESULT RenameItem(Pointer var1, WString var2, Pointer var3);

    public WinNT.HRESULT RenameItems(Pointer var1, WString var2);

    public WinNT.HRESULT MoveItem(Pointer var1, Pointer var2, WString var3, Pointer var4);

    public WinNT.HRESULT MoveItems(Pointer var1, Pointer var2);

    public WinNT.HRESULT CopyItem(Pointer var1, Pointer var2, WString var3, Pointer var4);

    public WinNT.HRESULT CopyItems(Pointer var1, Pointer var2);

    public WinNT.HRESULT DeleteItem(Pointer var1, Pointer var2);

    public WinNT.HRESULT DeleteItems(Pointer var1);

    public WinNT.HRESULT NewItem(Pointer var1, int var2, WString var3, WString var4, Pointer var5);

    public WinNT.HRESULT PerformOperations();

    public WinNT.HRESULT GetAnyOperationsAborted(WinDef.BOOLByReference var1);
}

