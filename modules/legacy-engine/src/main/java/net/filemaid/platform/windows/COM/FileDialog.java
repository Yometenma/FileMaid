package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IFileDialog;
import net.filemaid.platform.windows.COM.ModalWindow;
import net.filemaid.platform.windows.COM.ShTypes;

public class FileDialog
extends ModalWindow
implements IFileDialog {
    public FileDialog() {
    }

    public FileDialog(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT SetFileTypes(int n, ShTypes.COMDLG_FILTERSPEC[] cOMDLG_FILTERSPECArray) {
        return (WinNT.HRESULT)this._invokeNativeObject(4, new Object[]{this.getPointer(), n, cOMDLG_FILTERSPECArray}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetFileTypeIndex(int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(5, new Object[]{this.getPointer(), n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetFileTypeIndex(IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(6, new Object[]{this.getPointer(), intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT Advise(Pointer pointer, IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(7, new Object[]{this.getPointer(), pointer, intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT Unadvise(int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(8, new Object[]{this.getPointer(), n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetOptions(int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(9, new Object[]{this.getPointer(), n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetOptions(IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(10, new Object[]{this.getPointer(), intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetDefaultFolder(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(11, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetFolder(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(12, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetFolder(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(13, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetCurrentSelection(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(14, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetFileName(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(15, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetFileName(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(16, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetTitle(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(17, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetOkButtonLabel(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(18, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetFileNameLabel(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(19, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetResult(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(20, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT AddPlace(Pointer pointer, int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(21, new Object[]{this.getPointer(), pointer, n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetDefaultExtension(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(22, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT Close(WinNT.HRESULT hRESULT) {
        return (WinNT.HRESULT)this._invokeNativeObject(23, new Object[]{this.getPointer(), hRESULT}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetClientGuid(Guid.GUID.ByReference byReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(24, new Object[]{this.getPointer(), byReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT ClearClientData() {
        return (WinNT.HRESULT)this._invokeNativeObject(25, new Object[]{this.getPointer()}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetFilter(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(26, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }
}

