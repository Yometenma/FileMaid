package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import net.filemaid.util.ui.StackBlurImageFilter;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class GlassPane
extends JComponent {
    private final JRootPane rootPane;
    private Image glassBuffer;
    private Future<Image> glassBufferTask;
    public static final Function<BufferedImage, BufferedImage> BLUR = bufferedImage -> new StackBlurImageFilter(4, 3).filter((BufferedImage)bufferedImage);

    public GlassPane(JRootPane jRootPane) {
        this.setLayout((LayoutManager)new MigLayout("fill, nogrid, insets dialog"));
        this.rootPane = jRootPane;
        this.addMouseListener(SwingUI.mouseClicked(InputEvent::consume));
        this.setOpaque(false);
        this.setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (this.glassBuffer != null) {
            graphics.drawImage(this.glassBuffer, this.getX(), this.getY(), this.getWidth(), this.getHeight(), null);
        }
    }

    private BufferedImage captureContentBuffer() {
        Container container = this.rootPane.getContentPane();
        BufferedImage bufferedImage = new BufferedImage(container.getWidth(), container.getHeight(), 1);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        container.print(graphics2D);
        graphics2D.dispose();
        return bufferedImage;
    }

    public void effect(Function<BufferedImage, BufferedImage> function) {
        if (this.glassBufferTask != null) {
            this.glassBufferTask.cancel(true);
        }
        BufferedImage bufferedImage = this.captureContentBuffer();
        this.glassBufferTask = SwingUI.onSwingWorker(() -> (Image)function.apply(bufferedImage), this::publish);
    }

    private void publish(Image image) {
        this.glassBuffer = image;
        this.setOpaque(true);
        this.rootPane.repaint();
    }

    public void clear() {
        this.removeAll();
        if (this.glassBufferTask != null) {
            this.glassBufferTask.cancel(true);
            this.glassBufferTask = null;
        }
        this.glassBuffer = null;
        this.setOpaque(false);
    }

    public static GlassPane install(Component component, JRootPane jRootPane, Function<BufferedImage, BufferedImage> function) {
        GlassPane glassPane = new GlassPane(jRootPane);
        glassPane.add(component, "pos 0.5al 0.25al");
        Component component2 = jRootPane.getGlassPane();
        jRootPane.setGlassPane(glassPane);
        if (component.isVisible()) {
            glassPane.effect(function);
        }
        component.addComponentListener(SwingUI.componentShown(componentEvent -> glassPane.effect(function)));
        component.addComponentListener(SwingUI.componentHidden(componentEvent -> {
            glassPane.setVisible(false);
            glassPane.clear();
        }));
        glassPane.addComponentListener(SwingUI.componentHidden(componentEvent -> {
            jRootPane.setGlassPane(component2);
            component2.setVisible(false);
            jRootPane.revalidate();
        }));
        jRootPane.revalidate();
        component.requestFocusInWindow();
        return glassPane;
    }
}

