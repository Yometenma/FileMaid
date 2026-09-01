package net.filemaid.ui.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.tree.TreeNode;
import net.filemaid.Settings;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;

class FileTreeTransferablePolicy
extends BackgroundFileTransferablePolicy<TreeNode> {
    private final FileTree tree;

    public FileTreeTransferablePolicy(FileTree fileTree) {
        this.tree = fileTree;
    }

    @Override
    protected boolean accept(List<File> list) {
        return true;
    }

    @Override
    protected void clear() {
        this.tree.clear();
    }

    @Override
    protected void process(List<TreeNode> list) {
        this.tree.getModel().setRoot(new FileTree.FolderNode(list));
        this.tree.getModel().reload();
    }

    @Override
    protected void load(List<File> list, TransferablePolicy.TransferAction transferAction) {
        if (Settings.isMacSandbox()) {
            MacAppUtilities.askUnlockFolders(SwingUI.getWindow(this.tree), list);
        }
        TreeNode[] treeNodeArray = (TreeNode[])this.walkFileTree(list, FileUtilities.NOT_HIDDEN, 64).stream().map(file -> file.isDirectory() ? this.getTreeNode((File)file, 64) : new FileTree.FileNode((File)file)).toArray(TreeNode[]::new);
        this.publish(treeNodeArray);
    }

    private TreeNode getTreeNode(File file, int n) {
        if (n < 0) {
            return new FileTree.FolderNode(file, FileUtilities.getFolderName(file), Collections.emptyList());
        }
        ArrayList<TreeNode> arrayList = new ArrayList<TreeNode>();
        ArrayList<FileTree.FileNode> arrayList2 = new ArrayList<FileTree.FileNode>();
        for (File file2 : FileUtilities.getChildren(file, FileUtilities.NOT_HIDDEN, FileUtilities.HUMAN_NAME_ORDER)) {
            if (file2.isDirectory()) {
                arrayList.add(this.getTreeNode(file2, n - 1));
                continue;
            }
            arrayList2.add(new FileTree.FileNode(file2));
        }
        arrayList.addAll(arrayList2);
        return new FileTree.FolderNode(file, FileUtilities.getFolderName(file), arrayList);
    }
}

