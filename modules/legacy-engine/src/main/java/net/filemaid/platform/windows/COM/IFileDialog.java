package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IModalWindow;
import net.filemaid.platform.windows.COM.ShTypes;

public interface IFileDialog
extends IModalWindow {
    public static final Guid.IID IID_IFILEDIALOG = new Guid.IID("{42f85136-db7e-439c-85f1-e4075d135fc8}");

    public WinNT.HRESULT SetFileTypes(int var1, ShTypes.COMDLG_FILTERSPEC[] var2);

    public WinNT.HRESULT SetFileTypeIndex(int var1);

    public WinNT.HRESULT GetFileTypeIndex(IntByReference var1);

    public WinNT.HRESULT Advise(Pointer var1, IntByReference var2);

    public WinNT.HRESULT Unadvise(int var1);

    public WinNT.HRESULT SetOptions(int var1);

    public WinNT.HRESULT GetOptions(IntByReference var1);

    public WinNT.HRESULT SetDefaultFolder(Pointer var1);

    public WinNT.HRESULT SetFolder(Pointer var1);

    public WinNT.HRESULT GetFolder(PointerByReference var1);

    public WinNT.HRESULT GetCurrentSelection(PointerByReference var1);

    public WinNT.HRESULT SetFileName(WString var1);

    public WinNT.HRESULT GetFileName(PointerByReference var1);

    public WinNT.HRESULT SetTitle(WString var1);

    public WinNT.HRESULT SetOkButtonLabel(WString var1);

    public WinNT.HRESULT SetFileNameLabel(WString var1);

    public WinNT.HRESULT GetResult(PointerByReference var1);

    public WinNT.HRESULT AddPlace(Pointer var1, int var2);

    public WinNT.HRESULT SetDefaultExtension(WString var1);

    public WinNT.HRESULT Close(WinNT.HRESULT var1);

    public WinNT.HRESULT SetClientGuid(Guid.GUID.ByReference var1);

    public WinNT.HRESULT ClearClientData();

    public WinNT.HRESULT SetFilter(Pointer var1);
}

