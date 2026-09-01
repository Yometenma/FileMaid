package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import net.filemaid.util.ui.AbstractFancyListCellRenderer;

public class DefaultFancyListCellRenderer
extends AbstractFancyListCellRenderer {
    private final JLabel label = new DefaultListCellRenderer();

    public DefaultFancyListCellRenderer() {
        this.add(this.label);
    }

    public DefaultFancyListCellRenderer(int n) {
        super(new Insets(n, n, n, n));
        this.add(this.label);
    }

    public DefaultFancyListCellRenderer(Insets insets) {
        super(insets);
        this.add(this.label);
    }

    protected DefaultFancyListCellRenderer(int n, int n2, Color color) {
        super(new Insets(n, n, n, n), new Insets(n2, n2, n2, n2), color);
        this.add(this.label);
    }

    @Override
    protected void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        super.configureListCellRendererComponent(jList, object, n, bl, bl2);
        this.label.setOpaque(false);
        this.label.setText(String.valueOf(object));
    }

    public void setIcon(Icon icon) {
        this.label.setIcon(icon);
    }

    public void setText(String string) {
        this.label.setText(string);
    }

    public void setHorizontalTextPosition(int n) {
        this.label.setHorizontalTextPosition(n);
    }

    public void setVerticalTextPosition(int n) {
        this.label.setVerticalTextPosition(n);
    }

    @Override
    public void setForeground(Color color) {
        super.setForeground(color);
        if (this.label != null) {
            this.label.setForeground(color);
        }
    }
}

