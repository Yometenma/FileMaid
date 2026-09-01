package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.ShTypes;

public interface IShellItemArray
extends IUnknown {
    public static final Guid.IID IID_ISHELLITEMARRAY = new Guid.IID("{b63ea76d-1f85-456f-a19c-48159efa858b}");

    public WinNT.HRESULT BindToHandler(Pointer var1, Guid.GUID.ByReference var2, Guid.REFIID var3, PointerByReference var4);

    public WinNT.HRESULT GetPropertyStore(ShTypes.GETPROPERTYSTOREFLAGS var1, Guid.REFIID var2, PointerByReference var3);

    public WinNT.HRESULT GetPropertyDescriptionList(ShTypes.PROPERTYKEY var1, Guid.REFIID var2, PointerByReference var3);

    public WinNT.HRESULT GetAttributes(int var1, int var2, IntByReference var3);

    public WinNT.HRESULT GetCount(IntByReference var1);

    public WinNT.HRESULT GetItemAt(int var1, PointerByReference var2);
}

