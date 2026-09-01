package net.filemaid.util.ui.notification;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import javax.swing.border.AbstractBorder;
import net.filemaid.util.ui.GradientStyle;

public class SeparatorBorder
extends AbstractBorder {
    private int borderWidth;
    private Color beginColor;
    private Color endColor;
    private GradientStyle gradientStyle;
    private Position position;

    public SeparatorBorder(int n, Color color, Position position) {
        this(n, color, null, null, position);
    }

    public SeparatorBorder(int n, Color color, GradientStyle gradientStyle, Position position) {
        this(n, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0), gradientStyle, position);
    }

    public SeparatorBorder(int n, Color color, Color color2, GradientStyle gradientStyle, Position position) {
        this.borderWidth = n;
        this.beginColor = color;
        this.endColor = color2;
        this.gradientStyle = gradientStyle;
        this.position = position;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int n, int n2, int n3, int n4) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        Rectangle2D rectangle2D = this.position.getRectangle(new Rectangle2D.Double(n, n2, n3, n4), this.borderWidth);
        if (this.gradientStyle != null && this.endColor != null) {
            graphics2D.setPaint(this.gradientStyle.getGradientPaint(rectangle2D, this.beginColor, this.endColor));
        } else {
            graphics2D.setPaint(this.beginColor);
        }
        graphics2D.fill(rectangle2D);
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return this.getBorderInsets(component, new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        return this.position.getInsets(insets, this.borderWidth);
    }

    public Color getBeginColor() {
        return this.beginColor;
    }

    public void setBeginColor(Color color) {
        this.beginColor = color;
    }

    public Color getEndColor() {
        return this.endColor;
    }

    public void setEndColor(Color color) {
        this.endColor = color;
    }

    public GradientStyle getGradientStyle() {
        return this.gradientStyle;
    }

    public void setGradientStyle(GradientStyle gradientStyle) {
        this.gradientStyle = gradientStyle;
    }

    public int getBorderWidth() {
        return this.borderWidth;
    }

    public void setBorderWidth(int n) {
        this.borderWidth = n;
    }

    public Position getPosition() {
        return this.position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public static enum Position {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT;


        public Rectangle2D getRectangle(RectangularShape rectangularShape, int n) {
            switch (this) {
                case TOP: {
                    return new Rectangle2D.Double(rectangularShape.getX(), rectangularShape.getY(), rectangularShape.getWidth(), n);
                }
                case BOTTOM: {
                    return new Rectangle2D.Double(rectangularShape.getX(), rectangularShape.getMaxY() - (double)n, rectangularShape.getWidth(), n);
                }
                case LEFT: {
                    return new Rectangle2D.Double(rectangularShape.getX(), rectangularShape.getY(), n, rectangularShape.getHeight());
                }
                case RIGHT: {
                    return new Rectangle2D.Double(rectangularShape.getMaxX() - (double)n, rectangularShape.getY(), n, rectangularShape.getHeight());
                }
            }
            return null;
        }

        public Insets getInsets(Insets insets, int n) {
            switch (this) {
                case TOP: {
                    insets.top = n;
                    insets.bottom = 0;
                    insets.right = 0;
                    insets.left = 0;
                    return insets;
                }
                case BOTTOM: {
                    insets.bottom = n;
                    insets.top = 0;
                    insets.right = 0;
                    insets.left = 0;
                    return insets;
                }
                case LEFT: {
                    insets.left = n;
                    insets.bottom = 0;
                    insets.top = 0;
                    insets.right = 0;
                    return insets;
                }
                case RIGHT: {
                    insets.right = n;
                    insets.bottom = 0;
                    insets.top = 0;
                    insets.left = 0;
                    return insets;
                }
            }
            return null;
        }
    }
}

