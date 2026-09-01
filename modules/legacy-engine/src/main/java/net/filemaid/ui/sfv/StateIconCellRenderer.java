package net.filemaid.ui.sfv;

import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.ResourceManager;
import net.filemaid.ui.sfv.ChecksumRow;

class StateIconCellRenderer
extends DefaultTableCellRenderer {
    private final Map<ChecksumRow.State, Icon> icons = new EnumMap<ChecksumRow.State, Icon>(ChecksumRow.State.class);

    public StateIconCellRenderer() {
        this.icons.put(ChecksumRow.State.UNKNOWN, ResourceManager.getIcon("status.unknown"));
        this.icons.put(ChecksumRow.State.OK, ResourceManager.getIcon("status.ok"));
        this.icons.put(ChecksumRow.State.WARNING, ResourceManager.getIcon("status.warning"));
        this.icons.put(ChecksumRow.State.ERROR, ResourceManager.getIcon("status.error"));
        this.setVerticalAlignment(0);
        this.setHorizontalAlignment(0);
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, null, bl, false, n, n2);
        this.setIcon(this.icons.get(object));
        return this;
    }
}

