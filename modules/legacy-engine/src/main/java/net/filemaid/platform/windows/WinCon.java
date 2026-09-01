package net.filemaid.platform.windows;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;
import net.filemaid.platform.windows.Kernel32;

public class WinCon {
    public static int getVirtualTerminalLevel() {
        return Advapi32Util.registryGetIntValue((WinReg.HKEY)WinReg.HKEY_CURRENT_USER, (String)"Console", (String)"VirtualTerminalLevel");
    }

    public static boolean configureVirtualTerminal() {
        try {
            WinCon.setConsoleMode(WinCon.getStandardOutput(), 7);
            System.err.print("\u001b%G");
            System.err.flush();
            return true;
        }
        catch (Throwable throwable) {
            System.err.println(throwable.getMessage());
            return false;
        }
    }

    public static WinNT.HANDLE getStandardOutput() {
        WinNT.HANDLE hANDLE = Kernel32.INSTANCE.GetStdHandle(-11);
        if (Kernel32.INVALID_HANDLE_VALUE.equals((Object)hANDLE)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        return hANDLE;
    }

    public static int getConsoleMode(WinNT.HANDLE hANDLE) {
        IntByReference intByReference = new IntByReference();
        if (!Kernel32.INSTANCE.GetConsoleMode(hANDLE, intByReference)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        return intByReference.getValue();
    }

    public static void setConsoleMode(WinNT.HANDLE hANDLE, int n) {
        if (!Kernel32.INSTANCE.SetConsoleMode(hANDLE, n)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
    }

    private WinCon() {
        throw new UnsupportedOperationException();
    }
}

