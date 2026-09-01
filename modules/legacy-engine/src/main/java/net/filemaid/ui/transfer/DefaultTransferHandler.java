package net.filemaid.ui.transfer;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import net.filemaid.Logging;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.ui.transfer.DefaultClipboardHandler;
import net.filemaid.ui.transfer.TransferableExportHandler;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;

public class DefaultTransferHandler
extends TransferHandler {
    private static long dropping = 0L;
    private TransferablePolicy transferablePolicy;
    private TransferableExportHandler exportHandler;
    private ClipboardHandler clipboardHandler;
    private boolean dragging = false;

    public DefaultTransferHandler(TransferablePolicy transferablePolicy, TransferableExportHandler transferableExportHandler) {
        this(transferablePolicy, transferableExportHandler, new DefaultClipboardHandler());
    }

    public DefaultTransferHandler(TransferablePolicy transferablePolicy, TransferableExportHandler transferableExportHandler, ClipboardHandler clipboardHandler) {
        this.transferablePolicy = transferablePolicy;
        this.exportHandler = transferableExportHandler;
        this.clipboardHandler = clipboardHandler;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport transferSupport) {
        if (this.dragging) {
            return true;
        }
        if (transferSupport.isDrop()) {
            long l = System.currentTimeMillis();
            if (l - dropping >= 2000L) {
                SwingUI.requestForeground();
            }
            dropping = l;
        }
        if (this.transferablePolicy != null) {
            try {
                return this.transferablePolicy.canImport(transferSupport);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
        return false;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport transferSupport) {
        if (this.dragging) {
            return false;
        }
        if (this.canImport(transferSupport)) {
            try {
                if (this.transferablePolicy.importData(transferSupport)) {
                    return FileUtilities.UNIX;
                }
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent jComponent, Transferable transferable, int n) {
        this.dragging = false;
        if (transferable == null) {
            return;
        }
        if (this.exportHandler != null) {
            this.exportHandler.exportDone(jComponent, transferable, n);
        }
    }

    @Override
    public int getSourceActions(JComponent jComponent) {
        if (this.exportHandler != null) {
            return this.exportHandler.getSourceActions(jComponent);
        }
        return 0;
    }

    @Override
    protected Transferable createTransferable(JComponent jComponent) {
        this.dragging = true;
        if (this.exportHandler != null) {
            return this.exportHandler.createTransferable(jComponent);
        }
        return null;
    }

    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) throws IllegalStateException {
        if (this.clipboardHandler != null) {
            this.clipboardHandler.exportToClipboard(jComponent, clipboard, n);
        }
    }

    public TransferablePolicy getTransferablePolicy() {
        return this.transferablePolicy;
    }

    public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
        this.transferablePolicy = transferablePolicy;
    }

    public TransferableExportHandler getExportHandler() {
        return this.exportHandler;
    }

    public void setExportHandler(TransferableExportHandler transferableExportHandler) {
        this.exportHandler = transferableExportHandler;
    }

    public ClipboardHandler getClipboardHandler() {
        return this.clipboardHandler;
    }

    public void setClipboardHandler(ClipboardHandler clipboardHandler) {
        this.clipboardHandler = clipboardHandler;
    }
}

