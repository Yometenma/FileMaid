package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

public class SelectionPainter
implements Border {
    private Color color;

    public SelectionPainter(Color color) {
        this.color = color;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int n, int n2, int n3, int n4) {
        graphics.setColor(this.color);
        graphics.fillRect(n, n2, n3, n4);
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(0, 0, 0, 0);
    }
}

