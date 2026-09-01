package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import java.io.File;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;

class FileRenderer
extends DefaultTableCellRenderer {
    FileRenderer() {
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
        if (object != null) {
            File file = (File)object;
            this.setText(file.getName());
            this.setToolTipText(file.getPath());
            if (MediaTypes.SUBTITLE_FILES.accept(file)) {
                this.setIcon(ResourceManager.getIcon("file.subtitle"));
            } else if (MediaTypes.VIDEO_FILES.accept(file)) {
                this.setIcon(ResourceManager.getIcon("file.video"));
            }
            this.setForeground(jTable.getForeground());
        } else {
            this.setText("<Click to select video file>");
            this.setToolTipText(null);
            this.setIcon(null);
            this.setForeground(ThemeSupport.getPassiveColor());
        }
        return this;
    }
}

