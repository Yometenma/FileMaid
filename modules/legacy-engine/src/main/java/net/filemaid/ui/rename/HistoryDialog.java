package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import net.filemaid.CategoryFileFilter;
import net.filemaid.History;
import net.filemaid.HistorySpooler;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.TextColorizer;
import net.filemaid.ui.transfer.FileExportHandler;
import net.filemaid.ui.transfer.FileTransferablePolicy;
import net.filemaid.ui.transfer.LoadAction;
import net.filemaid.ui.transfer.SaveAction;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.LazyDocumentListener;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;

class HistoryDialog
extends BaseDialog {
    private final JLabel infoLabel = new JLabel();
    private final JTextField filterEditor = new JTextField();
    private final SequenceTableModel sequenceModel = new SequenceTableModel();
    private final ElementTableModel elementModel = new ElementTableModel();
    private final JTable sequenceTable = this.createTable(this.sequenceModel);
    private final JTable elementTable = this.createTable(this.elementModel);
    private final Action closeAction = SwingUI.newAction("Close", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
        this.setVisible(false);
        this.dispose();
    });
    private final Action clearFilterAction = SwingUI.newAction("Clear Filter", ResourceManager.getIcon("edit.clear"), actionEvent -> this.filterEditor.setText(""));
    private final MouseListener revealAction = SwingUI.mouseDoubleClicked(mouseEvent -> {
        List<History.Element> list = this.getMouseSelection((MouseEvent)mouseEvent);
        if (list.isEmpty()) {
            return;
        }
        this.reveal(list);
    });
    private final MouseListener revertPopupMenu = SwingUI.mousePopupMenu(mouseEvent -> {
        List<History.Element> list = this.getMouseSelection((MouseEvent)mouseEvent);
        if (list.isEmpty()) {
            return null;
        }
        JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Revert");
        jPopupMenu.add(SwingUI.newAction("Reveal", ResourceManager.getIcon("action.search"), actionEvent -> this.reveal(list)));
        jPopupMenu.add(new RevertAction("Revert", ResourceManager.getIcon("action.revert"), () -> list, this));
        return jPopupMenu;
    });
    private final FileTransferablePolicy importHandler = new FileTransferablePolicy(){

        @Override
        protected boolean accept(List<File> list) {
            return FileUtilities.containsOnly(list, (FileFilter)this.getFileFilter());
        }

        @Override
        protected void clear() {
        }

        @Override
        protected void load(List<File> list, TransferablePolicy.TransferAction transferAction) throws IOException {
            for (File file : list) {
                try {
                    HistorySpooler.HISTORY.append(History.importHistory(new FileInputStream(file)));
                }
                catch (Exception exception) {
                    Logging.log.log(Level.SEVERE, exception, Logging.cause("Failed to read history file", exception));
                }
            }
            HistoryDialog.this.setModel(HistorySpooler.HISTORY.getCompleteHistory());
        }

        @Override
        public CategoryFileFilter getFileFilter() {
            return new CategoryFileFilter("History", MediaTypes.XML);
        }
    };
    private final FileExportHandler exportHandler = new FileExportHandler(){

        @Override
        public boolean canExport() {
            return true;
        }

        @Override
        public void export(File file) throws IOException {
            try {
                History.exportHistory(HistoryDialog.this.getModel(), false, new FileOutputStream(file));
            }
            catch (Exception exception) {
                Logging.log.log(Level.SEVERE, exception, Logging.cause("Failed to write history file", exception));
            }
        }

        @Override
        public String getDefaultFileName() {
            return "history.xml";
        }

        @Override
        public CategoryFileFilter getFileFilter() {
            return new CategoryFileFilter("History", MediaTypes.XML);
        }
    };

    public HistoryDialog(Window window) {
        super(window, "History");
        JLabel jLabel = new JLabel(this.getTitle());
        jLabel.setFont(jLabel.getFont().deriveFont(1));
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets dialog, nogrid, fillx"));
        jPanel.setBackground(ThemeSupport.getPanelBackground());
        jPanel.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
        jPanel.add((Component)jLabel, "wrap");
        jPanel.add((Component)this.infoLabel, "gap indent*2, wrap");
        JPanel jPanel2 = new JPanel((LayoutManager)new MigLayout("fill, insets dialog, nogrid, novisualpadding", "", "[pref!][150px:pref:200px][200px:pref:max, grow][pref!]"));
        jPanel2.add((Component)new JLabel("Filter:"), "gap indent:push");
        jPanel2.add((Component)this.filterEditor, "wmin 120px, gap rel");
        jPanel2.add((Component)SwingUI.createImageButton(this.clearFilterAction), "w pref!, h pref!, gap right indent, wrap");
        jPanel2.add((Component)SwingUI.createScrollPaneGroup("Sequences", this.sequenceTable), "growx, wrap paragraph");
        jPanel2.add((Component)SwingUI.createScrollPaneGroup("Elements", this.elementTable), "growx, wrap paragraph");
        jPanel2.add((Component)SwingUI.newButton(new LoadAction("Import", ResourceManager.getIcon("action.load"), this::getTransferablePolicy)), "wmin button, hmin 25px, gap indent, sg button");
        jPanel2.add((Component)SwingUI.newButton(new SaveAction("Export", ResourceManager.getIcon("action.save"), this.exportHandler)), "gap rel, sg button");
        jPanel2.add((Component)SwingUI.newButton(new RevertAction("Revert Selection", ResourceManager.getIcon("action.revert"), this::getCurrentSelection, this)), "gap left unrel:push, sgy button");
        jPanel2.add((Component)SwingUI.newButton(this.closeAction), "gap left unrel, gap right indent, sg button");
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("fill, insets 0, nogrid"));
        jComponent.add((Component)jPanel, "h min!, growx, dock north");
        jComponent.add((Component)jPanel2, "grow");
        this.sequenceTable.setSelectionMode(2);
        this.elementTable.setSelectionMode(2);
        this.sequenceTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (listSelectionEvent.getValueIsAdjusting()) {
                return;
            }
            if (this.sequenceTable.getSelectedRow() >= 0) {
                ArrayList<History.Element> arrayList = new ArrayList<History.Element>();
                for (int n : this.sequenceTable.getSelectedRows()) {
                    arrayList.addAll(this.sequenceModel.getRow(this.sequenceTable.convertRowIndexToModel(n)).elements());
                }
                this.elementModel.setData(arrayList);
            }
        });
        this.elementTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (this.elementTable.getSelectedRow() >= 0) {
                this.sequenceTable.getSelectionModel().clearSelection();
            }
        });
        this.sequenceTable.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        this.sequenceTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer(){
            private final DateFormat format = DateFormat.getDateTimeInstance(2, 3);

            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
                return super.getTableCellRendererComponent(jTable, this.format.format(object), bl, bl2, n, n2);
            }
        });
        this.elementTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){

            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
                super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
                if (n2 == 2) {
                    if (HistoryDialog.this.elementModel.isBroken(jTable.convertRowIndexToModel(n))) {
                        this.setIcon(ResourceManager.getIcon("status.link.broken"));
                    } else {
                        this.setIcon(ResourceManager.getIcon("status.link.ok"));
                    }
                } else {
                    this.setIcon(null);
                }
                return this;
            }
        });
        this.filterEditor.getDocument().addDocumentListener(new LazyDocumentListener(documentEvent -> {
            List list = RegularExpressions.SPACE.splitAsStream(this.filterEditor.getText()).map(HistoryFilter::new).collect(Collectors.toList());
            Stream.of(this.sequenceTable, this.elementTable).forEach(jTable -> {
                TableRowSorter tableRowSorter = (TableRowSorter)jTable.getRowSorter();
                tableRowSorter.setRowFilter(RowFilter.andFilter(list));
            });
            if (this.sequenceTable.getSelectedRow() < 0 && this.sequenceTable.getRowCount() > 0) {
                this.sequenceTable.getSelectionModel().addSelectionInterval(0, 0);
            }
        }));
        this.sequenceTable.addMouseListener(this.revealAction);
        this.sequenceTable.addMouseListener(this.revertPopupMenu);
        this.elementTable.addMouseListener(this.revealAction);
        this.elementTable.addMouseListener(this.revertPopupMenu);
        this.setDefaultCloseOperation(2);
        this.setLocationByPlatform(true);
        this.setResizable(true);
        this.setSize(580, 640);
    }

    public void setModel(History history) {
        this.sequenceModel.setData(history.sequences());
        if (this.sequenceTable.getRowCount() > 0) {
            this.sequenceTable.getSelectionModel().addSelectionInterval(0, 0);
        } else {
            this.elementModel.setData(Collections.emptyList());
        }
        this.initializeInfoLabel();
    }

    public History getModel() {
        return new History(this.sequenceModel.getData());
    }

    public JLabel getInfoLabel() {
        return this.infoLabel;
    }

    private void initializeInfoLabel() {
        int n = this.sequenceModel.getData().stream().mapToInt(sequence -> sequence.elements().size()).sum();
        Date date = this.sequenceModel.getData().stream().map(sequence -> sequence.date()).min(Comparator.naturalOrder()).orElseGet(Date::new);
        this.infoLabel.setText(String.format("A total of %,d files have been renamed since %s.", n, DateFormat.getDateInstance().format(date)));
    }

    private JTable createTable(TableModel tableModel) {
        JTable jTable = new JTable(tableModel);
        jTable.setBackground(ThemeSupport.getPanelBackground());
        jTable.setAutoCreateRowSorter(true);
        jTable.setFillsViewportHeight(true);
        jTable.setShowGrid(false);
        jTable.setIntercellSpacing(new Dimension(0, 0));
        DefaultTableColumnModel defaultTableColumnModel = (DefaultTableColumnModel)jTable.getColumnModel();
        defaultTableColumnModel.getColumn(0).setMaxWidth(50);
        return jTable;
    }

    private List<History.Element> getCurrentSelection() {
        ArrayList<History.Element> arrayList;
        block3: {
            block2: {
                arrayList = new ArrayList<History.Element>();
                if (this.sequenceTable.getSelectedRow() < 0) break block2;
                for (int n : this.sequenceTable.getSelectedRows()) {
                    int n2 = this.sequenceTable.convertRowIndexToModel(n);
                    arrayList.addAll(this.sequenceModel.getRow(n2).elements());
                }
                break block3;
            }
            if (this.elementTable.getSelectedRow() < 0) break block3;
            for (int n : this.elementTable.getSelectedRows()) {
                int n3 = this.elementTable.convertRowIndexToModel(n);
                arrayList.add(this.elementModel.getRow(n3));
            }
        }
        return arrayList;
    }

    private List<History.Element> getMouseSelection(MouseEvent mouseEvent) {
        JTable jTable = (JTable)mouseEvent.getSource();
        int n = jTable.rowAtPoint(mouseEvent.getPoint());
        if (n < 0) {
            return Collections.emptyList();
        }
        if (!jTable.getSelectionModel().isSelectedIndex(n)) {
            jTable.getSelectionModel().setSelectionInterval(n, n);
        }
        ArrayList<History.Element> arrayList = new ArrayList<History.Element>();
        for (int n2 : jTable.getSelectedRows()) {
            int n3 = jTable.convertRowIndexToModel(n2);
            if (this.sequenceModel == jTable.getModel()) {
                arrayList.addAll(this.sequenceModel.getRow(n3).elements());
                continue;
            }
            if (this.elementModel != jTable.getModel()) continue;
            arrayList.add(this.elementModel.getRow(n3));
        }
        return arrayList;
    }

    private void reveal(List<History.Element> list) {
        List<File> list2 = this.getRenameMap(null, list).entrySet().stream().flatMap(entry -> Stream.of((File)entry.getKey(), (File)entry.getValue())).filter(File::exists).collect(Collectors.toList());
        UserInteraction.revealFiles(list2);
    }

    private void revert(Map<File, File> map) throws Exception {
        if (Settings.isMacSandbox()) {
            MacAppUtilities.askUnlockFolders(this, Stream.of(map.keySet(), map.values()).flatMap(collection -> collection.stream()).collect(Collectors.toList()));
        }
        SwingUI.disableSuddenTermination(this, () -> {
            List<File> list = GlassProgressMonitor.runTask(new RevertWorker(map), this);
            JLabel jLabel = this.getInfoLabel();
            if (list.size() == map.size()) {
                jLabel.setText(list.size() == 1 ? "1 file has been reverted." : String.format("%,d files have been reverted.", list.size()));
                jLabel.setIcon(ResourceManager.getIcon("status.ok"));
            } else {
                jLabel.setText(list.size() == 1 ? String.format("1 of %,d files has been reverted.", map.size()) : String.format("%,d of %,d files have been reverted.", list.size(), map.size()));
                jLabel.setIcon(ResourceManager.getIcon("status.error"));
            }
            UserInteraction.revealFiles(list);
        });
        this.repaint();
    }

    private Map<File, File> getRenameMap(File file, List<History.Element> list) {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        for (History.Element element : list) {
            File file2 = file != null ? file : element.dir();
            File file3 = new File(element.to());
            File file4 = new File(element.from());
            if (!file3.isAbsolute()) {
                file3 = new File(file2, file == null ? file3.getPath() : file3.getName());
            }
            if (!file4.isAbsolute()) {
                file4 = new File(file2, file == null ? file4.getPath() : file4.getName());
            }
            linkedHashMap.put(file3, file4);
        }
        return linkedHashMap;
    }

    private List<File> getMissingFiles(File file2, List<History.Element> list) {
        return this.getRenameMap(file2, list).keySet().stream().filter(file -> !file.exists()).collect(Collectors.toList());
    }

    public TransferablePolicy getTransferablePolicy() {
        return this.importHandler;
    }

    private static class SequenceTableModel
    extends AbstractTableModel {
        private List<History.Sequence> data = Collections.emptyList();

        private SequenceTableModel() {
        }

        public void setData(List<History.Sequence> list) {
            this.data = new ArrayList<History.Sequence>(list);
            this.fireTableDataChanged();
        }

        public List<History.Sequence> getData() {
            return Collections.unmodifiableList(this.data);
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "#";
                }
                case 1: {
                    return "Name";
                }
                case 2: {
                    return "Date";
                }
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 3;
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
                    return String.class;
                }
                case 2: {
                    return Date.class;
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
                    return this.getName(this.data.get(n));
                }
                case 2: {
                    return this.data.get(n).date();
                }
            }
            return null;
        }

        public History.Sequence getRow(int n) {
            return this.data.get(n);
        }

        private String getName(History.Sequence sequence) {
            return sequence.elements().stream().map(History.Element::dir).filter(Objects::nonNull).map(File::getName).sorted().distinct().collect(Collectors.joining(", "));
        }
    }

    private static class ElementTableModel
    extends AbstractTableModel {
        private History.Element[] data = new History.Element[0];

        private ElementTableModel() {
        }

        public void setData(List<History.Element> list) {
            this.data = list.toArray(new History.Element[0]);
            this.fireTableDataChanged();
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "#";
                }
                case 1: {
                    return "Original Name";
                }
                case 2: {
                    return "New Name";
                }
                case 3: {
                    return "Original Folder";
                }
                case 4: {
                    return "New Folder";
                }
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public Class<?> getColumnClass(int n) {
            switch (n) {
                case 0: {
                    return Integer.class;
                }
                case 1: {
                    return String.class;
                }
                case 2: {
                    return String.class;
                }
                case 3: {
                    return File.class;
                }
                case 4: {
                    return File.class;
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
                    return this.data[n].from();
                }
                case 2: {
                    return new File(this.data[n].to()).getName();
                }
                case 3: {
                    return this.data[n].dir();
                }
                case 4: {
                    return new File(this.data[n].to()).getParentFile();
                }
            }
            return null;
        }

        public History.Element getRow(int n) {
            return this.data[n];
        }

        public boolean isBroken(int n) {
            File file = new File(this.data[n].to());
            if (!file.isAbsolute()) {
                file = new File(this.data[n].dir(), file.getPath());
            }
            return !file.exists();
        }
    }

    private static class RevertAction
    extends AbstractAction {
        private final HistoryDialog parent;
        private final Supplier<List<History.Element>> elements;

        public RevertAction(String string, Icon icon, Supplier<List<History.Element>> supplier, HistoryDialog historyDialog) {
            super(string, icon);
            this.elements = supplier;
            this.parent = historyDialog;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            List<History.Element> list = this.elements.get();
            if (list.size() > 0) {
                this.prompt(null, list, actionEvent);
            }
        }

        private void prompt(File file, List<History.Element> list, ActionEvent actionEvent) {
            List<File> list2 = this.parent.getMissingFiles(file, list);
            if (list2.size() > 0) {
                GlassOptionPane glassOptionPane2 = new GlassOptionPane();
                glassOptionPane2.initConfirmDialog("Missing Files", ResourceManager.getIcon("status.link.broken"), this.getChangeDirectoryMessage(list2));
                glassOptionPane2.confirm.putValue("Name", "Change Directory");
                glassOptionPane2.open(this.parent, glassOptionPane -> {
                    if (glassOptionPane.isConfirmed()) {
                        File file2 = UserFiles.showOpenDialogSelectFolder(file, "Change Directory", actionEvent);
                        this.prompt(file2, list, actionEvent);
                    }
                });
                return;
            }
            if (SwingUI.isShiftOrAltDown(actionEvent)) {
                GlassOptionPane glassOptionPane3 = new GlassOptionPane();
                glassOptionPane3.initConfirmDialog("Revert Files", ResourceManager.getIcon("action.revert"), this.getRevertMessage(list.size()));
                glassOptionPane3.confirm.putValue("Name", "Select Directory");
                glassOptionPane3.open(this.parent, glassOptionPane -> {
                    if (glassOptionPane.isConfirmed()) {
                        SwingUI.withWaitCursor((Object)this.parent, () -> {
                            File file4 = UserFiles.showOpenDialogSelectFolder(null, "Select Directory", actionEvent);
                            if (file4 != null) {
                                Map map = this.parent.getRenameMap(file, list).entrySet().stream().collect(Collectors.toMap(entry -> (File)entry.getKey(), entry -> new File(file4, ((File)entry.getValue()).getName()), (fileA, fileB) -> fileA, LinkedHashMap::new));
                                this.parent.revert(map);
                            }
                        });
                    }
                });
                return;
            }
            GlassOptionPane glassOptionPane4 = new GlassOptionPane();
            glassOptionPane4.initConfirmDialog("Revert Files", ResourceManager.getIcon("action.revert"), this.getRevertMessage(list.size()));
            glassOptionPane4.confirm.putValue("Name", "Revert");
            glassOptionPane4.open(this.parent, glassOptionPane -> {
                if (glassOptionPane.isConfirmed()) {
                    SwingUI.withWaitCursor((Object)this.parent, () -> {
                        Map<File, File> map = this.parent.getRenameMap(file, list);
                        this.parent.revert(map);
                    });
                }
            });
        }

        private String getRevertMessage(int n) {
            StringBuilder stringBuilder = new StringBuilder(512);
            stringBuilder.append("<html>");
            stringBuilder.append((String)(n == 1 ? "Do you want to revert 1 file?" : "Do you want to revert " + n + " files?"));
            stringBuilder.append("<br>");
            stringBuilder.append("<br>");
            stringBuilder.append("</html>");
            return stringBuilder.toString();
        }

        private String getChangeDirectoryMessage(List<File> list) {
            StringBuilder stringBuilder = new StringBuilder(512);
            stringBuilder.append("<html>");
            TextColorizer textColorizer = new TextColorizer("<nobr>\u2022 ", "</nobr><br>");
            for (int i = 0; i < list.size(); ++i) {
                if (i > 20) {
                    stringBuilder.append("\u2022 ").append("\u2026").append("<br>");
                    break;
                }
                textColorizer.colorizePath(stringBuilder, list.get(i).getName(), true);
            }
            stringBuilder.append("<br>");
            stringBuilder.append("</html>");
            return stringBuilder.toString();
        }
    }

    private static class RevertWorker
    implements GlassProgressMonitor.ProgressWorker<List<File>> {
        private final Map<File, File> renameMap;

        public RevertWorker(Map<File, File> map) {
            this.renameMap = map;
        }

        @Override
        public String getName() {
            return "Revert " + this.renameMap.size() + " " + (this.renameMap.size() == 1 ? "file" : "files");
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("action.revert");
        }

        @Override
        public String getDescription() {
            return "Preparing...";
        }

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public List<File> call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            ArrayList<File> arrayList = new ArrayList<File>(this.renameMap.size());
            try {
                for (Map.Entry<File, File> entry : this.renameMap.entrySet()) {
                    biConsumer.accept(arrayList.size(), this.renameMap.size());
                    consumer2.accept(entry.getKey().getName());
                    XattrMetaInfo.xattr.clear(entry.getKey());
                    arrayList.add(StandardRenameAction.revert(entry.getKey(), entry.getValue()));
                }
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
            return arrayList;
        }
    }

    private static class HistoryFilter
    extends RowFilter<Object, Integer> {
        private final Pattern filter;

        public HistoryFilter(String string) {
            this.filter = Pattern.compile(Pattern.quote(string), 386);
        }

        @Override
        public boolean include(RowFilter.Entry<?, ? extends Integer> entry) {
            if (entry.getModel() instanceof SequenceTableModel) {
                SequenceTableModel sequenceTableModel = (SequenceTableModel)entry.getModel();
                for (History.Element element : sequenceTableModel.getRow(entry.getIdentifier()).elements()) {
                    if (!this.include(element)) continue;
                    return true;
                }
                return false;
            }
            if (entry.getModel() instanceof ElementTableModel) {
                ElementTableModel elementTableModel = (ElementTableModel)entry.getModel();
                return this.include(elementTableModel.getRow(entry.getIdentifier()));
            }
            throw new IllegalArgumentException("Illegal model: " + entry.getModel());
        }

        private boolean include(History.Element element) {
            return this.include(element.to()) || this.include(element.from()) || this.include(element.dir().getPath());
        }

        private boolean include(String string) {
            return this.filter.matcher(string).find();
        }
    }
}

