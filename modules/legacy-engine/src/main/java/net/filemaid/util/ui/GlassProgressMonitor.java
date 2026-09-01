package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.AccumulativeRunnable;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;

public class GlassProgressMonitor {
    private final Window window;
    private final JPanel dialogPane = new JPanel((LayoutManager)new MigLayout("insets 0"));
    private final JLabel headerText = new JLabel();
    private final JLabel progressCounter = new JLabel();
    private final JLabel contentText = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
    private final Action cancel = SwingUI.newAction("Cancel", actionEvent -> this.cancel());
    private final AccumulativeRunnable publisher = new AccumulativeRunnable(SwingUtilities::invokeLater);
    private Component glassPane;
    private Timer timer;
    private int millisToDecideToPopup = 500;
    private int millisToPopup = 2000;

    public GlassProgressMonitor(Window window, String string, Icon icon, String string2, boolean bl) {
        this.window = window;
        this.headerText.setText(string);
        this.headerText.setIcon(icon);
        this.headerText.setIconTextGap(10);
        this.contentText.setText(string2);
        this.progressCounter.setVisible(false);
        this.progressBar.setIndeterminate(bl);
        this.progressBar.setStringPainted(false);
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill, insets dialog"));
        jPanel.setBackground(ThemeSupport.getPanelBackground());
        jPanel.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
        jPanel.add((Component)this.headerText, "wmax 220px");
        jPanel.add((Component)this.progressCounter, "hidemode 3, align right");
        this.dialogPane.add((Component)jPanel, "hmin 60px, growx, dock north");
        JPanel jPanel2 = new JPanel((LayoutManager)new MigLayout("fill, insets dialog"));
        jPanel2.add((Component)this.contentText, "hmin 20px, growx, wrap rel, w 300px!");
        jPanel2.add((Component)this.progressBar, "hmin 20px, growx, wrap paragraph, push");
        jPanel2.add((Component)SwingUI.newButton(this.cancel), "align center");
        this.dialogPane.add((Component)jPanel2, "dock center");
        this.dialogPane.setBorder(SwingUI.shadow());
        this.dialogPane.setOpaque(false);
        this.dialogPane.setVisible(false);
    }

    public void cancel() {
        this.cancel.putValue("Name", "Cancelling...");
        this.cancel.putValue("SwingSelectedKey", true);
        this.cancel.setEnabled(false);
    }

    public boolean isCancelled() {
        return !this.cancel.isEnabled();
    }

    public void publishHeaderText(String string) {
        this.publisher.submit(() -> this.headerText.setText(string));
    }

    public void publishContentText(String string) {
        this.publisher.submit(() -> this.contentText.setText(string));
    }

    public void publishProgress(int n, int n2) {
        this.publisher.submit(() -> {
            if (n > 0 && n < n2) {
                this.progressBar.setValue(n);
                this.progressBar.setMaximum(n2);
                this.progressBar.setIndeterminate(false);
                if (this.isVisible()) {
                    SwingUI.setWindowProgressValue(this.window, n, n2);
                }
            } else {
                this.progressBar.setIndeterminate(true);
                if (this.isVisible()) {
                    SwingUI.setWindowProgressState(this.window, true);
                }
            }
            if (n >= 0 && n2 > 1) {
                this.progressCounter.setVisible(true);
                this.progressCounter.setText(n + 1 + " of " + n2);
            } else {
                this.progressCounter.setVisible(false);
                this.progressCounter.setText(null);
            }
        });
    }

    private void setVisible(boolean bl) {
        this.timer.stop();
        if (bl && !this.progressBar.isIndeterminate()) {
            SwingUI.setWindowProgressValue(this.window, this.progressBar.getValue(), this.progressBar.getMaximum());
        } else {
            SwingUI.setWindowProgressState(this.window, bl);
        }
        this.dialogPane.setVisible(bl);
        this.glassPane.setVisible(bl);
    }

    public boolean isVisible() {
        return this.glassPane != null && this.glassPane.isVisible();
    }

    public void openLater(boolean bl) {
        this.glassPane = SwingUI.createGlassPane(this.dialogPane, this.window);
        this.timer = SwingUI.invokeLater(bl ? this.millisToPopup : this.millisToDecideToPopup, () -> {
            if (bl || this.progressBar.getValue() <= this.progressBar.getMaximum() / (this.millisToPopup / this.millisToDecideToPopup)) {
                this.setVisible(true);
            }
        });
        if (this.glassPane instanceof JComponent) {
            this.glassPane.setVisible(true);
        }
        if (this.glassPane instanceof JDialog) {
            JDialog jDialog = (JDialog)this.glassPane;
            jDialog.setDefaultCloseOperation(0);
            jDialog.addWindowListener(SwingUI.windowClosed(windowEvent -> this.cancel()));
            this.window.setEnabled(false);
        }
    }

    public void close() {
        this.publisher.submit(() -> {
            this.setVisible(false);
            this.glassPane.setVisible(false);
            if (this.glassPane instanceof JDialog) {
                this.window.setEnabled(true);
            }
        });
    }

    public static <T> T runTask(ProgressWorker<T> progressWorker, Window window) throws Exception {
        GlassProgressMonitor glassProgressMonitor = new GlassProgressMonitor(window, progressWorker.getName(), progressWorker.getIcon(), progressWorker.getDescription(), progressWorker.isIndeterminate());
        glassProgressMonitor.openLater(progressWorker.isIndeterminate());
        return (T)SwingUI.onSecondaryLoop(() -> {
            try {
                Object t = progressWorker.call(glassProgressMonitor::publishHeaderText, glassProgressMonitor::publishContentText, glassProgressMonitor::publishProgress, glassProgressMonitor::isCancelled);
                return t;
            }
            finally {
                glassProgressMonitor.close();
            }
        });
    }

    public static interface ProgressWorker<T> {
        public static final int INDETERMINATE = -1;

        public String getName();

        public Icon getIcon();

        public String getDescription();

        public boolean isIndeterminate();

        public T call(Consumer<String> var1, Consumer<String> var2, BiConsumer<Integer, Integer> var3, Supplier<Boolean> var4) throws Exception;
    }
}

