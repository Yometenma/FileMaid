package net.filemaid.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import net.filemaid.Logging;
import net.filemaid.platform.windows.AppCore;
import net.filemaid.platform.windows.AppCoreException;
import net.filemaid.platform.windows.COM.Shell32Util;
import net.filemaid.platform.windows.Dwmapi;
import net.filemaid.platform.windows.GDI32;
import net.filemaid.platform.windows.PackageOrigin;
import net.filemaid.platform.windows.User32;
import net.filemaid.ui.ThemeSupport;

public final class WinAppUtilities {
    public static void setAppUserModelID(String string) {
        try {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(string));
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static String getAppUserModelID() {
        try {
            PointerByReference pointerByReference = new PointerByReference();
            if (Shell32.INSTANCE.GetCurrentProcessExplicitAppUserModelID(pointerByReference).equals((Object)WinError.S_OK)) {
                return pointerByReference.getValue().getWideString(0L);
            }
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
        return null;
    }

    public static String getPackageName() {
        WinDef.UINTByReference uINTByReference = new WinDef.UINTByReference(new WinDef.UINT(64L));
        WTypes.LPWSTR lPWSTR = new WTypes.LPWSTR((Pointer)new Memory((long)(uINTByReference.getValue().intValue() * Native.WCHAR_SIZE)));
        NativeLong nativeLong = AppCore.INSTANCE.GetCurrentPackageFullName(uINTByReference, lPWSTR);
        WinAppUtilities.checkErrorSuccess((Number)nativeLong);
        return lPWSTR.getValue();
    }

    public static String getPackageFamilyName() {
        WinDef.UINTByReference uINTByReference = new WinDef.UINTByReference(new WinDef.UINT(64L));
        WTypes.LPWSTR lPWSTR = new WTypes.LPWSTR((Pointer)new Memory((long)(uINTByReference.getValue().intValue() * Native.WCHAR_SIZE)));
        NativeLong nativeLong = AppCore.INSTANCE.GetCurrentPackageFamilyName(uINTByReference, lPWSTR);
        WinAppUtilities.checkErrorSuccess((Number)nativeLong);
        return lPWSTR.getValue();
    }

    public static PackageOrigin getStagedPackageOrigin() {
        WTypes.LPWSTR lPWSTR = new WTypes.LPWSTR(WinAppUtilities.getPackageName());
        PackageOrigin.ByReference byReference = new PackageOrigin.ByReference();
        NativeLong nativeLong = AppCore.INSTANCE.GetStagedPackageOrigin(lPWSTR, byReference);
        WinAppUtilities.checkErrorSuccess((Number)nativeLong);
        return byReference.getValue();
    }

    public static String getPackageAppUserModelID() {
        WinDef.UINTByReference uINTByReference = new WinDef.UINTByReference(new WinDef.UINT(64L));
        WTypes.LPWSTR lPWSTR = new WTypes.LPWSTR((Pointer)new Memory((long)(uINTByReference.getValue().intValue() * Native.WCHAR_SIZE)));
        NativeLong nativeLong = AppCore.INSTANCE.GetCurrentApplicationUserModelId(uINTByReference, lPWSTR);
        WinAppUtilities.checkErrorSuccess((Number)nativeLong);
        return lPWSTR.getValue();
    }

    private static void checkErrorSuccess(Number number) {
        if (number.intValue() != 0) {
            throw new AppCoreException(number.intValue());
        }
    }

    public static void requirePackageIdentity() {
        try {
            WinAppUtilities.getPackageFamilyName();
        }
        catch (AppCoreException appCoreException) {
            System.err.println(appCoreException.getMessage());
            System.exit(appCoreException.getError());
        }
    }

    public static File getPackageAppDataFolder() {
        return new File(System.getenv("LOCALAPPDATA") + "/Packages/" + WinAppUtilities.getPackageFamilyName() + "/LocalCache/Roaming");
    }

    public static boolean isAppsUseLightTheme() {
        try {
            return 0 != Advapi32Util.registryGetIntValue((WinReg.HKEY)WinReg.HKEY_CURRENT_USER, (String)"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", (String)"AppsUseLightTheme");
        }
        catch (Throwable throwable) {
            return true;
        }
    }

    public static boolean isRDP() {
        String string = System.getenv("SESSIONNAME");
        return string != null && string.startsWith("rdp");
    }

    public static void openFolderAndSelectItem(File file) {
        Shell32Util.SHOpenFolderAndSelectItems(file);
    }

    public static void openFolderAndSelectItems(Collection<File> collection) {
        if (collection.isEmpty()) {
            return;
        }
        Shell32Util.SHOpenFolderAndSelectItems(collection.toArray(new File[0]));
    }

    public static void initializeApplication(String string, Runnable runnable) {
        if (string != null) {
            WinAppUtilities.setAppUserModelID(string);
        }
        if (ThemeSupport.getTheme().isDark()) {
            UIManager.put("TitledBorder.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeSupport.getColor(0xD7D7D7), 1, true), BorderFactory.createEmptyBorder(6, 5, 6, 5)));
        } else {
            UIManager.put("TitledBorder.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeSupport.getColor(0xD7D7D7), 1, true), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(6, 5, 6, 5, ThemeSupport.getColor(0xE5E5E5)), BorderFactory.createEmptyBorder(0, 2, 0, 2))));
        }
        WinAppUtilities.setQuitStrategy(runnable);
    }

    public static void setQuitStrategy(Runnable runnable) {
        try {
            Class<?> clazz = Class.forName("sun.misc.Signal");
            Class<?> clazz2 = Class.forName("sun.misc.SignalHandler");
            Object obj = clazz.getConstructor(String.class).newInstance("TERM");
            Object object2 = Proxy.newProxyInstance(clazz2.getClassLoader(), new Class[]{clazz2}, (object, method, objectArray) -> {
                runnable.run();
                return null;
            });
            Method method2 = clazz.getMethod("handle", clazz, clazz2);
            method2.invoke(null, obj, object2);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void disableTransitions(Window window) {
        try {
            WinAppUtilities.setWindowAttribute(window, 3, true);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void disableRoundCorner(Window window) {
        try {
            WinAppUtilities.setWindowAttribute(window, 33, 1);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void setBorderColor(Window window, Color color) {
        try {
            WinAppUtilities.setWindowAttribute(window, 34, WinAppUtilities.abgr(color));
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void setCaptionColor(Window window, Color color) {
        try {
            WinAppUtilities.setWindowAttribute(window, 35, WinAppUtilities.abgr(color));
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void setTextColor(Window window, Color color) {
        try {
            WinAppUtilities.setWindowAttribute(window, 36, WinAppUtilities.abgr(color));
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void useDarkMode(Window window) {
        try {
            WinAppUtilities.setWindowAttribute(window, 20, true);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void setTransient(Window window) {
        try {
            WinAppUtilities.setWindowAttribute(window, 38, 3);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static void setBackgroundBrush(Window window, Color color) {
        try {
            User32.INSTANCE.SetClassLongPtr(Native.getWindowPointer((Window)window), -10, GDI32.INSTANCE.CreateSolidBrush(WinAppUtilities.abgr(color)));
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
    }

    public static int setWindowAttribute(Window window, int n, boolean bl) {
        return Dwmapi.INSTANCE.DwmSetWindowAttribute(Native.getWindowPointer((Window)window), n, (PointerType)new WinDef.BOOLByReference(new WinDef.BOOL(bl)), 4);
    }

    public static int setWindowAttribute(Window window, int n, int n2) {
        return Dwmapi.INSTANCE.DwmSetWindowAttribute(Native.getWindowPointer((Window)window), n, (PointerType)new WinDef.DWORDByReference(new WinDef.DWORD((long)n2)), 4);
    }

    private static int abgr(Color color) {
        return color.getRed() | color.getGreen() << 8 | color.getBlue() << 16;
    }

    private WinAppUtilities() {
        throw new UnsupportedOperationException();
    }
}

