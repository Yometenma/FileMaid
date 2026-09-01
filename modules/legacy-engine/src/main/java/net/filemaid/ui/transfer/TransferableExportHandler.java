package net.filemaid.ui.transfer;

import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;

public interface TransferableExportHandler {
    public Transferable createTransferable(JComponent var1);

    public int getSourceActions(JComponent var1);

    public void exportDone(JComponent var1, Transferable var2, int var3);
}

