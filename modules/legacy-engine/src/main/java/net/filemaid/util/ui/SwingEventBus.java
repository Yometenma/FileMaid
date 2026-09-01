package net.filemaid.util.ui;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import javax.swing.SwingUtilities;
import net.filemaid.Logging;

public class SwingEventBus
extends AsyncEventBus {
    private static SwingEventBus instance;

    public static synchronized SwingEventBus getInstance() {
        if (instance == null) {
            instance = new SwingEventBus();
        }
        return instance;
    }

    public static synchronized boolean isActive() {
        return instance != null;
    }

    public SwingEventBus() {
        super(SwingUtilities::invokeLater, SwingEventBus::handleException);
    }

    public void register(Object object) {
        SwingUtilities.invokeLater(() -> super.register(object));
    }

    public void unregister(Object object) {
        SwingUtilities.invokeLater(() -> super.unregister(object));
    }

    public void post(Object object) {
        SwingUtilities.invokeLater(() -> super.post(object));
    }

    private static void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        Logging.trace("Failed to handle event", throwable);
    }
}

