package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IFileDialog;

public interface IFileSaveDialog
extends IFileDialog {
    public static final Guid.IID IID_IFILESAVEDIALOG = new Guid.IID("{84bccd23-5fde-4cdb-aea4-af64b83d78ab}");
    public static final Guid.CLSID CLSID_FILESAVEDIALOG = new Guid.CLSID("{C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}");

    public WinNT.HRESULT SetSaveAsItem(Pointer var1);

    public WinNT.HRESULT SetProperties(Pointer var1);

    public WinNT.HRESULT SetCollectedProperties(Pointer var1, boolean var2);

    public WinNT.HRESULT GetProperties(PointerByReference var1);

    public WinNT.HRESULT ApplyProperties(Pointer var1, Pointer var2, WinDef.HWND var3, Pointer var4);
}

