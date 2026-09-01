package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IShellItem;

public class ShellItem
extends Unknown
implements IShellItem {
    public ShellItem() {
    }

    public ShellItem(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT BindToHandler(Pointer pointer, Guid.GUID.ByReference byReference, Guid.REFIID rEFIID, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(3, new Object[]{this.getPointer(), pointer, byReference, rEFIID, pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetParent(PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(4, new Object[]{this.getPointer(), pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetDisplayName(int n, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(5, new Object[]{this.getPointer(), n, pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetAttributes(int n, IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(6, new Object[]{this.getPointer(), n, intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT Compare(Pointer pointer, int n, IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(7, new Object[]{this.getPointer(), pointer, n, intByReference}, WinNT.HRESULT.class);
    }
}

