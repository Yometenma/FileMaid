package net.filemaid.ui.sfv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.TableCellRenderer;
import net.filemaid.Settings;
import net.filemaid.ui.ThemeSupport;

class SwingWorkerCellRenderer
extends JPanel
implements TableCellRenderer {
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    public SwingWorkerCellRenderer() {
        super(new BorderLayout());
        if (ThemeSupport.getTheme().isDark()) {
            this.progressBar.setStringPainted(true);
        } else {
            if (Settings.isWindowsApp()) {
                this.progressBar.setStringPainted(true);
            } else {
                this.progressBar.setStringPainted(false);
            }
            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        this.add((Component)this.progressBar, "Center");
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        this.setBackground(bl ? jTable.getSelectionBackground() : jTable.getBackground());
        this.progressBar.setValue(((SwingWorker)object).getProgress());
        return this;
    }

    @Override
    public void repaint(long l, int n, int n2, int n3, int n4) {
    }

    @Override
    public void repaint(Rectangle rectangle) {
    }

    @Override
    public void repaint() {
    }

    @Override
    protected void firePropertyChange(String string, Object object, Object object2) {
    }
}

