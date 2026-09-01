package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import net.filemaid.platform.windows.COM.IModalWindow;

public class ModalWindow
extends Unknown
implements IModalWindow {
    public ModalWindow() {
    }

    public ModalWindow(Pointer pointer) {
        super(pointer);
    }

    @Override
    public WinNT.HRESULT Show(WinDef.HWND hWND) {
        return (WinNT.HRESULT)this._invokeNativeObject(3, new Object[]{this.getPointer(), hWND}, WinNT.HRESULT.class);
    }
}

