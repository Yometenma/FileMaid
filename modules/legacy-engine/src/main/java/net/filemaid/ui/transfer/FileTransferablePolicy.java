package net.filemaid.ui.transfer;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import net.filemaid.CategoryFileFilter;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.TemporaryFolder;

public abstract class FileTransferablePolicy
extends TransferablePolicy {
    private static final FileFilter TEMPORARY = FileUtilities.newParentFilter(TemporaryFolder.root());

    public boolean acceptFileTransferable(List<File> list) throws Exception {
        if (FileUtilities.containsOnly(list, TEMPORARY)) {
            return false;
        }
        return this.accept(list);
    }

    public void handleFileTransferable(List<File> list, TransferablePolicy.TransferAction transferAction) throws Exception {
        if (transferAction != TransferablePolicy.TransferAction.ADD) {
            this.clear();
        }
        this.load(list, transferAction);
    }

    @Override
    public boolean accept(Transferable transferable) throws Exception {
        try {
            return this.acceptFileTransferable(FileTransferable.getFilesFromTransferable(transferable));
        }
        catch (UnsupportedFlavorException unsupportedFlavorException) {
            return false;
        }
    }

    @Override
    public void handleTransferable(Transferable transferable, TransferablePolicy.TransferAction transferAction) throws Exception {
        try {
            this.handleFileTransferable(FileTransferable.getFilesFromTransferable(transferable), transferAction);
        }
        catch (UnsupportedFlavorException unsupportedFlavorException) {
            // empty catch block
        }
    }

    @Override
    public boolean importData(Transferable transferable, TransferablePolicy.TransferAction transferAction) throws Exception {
        try {
            List<File> list = FileTransferable.getFilesFromTransferable(transferable);
            if (this.acceptFileTransferable(list)) {
                this.handleFileTransferable(list, transferAction);
                return true;
            }
        }
        catch (UnsupportedFlavorException unsupportedFlavorException) {
            // empty catch block
        }
        return false;
    }

    protected abstract boolean accept(List<File> var1);

    protected abstract void load(List<File> var1, TransferablePolicy.TransferAction var2) throws IOException;

    protected abstract void clear();

    public CategoryFileFilter getFileFilter() {
        return null;
    }
}

