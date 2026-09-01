package net.filemaid.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import net.filemaid.ResourceManager;
import net.filemaid.UserInteraction;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.Direction;
import net.filemaid.util.ui.notification.MessageNotification;
import net.filemaid.util.ui.notification.NotificationManager;
import net.filemaid.util.ui.notification.QueueNotificationLayout;

public class NotificationHandler
extends Handler {
    private final String title;
    private final int timeout;
    private final NotificationManager manager;

    public NotificationHandler(String string, boolean bl) {
        this(string, 5000, new NotificationManager(new QueueNotificationLayout(Direction.NORTH, Direction.SOUTH), 5, bl));
    }

    public NotificationHandler(String string, int n, NotificationManager notificationManager) {
        this.title = string;
        this.timeout = n;
        this.manager = notificationManager;
        this.setFormatter(new SimpleMessageFormatter());
        this.setLevel(Level.INFO);
    }

    @Override
    public void publish(LogRecord logRecord) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String string = this.getFormatter().format(logRecord);
        Level level = logRecord.getLevel();
        SwingUtilities.invokeLater(() -> {
            if (level == Level.INFO) {
                this.show(string, ResourceManager.getIcon("message.info"), this.timeout * 1);
            } else if (level == Level.WARNING) {
                this.show(string, ResourceManager.getIcon("message.warning"), this.timeout * 2);
            } else if (level == Level.SEVERE) {
                this.show(string, ResourceManager.getIcon("message.error"), this.timeout * 3);
            }
        });
    }

    protected void show(String string, Icon icon, int n) {
        Window window = SwingUI.getMainWindow();
        if (window == null) {
            SwingUI.invokeLater(n, () -> this.show(string, icon, n, SwingUI.getMainWindow()));
            return;
        }
        this.show(string, icon, n, window);
    }

    protected void show(String string, Icon icon, int n, Window window) {
        MessageNotification messageNotification = new MessageNotification(this.title, string, icon, n);
        Component component = messageNotification.getRootPane().getGlassPane();
        component.setCursor(Cursor.getPredefinedCursor(12));
        component.addMouseListener(SwingUI.mouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == 1) {
                SwingUI.invokeLater(50, () -> UserInteraction.copy(string));
            } else {
                SwingUI.invokeLater(50, () -> UserInteraction.openErrorLog());
            }
        }));
        this.manager.show(messageNotification, window == null ? messageNotification.getGraphicsConfiguration() : window.getGraphicsConfiguration());
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    public static class SimpleMessageFormatter
    extends Formatter {
        @Override
        public String format(LogRecord logRecord) {
            String string = logRecord.getMessage();
            if (string == null && logRecord.getThrown() != null) {
                string = logRecord.getThrown().toString();
            }
            return string;
        }
    }
}

