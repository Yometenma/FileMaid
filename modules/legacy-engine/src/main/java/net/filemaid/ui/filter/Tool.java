package net.filemaid.ui.filter;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.tree.TreeNode;
import net.filemaid.Logging;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.util.FileUtilities;
import org.apache.commons.io.FileUtils;

abstract class Tool<M>
extends JComponent {
    private List<File> root = Collections.emptyList();
    private UpdateModelTask updateTask;

    public Tool(String string) {
        this.setName(string);
    }

    public List<File> getRoot() {
        return this.root;
    }

    public void setRoot(List<File> list) {
        this.root = list;
        if (this.updateTask != null) {
            this.updateTask.cancel(true);
        }
        this.setLoading(true);
        this.updateTask = new UpdateModelTask(list);
        this.updateTask.execute();
    }

    protected void setLoading(boolean bl) {
        this.firePropertyChange("loading", !bl, bl);
    }

    protected abstract M createModelInBackground(List<File> var1) throws Exception;

    protected abstract void setModel(M var1);

    protected List<TreeNode> createFileNodes(Collection<File> collection) {
        return collection.stream().map(FileTree.FileNode::new).collect(Collectors.toList());
    }

    protected FileTree.FolderNode createStatisticsNode(String string, Collection<File> collection) {
        String string2 = collection.stream().anyMatch(File::isFile) ? "file" : "folder";
        long l = collection.stream().mapToLong(FileUtils::sizeOf).sum();
        String string3 = String.format("%s (%,d %s, %s)", string, collection.size(), collection.size() == 1 ? string2 : string2 + "s", FileUtilities.formatSize(l));
        return new FileTree.FolderNode(string3, this.createFileNodes(collection));
    }

    private class UpdateModelTask
    extends SwingWorker<M, Void> {
        private final List<File> root;

        public UpdateModelTask(List<File> list) {
            this.root = list;
        }

        @Override
        protected M doInBackground() throws Exception {
            return Tool.this.createModelInBackground(this.root);
        }

        @Override
        protected void done() {
            try {
                if (!this.isCancelled()) {
                    Tool.this.setModel(this.get());
                }
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
            finally {
                Tool.this.setLoading(false);
            }
        }
    }
}

