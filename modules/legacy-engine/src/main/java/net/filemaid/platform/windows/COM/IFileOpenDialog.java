package net.filemaid.platform.windows.COM;

import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IFileDialog;

public interface IFileOpenDialog
extends IFileDialog {
    public static final Guid.IID IID_IFILEOPENDIALOG = new Guid.IID("{d57c7288-d4ad-4768-be02-9d969532d960}");
    public static final Guid.CLSID CLSID_FILEOPENDIALOG = new Guid.CLSID("{DC1C5A9C-E88A-4dde-A5A1-60F82A20AEF7}");

    public WinNT.HRESULT GetResults(PointerByReference var1);

    public WinNT.HRESULT GetSelectedItems(PointerByReference var1);
}

