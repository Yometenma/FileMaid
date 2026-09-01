package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class COM {
    public static <T> T call(Supplier<T> supplier) {
        if (COM.CoInitializeEx()) {
            try {
                T t = supplier.get();
                return t;
            }
            finally {
                COM.CoUninitialize();
            }
        }
        return supplier.get();
    }

    public static <T extends IUnknown, R> R call(Guid.CLSID cLSID, Guid.IID iID, Function<Pointer, T> function, Function<T, R> function2) {
        return (R)COM.call(() -> {
            T t = COM.createInstance(cLSID, iID, function);
            try {
                R r = function2.apply(t);
                return r;
            }
            finally {
                t.Release();
            }
        });
    }

    public static <T extends IUnknown> T createInstance(Guid.CLSID cLSID, Guid.IID iID, Function<Pointer, T> function) {
        PointerByReference pointerByReference = new PointerByReference();
        WinNT.HRESULT hRESULT = Ole32.INSTANCE.CoCreateInstance((Guid.GUID)cLSID, null, 7, (Guid.GUID)iID, pointerByReference);
        if (COMUtils.FAILED((WinNT.HRESULT)hRESULT)) {
            throw new COMException("Ole32.CoCreateInstance", hRESULT);
        }
        return (T)((IUnknown)function.apply(pointerByReference.getValue()));
    }

    public static boolean CoInitializeEx() {
        if (COM.CoInitializeEx(0)) {
            return true;
        }
        return COM.CoInitializeEx(2);
    }

    private static boolean CoInitializeEx(int n) {
        WinNT.HRESULT hRESULT = Ole32.INSTANCE.CoInitializeEx(null, n);
        switch (hRESULT.intValue()) {
            case 0: {
                return true;
            }
            case 1: {
                return true;
            }
            case -2147417850: {
                return false;
            }
        }
        throw new COMException("Ole32.CoInitializeEx", hRESULT);
    }

    public static void CoUninitialize() {
        Ole32.INSTANCE.CoUninitialize();
    }
}

