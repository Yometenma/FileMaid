package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SingleSelectionModel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.GradientStyle;
import net.filemaid.util.ui.LabelProvider;
import net.filemaid.util.ui.NullLabelProvider;
import net.filemaid.util.ui.SwingUI;

public class SelectButton<T>
extends JButton {
    public static final String SELECTED_VALUE = "selected value";
    private final Color beginColor = ThemeSupport.getColor(15789796);
    private final Color endColor = ThemeSupport.getColor(14737108);
    private final Color beginColorHover = this.beginColor;
    private final Color endColorHover = ThemeSupport.getColor(14211021);
    private final SelectIcon selectIcon = new SelectIcon();
    private List<T> model = Collections.emptyList();
    private SingleSelectionModel selectionModel = new DefaultSingleSelectionModel();
    private LabelProvider<T> labelProvider = new NullLabelProvider();
    private boolean hover = false;

    public SelectButton() {
        this.setContentAreaFilled(false);
        this.setFocusable(false);
        super.setIcon(this.selectIcon);
        this.setHorizontalAlignment(0);
        this.setVerticalAlignment(0);
        this.setBorder(BorderFactory.createLineBorder(ThemeSupport.getColor(0xA4A4A4), 1));
        this.setPreferredSize(new Dimension(32, 22));
        this.addActionListener(actionEvent -> this.showPopupMenu());
    }

    public void setModel(Collection<T> collection) {
        this.model = new ArrayList<T>(collection);
        this.setSelectedIndex(0);
    }

    public LabelProvider<T> getLabelProvider() {
        return this.labelProvider;
    }

    public void setLabelProvider(LabelProvider<T> labelProvider) {
        this.labelProvider = labelProvider;
        this.setIcon(labelProvider.getIcon(this.getSelectedValue()));
    }

    @Override
    public void setIcon(Icon icon) {
        this.selectIcon.setInnerIcon(icon);
        this.repaint();
    }

    public void setSelectedValue(T t) {
        this.setSelectedIndex(this.model.indexOf(t));
    }

    public T getSelectedValue() {
        if (!this.selectionModel.isSelected()) {
            return null;
        }
        return this.model.get(this.selectionModel.getSelectedIndex());
    }

    public void setSelectedIndex(int n) {
        if (n < 0 || n >= this.model.size()) {
            this.selectionModel.clearSelection();
            this.setIcon(null);
            return;
        }
        this.selectionModel.setSelectedIndex(n);
        T t = this.model.get(n);
        this.setIcon(this.labelProvider.getIcon(t));
        this.firePropertyChange(SELECTED_VALUE, null, t);
    }

    public int getSelectedIndex() {
        return this.selectionModel.getSelectedIndex();
    }

    public SingleSelectionModel getSelectionModel() {
        return this.selectionModel;
    }

    public void spinValue(int n) {
        int n2 = this.model.size();
        int n3 = this.getSelectedIndex() + (n %= n2);
        if (n3 < 0) {
            n3 += n2;
        } else if (n3 >= n2) {
            n3 -= n2;
        }
        this.setSelectedIndex(n3);
    }

    public void showPopupMenu() {
        JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Select");
        for (T t : this.model) {
            JMenuItem jMenuItem = jPopupMenu.add(SwingUI.newAction(this.labelProvider.getText(t), this.labelProvider.getIcon(t), actionEvent -> this.setSelectedValue(t)));
            jMenuItem.setSelected(t == this.getSelectedValue());
            jMenuItem.setMargin(new Insets(3, 0, 3, 0));
            jMenuItem.setFont(jMenuItem.getFont().deriveFont(jMenuItem.isSelected() ? 1 : 0));
        }
        jPopupMenu.show(this, 0, this.getHeight() - 1);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        Rectangle rectangle = new Rectangle(this.getSize());
        if (this.hover) {
            graphics2D.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(rectangle, this.beginColorHover, this.endColorHover));
        } else {
            graphics2D.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(rectangle, this.beginColor, this.endColor));
        }
        graphics2D.fill(rectangle);
        super.paintComponent(graphics);
    }

    @Override
    protected void processMouseEvent(MouseEvent mouseEvent) {
        switch (mouseEvent.getID()) {
            case 504: {
                this.hover = true;
                this.repaint();
                break;
            }
            case 505: {
                this.hover = false;
                this.repaint();
            }
        }
        super.processMouseEvent(mouseEvent);
    }

    private static class SelectIcon
    implements Icon {
        private final GeneralPath arrow = new GeneralPath(0, 3);
        private Icon icon;

        public SelectIcon() {
            int n = 25;
            int n2 = 10;
            this.arrow.moveTo(n - 2, n2);
            this.arrow.lineTo(n, n2 + 3);
            this.arrow.lineTo(n + 3, n2);
            this.arrow.lineTo(n - 2, n2);
        }

        public void setInnerIcon(Icon icon) {
            this.icon = icon;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int n, int n2) {
            Graphics2D graphics2D = (Graphics2D)graphics;
            if (this.icon != null) {
                this.icon.paintIcon(component, graphics2D, 4, 3);
            }
            graphics2D.setPaint(Color.black);
            graphics2D.fill(this.arrow);
        }

        @Override
        public int getIconWidth() {
            return 30;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    }
}

