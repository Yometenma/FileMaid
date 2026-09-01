package net.filemaid.util.ui.notification;

import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.JWindow;
import javax.swing.Timer;
import net.filemaid.util.ui.SwingUI;

public class NotificationWindow
extends JWindow {
    protected final int timeout;

    public NotificationWindow(final int n, boolean bl) {
        this.timeout = n;
        this.setType(Window.Type.POPUP);
        this.setAlwaysOnTop(true);
        this.setAutoRequestFocus(false);
        this.setFocusableWindowState(false);
        if (bl) {
            this.getGlassPane().addMouseListener(SwingUI.mouseClicked(this::close));
            this.getGlassPane().setVisible(true);
        }
        this.addComponentListener(new ComponentAdapter(){
            private Timer timer = null;

            @Override
            public void componentShown(ComponentEvent componentEvent) {
                if (n >= 0) {
                    this.timer = SwingUI.invokeLater(n, () -> NotificationWindow.this.close(componentEvent));
                }
            }

            @Override
            public void componentHidden(ComponentEvent componentEvent) {
                if (this.timer != null) {
                    this.timer.stop();
                }
            }
        });
    }

    public void close(EventObject eventObject) {
        this.processWindowEvent(new WindowEvent(this, 201));
        this.setVisible(false);
        this.processComponentEvent(new ComponentEvent(this, 103));
        this.dispose();
    }
}

