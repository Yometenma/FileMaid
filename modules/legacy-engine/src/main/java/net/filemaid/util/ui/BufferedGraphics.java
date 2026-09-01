package net.filemaid.util.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.VolatileImage;
import java.util.function.Consumer;

public class BufferedGraphics {
    private VolatileImage volatileImage;
    public static final Color CLEAR = new Color(255, 255, 255, 0);
    public static final boolean VOLATILE_IMAGE_BUFFER_ENABLED = Boolean.parseBoolean(System.getProperty("swing.volatileImageBufferEnabled", "true"));

    public void draw(Graphics2D graphics2D, Consumer<Graphics2D> consumer, Component component) {
        if (!VOLATILE_IMAGE_BUFFER_ENABLED) {
            consumer.accept(graphics2D);
            return;
        }
        if (this.volatileImage == null) {
            this.volatileImage = this.createVolatileImage(component);
        }
        do {
            int n;
            if ((n = this.volatileImage.validate(component.getGraphicsConfiguration())) != 0) {
                if (n == 2) {
                    this.volatileImage = this.createVolatileImage(component);
                }
                Graphics2D graphics2D2 = this.volatileImage.createGraphics();
                graphics2D2.setComposite(AlphaComposite.Clear);
                graphics2D2.setColor(CLEAR);
                graphics2D2.fillRect(0, 0, this.volatileImage.getWidth(), this.volatileImage.getHeight());
                graphics2D2.setComposite(AlphaComposite.SrcOver);
                consumer.accept(graphics2D2);
                graphics2D2.dispose();
            }
            graphics2D.drawImage(this.volatileImage, 0, 0, component);
        } while (this.volatileImage.contentsLost());
    }

    public void dispose() {
        if (this.volatileImage != null) {
            this.volatileImage.flush();
            this.volatileImage = null;
        }
    }

    private VolatileImage createVolatileImage(Component component) {
        return component.getGraphicsConfiguration().createCompatibleVolatileImage(component.getWidth(), component.getHeight(), 3);
    }
}

