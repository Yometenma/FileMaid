package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import net.filemaid.Language;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;

class LanguageRenderer
implements TableCellRenderer,
ListCellRenderer {
    private DefaultTableCellRenderer tableCell = new DefaultTableCellRenderer();
    private DefaultListCellRenderer listCell = new DefaultListCellRenderer();

    LanguageRenderer() {
    }

    private Component configure(JLabel jLabel, JComponent jComponent, Object object, boolean bl, boolean bl2) {
        if (object != null) {
            Language language = (Language)object;
            jLabel.setText(language.getName());
            jLabel.setIcon(ResourceManager.getFlagIcon(language.getCode()));
            jLabel.setForeground(jComponent.getForeground());
        } else {
            jLabel.setText("<Click to select language>");
            jLabel.setIcon(null);
            jLabel.setForeground(ThemeSupport.getPassiveColor());
        }
        return jLabel;
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        return this.configure((DefaultTableCellRenderer)this.tableCell.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2), jTable, object, bl, bl2);
    }

    public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        return this.configure((DefaultListCellRenderer)this.listCell.getListCellRendererComponent((JList<?>)jList, object, n, bl, bl2), jList, object, bl, bl2);
    }
}

