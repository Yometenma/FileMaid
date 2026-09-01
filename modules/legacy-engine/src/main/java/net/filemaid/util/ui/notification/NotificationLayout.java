package net.filemaid.util.ui.notification;

import java.awt.GraphicsConfiguration;
import net.filemaid.util.ui.notification.NotificationWindow;

public interface NotificationLayout {
    public void add(NotificationWindow var1, GraphicsConfiguration var2);

    public void remove(NotificationWindow var1);

    public int size();
}

