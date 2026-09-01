package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.FileDialog;
import net.filemaid.platform.windows.COM.IFileSaveDialog;

public class FileSaveDialog
extends FileDialog
implements IFileSaveDialog {
    public FileSaveDialog() {
    }

    public FileSaveDialog(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT SetSaveAsItem(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(27, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetProperties(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(28, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetCollectedProperties(Pointer pointer, boolean bl) {
        return (WinNT.HRESULT)this._invokeNativeObject(29, new Object[]{this.getPointer(), pointer, bl}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetProperties(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(30, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT ApplyProperties(Pointer pointer, Pointer pointer2, WinDef.HWND hWND, Pointer pointer3) {
        return (WinNT.HRESULT)this._invokeNativeObject(31, new Object[]{this.getPointer(), pointer, pointer2, hWND, pointer3}, WinNT.HRESULT.class);
    }
}

