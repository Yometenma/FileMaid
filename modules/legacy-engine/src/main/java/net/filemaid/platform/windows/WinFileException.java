package net.filemaid.platform.windows;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinNT;
import java.io.IOException;

public final class WinFileException
extends IOException {
    private final int error;

    public WinFileException(int n) {
        this.error = n;
    }

    public int getError() {
        return this.error;
    }

    @Override
    public String getMessage() {
        return Kernel32Util.formatMessage((WinNT.HRESULT)W32Errors.HRESULT_FROM_WIN32((int)this.error));
    }
}

