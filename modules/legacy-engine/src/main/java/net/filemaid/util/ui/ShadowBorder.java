package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import javax.swing.border.AbstractBorder;
import net.filemaid.util.ui.GradientStyle;

public class ShadowBorder
extends AbstractBorder {
    private int smoothness;
    private int smoothnessOffset;
    private int offset;

    public ShadowBorder() {
        this(2, 2, 12);
    }

    public ShadowBorder(int n, int n2, int n3) {
        this.offset = n;
        this.smoothness = n2;
        this.smoothnessOffset = n3;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int n, int n2, int n3, int n4) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        Color color = new Color(0, 0, 0, 81);
        Color color2 = new Color(0, 0, 0, 0);
        int n5 = this.smoothness + this.smoothnessOffset;
        Rectangle2D.Double double_ = new Rectangle2D.Double(n5, n5, n3 - n5 * 2, n4 - n5 * 2);
        graphics2D.setPaint(color);
        graphics2D.fill(double_);
        Rectangle2D.Double double_2 = new Rectangle2D.Double(double_.getMaxX(), n5, n5, ((RectangularShape)double_).getHeight());
        Rectangle2D.Double double_3 = new Rectangle2D.Double(0.0, n5, n5, ((RectangularShape)double_).getHeight());
        Rectangle2D.Double double_4 = new Rectangle2D.Double(n5, 0.0, ((RectangularShape)double_).getWidth(), n5);
        Rectangle2D.Double double_5 = new Rectangle2D.Double(n5, double_.getMaxY(), ((RectangularShape)double_).getWidth(), n5);
        graphics2D.setPaint(GradientStyle.LEFT_TO_RIGHT.getGradientPaint(double_2, color, color2));
        graphics2D.fill(double_2);
        graphics2D.setPaint(GradientStyle.RIGHT_TO_LEFT.getGradientPaint(double_3, color, color2));
        graphics2D.fill(double_3);
        graphics2D.setPaint(GradientStyle.BOTTOM_TO_TOP.getGradientPaint(double_4, color, color2));
        graphics2D.fill(double_4);
        graphics2D.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(double_5, color, color2));
        graphics2D.fill(double_5);
        Rectangle2D.Double double_6 = new Rectangle2D.Double(0.0, 0.0, n5, n5);
        Rectangle2D.Double double_7 = new Rectangle2D.Double(n3 - n5, 0.0, n5, n5);
        Rectangle2D.Double double_8 = new Rectangle2D.Double(0.0, n4 - n5, n5, n5);
        Rectangle2D.Double double_9 = new Rectangle2D.Double(n3 - n5, n4 - n5, n5, n5);
        graphics2D.setPaint(CornerGradientStyle.TOP_LEFT.getGradientPaint(double_6, n5, color, color2));
        graphics2D.fill(double_6);
        graphics2D.setPaint(CornerGradientStyle.TOP_RIGHT.getGradientPaint(double_7, n5, color, color2));
        graphics2D.fill(double_7);
        graphics2D.setPaint(CornerGradientStyle.BOTTOM_LEFT.getGradientPaint(double_8, n5, color, color2));
        graphics2D.fill(double_8);
        graphics2D.setPaint(CornerGradientStyle.BOTTOM_RIGHT.getGradientPaint(double_9, n5, color, color2));
        graphics2D.fill(double_9);
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return this.getBorderInsets(component, new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.top = insets.left = Math.max(this.smoothness - this.offset, 4);
        insets.bottom = insets.right = Math.max(this.smoothness + this.offset, 4);
        return insets;
    }

    private static enum CornerGradientStyle {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;


        public RadialGradientPaint getGradientPaint(RectangularShape rectangularShape, float f, Color color, Color color2) {
            Point2D.Double double_ = null;
            switch (this) {
                case TOP_LEFT: {
                    double_ = new Point2D.Double(rectangularShape.getX() + (double)f, rectangularShape.getY() + (double)f);
                    break;
                }
                case TOP_RIGHT: {
                    double_ = new Point2D.Double(rectangularShape.getX() + 0.0, rectangularShape.getY() + (double)f);
                    break;
                }
                case BOTTOM_LEFT: {
                    double_ = new Point2D.Double(rectangularShape.getX() + (double)f, rectangularShape.getY() + 0.0);
                    break;
                }
                case BOTTOM_RIGHT: {
                    double_ = new Point2D.Double(rectangularShape.getX() + 0.0, rectangularShape.getY() + 0.0);
                    break;
                }
                default: {
                    return null;
                }
            }
            float[] fArray = new float[]{0.0f, 1.0f};
            Color[] colorArray = new Color[]{color, color2};
            return new RadialGradientPaint(double_, f, fArray, colorArray);
        }
    }
}

