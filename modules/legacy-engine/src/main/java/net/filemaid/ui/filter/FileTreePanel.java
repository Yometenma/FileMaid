package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import net.filemaid.ResourceManager;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.ui.filter.FileTreeTransferablePolicy;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.ui.transfer.LoadAction;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class FileTreePanel
extends JComponent {
    private FileTree fileTree = new FileTree();
    private FileTreeTransferablePolicy transferablePolicy = new FileTreeTransferablePolicy(this.fileTree);
    private final LoadAction loadAction = new LoadAction(this::getTransferablePolicy);
    private final Action clearAction = SwingUI.newAction("Clear", ResourceManager.getIcon("action.clear"), actionEvent -> {
        this.transferablePolicy.reset();
        this.fileTree.clear();
        this.fireFileTreeChange();
    });
    public static final String FILE_TREE_PROPERTY = "FILE_TREE";

    public FileTreePanel() {
        this.fileTree.setTransferHandler(new DefaultTransferHandler(this.transferablePolicy, null));
        this.setBorder(BorderFactory.createTitledBorder("Files"));
        this.setLayout((LayoutManager)new MigLayout("insets 0, nogrid, fill", "align center", "[fill]0px[pref!]"));
        this.add((Component)new LoadingOverlayPane((Component)new JScrollPane(this.fileTree), this), "grow");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets rel, nogrid, novisualpadding, fill", "align center"));
        jComponent.add(SwingUI.newButton(this.loadAction));
        jComponent.add(SwingUI.newButton(this.clearAction));
        this.add((Component)jComponent, "dock south");
        this.transferablePolicy.addPropertyChangeListener(propertyChangeEvent -> {
            if ("loading".equals(propertyChangeEvent.getPropertyName())) {
                this.firePropertyChange(propertyChangeEvent.getPropertyName(), propertyChangeEvent.getOldValue(), propertyChangeEvent.getNewValue());
            }
        });
        this.transferablePolicy.addPropertyChangeListener(propertyChangeEvent -> {
            if ("loading".equals(propertyChangeEvent.getPropertyName()) && !((Boolean)propertyChangeEvent.getNewValue()).booleanValue()) {
                this.fireFileTreeChange();
            }
        });
    }

    public FileTree getFileTree() {
        return this.fileTree;
    }

    public TransferablePolicy getTransferablePolicy() {
        return this.transferablePolicy;
    }

    private void fireFileTreeChange() {
        this.firePropertyChange(FILE_TREE_PROPERTY, null, this.fileTree);
    }
}

