package net.filemaid.ui.subtitle;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import net.filemaid.ResourceManager;
import net.filemaid.similarity.Normalization;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.ui.BaseFrame;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.LazyDocumentListener;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;

public class SubtitleViewer
extends BaseFrame {
    private JLabel titleLabel = new JLabel();
    private JLabel infoLabel = new JLabel();
    private SubtitleTableModel model = new SubtitleTableModel();
    private JTable subtitleTable = this.createTable(this.model);
    private JTextField filterEditor = this.createFilterEditor();
    private Color defaultFilterForeground = this.filterEditor.getForeground();
    private Color disabledFilterForeground = ThemeSupport.getPassiveColor();

    public SubtitleViewer(String string) {
        super(string);
        this.titleLabel.setText(string);
        this.titleLabel.setFont(this.titleLabel.getFont().deriveFont(1));
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets dialog, nogrid, novisualpadding, fillx"));
        jPanel.setBackground(ThemeSupport.getPanelBackground());
        jPanel.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
        jPanel.add((Component)this.titleLabel, "wrap, h pref!");
        jPanel.add((Component)this.infoLabel, "gap indent*2, h pref!, wrap");
        JPanel jPanel2 = new JPanel((LayoutManager)new MigLayout("fill, insets dialog, nogrid, novisualpadding", "[fill]", "[pref!][fill]"));
        jPanel2.add((Component)new JLabel("Filter:"), "gap indent:push");
        jPanel2.add((Component)this.filterEditor, "wmin 120px, gap rel");
        jPanel2.add((Component)SwingUI.createImageButton(SwingUI.newAction("Clear Filter", ResourceManager.getIcon("edit.clear"), actionEvent -> this.filterEditor.setText(""))), "w pref!, h pref!, wrap");
        jPanel2.add((Component)new JScrollPane(this.subtitleTable), "grow");
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("fill, novisualpadding, insets 0 0 rel 0"));
        jComponent.add((Component)jPanel, "h min!, growx, dock north");
        jComponent.add((Component)jPanel2, "grow");
        this.setIconImages(ResourceManager.getApplicationIconImages());
        this.setDefaultCloseOperation(2);
        this.setLocationByPlatform(true);
        this.setResizable(true);
        this.pack();
        SwingUI.installAction((JComponent)this.getRootPane(), 27, SwingUI.newAction("Close", actionEvent -> this.dispose()));
    }

    private JTable createTable(TableModel tableModel) {
        JTable jTable = new JTable(tableModel);
        jTable.setBackground(ThemeSupport.getPanelBackground());
        jTable.setAutoCreateRowSorter(true);
        jTable.setFillsViewportHeight(true);
        jTable.setRowHeight(18);
        DefaultTableColumnModel defaultTableColumnModel = (DefaultTableColumnModel)jTable.getColumnModel();
        defaultTableColumnModel.getColumn(0).setMaxWidth(40);
        defaultTableColumnModel.getColumn(1).setMaxWidth(60);
        defaultTableColumnModel.getColumn(2).setMaxWidth(60);
        jTable.setSelectionMode(2);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        jTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer(){

            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
                return super.getTableCellRendererComponent(jTable, object == null ? null : simpleDateFormat.format(object), bl, bl2, n, n2);
            }
        });
        jTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){

            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
                return super.getTableCellRendererComponent(jTable, object == null ? null : Normalization.replaceSpace(object.toString(), " "), bl, bl2, n, n2);
            }
        });
        SwingUI.installAction((JComponent)jTable, 10, SwingUI.newAction("Focus", this::enterFocus));
        jTable.addMouseListener(SwingUI.mouseDoubleClicked(this::enterFocus));
        return jTable;
    }

    private void enterFocus(EventObject eventObject) {
        this.setTableFilter(null);
        Rectangle rectangle = this.subtitleTable.getCellRect(Math.max(this.subtitleTable.getSelectedRow() - 7, 0), 0, true);
        rectangle.height = this.subtitleTable.getSize().height;
        this.subtitleTable.scrollRectToVisible(rectangle);
    }

    private JTextField createFilterEditor() {
        JTextField jTextField = new JTextField(){

            @Override
            protected void processKeyEvent(KeyEvent keyEvent) {
                int n = keyEvent.getKeyCode();
                if (n == 38 || n == 40 || n == 10) {
                    SubtitleViewer.this.subtitleTable.dispatchEvent(keyEvent);
                    return;
                }
                if (n == 8 && !SubtitleViewer.this.filterEditor.getText().isEmpty() && SubtitleViewer.this.getTableFilter() == null) {
                    SubtitleViewer.this.setTableFilter(this.getText());
                    return;
                }
                super.processKeyEvent(keyEvent);
            }
        };
        jTextField.getDocument().addDocumentListener(new LazyDocumentListener(0, documentEvent -> this.setTableFilter(jTextField.getText())));
        return jTextField;
    }

    private RowFilter<?, ?> getTableFilter() {
        TableRowSorter tableRowSorter = (TableRowSorter)this.subtitleTable.getRowSorter();
        return tableRowSorter.getRowFilter();
    }

    private void setTableFilter(String string2) {
        List list = string2 == null ? Collections.emptyList() : RegularExpressions.SPACE.splitAsStream(string2).filter(string -> string.length() > 0).map(SubtitleFilter::new).collect(Collectors.toList());
        TableRowSorter tableRowSorter = (TableRowSorter)this.subtitleTable.getRowSorter();
        tableRowSorter.setRowFilter(list.isEmpty() ? null : RowFilter.andFilter(list));
        this.filterEditor.setForeground(list.isEmpty() ? this.disabledFilterForeground : this.defaultFilterForeground);
    }

    public void setData(List<SubtitleElement> list) {
        this.model.setData(list);
    }

    public JLabel getTitleLabel() {
        return this.titleLabel;
    }

    public JLabel getInfoLabel() {
        return this.infoLabel;
    }

    private static class SubtitleTableModel
    extends AbstractTableModel {
        private List<SubtitleElement> data = Collections.emptyList();

        private SubtitleTableModel() {
        }

        public void setData(List<SubtitleElement> list) {
            this.data = new ArrayList<SubtitleElement>(list);
            this.fireTableDataChanged();
        }

        public SubtitleElement getRow(int n) {
            return this.data.get(n);
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "#";
                }
                case 1: {
                    return "Start";
                }
                case 2: {
                    return "End";
                }
                case 3: {
                    return "Text";
                }
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public int getRowCount() {
            return this.data.size();
        }

        @Override
        public Class<?> getColumnClass(int n) {
            switch (n) {
                case 0: {
                    return Integer.class;
                }
                case 1: {
                    return Date.class;
                }
                case 2: {
                    return Date.class;
                }
                case 3: {
                    return String.class;
                }
            }
            return null;
        }

        @Override
        public Object getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return n + 1;
                }
                case 1: {
                    return this.getRow(n).getStart();
                }
                case 2: {
                    return this.getRow(n).getEnd();
                }
                case 3: {
                    return this.getRow(n).getText();
                }
            }
            return null;
        }
    }

    private static class SubtitleFilter
    extends RowFilter<Object, Integer> {
        private final Pattern filter;

        public SubtitleFilter(String string) {
            this.filter = Pattern.compile(Pattern.quote(string), 386);
        }

        @Override
        public boolean include(RowFilter.Entry<?, ? extends Integer> entry) {
            SubtitleTableModel subtitleTableModel = (SubtitleTableModel)entry.getModel();
            SubtitleElement subtitleElement = subtitleTableModel.getRow(entry.getIdentifier());
            return this.filter.matcher(subtitleElement.getText()).find();
        }
    }
}

