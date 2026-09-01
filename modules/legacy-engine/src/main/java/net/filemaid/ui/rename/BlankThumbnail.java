package net.filemaid.ui.rename;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import net.filemaid.ui.ThemeSupport;

public class BlankThumbnail
implements Icon {
    public static final BlankThumbnail BLANK_POSTER = new BlankThumbnail(48, 48, ThemeSupport.getBlankBackgroundColor(), ThemeSupport.getPanelSelectionBorderColor(), 0.68f, 1.0f);
    private int width;
    private int height;
    private Color fill;
    private Color draw;
    private float squeezeX;
    private float squeezeY;

    public BlankThumbnail(int n, int n2, Color color, Color color2, float f, float f2) {
        this.width = n;
        this.height = n2;
        this.fill = color;
        this.draw = color2;
        this.squeezeX = f;
        this.squeezeY = f2;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int n, int n2) {
        int n3 = (int)((float)this.width * this.squeezeX);
        int n4 = (int)((float)this.height * this.squeezeY);
        graphics.setColor(this.fill);
        graphics.fillRect(n += (this.width - n3) / 2, n2 += (this.width - n4) / 2, n3, n4);
        graphics.setColor(this.draw);
        graphics.drawRect(n, n2, n3, n4);
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

