package net.filemaid.ui.sfv;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import net.filemaid.ui.sfv.ChecksumComputationService;
import net.miginfocom.swing.MigLayout;

class TotalProgressPanel
extends JComponent {
    private final JProgressBar progressBar = new JProgressBar(0, 0);
    private final PropertyChangeListener progressListener = new PropertyChangeListener(){
        private static final String SHOW = "show";
        private static final String HIDE = "hide";
        private final DelayedToggle delayed = new DelayedToggle();

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            int n = this.getComputationService(propertyChangeEvent).getCompletedTaskCount();
            int n2 = this.getComputationService(propertyChangeEvent).getTotalTaskCount();
            SwingUtilities.invokeLater(() -> {
                if (n == n2) {
                    this.delayed.toggle(HIDE, 400, actionEvent -> TotalProgressPanel.this.setVisible(actionEvent.getActionCommand() == SHOW));
                } else if (n2 != 0) {
                    this.delayed.toggle(SHOW, 400, actionEvent -> TotalProgressPanel.this.setVisible(actionEvent.getActionCommand() == SHOW));
                }
                if (n2 != 0) {
                    TotalProgressPanel.this.progressBar.setValue(n);
                    TotalProgressPanel.this.progressBar.setMaximum(n2);
                    TotalProgressPanel.this.progressBar.setString(n + " / " + n2);
                }
            });
        }

        private ChecksumComputationService getComputationService(PropertyChangeEvent propertyChangeEvent) {
            return (ChecksumComputationService)propertyChangeEvent.getSource();
        }
    };

    public TotalProgressPanel(ChecksumComputationService checksumComputationService) {
        this.setLayout((LayoutManager)new MigLayout("insets 1px"));
        this.setBorder(new TitledBorder("Total Progress"));
        this.progressBar.setStringPainted(true);
        this.add((Component)this.progressBar, "growx");
        this.setVisible(false);
        checksumComputationService.addPropertyChangeListener(this.progressListener);
    }

    protected static class DelayedToggle {
        private Timer timer = null;

        protected DelayedToggle() {
        }

        public void toggle(String string, int n, ActionListener actionListener) {
            if (this.timer != null) {
                if (string.equals(this.timer.getActionCommand())) {
                    return;
                }
                this.timer.stop();
            }
            this.timer = new Timer(n, actionListener);
            this.timer.setActionCommand(string);
            this.timer.setRepeats(false);
            this.timer.start();
        }
    }
}

