package net.filemaid.ui.filter;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import net.filemaid.ResourceManager;
import net.filemaid.util.ui.FancyTreeCellRenderer;
import net.filemaid.util.ui.GradientStyle;

class FileTreeCellRenderer
extends FancyTreeCellRenderer {
    public FileTreeCellRenderer() {
        super(GradientStyle.TOP_TO_BOTTOM);
        this.openIcon = ResourceManager.getIcon("tree.open");
        this.closedIcon = ResourceManager.getIcon("tree.closed");
        this.leafIcon = ResourceManager.getIcon("file.generic");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree jTree, Object object, boolean bl, boolean bl2, boolean bl3, int n, boolean bl4) {
        if (bl3 && this.isFolder(object)) {
            bl2 = true;
            bl3 = false;
        }
        super.getTreeCellRendererComponent(jTree, object, bl, bl2, bl3, n, bl4);
        return this;
    }

    private boolean isFolder(Object object) {
        return ((TreeNode)object).getAllowsChildren();
    }
}

