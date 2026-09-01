package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public enum GradientStyle {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_LEFT_TO_BOTTOM_RIGHT,
    BOTTOM_RIGHT_TO_TOP_LEFT,
    TOP_RIGHT_TO_BOTTOM_LEFT,
    BOTTOM_LEFT_TO_TOP_RIGHT;


    public LinearGradientPaint getGradientPaint(RectangularShape rectangularShape, Color color, Color color2) {
        Point2D.Double double_ = null;
        Point2D.Double double_2 = null;
        switch (this) {
            case BOTTOM_TO_TOP: {
                double_ = new Point2D.Double(rectangularShape.getCenterX(), rectangularShape.getMaxY());
                double_2 = new Point2D.Double(rectangularShape.getCenterX(), rectangularShape.getMinY());
                break;
            }
            case TOP_TO_BOTTOM: {
                double_2 = new Point2D.Double(rectangularShape.getCenterX(), rectangularShape.getMaxY());
                double_ = new Point2D.Double(rectangularShape.getCenterX(), rectangularShape.getMinY());
                break;
            }
            case LEFT_TO_RIGHT: {
                double_ = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getCenterY());
                double_2 = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getCenterY());
                break;
            }
            case RIGHT_TO_LEFT: {
                double_2 = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getCenterY());
                double_ = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getCenterY());
                break;
            }
            case TOP_LEFT_TO_BOTTOM_RIGHT: {
                double_ = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getMinY());
                double_2 = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getMaxY());
                break;
            }
            case BOTTOM_RIGHT_TO_TOP_LEFT: {
                double_2 = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getMinY());
                double_ = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getMaxY());
                break;
            }
            case TOP_RIGHT_TO_BOTTOM_LEFT: {
                double_ = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getMinY());
                double_2 = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getMaxY());
                break;
            }
            case BOTTOM_LEFT_TO_TOP_RIGHT: {
                double_2 = new Point2D.Double(rectangularShape.getMaxX(), rectangularShape.getMinY());
                double_ = new Point2D.Double(rectangularShape.getMinX(), rectangularShape.getMaxY());
                break;
            }
            default: {
                return null;
            }
        }
        Color[] colorArray = new Color[]{color, color2};
        float[] fArray = new float[]{0.0f, 1.0f};
        return new LinearGradientPaint(double_, double_2, fArray, colorArray, MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}

