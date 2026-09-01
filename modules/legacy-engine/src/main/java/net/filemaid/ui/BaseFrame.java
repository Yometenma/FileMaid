package net.filemaid.ui;

import java.awt.Rectangle;
import javax.swing.JFrame;
import net.filemaid.Settings;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.ui.ThemeSupport;

public class BaseFrame
extends JFrame {
    public BaseFrame(String string) {
        super(string);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (Settings.isWindowsApp()) {
            if (ThemeSupport.getTheme().isDark()) {
                WinAppUtilities.useDarkMode(this);
            }
            WinAppUtilities.setBackgroundBrush(this, this.getBackground());
        }
    }

    @Override
    public void setMaximizedBounds(Rectangle rectangle) {
        if (ThemeSupport.getTheme() == ThemeSupport.Theme.Darcula) {
            return;
        }
        super.setMaximizedBounds(rectangle);
    }
}

