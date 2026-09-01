package net.filemaid.ui.transfer;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.ui.transfer.FileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;

public class LoadAction
extends AbstractAction {
    protected final Supplier<TransferablePolicy> handler;

    public LoadAction(Supplier<TransferablePolicy> supplier) {
        this("Load", ResourceManager.getIcon("action.load"), supplier);
    }

    public LoadAction(String string, Icon icon, Supplier<TransferablePolicy> supplier) {
        super(string, icon);
        this.handler = supplier;
    }

    public String getName() {
        return (String)this.getValue("Name");
    }

    protected File getDefaultFile() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        TransferablePolicy transferablePolicy = this.handler.get();
        if (transferablePolicy == null) {
            return;
        }
        CategoryFileFilter categoryFileFilter = this.getFileFilter(transferablePolicy);
        boolean bl = categoryFileFilter == null || categoryFileFilter.acceptAny();
        List<File> list = UserFiles.showLoadDialogSelectFiles(bl, true, this.getDefaultFile(), categoryFileFilter, this.getName(), actionEvent);
        if (list.isEmpty()) {
            return;
        }
        try {
            transferablePolicy.importData((Transferable)new FileTransferable(list), actionEvent);
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
    }

    protected CategoryFileFilter getFileFilter(TransferablePolicy transferablePolicy) {
        if (transferablePolicy instanceof FileTransferablePolicy) {
            FileTransferablePolicy fileTransferablePolicy = (FileTransferablePolicy)transferablePolicy;
            return fileTransferablePolicy.getFileFilter();
        }
        return null;
    }
}

