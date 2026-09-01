package net.filemaid.util.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.filemaid.util.ui.BufferedGraphics;

public class ProgressIndicator
extends JComponent {
    private final double radius = 4.0;
    private final int shapeCount = 3;
    private final Stroke stroke = new BasicStroke(2.0f, 0, 1);
    private final Dimension baseSize = new Dimension(32, 32);
    private final Rectangle2D frame;
    private final int refresh = 16;
    private final int stepCount = 20;
    private int step;
    private final Color progressShapeColor;
    private final Color backgroundShapeColor;
    private final List<BufferedGraphics> bufferedGraphics;
    private final Timer updateTimer;

    public ProgressIndicator(Color color, Color color2) {
        this.frame = new Rectangle2D.Double(4.0, 4.0, (double)this.baseSize.width - 8.0 - 1.0, (double)this.baseSize.height - 8.0 - 1.0);
        this.step = 0;
        this.bufferedGraphics = Stream.generate(BufferedGraphics::new).limit(20L).collect(Collectors.toList());
        this.updateTimer = new Timer(16, actionEvent -> {
            this.stepAnimation();
            this.repaint();
        });
        this.progressShapeColor = color;
        this.backgroundShapeColor = color2;
        this.setPreferredSize(this.baseSize);
        this.setVisible(false);
        this.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentShown(ComponentEvent componentEvent) {
                ProgressIndicator.this.startAnimation();
            }

            @Override
            public void componentHidden(ComponentEvent componentEvent) {
                ProgressIndicator.this.stopAnimation();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        this.bufferedGraphics.get(this.step).draw((Graphics2D)graphics, this::render, this);
    }

    protected void render(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double d = Math.min(this.getWidth(), this.getHeight());
        graphics2D.scale(d / (double)this.baseSize.width, d / (double)this.baseSize.height);
        Ellipse2D.Double double_ = new Ellipse2D.Double();
        double_.setFrame(this.frame);
        graphics2D.setStroke(this.stroke);
        graphics2D.setPaint(this.backgroundShapeColor);
        graphics2D.draw(double_);
        Point2D.Double double_2 = new Point2D.Double(this.frame.getCenterX(), this.frame.getMinY());
        double_.setFrameFromCenter(double_2, new Point2D.Double(((Point2D)double_2).getX() + 4.0, ((Point2D)double_2).getY() + 4.0));
        graphics2D.setStroke(this.stroke);
        graphics2D.setPaint(this.progressShapeColor);
        graphics2D.rotate(this.getTheta(this.step, 60.0), this.frame.getCenterX(), this.frame.getCenterY());
        double d2 = this.getTheta(1.0, 3.0);
        for (int i = 0; i < 3; ++i) {
            graphics2D.rotate(d2, this.frame.getCenterX(), this.frame.getCenterY());
            graphics2D.fill(double_);
        }
    }

    private double getTheta(double d, double d2) {
        return d / d2 * 2.0 * Math.PI;
    }

    private void stepAnimation() {
        ++this.step;
        if (this.step >= 20) {
            this.step = 0;
        }
    }

    public void startAnimation() {
        this.updateTimer.start();
    }

    public void stopAnimation() {
        this.updateTimer.stop();
        this.bufferedGraphics.forEach(BufferedGraphics::dispose);
    }
}

