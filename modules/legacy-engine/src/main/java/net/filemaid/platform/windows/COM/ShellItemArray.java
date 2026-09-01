package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.filemaid.platform.windows.COM.IShellItemArray;
import net.filemaid.platform.windows.COM.ShTypes;

public class ShellItemArray
extends Unknown
implements IShellItemArray {
    public ShellItemArray() {
    }

    public ShellItemArray(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT BindToHandler(Pointer pointer, Guid.GUID.ByReference byReference, Guid.REFIID rEFIID, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(3, new Object[]{this.getPointer(), pointer, byReference, rEFIID, pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetPropertyStore(ShTypes.GETPROPERTYSTOREFLAGS gETPROPERTYSTOREFLAGS, Guid.REFIID rEFIID, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(4, new Object[]{this.getPointer(), gETPROPERTYSTOREFLAGS, rEFIID, pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetPropertyDescriptionList(ShTypes.PROPERTYKEY pROPERTYKEY, Guid.REFIID rEFIID, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(5, new Object[]{this.getPointer(), pROPERTYKEY, rEFIID, pointerByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetAttributes(int n, int n2, IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(6, new Object[]{this.getPointer(), n, n2, intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetCount(IntByReference intByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(7, new Object[]{this.getPointer(), intByReference}, WinNT.HRESULT.class);
    }

    @Override
    public WinNT.HRESULT GetItemAt(int n, PointerByReference pointerByReference) {
        return (WinNT.HRESULT)this._invokeNativeObject(8, new Object[]{this.getPointer(), n, pointerByReference}, WinNT.HRESULT.class);
    }
}

