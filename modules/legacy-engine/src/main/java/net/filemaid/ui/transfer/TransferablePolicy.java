package net.filemaid.ui.transfer;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.InvalidDnDOperationException;
import java.util.EventObject;
import javax.swing.TransferHandler;
import net.filemaid.Logging;
import net.filemaid.util.ui.SwingUI;

public abstract class TransferablePolicy {
    protected abstract boolean accept(Transferable var1) throws Exception;

    protected abstract void handleTransferable(Transferable var1, TransferAction var2) throws Exception;

    public boolean importData(Transferable transferable, TransferAction transferAction) throws Exception {
        if (this.accept(transferable)) {
            this.handleTransferable(transferable, transferAction);
            return true;
        }
        return false;
    }

    public boolean canImport(TransferHandler.TransferSupport transferSupport) {
        try {
            if (transferSupport.isDrop()) {
                transferSupport.setShowDropLocation(false);
            }
            return this.accept(transferSupport.getTransferable());
        }
        catch (InvalidDnDOperationException invalidDnDOperationException) {
            return true;
        }
        catch (Exception exception) {
            Logging.trace(exception);
            return false;
        }
    }

    public boolean importData(TransferHandler.TransferSupport transferSupport) throws Exception {
        return this.importData(transferSupport.getTransferable(), this.getTransferAction(transferSupport));
    }

    public boolean importData(Transferable transferable, EventObject eventObject) throws Exception {
        return this.importData(transferable, this.getTransferAction(eventObject));
    }

    public boolean importDataFromSystemClipboard(EventObject eventObject) throws Exception {
        return this.importData(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(eventObject.getSource()), this.getTransferAction(eventObject));
    }

    public TransferAction getTransferAction(TransferHandler.TransferSupport transferSupport) {
        if (transferSupport.isDrop()) {
            return TransferAction.fromDnDConstant(transferSupport.getDropAction());
        }
        return TransferAction.INSERT;
    }

    public TransferAction getTransferAction(EventObject eventObject) {
        return SwingUI.isControlOrMetaDown(eventObject) ? TransferAction.ADD : (SwingUI.isShiftOrAltDown(eventObject) ? TransferAction.LINK : TransferAction.PUT);
    }

    public static enum TransferAction {
        PUT(2),
        ADD(1),
        LINK(0x40000000),
        INSERT(0);

        private final int dndConstant;

        private TransferAction(int n2) {
            this.dndConstant = n2;
        }

        public int getDnDConstant() {
            return this.dndConstant;
        }

        public static TransferAction fromDnDConstant(int n) {
            for (TransferAction transferAction : TransferAction.values()) {
                if (n != transferAction.dndConstant) continue;
                return transferAction;
            }
            throw new IllegalArgumentException("Unsupported dndConstant: " + n);
        }
    }
}

