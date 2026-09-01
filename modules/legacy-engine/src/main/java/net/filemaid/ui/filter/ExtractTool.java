package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.archive.Archive;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.filter.Tool;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.vfs.FileInfo;
import net.miginfocom.swing.MigLayout;

class ExtractTool
extends Tool<TableModel> {
    private JTable table = new JTable(new ArchiveEntryModel());
    private final Action extractAction = SwingUI.newAction("Extract All", ResourceManager.getIcon("package.extract"), actionEvent -> {
        List<File> list = ((ArchiveEntryModel)this.table.getModel()).getArchiveList();
        if (list.isEmpty()) {
            return;
        }
        File file = UserFiles.showOpenDialogSelectFolder(list.get(0).getParentFile(), "Extract to ...", actionEvent);
        if (file == null) {
            return;
        }
        Window window = SwingUI.getWindow(this);
        SwingUI.withWaitCursor((Object)window, () -> {
            ExtractWorker extractWorker = new ExtractWorker(list, file);
            GlassProgressMonitor.runTask(extractWorker, window);
            UserInteraction.open(file);
        });
    });

    public ExtractTool() {
        super("Archives");
        this.table.setAutoCreateRowSorter(true);
        this.table.setAutoCreateColumnsFromModel(true);
        this.table.setFillsViewportHeight(true);
        this.table.setAutoResizeMode(2);
        this.table.setSelectionMode(2);
        this.table.setBackground(ThemeSupport.getPanelBackground());
        this.table.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        this.table.setRowHeight(25);
        JScrollPane jScrollPane = new JScrollPane(this.table);
        jScrollPane.setBorder(ThemeSupport.getHorizontalRule());
        this.setLayout((LayoutManager)new MigLayout("insets 0, nogrid, fill", "align center", "[fill]0px[pref!]"));
        this.add((Component)new LoadingOverlayPane(jScrollPane, this, "25px", "30px"), "grow, wrap");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets 10px, nogrid, novisualpadding, fill", "align center"));
        jComponent.add(SwingUI.newButton(this.extractAction));
        this.add((Component)jComponent, "dock south");
    }

    @Override
    protected void setModel(TableModel tableModel) {
        this.table.setModel(tableModel);
    }

    @Override
    protected TableModel createModelInBackground(List<File> list) throws Exception {
        if (list.isEmpty()) {
            return new ArchiveEntryModel();
        }
        ArrayList<ArchiveEntry> arrayList = new ArrayList<ArchiveEntry>();
        for (File file : FileUtilities.listFiles(list, Archive::isArchive, FileUtilities.HUMAN_NAME_ORDER)) {
            try (Archive archive = Archive.open(file);){
                archive.listFiles().stream().filter(fileInfo -> !fileInfo.getName().startsWith(".")).map(fileInfo -> ArchiveEntry.of(file, fileInfo)).forEach(arrayList::add);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to read archive file", file, exception));
            }
            if (!Thread.interrupted()) continue;
            throw new CancellationException();
        }
        return new ArchiveEntryModel(arrayList);
    }

    private static class ArchiveEntryModel
    extends AbstractTableModel {
        private final ArchiveEntry[] data;

        public ArchiveEntryModel() {
            this.data = new ArchiveEntry[0];
        }

        public ArchiveEntryModel(Collection<ArchiveEntry> collection) {
            this.data = collection.toArray(new ArchiveEntry[collection.size()]);
        }

        public List<File> getArchiveList() {
            return Stream.of(this.data).map(archiveEntry -> archiveEntry.archive).distinct().collect(Collectors.toList());
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "File";
                }
                case 1: {
                    return "Path";
                }
                case 2: {
                    return "Size";
                }
            }
            return null;
        }

        @Override
        public Object getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return this.data[n].file;
                }
                case 1: {
                    return this.data[n].path;
                }
                case 2: {
                    return this.data[n].size;
                }
            }
            return null;
        }
    }

    private static class ExtractWorker
    implements GlassProgressMonitor.ProgressWorker<Void> {
        private final File[] archives;
        private final File outputFolder;

        public ExtractWorker(Collection<File> collection, File file) {
            this.archives = collection.toArray(new File[collection.size()]);
            this.outputFolder = file;
        }

        @Override
        public String getName() {
            return String.format("Extract %,d %s", this.archives.length, this.archives.length == 1 ? "archive" : "archives");
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("package.extract");
        }

        @Override
        public String getDescription() {
            return "Extracting files...";
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public Void call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            for (File file : this.archives) {
                consumer2.accept("Extracting " + file.getName());
                try (Archive archive = Archive.open(file);){
                    boolean bl = archive.listFiles().stream().anyMatch(fileInfo -> {
                        File targetFile = new File(this.outputFolder, fileInfo.getPath());
                        return !targetFile.exists() || targetFile.length() != fileInfo.getLength();
                    });
                    if (bl) {
                        archive.extract(this.outputFolder);
                    }
                }
                catch (Exception exception) {
                    Logging.log.warning(Logging.cause("Failed to extract archive", file.getName(), exception));
                }
                if (!supplier.get().booleanValue()) continue;
                throw new CancellationException();
            }
            return null;
        }
    }

    private static class ArchiveEntry {
        public final File archive;
        public final String file;
        public final String path;
        public final String size;

        public ArchiveEntry(File file, String string, String string2, String string3) {
            this.archive = file;
            this.file = string;
            this.path = string2;
            this.size = string3;
        }

        public static ArchiveEntry of(File file, FileInfo fileInfo) {
            File file2 = fileInfo.toFile().getParentFile();
            File file3 = file2 == null ? new File(file.getName()) : new File(file.getName(), file2.getPath());
            return new ArchiveEntry(file, fileInfo.toFile().getName(), FileUtilities.normalizePathSeparators(file3.toString()), FileUtilities.formatSize(fileInfo.getLength()));
        }
    }
}

