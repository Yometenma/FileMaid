package net.filemaid.ui.list;

import ca.odell.glazedlists.EventList;
import java.awt.datatransfer.Clipboard;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.JComponent;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.UserInteraction;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.list.ListItem;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.ui.transfer.TextFileExportHandler;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.SwingUI;

public class ListExportHandler
extends TextFileExportHandler
implements ClipboardHandler {
    protected final FileBotList<ListItem> list;

    public ListExportHandler(FileBotList<ListItem> fileBotList) {
        this.list = fileBotList;
    }

    @Override
    public boolean canExport() {
        return this.list.getModel().size() > 0;
    }

    @Override
    public void export(PrintWriter printWriter, boolean bl) {
        List<ListItem> eventList = bl && this.list.getListComponent().getSelectedIndex() >= 0 ? this.list.getListComponent().getSelectedValuesList() : this.list.getModel();
        SwingUI.withWaitCursor(this.list, () -> GlassProgressMonitor.runTask(new ExportWorker((List<ListItem>)eventList, printWriter), SwingUI.getWindow(this.list)));
    }

    @Override
    public String getDefaultFileName() {
        return this.list.getTitle() + "." + this.getFileFilter().extension();
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        return new CategoryFileFilter("List", MediaTypes.LIST_FILES);
    }

    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) throws IllegalStateException {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter);){
            this.export(printWriter, true);
        }
        UserInteraction.copy(clipboard, stringWriter.toString());
    }

    private static class ExportWorker
    implements GlassProgressMonitor.ProgressWorker<Integer> {
        private final List<ListItem> queue;
        private final PrintWriter writer;

        public ExportWorker(List<ListItem> list, PrintWriter printWriter) {
            this.queue = new ArrayList<ListItem>(list);
            this.writer = printWriter;
        }

        @Override
        public String getName() {
            return "Export";
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("action.save");
        }

        @Override
        public String getDescription() {
            return "Preparing...";
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public Integer call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            for (int i = 0; i < this.queue.size(); ++i) {
                if (supplier.get().booleanValue()) {
                    return i;
                }
                String string = this.queue.get(i).getFormattedValue();
                this.writer.println(string);
                consumer2.accept(string);
                biConsumer.accept(i + 1, this.queue.size());
            }
            return this.queue.size();
        }
    }
}

