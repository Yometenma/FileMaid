package net.filemaid.ui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.LinkButton;
import net.miginfocom.swing.MigLayout;

public class HistoryPanel
extends JPanel {
    private final List<JLabel> columnHeaders = new ArrayList<JLabel>(3);

    public HistoryPanel() {
        super((LayoutManager)new MigLayout("fillx, insets 10 30 10 50, wrap 3"));
        this.setBackground(ThemeSupport.getPanelBackground());
        this.setOpaque(true);
        this.setupHeader();
    }

    private void setupHeader() {
        for (int i = 0; i < 3; ++i) {
            JLabel jLabel = new JLabel();
            jLabel.setFont(jLabel.getFont().deriveFont(1));
            this.columnHeaders.add(jLabel);
            this.add((Component)jLabel, this.getHeaderConstraint(i));
        }
    }

    private String getHeaderConstraint(int n) {
        switch (n) {
            case 0: {
                return "align left, gapbefore 24";
            }
            case 1: {
                return "align center";
            }
        }
        return "align right, gapafter 12";
    }

    public void setColumnHeader(int n, String string) {
        this.columnHeaders.get(n).setText(string);
    }

    public void add(String string, URI uRI, Icon icon, String string2, String string3) {
        JComponent jComponent = uRI != null ? new LinkButton(string, null, icon, uRI) : new JLabel(string, icon, 2);
        JLabel jLabel = new JLabel(string2, 4);
        JLabel jLabel2 = new JLabel(string3, 4);
        this.add((Component)jComponent, "align left");
        this.add((Component)jLabel, "align center, wmin 100");
        this.add((Component)jLabel2, "align right");
    }
}

