package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.FilteredImageSource;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.ColorTintImageFilter;
import net.filemaid.util.ui.SwingUI;

public class ListView
extends JList {
    protected final BlockSelectionHandler blockSelectionHandler = new BlockSelectionHandler();

    public ListView(ListModel listModel) {
        super(listModel);
        this.setSelectionMode(2);
        this.putClientProperty("List.isFileList", Boolean.TRUE);
        this.setDropMode(DropMode.ON);
        this.setLayoutOrientation(1);
        this.setVisibleRowCount(-1);
        this.setCellRenderer(new ListViewRenderer());
        this.addMouseListener(this.blockSelectionHandler);
        this.addMouseMotionListener(this.blockSelectionHandler);
    }

    public void addSelectionInterval(Rectangle rectangle) {
        Point point = rectangle.getLocation();
        Point point2 = new Point(point.x + rectangle.width, point.y + rectangle.height);
        int n = this.locationToIndex(point);
        int n2 = this.locationToIndex(point2);
        for (int i = n; i <= n2; ++i) {
            Rectangle rectangle2 = this.getCellBounds(i, i);
            if (rectangle2 == null || !rectangle.intersects(rectangle2)) continue;
            this.addSelectionInterval(i, i);
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Rectangle rectangle = this.blockSelectionHandler.getSelection();
        if (rectangle != null) {
            this.paintBlockSelection((Graphics2D)graphics, rectangle);
        }
    }

    protected void paintBlockSelection(Graphics2D graphics2D, Rectangle rectangle) {
        graphics2D.setPaint(ThemeSupport.withAlpha(this.getSelectionBackground(), 0.3f));
        graphics2D.fill(rectangle);
        graphics2D.setPaint(this.getSelectionBackground());
        graphics2D.draw(rectangle);
    }

    protected String convertValueToText(Object object) {
        return object.toString();
    }

    protected Icon convertValueToIcon(Object object) {
        return null;
    }

    protected class BlockSelectionHandler
    extends MouseInputAdapter {
        private Rectangle selection;
        private Point origin;

        protected BlockSelectionHandler() {
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if (SwingUtilities.isLeftMouseButton(mouseEvent) && !ListView.this.isSelectedIndex(ListView.this.locationToIndex(mouseEvent.getPoint()))) {
                this.origin = mouseEvent.getPoint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            if (this.origin == null) {
                return;
            }
            if (this.selection == null) {
                this.selection = new Rectangle();
            }
            Point point = mouseEvent.getPoint();
            point.x = Math.max(0, Math.min(ListView.this.getWidth() - 1, point.x));
            point.y = Math.max(0, Math.min(ListView.this.getHeight() - 1, point.y));
            this.selection.setFrameFromDiagonal(this.origin, point);
            ListView.this.ensureIndexIsVisible(ListView.this.locationToIndex(point));
            ListView.this.clearSelection();
            ListView.this.addSelectionInterval(this.selection);
            ListView.this.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            this.origin = null;
            this.selection = null;
            ListView.this.repaint();
        }

        public Rectangle getSelection() {
            return this.selection;
        }
    }

    protected class ListViewRenderer
    extends DefaultListCellRenderer {
        public ListViewRenderer() {
            this.setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
            Icon icon = ListView.this.convertValueToIcon(object);
            if (bl && icon != null) {
                icon = new ImageIcon(this.createImage(new FilteredImageSource(SwingUI.getImage(icon).getSource(), new ColorTintImageFilter(jList.getSelectionBackground(), 0.5f))));
            }
            this.setText(ListView.this.convertValueToText(object));
            this.setIcon(icon);
            if (bl) {
                this.setBackground(jList.getSelectionBackground());
                this.setForeground(jList.getSelectionForeground());
            } else {
                this.setBackground(jList.getBackground());
                this.setForeground(jList.getForeground());
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int n = this.getIcon() == null ? 0 : this.getIcon().getIconHeight();
            int n2 = n + this.getIconTextGap();
            Rectangle2D rectangle2D = this.getFontMetrics(this.getFont()).getStringBounds(this.getText(), graphics);
            graphics.setColor(this.getBackground());
            graphics.fillRect(n2 - 2, 1, (int)(rectangle2D.getWidth() + 6.0), this.getHeight() - 1);
            super.paintComponent(graphics);
        }
    }
}

