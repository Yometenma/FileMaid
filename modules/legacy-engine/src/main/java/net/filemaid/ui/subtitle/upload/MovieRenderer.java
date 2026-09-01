package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;

class MovieRenderer
extends DefaultTableCellRenderer {
    private Icon icon;

    public MovieRenderer(Icon icon) {
        this.icon = icon;
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
        if (object != null) {
            Movie movie = (Movie)object;
            this.setText(movie.toString());
            this.setToolTipText(String.format("%s [%s]", movie, Link.IMDb.getID(movie)));
            this.setIcon(this.icon);
            this.setForeground(jTable.getForeground());
        } else {
            this.setText("<Click to select movie / series>");
            this.setToolTipText(null);
            this.setIcon(null);
            this.setForeground(ThemeSupport.getPassiveColor());
        }
        return this;
    }
}

