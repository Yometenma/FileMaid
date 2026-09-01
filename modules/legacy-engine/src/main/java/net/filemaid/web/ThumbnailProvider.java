package net.filemaid.web;

import java.awt.Component;
import java.util.concurrent.CompletableFuture;
import javax.swing.Icon;
import net.filemaid.WebServices;

public interface ThumbnailProvider {
    public Icon getThumbnail(int var1, ResolutionVariant var2) throws Exception;

    default public CompletableFuture<Icon> requestThumbnail(int n, ResolutionVariant resolutionVariant) {
        return WebServices.requestPool().async(() -> this.getThumbnail(n, resolutionVariant));
    }

    public static enum ResolutionVariant {
        NORMAL(1),
        HIGH(2);

        public final int scaleFactor;

        private ResolutionVariant(int n2) {
            this.scaleFactor = n2;
        }

        public static ResolutionVariant fromScaleFactor(double d) {
            return d > 1.0 ? HIGH : NORMAL;
        }

        public static ResolutionVariant fromScaleFactor(Component component) {
            return ResolutionVariant.fromScaleFactor(component.getGraphicsConfiguration().getDefaultTransform().getScaleX());
        }
    }
}

