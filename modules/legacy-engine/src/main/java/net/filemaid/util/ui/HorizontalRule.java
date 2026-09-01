package net.filemaid.util.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class HorizontalRule
implements Border {
    private final Insets insets;
    private final Stroke stroke;
    private final Color foreground;
    private final Color background;
    public static final int DASH_WIDTH = 4;
    public static final BasicStroke DASH = new BasicStroke(1.0f, 1, 1, 1.0f, new float[]{4.0f}, 0.0f);

    public HorizontalRule(Insets insets, Stroke stroke, Color color, Color color2) {
        this.insets = insets;
        this.stroke = stroke;
        this.foreground = color;
        this.background = color2;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(this.insets.top, 0, this.insets.bottom, 0);
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int n, int n2, int n3, int n4) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setStroke(this.stroke);
        if (this.insets.top > 0) {
            graphics2D.setPaint(this.background);
            graphics2D.fillRect(n, n2, n + n3, n2 + this.insets.top);
            graphics2D.setPaint(this.foreground);
            graphics2D.drawLine(n + this.insets.left, n2 + this.insets.top / 2, n + n3 - this.insets.right, n2 + this.insets.top / 2);
        }
        if (this.insets.bottom > 0) {
            graphics2D.setPaint(this.background);
            graphics2D.fillRect(n, n2 + n4 - this.insets.bottom, n + n3, n2 + n4 - this.insets.bottom);
            graphics2D.setPaint(this.foreground);
            graphics2D.drawLine(n + this.insets.left, n2 + n4 - this.insets.bottom / 2, n + n3 - this.insets.right, n2 + n4 - this.insets.bottom / 2);
        }
    }

    public static void north(JComponent jComponent, int n, Color color, Color color2) {
        jComponent.setBorder(new CompoundBorder(new HorizontalRule(new Insets(n, 4, 0, 4), DASH, color, color2), jComponent.getBorder()));
    }

    public static void south(JComponent jComponent, int n, Color color, Color color2) {
        jComponent.setBorder(new CompoundBorder(new HorizontalRule(new Insets(0, 4, n, 4), DASH, color, color2), jComponent.getBorder()));
    }
}

