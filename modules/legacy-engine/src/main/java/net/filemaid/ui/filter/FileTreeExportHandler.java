package net.filemaid.ui.filter;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import javax.swing.JComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.ui.transfer.TransferableExportHandler;

class FileTreeExportHandler
implements TransferableExportHandler {
    FileTreeExportHandler() {
    }

    @Override
    public Transferable createTransferable(JComponent jComponent) {
        FileTree fileTree = (FileTree)jComponent;
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>();
        for (TreePath treePath : fileTree.getSelectionPaths()) {
            TreeNode treeNode = (TreeNode)treePath.getLastPathComponent();
            if (treeNode instanceof FileTree.FileNode) {
                linkedHashSet.add(((FileTree.FileNode)treeNode).getFile());
                continue;
            }
            if (!(treeNode instanceof FileTree.FolderNode)) continue;
            Iterator<File> iterator = ((FileTree.FolderNode)treeNode).fileIterator();
            while (iterator.hasNext()) {
                linkedHashSet.add(iterator.next());
            }
        }
        if (linkedHashSet.isEmpty()) {
            return null;
        }
        return new FileTransferable(linkedHashSet);
    }

    @Override
    public void exportDone(JComponent jComponent, Transferable transferable, int n) {
    }

    @Override
    public int getSourceActions(JComponent jComponent) {
        return 1;
    }
}

