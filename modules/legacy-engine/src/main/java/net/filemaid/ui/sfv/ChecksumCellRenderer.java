package net.filemaid.ui.sfv;

import java.awt.Color;
import java.awt.Component;
import java.io.FileNotFoundException;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.Logging;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.sfv.ChecksumRow;
import net.filemaid.ui.sfv.ChecksumTableModel;
import net.filemaid.ui.sfv.SwingWorkerCellRenderer;

public class ChecksumCellRenderer
extends DefaultTableCellRenderer {
    private final SwingWorkerCellRenderer progressRenderer = new SwingWorkerCellRenderer();
    private final Color verificationForeground = ThemeSupport.getVerificationColor();

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        boolean bl3;
        boolean bl4 = false;
        if (object instanceof SwingWorker) {
            if (((SwingWorker)object).getState() != SwingWorker.StateValue.PENDING) {
                return this.progressRenderer.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
            }
            bl4 = true;
        }
        super.getTableCellRendererComponent(jTable, object, bl, false, n, n2);
        boolean bl5 = bl3 = jTable.getValueAt(n, 0) == ChecksumRow.State.ERROR;
        this.setForeground(bl ? jTable.getSelectionForeground() : (bl3 ? ThemeSupport.getErrorColor() : (this.isVerificationColumn(jTable, n2) ? this.verificationForeground : jTable.getForeground())));
        this.setBackground(bl ? jTable.getSelectionBackground() : jTable.getBackground());
        this.setFont(this.getFont().deriveFont(bl3 ? 1 : 0));
        if (bl4) {
            this.setText("Pending...");
        } else if (object == null && !bl) {
            this.setBackground(ThemeSupport.withAlpha(jTable.getGridColor(), 0.1f));
        } else if (object instanceof FileNotFoundException) {
            this.setText("File not found");
        } else if (object instanceof Throwable) {
            this.setText(Logging.getRootCauseMessage((Throwable)object));
        }
        return this;
    }

    private boolean isVerificationColumn(JTable jTable, int n) {
        ChecksumTableModel checksumTableModel = (ChecksumTableModel)jTable.getModel();
        int n2 = jTable.getColumnModel().getColumn(n).getModelIndex();
        return checksumTableModel.isVerificationColumn(n2);
    }
}

