package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.FileDialog;
import net.filemaid.platform.windows.COM.IFileOpenDialog;

public class FileOpenDialog
extends FileDialog
implements IFileOpenDialog {
    public FileOpenDialog() {
    }

    public FileOpenDialog(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT GetResults(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(27, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetSelectedItems(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(28, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }
}

