package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import net.filemaid.Logging;
import net.filemaid.UserInteraction;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class MediaInfoDialog
extends BaseDialog {
    public MediaInfoDialog(Window window, MediaInfoTable mediaInfoTable) {
        super(window, "MediaInfo");
        Action action = SwingUI.newAction("Close", actionEvent -> this.setVisible(false));
        Action action2 = SwingUI.newAction("Copy to Clipboard", actionEvent -> {
            UserInteraction.copy(this.exportMediaInfo(mediaInfoTable));
            action.actionPerformed((ActionEvent)actionEvent);
        });
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("nogrid, fill", "[align center]", "[fill][pref!]"));
        jComponent.add((Component)this.createMediaInfoTable(mediaInfoTable), "grow, wrap paragraph");
        jComponent.add((Component)SwingUI.newButton(action2), "wmin 80px, wrap");
        this.pack();
        SwingUI.installAction(jComponent, 27, action);
    }

    private JComponent createMediaInfoTable(MediaInfoTable mediaInfoTable) {
        RowFilter rowFilter = RowFilter.notFilter(RowFilter.regexFilter("^StreamKind|^UniqueID|^StreamOrder|^ID|Count$", new int[0]));
        JTabbedPane jTabbedPane = new JTabbedPane(1, 1);
        mediaInfoTable.forEach((streamKind, list) -> list.forEach(map -> {
            JTable jTable = new JTable(new ParameterTableModel((Map<?, ?>)map));
            jTable.setAutoCreateRowSorter(true);
            jTable.setAutoCreateColumnsFromModel(true);
            jTable.setFillsViewportHeight(true);
            jTable.setAutoResizeMode(2);
            jTable.setSelectionMode(2);
            jTable.setBackground(ThemeSupport.getPanelBackground());
            jTable.setGridColor(ThemeSupport.getColor(0xEEEEEE));
            jTable.setRowHeight(25);
            TableRowSorter tableRowSorter = (TableRowSorter)jTable.getRowSorter();
            tableRowSorter.setRowFilter(rowFilter);
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill"));
            jPanel.setOpaque(false);
            jPanel.add((Component)new JScrollPane(jTable), "grow");
            jTabbedPane.addTab(this.getTabTitle((StreamKind)((Object)streamKind)), jPanel);
        }));
        return jTabbedPane;
    }

    private String getTabTitle(StreamKind streamKind) {
        switch (streamKind) {
            case General: {
                return "Media";
            }
        }
        return streamKind.toString();
    }

    private String exportMediaInfo(MediaInfoTable mediaInfoTable) {
        Formatter formatter = new Formatter(new StringBuilder(16384), Locale.ROOT);
        mediaInfoTable.forEach((streamKind, list) -> list.forEach(map -> {
            int n = map.keySet().stream().mapToInt(String::length).max().orElse(0);
            String string = "%-" + n + "s : %s%n";
            formatter.format("[%s]%n", streamKind);
            map.forEach((string2, string3) -> formatter.format(string, string2, string3));
            formatter.format("%n", new Object[0]);
        }));
        return formatter.toString();
    }

    public static void show(File file, Window window) {
        SwingUI.withWaitCursor((Object)window, () -> {
            try {
                return MediaInfoTable.read(file);
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
                return null;
            }
        }).ifPresent(mediaInfoTable -> {
            MediaInfoDialog mediaInfoDialog = new MediaInfoDialog(window, (MediaInfoTable)mediaInfoTable);
            mediaInfoDialog.setLocationRelativeTo(window);
            mediaInfoDialog.setVisible(true);
        });
    }

    private static class ParameterTableModel
    extends AbstractTableModel {
        private final List<Map.Entry<?, ?>> data;

        public ParameterTableModel(Map<?, ?> map) {
            this.data = new ArrayList(map.entrySet());
        }

        @Override
        public int getRowCount() {
            return this.data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "Parameter";
                }
                case 1: {
                    return "Value";
                }
            }
            return null;
        }

        @Override
        public Object getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return this.data.get(n).getKey();
                }
                case 1: {
                    return this.data.get(n).getValue();
                }
            }
            return null;
        }
    }
}

