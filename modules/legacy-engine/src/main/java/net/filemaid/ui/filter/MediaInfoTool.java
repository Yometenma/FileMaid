package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;
import java.nio.channels.ClosedByInterruptException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.stream.IntStream;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.filter.Tool;
import net.filemaid.ui.transfer.DefaultTableExportHandler;
import net.filemaid.ui.transfer.SaveAction;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class MediaInfoTool
extends Tool<TableModel> {
    private JTable table = new JTable(new MediaInfoTableModel());

    public MediaInfoTool() {
        super("MediaInfo");
        this.table.setAutoCreateRowSorter(true);
        this.table.setAutoCreateColumnsFromModel(true);
        this.table.setFillsViewportHeight(true);
        this.table.setCellSelectionEnabled(true);
        this.table.setSelectionMode(2);
        this.table.setBackground(ThemeSupport.getPanelBackground());
        this.table.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        this.table.setRowHeight(25);
        JScrollPane jScrollPane = new JScrollPane(this.table);
        jScrollPane.setBorder(ThemeSupport.getHorizontalRule());
        this.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
        this.add((Component)new LoadingOverlayPane(jScrollPane, this, "25px", "30px"), "grow");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets 10px, nogrid, novisualpadding, fill", "align center"));
        jComponent.add(SwingUI.newButton(new SaveAction(new DefaultTableExportHandler(this.table))));
        this.add((Component)jComponent, "dock south");
    }

    @Override
    protected TableModel createModelInBackground(List<File> list) {
        if (list.isEmpty()) {
            return new MediaInfoTableModel();
        }
        List<File> list2 = FileUtilities.listFiles(list, FileUtilities.filter(MediaTypes.VIDEO_FILES, MediaTypes.AUDIO_FILES, MediaTypes.IMAGE_FILES), FileUtilities.HUMAN_NAME_ORDER);
        TreeMap<MediaInfoKey, String[]> treeMap = new TreeMap<MediaInfoKey, String[]>();
        IntStream.range(0, list2.size()).forEach(n -> {
            File file = (File)list2.get(n);
            if (MediaTypes.VIDEO_FILES.accept(file) && file.length() > 1000000L || MediaTypes.AUDIO_FILES.accept(file) && file.length() > 1000L || MediaTypes.IMAGE_FILES.accept(file) && file.length() > 0L) {
                try {
                    MediaInfoTable.read(file).forEach((streamKind, values) -> IntStream.range(0, values.size()).forEach(n2 -> values.get(n2).forEach((string, string2) -> {
                        String[] stringArray = treeMap.computeIfAbsent(new MediaInfoKey(streamKind, n2, string), mediaInfoKey -> new String[values.size()]);
                        stringArray[n2] = string2;
                    })));
                }
                catch (IllegalArgumentException | IllegalStateException | ClosedByInterruptException exception) {
                    Logging.debug.finest(Logging.cause("MediaInfo", exception));
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("MediaInfo", exception));
                }
            }
            if (Thread.interrupted()) {
                throw new CancellationException();
            }
        });
        return new MediaInfoTableModel(treeMap.isEmpty() ? Collections.emptyList() : list2, treeMap);
    }

    @Override
    protected void setModel(TableModel tableModel) {
        this.table.setModel(tableModel);
        this.table.setAutoResizeMode(this.table.getRowCount() > 0 ? 0 : 2);
        TableColumnModel tableColumnModel = this.table.getColumnModel();
        IntStream.range(0, tableColumnModel.getColumnCount()).forEach(n -> tableColumnModel.getColumn(n).setMinWidth(150));
    }

    private static class MediaInfoTableModel
    extends AbstractTableModel {
        private final MediaInfoKey[] keys;
        private final String[][] values;
        private final String[] files;
        private final Class<?>[] columnClass;

        public MediaInfoTableModel() {
            this(Collections.emptyList(), Collections.emptyMap());
        }

        public MediaInfoTableModel(List<File> list, Map<MediaInfoKey, String[]> map) {
            this.keys = map.keySet().toArray(new MediaInfoKey[0]);
            this.values = map.values().toArray(new String[0][]);
            this.files = (String[])list.stream().map(File::getName).toArray(String[]::new);
            this.columnClass = new Class[this.getColumnCount()];
        }

        public int getHeaderColumnCount() {
            return 1;
        }

        @Override
        public int getColumnCount() {
            return this.keys.length + this.getHeaderColumnCount();
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "File";
                }
            }
            return this.keys[n - this.getHeaderColumnCount()].toString();
        }

        private boolean isNumber(String string) {
            try {
                Double.parseDouble(string);
                return true;
            }
            catch (Exception exception) {
                return false;
            }
        }

        @Override
        public Class<?> getColumnClass(int n) {
            int n3 = n - this.getHeaderColumnCount();
            if (n3 < 0) {
                return String.class;
            }
            if (this.columnClass[n3] != null) {
                return this.columnClass[n3];
            }
            if (IntStream.range(0, this.files.length).mapToObj(n2 -> this.values[n3][n2]).filter(Objects::nonNull).allMatch(this::isNumber)) {
                this.columnClass[n3] = Number.class;
                return this.columnClass[n3];
            }
            this.columnClass[n3] = String.class;
            return this.columnClass[n3];
        }

        @Override
        public int getRowCount() {
            return this.files.length;
        }

        @Override
        public String getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return this.files[n];
                }
            }
            return this.values[n2 - this.getHeaderColumnCount()][n];
        }
    }

    private static class MediaInfoKey
    implements Comparable<MediaInfoKey> {
        public final StreamKind kind;
        public final int stream;
        public final String name;

        public MediaInfoKey(StreamKind streamKind, int n, String string) {
            this.kind = streamKind;
            this.stream = n;
            this.name = RegularExpressions.NON_WORD.matcher(string).replaceAll("");
        }

        public boolean equals(Object object) {
            if (object instanceof MediaInfoKey) {
                MediaInfoKey mediaInfoKey = (MediaInfoKey)object;
                return this.kind == mediaInfoKey.kind && this.stream == mediaInfoKey.stream && this.name.equals(mediaInfoKey.name);
            }
            return false;
        }

        public int hashCode() {
            return this.kind.ordinal() + (this.stream << 8) + this.name.hashCode();
        }

        @Override
        public int compareTo(MediaInfoKey mediaInfoKey) {
            if (this.kind != mediaInfoKey.kind) {
                return this.kind.compareTo(mediaInfoKey.kind);
            }
            if (this.stream != mediaInfoKey.stream) {
                return Integer.compare(this.stream, mediaInfoKey.stream);
            }
            return this.name.compareTo(mediaInfoKey.name);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (this.kind != StreamKind.General) {
                stringBuilder.append(this.kind.name());
                if (this.stream > 0) {
                    stringBuilder.append('[').append(this.stream).append(']');
                }
                stringBuilder.append('.');
            }
            return stringBuilder.append(this.name).toString();
        }
    }
}

