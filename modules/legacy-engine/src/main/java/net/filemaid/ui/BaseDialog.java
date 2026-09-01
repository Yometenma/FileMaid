package net.filemaid.ui;

import java.awt.Dialog;
import java.awt.Window;
import javax.swing.JDialog;
import net.filemaid.Settings;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.ui.ThemeSupport;

public class BaseDialog
extends JDialog {
    public BaseDialog(Window window) {
        super(window, null, Dialog.ModalityType.DOCUMENT_MODAL);
        if (Settings.isWindowsApp()) {
            this.setType(Window.Type.POPUP);
        } else {
            this.setType(Window.Type.UTILITY);
        }
    }

    public BaseDialog(Window window, String string) {
        super(window, string, Dialog.ModalityType.DOCUMENT_MODAL);
    }

    public BaseDialog(Window window, String string, Dialog.ModalityType modalityType) {
        super(window, string, modalityType);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (Settings.isWindowsApp()) {
            if (this.getType() == Window.Type.POPUP) {
                WinAppUtilities.disableTransitions(this);
                WinAppUtilities.disableRoundCorner(this);
                WinAppUtilities.setTransient(this);
            }
            if (ThemeSupport.getTheme().isDark()) {
                WinAppUtilities.useDarkMode(this);
            }
        }
    }
}

