package net.filemaid.ui.transfer;

import java.awt.datatransfer.Clipboard;
import javax.swing.JComponent;

public interface ClipboardHandler {
    public void exportToClipboard(JComponent var1, Clipboard var2, int var3) throws IllegalStateException;
}

