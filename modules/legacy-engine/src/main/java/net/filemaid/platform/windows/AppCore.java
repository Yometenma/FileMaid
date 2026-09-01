package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;
import net.filemaid.platform.windows.PackageOrigin;

public interface AppCore
extends StdCallLibrary {
    public static final AppCore INSTANCE = (AppCore)Native.load((String)"kernel.appcore", AppCore.class, (Map)W32APIOptions.DEFAULT_OPTIONS);
    public static final int APPMODEL_ERROR_NO_PACKAGE = 15700;
    public static final int APPMODEL_ERROR_PACKAGE_RUNTIME_CORRUPT = 15701;
    public static final int APPMODEL_ERROR_PACKAGE_IDENTITY_CORRUPT = 15702;
    public static final int APPMODEL_ERROR_NO_APPLICATION = 15703;
    public static final int ERROR_INSUFFICIENT_BUFFER = 122;

    public NativeLong GetCurrentPackageFullName(WinDef.UINTByReference var1, WTypes.LPWSTR var2);

    public NativeLong GetCurrentPackageFamilyName(WinDef.UINTByReference var1, WTypes.LPWSTR var2);

    public NativeLong GetCurrentApplicationUserModelId(WinDef.UINTByReference var1, WTypes.LPWSTR var2);

    public NativeLong GetStagedPackageOrigin(WTypes.LPWSTR var1, PackageOrigin.ByReference var2);
}

