package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.GradientStyle;

public class FancyTreeCellRenderer
extends DefaultTreeCellRenderer {
    private Color gradientBeginColor;
    private Color gradientEndColor;
    private GradientStyle gradientStyle;
    private boolean paintGradient;
    private Color backgroundSelectionColor;

    public FancyTreeCellRenderer() {
        this(GradientStyle.TOP_TO_BOTTOM);
    }

    public FancyTreeCellRenderer(GradientStyle gradientStyle) {
        this.gradientStyle = gradientStyle;
        this.backgroundSelectionColor = this.getBackgroundSelectionColor();
        this.setBackgroundSelectionColor(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree jTree, Object object, boolean bl, boolean bl2, boolean bl3, int n, boolean bl4) {
        super.getTreeCellRendererComponent(jTree, object, bl, bl2, bl3, n, false);
        this.setIconTextGap(5);
        if (bl && !ThemeSupport.getTheme().isDark()) {
            this.setPaintGradient(true);
            this.setGradientBeginColor(this.backgroundSelectionColor.brighter());
            this.setGradientEndColor(this.backgroundSelectionColor);
        } else {
            this.setPaintGradient(false);
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (this.isPaintGradient()) {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n = this.getLabelStart() - 2;
            int n2 = 16;
            RoundRectangle2D.Double double_ = new RoundRectangle2D.Double(n, 1.0, this.getWidth() - n, this.getHeight() - 2, n2, n2);
            graphics2D.setPaint(this.gradientStyle.getGradientPaint(double_, this.gradientBeginColor, this.gradientEndColor));
            graphics2D.fill(double_);
        }
        super.paintComponent(graphics);
    }

    protected int getLabelStart() {
        Icon icon = this.getIcon();
        if (icon != null && this.getText() != null) {
            return icon.getIconWidth() + Math.max(0, this.getIconTextGap() - 1);
        }
        return 0;
    }

    public Color getGradientBeginColor() {
        return this.gradientBeginColor;
    }

    public void setGradientBeginColor(Color color) {
        this.gradientBeginColor = color;
    }

    public boolean isPaintGradient() {
        return this.paintGradient;
    }

    public void setPaintGradient(boolean bl) {
        this.paintGradient = bl;
    }

    public Color getGradientEndColor() {
        return this.gradientEndColor;
    }

    public void setGradientEndColor(Color color) {
        this.gradientEndColor = color;
    }

    public GradientStyle getGradientStyle() {
        return this.gradientStyle;
    }

    public void setGradientStyle(GradientStyle gradientStyle) {
        this.gradientStyle = gradientStyle;
    }
}

