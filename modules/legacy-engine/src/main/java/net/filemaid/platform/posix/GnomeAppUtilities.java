package net.filemaid.platform.posix;

import com.sun.jna.Platform;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import net.filemaid.Logging;
import net.filemaid.platform.posix.Zenity;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.SystemProperty;

public class GnomeAppUtilities {
    public static final Zenity zenity = SystemProperty.get("net.filemaid.zenity", Zenity::new, new Zenity("/usr/bin/zenity"));

    public static boolean isX11() {
        try {
            return Platform.isX11();
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("X11", throwable));
            return false;
        }
    }

    public static boolean useZenity() {
        if (null != System.getProperty("zenity.version")) {
            return true;
        }
        if (null != System.getProperty("zenity.error") || !GnomeAppUtilities.isX11()) {
            return false;
        }
        try {
            System.load(System.getProperty("sun.boot.library.path") + "/libjawt.so");
            System.setProperty("zenity.version", zenity.version());
            return true;
        }
        catch (Throwable throwable) {
            System.setProperty("zenity.error", throwable.getMessage());
            return false;
        }
    }

    public static void initializeApplication() {
        UIManager.put("TitledBorder.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeSupport.getColor(0xD7D7D7), 1, true), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(6, 5, 6, 5, ThemeSupport.getPanelBackground()), BorderFactory.createEmptyBorder(0, 2, 0, 2))));
    }

    private GnomeAppUtilities() {
        throw new UnsupportedOperationException();
    }
}

