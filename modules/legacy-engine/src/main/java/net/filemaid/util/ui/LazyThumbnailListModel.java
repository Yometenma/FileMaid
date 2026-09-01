package net.filemaid.util.ui;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import net.filemaid.Logging;
import net.filemaid.util.ui.BufferedGraphics;

public class LazyThumbnailListModel<T>
extends AbstractListModel<T> {
    private final Object[] items;
    private final Future<Icon>[] icons;
    private final Function<T, Icon> previewLoader;
    private final Function<T, CompletableFuture<Icon>> imageLoader;

    public LazyThumbnailListModel(Object[] objectArray, Function<T, Icon> function, Function<T, CompletableFuture<Icon>> function2) {
        this.items = objectArray;
        this.icons = new Future[objectArray.length];
        this.previewLoader = function;
        this.imageLoader = function2;
    }

    @Override
    public int getSize() {
        return this.items.length;
    }

    @Override
    public T getElementAt(int n) {
        return (T)this.items[n];
    }

    public Icon getPreviewIcon(int n) {
        return this.previewLoader.apply(this.getElementAt(n));
    }

    public Icon getIcon(int n) {
        if (!BufferedGraphics.VOLATILE_IMAGE_BUFFER_ENABLED) {
            return this.getPreviewIcon(n);
        }
        Future<Icon> future = this.icons[n];
        if (future != null && future.isDone()) {
            try {
                return future.get();
            }
            catch (Exception exception) {
                return null;
            }
        }
        T t = this.getElementAt(n);
        if (future != null) {
            return this.previewLoader.apply(t);
        }
        if (this.imageLoader == null) {
            Icon icon2 = this.previewLoader.apply(t);
            this.icons[n] = CompletableFuture.completedFuture(icon2);
            return icon2;
        }
        this.icons[n] = this.imageLoader.apply(t).handle((icon, throwable) -> {
            if (icon != null) {
                SwingUtilities.invokeLater(() -> this.fireContentsChanged(this, n, n));
                return icon;
            }
            if (throwable != null) {
                Logging.debug.warning(Logging.cause(this, throwable));
            }
            return this.previewLoader.apply(t);
        });
        return this.previewLoader.apply(t);
    }
}

