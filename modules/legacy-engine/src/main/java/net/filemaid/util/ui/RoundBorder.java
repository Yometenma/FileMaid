package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

public class RoundBorder
extends AbstractBorder {
    private final Color color;
    private final Insets insets;
    private final int arc;

    public RoundBorder(Color color, int n, Insets insets) {
        this.color = color;
        this.arc = n;
        this.insets = insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int n, int n2, int n3, int n4) {
        Graphics2D graphics2D = (Graphics2D)graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setPaint(component.getBackground());
        graphics2D.fillRoundRect(n, n2, n3 - 1, n4 - 1, this.arc, this.arc);
        graphics2D.setPaint(this.color);
        graphics2D.drawRoundRect(n, n2, n3 - 1, n4 - 1, this.arc, this.arc);
        graphics2D.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(this.insets.top, this.insets.left, this.insets.bottom, this.insets.right);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.top = this.insets.top;
        insets.left = this.insets.left;
        insets.bottom = this.insets.bottom;
        insets.right = this.insets.right;
        return insets;
    }
}

