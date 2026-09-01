package net.filemaid.ui.filter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.ui.Mode;
import net.filemaid.ui.TargetTransferable;
import net.filemaid.ui.filter.FileTreeCellRenderer;
import net.filemaid.ui.filter.FilterPanel;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.FilterIterator;
import net.filemaid.util.TreeIterator;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;

public class FileTree
extends JTree {
    public FileTree() {
        super(new DefaultTreeModel(new FolderNode()));
        this.getSelectionModel().setSelectionMode(4);
        this.setCellRenderer(new FileTreeCellRenderer());
        this.setShowsRootHandles(true);
        this.setRootVisible(false);
        this.setRowHeight(22);
        this.setLargeModel(true);
        this.addMouseListener(SwingUI.mousePopupTriggerClicked(mouseEvent -> {
            TreePath treePath = this.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (!this.getSelectionModel().isPathSelected(treePath)) {
                this.setSelectionPath(treePath);
            }
            OpenExpandCollapsePopup openExpandCollapsePopup = new OpenExpandCollapsePopup();
            openExpandCollapsePopup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }));
    }

    @Override
    public DefaultTreeModel getModel() {
        return (DefaultTreeModel)super.getModel();
    }

    public List<File> getRoot() {
        FolderNode folderNode = (FolderNode)this.getModel().getRoot();
        return folderNode.getChildren().stream().map(treeNode -> {
            if (treeNode instanceof FolderNode) {
                FolderNode childNode = (FolderNode)treeNode;
                return childNode.getFile();
            }
            if (treeNode instanceof FileNode) {
                FileNode fileNode = (FileNode)treeNode;
                return fileNode.getFile();
            }
            return null;
        }).filter(file -> file != null && file.exists()).collect(Collectors.toList());
    }

    public void clear() {
        this.getModel().setRoot(new FolderNode());
        this.getModel().reload();
    }

    public void expandAll() {
        for (int i = 0; i < this.getRowCount(); ++i) {
            this.expandRow(i);
        }
    }

    public void collapseAll() {
        for (int i = 0; i < this.getRowCount(); ++i) {
            this.collapseRow(i);
        }
    }

    public static class FolderNode
    extends AbstractTreeNode {
        private final File file;
        private final String title;
        private final List<TreeNode> children;

        public FolderNode() {
            this(Collections.emptyList());
        }

        public FolderNode(List<TreeNode> list) {
            this(null, "/", list);
        }

        public FolderNode(String string, List<TreeNode> list) {
            this(null, string, list);
        }

        public FolderNode(File file, String string, List<TreeNode> list) {
            this.file = file;
            this.title = string;
            this.children = list;
        }

        public File getFile() {
            return this.file;
        }

        public String toString() {
            return this.title;
        }

        public List<TreeNode> getChildren() {
            return this.children;
        }

        @Override
        public Enumeration<? extends TreeNode> children() {
            return Collections.enumeration(this.children);
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public TreeNode getChildAt(int n) {
            return this.children.get(n);
        }

        @Override
        public int getChildCount() {
            return this.children.size();
        }

        @Override
        public int getIndex(TreeNode treeNode) {
            return this.children.indexOf(treeNode);
        }

        public Iterator<TreeNode> treeIterator() {
            return new TreeIterator<TreeNode>(new TreeNode[]{this}){

                @Override
                protected Iterator<TreeNode> children(TreeNode treeNode) {
                    if (treeNode instanceof FolderNode) {
                        return ((FolderNode)treeNode).getChildren().iterator();
                    }
                    return null;
                }
            };
        }

        public Iterator<File> fileIterator() {
            return new FilterIterator<TreeNode, File>(this.treeIterator()){

                @Override
                protected File filter(TreeNode treeNode) {
                    if (treeNode instanceof FileNode) {
                        return ((FileNode)treeNode).getFile();
                    }
                    return null;
                }
            };
        }
    }

    public static class FileNode
    extends AbstractTreeNode {
        private final File file;

        public FileNode(File file) {
            this.file = file;
        }

        public File getFile() {
            return this.file;
        }

        public String toString() {
            return this.file.getName();
        }
    }

    private class OpenExpandCollapsePopup
    extends JPopupMenu {
        public OpenExpandCollapsePopup() {
            Collection<File> collection = this.getFiles(FileTree.this.getSelectionPaths());
            if (collection != null && !collection.isEmpty()) {
                JMenu jMenu = new JMenu("Send to");
                for (Mode mode : Mode.fileHandlerSequence()) {
                    jMenu.add(new JMenuItem(new ImportAction(mode, collection)));
                }
                this.add(jMenu);
                this.addSeparator();
            }
            if (collection.size() > 0) {
                this.add(new JMenuItem(new RevealAction("Reveal", collection)));
                this.add(new RevealAction("Reveal Folder", collection.stream().map(File::getParentFile).distinct().collect(Collectors.toList())));
                this.addSeparator();
            }
            this.add(SwingUI.newAction("Expand all", ResourceManager.getIcon("tree.expand"), actionEvent -> FileTree.this.expandAll()));
            this.add(SwingUI.newAction("Collapse all", ResourceManager.getIcon("tree.collapse"), actionEvent -> FileTree.this.collapseAll()));
            this.addSeparator();
            this.add(new TrashAction(collection));
        }

        private Collection<File> getFiles(TreePath[] treePathArray) {
            if (treePathArray == null || treePathArray.length == 0) {
                return Collections.emptySet();
            }
            LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>();
            for (TreePath treePath : treePathArray) {
                this.collectFiles(treePath.getLastPathComponent(), linkedHashSet);
            }
            return linkedHashSet;
        }

        private void collectFiles(Object object, Collection<File> collection) {
            if (object instanceof FileNode) {
                collection.add(((FileNode)object).getFile());
            } else if (object instanceof FolderNode) {
                for (TreeNode treeNode : ((FolderNode)object).getChildren()) {
                    this.collectFiles(treeNode, collection);
                }
            }
        }

        private class ImportAction
        extends AbstractAction {
            private final Mode mode;
            private final Collection<File> files;

            public ImportAction(Mode mode, Collection<File> collection) {
                super(mode.toString(), mode.getIcon());
                this.mode = mode;
                this.files = collection;
            }

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingEventBus.getInstance().post(new TargetTransferable(this.mode, new FileTransferable(this.files)));
            }
        }

        private class RevealAction
        extends AbstractAction {
            private Collection<File> files;

            public RevealAction(String string, Collection<File> collection) {
                super(string);
                this.files = collection;
            }

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                UserInteraction.revealFiles(this.files);
            }
        }

        private class TrashAction
        extends AbstractAction {
            private Collection<File> files;

            public TrashAction(Collection<File> collection) {
                super("Move to Trash");
                this.files = collection;
            }

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    for (File file : this.files) {
                        UserFiles.trash(file);
                    }
                }
                catch (Exception exception) {
                    Logging.log.log(Level.SEVERE, exception, Logging.cause(exception));
                }
                FilterPanel filterPanel = (FilterPanel)SwingUtilities.getAncestorOfClass(FilterPanel.class, FileTree.this);
                filterPanel.reload();
            }
        }
    }

    public static class AbstractTreeNode
    implements TreeNode {
        private TreeNode parent;

        @Override
        public TreeNode getParent() {
            return this.parent;
        }

        public void setParent(TreeNode treeNode) {
            this.parent = treeNode;
        }

        @Override
        public Enumeration<? extends TreeNode> children() {
            return null;
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public TreeNode getChildAt(int n) {
            return null;
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public int getIndex(TreeNode treeNode) {
            return -1;
        }

        @Override
        public boolean isLeaf() {
            return this.getChildCount() == 0;
        }
    }
}

