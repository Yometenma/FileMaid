package net.filemaid.ui.transfer;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JComponent;
import net.filemaid.ui.transfer.FileExportHandler;
import net.filemaid.ui.transfer.TextFileTransferable;
import net.filemaid.ui.transfer.TransferableExportHandler;

public abstract class TextFileExportHandler
implements TransferableExportHandler,
FileExportHandler {
    @Override
    public abstract boolean canExport();

    public abstract void export(PrintWriter var1, boolean var2);

    @Override
    public abstract String getDefaultFileName();

    @Override
    public void export(File file) throws Exception {
        try (PrintWriter printWriter = new PrintWriter(file, "UTF-8");){
            this.export(printWriter, false);
        }
    }

    public String export(boolean bl) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter);){
            this.export(printWriter, bl);
        }
        return stringWriter.toString();
    }

    @Override
    public int getSourceActions(JComponent jComponent) {
        return this.canExport() ? 3 : 0;
    }

    @Override
    public Transferable createTransferable(JComponent jComponent) {
        return new TextFileTransferable(this.getDefaultFileName(), this.export(false));
    }

    @Override
    public void exportDone(JComponent jComponent, Transferable transferable, int n) {
    }
}

