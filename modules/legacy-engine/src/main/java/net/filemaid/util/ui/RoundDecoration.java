package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;

public class RoundDecoration
implements Icon {
    private Icon icon;
    private Color fill;
    private Color draw;
    private int width;
    private int height;

    public RoundDecoration(Icon icon, int n, int n2, Color color, Color color2) {
        this.icon = icon;
        this.width = n;
        this.height = n2;
        this.fill = color;
        this.draw = color2;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int n, int n2) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ellipse2D.Float float_ = new Ellipse2D.Float(n, n2, this.width - 1, this.height - 1);
        graphics2D.setColor(this.fill);
        graphics2D.fill(float_);
        graphics2D.setColor(this.draw);
        graphics2D.draw(float_);
        this.icon.paintIcon(component, graphics, n + (this.width - this.icon.getIconWidth()) / 2, n2 + (this.height - this.icon.getIconHeight()) / 2);
    }

    @Override
    public int getIconWidth() {
        return this.width;
    }

    @Override
    public int getIconHeight() {
        return this.height;
    }
}

