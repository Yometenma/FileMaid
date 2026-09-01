package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import net.filemaid.util.ui.GradientStyle;

public abstract class AbstractFancyListCellRenderer
extends JPanel
implements ListCellRenderer {
    private Color gradientBeginColor;
    private Color gradientEndColor;
    private Color highlightColor;
    private boolean borderPainted = false;
    private boolean gradientPainted = false;
    private GradientStyle gradientStyle = GradientStyle.TOP_TO_BOTTOM;
    private boolean highlightingEnabled = true;
    private final Insets margin;
    private static final Insets DEFAULT_PADDING = new Insets(7, 7, 7, 7);
    private static final Insets DEFAULT_MARGIN = new Insets(1, 1, 0, 1);

    public AbstractFancyListCellRenderer() {
        this(DEFAULT_PADDING, DEFAULT_MARGIN, null);
    }

    public AbstractFancyListCellRenderer(Insets insets) {
        this(insets, DEFAULT_MARGIN, null);
    }

    public AbstractFancyListCellRenderer(Insets insets, Insets insets2) {
        this(insets, insets2, null);
    }

    public AbstractFancyListCellRenderer(Insets insets, Insets insets2, Color color) {
        this.setLayout(new FlowLayout(0, 0, 0));
        AbstractBorder abstractBorder = null;
        if (insets != null) {
            abstractBorder = new EmptyBorder(insets);
        }
        if (color != null) {
            abstractBorder = new CompoundBorder(new LineBorder(color, 1), abstractBorder);
        }
        if (insets2 != null) {
            this.margin = insets2;
            abstractBorder = new CompoundBorder(new EmptyBorder(insets2), abstractBorder);
        } else {
            this.margin = new Insets(0, 0, 0, 0);
        }
        this.setBorder(abstractBorder);
        this.setOpaque(false);
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        if (this.borderPainted) {
            super.paintBorder(graphics);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        Rectangle2D.Double double_ = new Rectangle2D.Double(this.margin.left, this.margin.top, this.getWidth() - (this.margin.left + this.margin.right), this.getHeight() - (this.margin.top + this.margin.bottom));
        if (this.isOpaque()) {
            graphics2D.setPaint(this.getBackground());
            graphics2D.fill(double_);
        }
        if (this.highlightingEnabled && this.highlightColor != null) {
            graphics2D.setPaint(this.highlightColor);
            graphics2D.fill(double_);
        }
        if (this.gradientPainted) {
            graphics2D.setPaint(this.gradientStyle.getGradientPaint(double_, this.gradientBeginColor, this.gradientEndColor));
            graphics2D.fill(double_);
        }
        super.paintComponent(graphics);
    }

    public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        this.configureListCellRendererComponent(jList, object, n, bl, bl2);
        this.invalidate();
        this.validate();
        return this;
    }

    protected void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        this.setGradientPainted(bl);
        this.setBorderPainted(bl);
        Color color = jList.getSelectionBackground();
        if (bl) {
            this.setGradientColors(color.brighter(), color);
        }
        if (this.highlightingEnabled && n % 2 == 0) {
            this.setHighlightColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
        } else {
            this.setHighlightColor(null);
        }
        if (bl) {
            this.setBackground(jList.getSelectionBackground());
            this.setForeground(jList.getSelectionForeground());
        } else {
            this.setBackground(jList.getBackground());
            this.setForeground(jList.getForeground());
        }
    }

    public void setGradientColors(Color color, Color color2) {
        this.gradientBeginColor = color;
        this.gradientEndColor = color2;
    }

    public Color getGradientBeginColor() {
        return this.gradientBeginColor;
    }

    public Color getGradientEndColor() {
        return this.gradientEndColor;
    }

    public void setHighlightColor(Color color) {
        this.highlightColor = color;
    }

    public void setGradientStyle(GradientStyle gradientStyle) {
        this.gradientStyle = gradientStyle;
    }

    public void setHighlightingEnabled(boolean bl) {
        this.highlightingEnabled = bl;
    }

    public void setBorderPainted(boolean bl) {
        this.borderPainted = bl;
    }

    public void setGradientPainted(boolean bl) {
        this.gradientPainted = bl;
    }

    public Color getHighlightColor() {
        return this.highlightColor;
    }

    public boolean isBorderPainted() {
        return this.borderPainted;
    }

    public GradientStyle getGradientStyle() {
        return this.gradientStyle;
    }

    public boolean isHighlightingEnabled() {
        return this.highlightingEnabled;
    }

    @Override
    public void repaint() {
    }

    @Override
    public void repaint(long l, int n, int n2, int n3, int n4) {
    }

    @Override
    public void repaint(Rectangle rectangle) {
    }

    @Override
    protected void firePropertyChange(String string, Object object, Object object2) {
    }

    @Override
    public void firePropertyChange(String string, byte by, byte by2) {
    }

    @Override
    public void firePropertyChange(String string, char c, char c2) {
    }

    @Override
    public void firePropertyChange(String string, short s, short s2) {
    }

    @Override
    public void firePropertyChange(String string, int n, int n2) {
    }

    @Override
    public void firePropertyChange(String string, long l, long l2) {
    }

    @Override
    public void firePropertyChange(String string, float f, float f2) {
    }

    @Override
    public void firePropertyChange(String string, double d, double d2) {
    }

    @Override
    public void firePropertyChange(String string, boolean bl, boolean bl2) {
    }
}

