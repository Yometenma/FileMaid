package net.filemaid.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.filemaid.ui.BaseFrame;
import net.filemaid.ui.Mode;
import net.filemaid.util.ui.SwingEventBus;

public class SinglePanelFrame
extends BaseFrame {
    private final Mode mode;

    public SinglePanelFrame(Mode mode) {
        super(mode.toString());
        this.mode = mode;
        this.setSize(860, 640);
        this.setMinimumSize(new Dimension(740, 340));
    }

    private void showPanel(Mode mode) {
        if (this.getContentPane().getComponentCount() > 0) {
            return;
        }
        JComponent jComponent = mode.createPanel();
        this.getContentPane().add((Component)jComponent, "Center");
        SwingEventBus.getInstance().register(jComponent);
        SwingUtilities.invokeLater(jComponent::requestFocusInWindow);
    }

    @Override
    public void setVisible(boolean bl) {
        if (bl) {
            this.showPanel(this.mode);
        }
        super.setVisible(bl);
    }
}

