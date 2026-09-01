package net.filemaid.util.ui.notification;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.filemaid.util.ui.notification.Direction;
import net.filemaid.util.ui.notification.NotificationLayout;
import net.filemaid.util.ui.notification.NotificationWindow;

public class QueueNotificationLayout
implements NotificationLayout {
    private final List<NotificationWindow> notifications = new ArrayList<NotificationWindow>();
    private final Direction alignment;
    private final Direction direction;
    private final Direction growAnchor;

    public QueueNotificationLayout() {
        this(Direction.SOUTH_EAST, Direction.WEST);
    }

    public QueueNotificationLayout(Direction direction, Direction direction2) {
        this.alignment = direction;
        this.growAnchor = direction;
        this.direction = direction2;
    }

    public QueueNotificationLayout(Direction direction, Direction direction2, Direction direction3) {
        this.alignment = direction;
        this.direction = direction2;
        this.growAnchor = direction3;
    }

    private Point getBaseAnchor(Rectangle rectangle, Insets insets) {
        int n = (int)(this.alignment.ax * (double)(rectangle.width - insets.left + insets.right)) + rectangle.x + insets.left;
        int n2 = (int)(this.alignment.ay * (double)(rectangle.height - insets.top + insets.bottom)) + rectangle.y + insets.top;
        return new Point(n, n2);
    }

    private Point getLocation(Point point, Dimension dimension) {
        int n = (int)((double)point.x - (double)dimension.width * this.growAnchor.ax);
        int n2 = (int)((double)point.y - (double)dimension.height * this.growAnchor.ay);
        return new Point(n, n2);
    }

    private Point getNextAnchor(Point point, Dimension dimension) {
        int n = point.x + dimension.width * this.direction.vx;
        int n2 = point.y + dimension.height * this.direction.vy;
        return new Point(n, n2);
    }

    @Override
    public void add(NotificationWindow notificationWindow, GraphicsConfiguration graphicsConfiguration) {
        this.notifications.add(notificationWindow);
        this.align(graphicsConfiguration);
    }

    private void align(GraphicsConfiguration graphicsConfiguration) {
        Rectangle rectangle = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        Point point = this.getBaseAnchor(rectangle, insets);
        this.align(point, rectangle, this.notifications.iterator());
    }

    private void align(Point point, Rectangle rectangle, Iterator<NotificationWindow> iterator) {
        if (!iterator.hasNext()) {
            return;
        }
        NotificationWindow notificationWindow = iterator.next();
        Dimension dimension = notificationWindow.getSize();
        dimension.width = Math.min(dimension.width, (int)((double)rectangle.width * 0.8));
        dimension.height = Math.min(dimension.height, (int)((double)rectangle.height * 0.2));
        Point point2 = this.getLocation(point, dimension);
        this.align(this.getNextAnchor(point, dimension), rectangle, iterator);
        notificationWindow.setBounds(point2.x, point2.y, dimension.width, dimension.height);
    }

    @Override
    public void remove(NotificationWindow notificationWindow) {
        if (this.notifications.remove(notificationWindow)) {
            this.align(notificationWindow.getGraphicsConfiguration());
        }
    }

    @Override
    public int size() {
        return this.notifications.size();
    }
}

