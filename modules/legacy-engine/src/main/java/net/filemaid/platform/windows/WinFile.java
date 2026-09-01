package net.filemaid.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.filemaid.platform.windows.Kernel32;
import net.filemaid.platform.windows.WinFileException;
import net.filemaid.platform.windows.WinFileKey;

public final class WinFile {
    public static WinFileKey getFileKey(File file) throws IOException {
        return WinFile.getFileInfo(file, 18, WinBase.FILE_ID_INFO::sizeOf, memory -> {
            WinBase.FILE_ID_INFO fILE_ID_INFO = new WinBase.FILE_ID_INFO((Pointer)memory);
            return new WinFileKey(fILE_ID_INFO.VolumeSerialNumber, WinFile.asBigInteger(fILE_ID_INFO.FileId));
        });
    }

    public static int getLinkCount(File file) throws IOException {
        return WinFile.getFileInfo(file, 1, WinBase.FILE_STANDARD_INFO::sizeOf, memory -> {
            WinBase.FILE_STANDARD_INFO fILE_STANDARD_INFO = new WinBase.FILE_STANDARD_INFO((Pointer)memory);
            return fILE_STANDARD_INFO.NumberOfLinks;
        });
    }

    public static int getDriveType(File file) {
        return Kernel32Util.getDriveType((String)file.getPath());
    }

    private static <T> T getFileInfo(File file, int n, IntSupplier intSupplier, Function<Memory, T> function) throws IOException {
        WinNT.HANDLE hANDLE = Kernel32.INSTANCE.CreateFile(file.getPath(), Integer.MIN_VALUE, 1, null, 3, 128, null);
        if (WinBase.INVALID_HANDLE_VALUE.equals((Object)hANDLE)) {
            throw new WinFileException(Kernel32.INSTANCE.GetLastError());
        }
        try {
            Memory memory = new Memory((long)intSupplier.getAsInt());
            WinDef.DWORD dWORD = new WinDef.DWORD(memory.size());
            if (!Kernel32.INSTANCE.GetFileInformationByHandleEx(hANDLE, n, (Pointer)memory, dWORD)) {
                throw new WinFileException(Kernel32.INSTANCE.GetLastError());
            }
            T t = function.apply(memory);
            return t;
        }
        finally {
            Kernel32.INSTANCE.CloseHandle(hANDLE);
        }
    }

    private static BigInteger asBigInteger(WinBase.FILE_ID_INFO.FILE_ID_128 fILE_ID_128) {
        byte[] byArray = new byte[fILE_ID_128.Identifier.length];
        for (int i = 0; i < byArray.length; ++i) {
            byArray[i] = fILE_ID_128.Identifier[i].byteValue();
        }
        return new BigInteger(1, byArray);
    }

    private WinFile() {
        throw new UnsupportedOperationException();
    }
}

