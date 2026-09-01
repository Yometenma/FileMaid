package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.ui.filter.FileTreeExportHandler;
import net.filemaid.ui.filter.Tool;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class SplitTool
extends Tool<TreeModel> {
    private FileTree tree = new FileTree();
    private SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4480, 0, Integer.MAX_VALUE, 100);

    public SplitTool() {
        super("Parts");
        JScrollPane jScrollPane = new JScrollPane(this.tree);
        jScrollPane.setBorder(ThemeSupport.getHorizontalRule());
        JSpinner jSpinner = new JSpinner(this.spinnerModel);
        jSpinner.setEditor(new JSpinner.NumberEditor(jSpinner, "#"));
        this.setLayout((LayoutManager)new MigLayout("insets 0, nogrid, fill", "align center", "[fill]0px[pref!]"));
        this.add((Component)new LoadingOverlayPane((Component)jScrollPane, this), "grow");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets 15px, nogrid, fill", "align center"));
        jComponent.add(new JLabel("Split every"));
        jComponent.add((Component)jSpinner, "wmax 80");
        jComponent.add(new JLabel("MB"));
        this.add((Component)jComponent, "dock south");
        this.tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
        this.tree.setDragEnabled(true);
        this.spinnerModel.addChangeListener(changeEvent -> {
            List<File> list = this.getRoot();
            if (list.size() > 0) {
                this.setRoot(list);
            }
        });
    }

    private long getSplitSize() {
        return (long)this.spinnerModel.getNumber().intValue() * 1000000L;
    }

    @Override
    protected TreeModel createModelInBackground(List<File> list) {
        if (list.isEmpty()) {
            return new DefaultTreeModel(new FileTree.FolderNode("Volumes", Collections.emptyList()));
        }
        int n = 1;
        long l = this.getSplitSize();
        List<File> list2 = FileUtilities.listFiles(list, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER);
        ArrayList<TreeNode> arrayList = new ArrayList<TreeNode>();
        ArrayList<File> arrayList2 = new ArrayList<File>();
        ArrayList<File> arrayList3 = new ArrayList<File>();
        long l2 = 0L;
        for (File file : list2) {
            long l3 = file.length();
            if (l3 > l) {
                arrayList3.add(file);
                continue;
            }
            if (l2 + l3 > l) {
                arrayList.add(this.createStatisticsNode(n++, arrayList2));
                l2 = 0L;
                arrayList2.clear();
            }
            l2 += l3;
            arrayList2.add(file);
            if (!Thread.interrupted()) continue;
            throw new CancellationException();
        }
        if (!arrayList2.isEmpty()) {
            arrayList.add(this.createStatisticsNode(n++, arrayList2));
        }
        if (!arrayList3.isEmpty()) {
            arrayList.add(this.createStatisticsNode("Remainder", arrayList3));
        }
        return new DefaultTreeModel(new FileTree.FolderNode("Volumes", arrayList));
    }

    protected FileTree.FolderNode createStatisticsNode(int n, List<File> list) {
        return this.createStatisticsNode(String.format("Disk %,d", n), list);
    }

    @Override
    protected void setModel(TreeModel treeModel) {
        this.tree.setModel(treeModel);
    }
}

