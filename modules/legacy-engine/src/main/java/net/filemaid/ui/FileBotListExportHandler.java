package net.filemaid.ui;

import ca.odell.glazedlists.EventList;
import java.awt.datatransfer.Clipboard;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JComponent;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.UserInteraction;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.ui.transfer.TextFileExportHandler;

public class FileBotListExportHandler<T>
extends TextFileExportHandler
implements ClipboardHandler {
    protected final FileBotList<T> list;
    protected final BiConsumer<T, PrintWriter> writer;

    public FileBotListExportHandler(FileBotList<T> fileBotList) {
        this(fileBotList, (object, printWriter) -> printWriter.println(object));
    }

    public FileBotListExportHandler(FileBotList<T> fileBotList, BiConsumer<T, PrintWriter> biConsumer) {
        this.list = fileBotList;
        this.writer = biConsumer;
    }

    @Override
    public boolean canExport() {
        return this.list.getModel().size() > 0;
    }

    @Override
    public void export(PrintWriter printWriter, boolean bl) {
        List<T> eventList = bl && this.list.getListComponent().getSelectedIndex() >= 0 ? this.list.getListComponent().getSelectedValuesList() : this.list.getModel();
        for (T e : eventList) {
            this.writer.accept(e, printWriter);
        }
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
}

