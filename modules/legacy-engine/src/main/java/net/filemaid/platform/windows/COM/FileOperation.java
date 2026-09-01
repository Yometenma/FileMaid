package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import net.filemaid.platform.windows.COM.IFileOperation;

public class FileOperation
extends Unknown
implements IFileOperation {
    public FileOperation() {
    }

    public FileOperation(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT Advise(Pointer pointer, IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(3, new Object[]{this.getPointer(), pointer, intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT Unadvise(int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(4, new Object[]{this.getPointer(), n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetOperationFlags(int n) {
        return (WinNT.HRESULT)this._invokeNativeObject(5, new Object[]{this.getPointer(), n}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetProgressMessage(WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(6, new Object[]{this.getPointer(), wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetProgressDialog(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(7, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetProperties(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(8, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT SetOwnerWindow(WinDef.HWND hWND) {
        return (WinNT.HRESULT)this._invokeNativeObject(9, new Object[]{this.getPointer(), hWND}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT ApplyPropertiesToItem(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(10, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT ApplyPropertiesToItems(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(11, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT RenameItem(Pointer pointer, WString wString, Pointer pointer2) {
        return (WinNT.HRESULT)this._invokeNativeObject(12, new Object[]{this.getPointer(), pointer, wString, pointer2}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT RenameItems(Pointer pointer, WString wString) {
        return (WinNT.HRESULT)this._invokeNativeObject(13, new Object[]{this.getPointer(), pointer, wString}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT MoveItem(Pointer pointer, Pointer pointer2, WString wString, Pointer pointer3) {
        return (WinNT.HRESULT)this._invokeNativeObject(14, new Object[]{this.getPointer(), pointer, pointer2, wString, pointer3}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT MoveItems(Pointer pointer, Pointer pointer2) {
        return (WinNT.HRESULT)this._invokeNativeObject(15, new Object[]{this.getPointer(), pointer, pointer2}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT CopyItem(Pointer pointer, Pointer pointer2, WString wString, Pointer pointer3) {
        return (WinNT.HRESULT)this._invokeNativeObject(16, new Object[]{this.getPointer(), pointer, pointer2, wString, pointer3}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT CopyItems(Pointer pointer, Pointer pointer2) {
        return (WinNT.HRESULT)this._invokeNativeObject(17, new Object[]{this.getPointer(), pointer, pointer2}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT DeleteItem(Pointer pointer, Pointer pointer2) {
        return (WinNT.HRESULT)this._invokeNativeObject(18, new Object[]{this.getPointer(), pointer, pointer2}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT DeleteItems(Pointer pointer) {
        return (WinNT.HRESULT)this._invokeNativeObject(19, new Object[]{this.getPointer(), pointer}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT NewItem(Pointer pointer, int n, WString wString, WString wString2, Pointer pointer2) {
        return (WinNT.HRESULT)this._invokeNativeObject(20, new Object[]{this.getPointer(), pointer, n, wString, wString2, pointer2}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT PerformOperations() {
        return (WinNT.HRESULT)this._invokeNativeObject(21, new Object[]{this.getPointer()}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetAnyOperationsAborted(WinDef.BOOLByReference bOOLByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(22, new Object[]{this.getPointer(), bOOLByReference}, WinNT.HRESULT.class);
    }
}

