package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class LoadingOverlayPane
extends JComponent
implements PropertyChangeListener {
    public static final String LOADING_PROPERTY = "loading";
    public static final int LOADING_OVERLAY_DELAY = 400;
    private final JComponent animationComponent = ThemeSupport.getProgressIndicator();
    private final AtomicInteger count = new AtomicInteger(0);
    private Timer timer;

    public LoadingOverlayPane(Component component, Component component2) {
        this(component, component2, null, null);
    }

    public LoadingOverlayPane(Component component, Component component2, String string, String string2) {
        this.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
        this.add((Component)this.animationComponent, "pos n " + (string2 != null ? string2 : "8px") + " 100%-" + (string != null ? string : "20px") + " n");
        this.add(component, "grow");
        if (component2 != null) {
            component2.addPropertyChangeListener(LOADING_PROPERTY, this);
        }
    }

    public LoadingOverlayPane(FileBotList<?> fileBotList) {
        this(fileBotList, fileBotList, "37px", "30px");
        TransferablePolicy transferablePolicy = fileBotList.getTransferablePolicy();
        if (transferablePolicy instanceof BackgroundFileTransferablePolicy) {
            ((BackgroundFileTransferablePolicy)transferablePolicy).addPropertyChangeListener(this);
        }
    }

    public LoadingOverlayPane(JScrollPane jScrollPane, BackgroundFileTransferablePolicy<?> backgroundFileTransferablePolicy) {
        this(jScrollPane, null, "30px", "26px");
        backgroundFileTransferablePolicy.addPropertyChangeListener(this);
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return !this.animationComponent.isVisible();
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (LOADING_PROPERTY.equals(propertyChangeEvent.getPropertyName())) {
            this.setLoading((Boolean)propertyChangeEvent.getNewValue());
        }
    }

    public synchronized void setLoading(boolean bl) {
        if (bl) {
            if (this.count.incrementAndGet() >= 1 && this.timer == null) {
                this.timer = SwingUI.invokeLater(400, () -> {
                    this.animationComponent.setVisible(true);
                    this.repaint();
                });
            }
        } else if (this.count.decrementAndGet() <= 0) {
            if (this.timer != null) {
                this.timer.stop();
                this.timer = null;
            }
            SwingUtilities.invokeLater(() -> {
                this.animationComponent.setVisible(false);
                this.repaint();
            });
        }
    }
}

