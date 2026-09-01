package net.filemaid.util.ui.notification;

import java.awt.GraphicsConfiguration;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.NotificationLayout;
import net.filemaid.util.ui.notification.NotificationWindow;

public class NotificationManager {
    private final NotificationLayout layout;
    private final int limit;
    private final boolean opacity;

    public NotificationManager(NotificationLayout notificationLayout, int n, boolean bl) {
        this.layout = notificationLayout;
        this.limit = n;
        this.opacity = bl;
    }

    public void show(NotificationWindow notificationWindow, GraphicsConfiguration graphicsConfiguration) {
        if (this.layout.size() < this.limit) {
            this.layout.add(notificationWindow, graphicsConfiguration);
            notificationWindow.addWindowListener(SwingUI.windowClosed(windowEvent -> this.layout.remove((NotificationWindow)windowEvent.getWindow())));
            if (this.opacity) {
                notificationWindow.setOpacity(0.0f);
                SwingUI.animate(50, 16, notificationWindow::setOpacity, Float.valueOf(0.25f), Float.valueOf(0.5f), Float.valueOf(1.0f));
            }
            notificationWindow.setVisible(true);
        }
    }
}

