package net.filemaid.ui.sfv;

import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.sfv.ChecksumCell;
import net.filemaid.ui.sfv.ChecksumCellRenderer;
import net.filemaid.ui.sfv.ChecksumRow;
import net.filemaid.ui.sfv.ChecksumTableModel;
import net.filemaid.ui.sfv.HighlightPatternCellRenderer;
import net.filemaid.ui.sfv.StateIconCellRenderer;
import net.filemaid.util.ui.SwingUI;

class ChecksumTable
extends JTable {
    public ChecksumTable() {
        this.setFillsViewportHeight(true);
        this.setAutoCreateRowSorter(true);
        this.setAutoCreateColumnsFromModel(true);
        this.setAutoResizeMode(2);
        this.setSelectionMode(2);
        this.setRowHeight(20);
        this.setDragEnabled(true);
        this.setUI(new SwingUI.DragDropRowTableUI());
        this.setBackground(ThemeSupport.getPanelBackground());
        this.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        this.setDefaultRenderer(String.class, new HighlightPatternCellRenderer(VerificationUtilities.EMBEDDED_CHECKSUM));
        this.setDefaultRenderer(ChecksumRow.State.class, new StateIconCellRenderer());
        this.setDefaultRenderer(ChecksumCell.class, new ChecksumCellRenderer());
    }

    @Override
    protected ChecksumTableModel createDefaultDataModel() {
        return new ChecksumTableModel();
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(this.columnModel){

            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                try {
                    int n = this.columnModel.getColumnIndexAtX(mouseEvent.getX());
                    int n2 = this.columnModel.getColumn(n).getModelIndex();
                    return ChecksumTable.this.getModel().getColumnRoot(n2).getPath();
                }
                catch (Exception exception) {
                    return null;
                }
            }
        };
    }

    @Override
    public ChecksumTableModel getModel() {
        return (ChecksumTableModel)super.getModel();
    }

    @Override
    public void createDefaultColumnsFromModel() {
        super.createDefaultColumnsFromModel();
        for (int i = 0; i < this.getColumnCount(); ++i) {
            TableColumn tableColumn = this.getColumnModel().getColumn(i);
            if (i == 0) {
                tableColumn.setPreferredWidth(45);
                continue;
            }
            if (i == 1) {
                tableColumn.setPreferredWidth(400);
                continue;
            }
            if (i < 2) continue;
            tableColumn.setPreferredWidth(150);
        }
    }
}

