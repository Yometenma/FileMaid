package net.filemaid.platform.windows.COM;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.util.Objects;
import java.util.stream.Stream;
import net.filemaid.platform.windows.COM.COM;
import net.filemaid.platform.windows.COM.IShellItem;
import net.filemaid.platform.windows.COM.Shell32;

public class Shell32Util
extends com.sun.jna.platform.win32.Shell32Util {
    public static Pointer SHCreateItemFromParsingName(File file) {
        try {
            PointerByReference pointerByReference = new PointerByReference();
            WinNT.HRESULT hRESULT = Shell32.INSTANCE.SHCreateItemFromParsingName(new WString(file.getCanonicalPath()), null, new Guid.REFIID(IShellItem.IID_ISHELLITEM), pointerByReference);
            if (W32Errors.FAILED((WinNT.HRESULT)hRESULT)) {
                throw new Win32Exception(hRESULT);
            }
            return pointerByReference.getValue();
        }
        catch (Exception exception) {
            throw new InvalidPathException(file.getPath(), exception.getMessage());
        }
    }

    public static Pointer SHParseDisplayName(File file) {
        try {
            PointerByReference pointerByReference = new PointerByReference();
            WinNT.HRESULT hRESULT = Shell32.INSTANCE.SHParseDisplayName(new WString(file.getCanonicalPath()), null, pointerByReference, new WinDef.ULONG(0L), null);
            if (W32Errors.FAILED((WinNT.HRESULT)hRESULT)) {
                throw new Win32Exception(hRESULT);
            }
            return pointerByReference.getValue();
        }
        catch (Exception exception) {
            throw new InvalidPathException(file.getPath(), exception.getMessage());
        }
    }

    public static Pointer ILCreateFromPath(File file) {
        Pointer pointer = Shell32.INSTANCE.ILCreateFromPath(file.getAbsolutePath());
        if (pointer == null) {
            throw new InvalidPathException(file.getPath(), "ILCreateFromPath is null");
        }
        return pointer;
    }

    public static void SHOpenFolderAndSelectItems(File ... fileArray) {
        COM.call(() -> {
            Pointer pointer = Shell32Util.ILCreateFromPath(fileArray[0].getParentFile());
            Pointer[] pointerArray = new Pointer[fileArray.length];
            try {
                for (int i = 0; i < fileArray.length; ++i) {
                    pointerArray[i] = Shell32Util.ILCreateFromPath(fileArray[i]);
                }
                Shell32.INSTANCE.SHOpenFolderAndSelectItems(pointer, pointerArray.length, pointerArray, 0);
            }
            finally {
                Shell32.INSTANCE.ILFree(pointer);
                Stream.of(pointerArray).filter(Objects::nonNull).forEach(Shell32.INSTANCE::ILFree);
            }
            return null;
        });
    }
}

