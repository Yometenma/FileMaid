package net.filemaid.ui.rename;

import java.awt.Dimension;
import javax.swing.BoundedRangeModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.filemaid.ui.rename.RenameList;
import net.filemaid.util.ui.SwingUI;

class ScrollPaneSynchronizer
implements ListDataListener {
    private final RenameList[] components;

    public ScrollPaneSynchronizer(RenameList ... renameListArray) {
        this.components = renameListArray;
        BoundedRangeModel boundedRangeModel = renameListArray[0].getListScrollPane().getHorizontalScrollBar().getModel();
        BoundedRangeModel boundedRangeModel2 = renameListArray[0].getListScrollPane().getVerticalScrollBar().getModel();
        for (RenameList renameList : renameListArray) {
            renameList.getListScrollPane().getHorizontalScrollBar().setModel(boundedRangeModel);
            renameList.getListScrollPane().getVerticalScrollBar().setModel(boundedRangeModel2);
            renameList.getListComponent().getModel().addListDataListener(this);
        }
    }

    public void updatePreferredSize() {
        Dimension dimension = new Dimension();
        for (RenameList renameList : this.components) {
            renameList.getListComponent().setPreferredSize(null);
            Dimension dimension2 = renameList.getListComponent().getPreferredSize();
            if (dimension2.width > dimension.width) {
                dimension.width = dimension2.width;
            }
            if (dimension2.height <= dimension.height) continue;
            dimension.height = dimension2.height;
        }
        for (RenameList renameList : this.components) {
            renameList.getListComponent().setPreferredSize(dimension);
            renameList.getListComponent().revalidate();
            renameList.getListScrollPane().revalidate();
        }
    }

    @Override
    public void intervalAdded(ListDataEvent listDataEvent) {
        SwingUI.invokeLater(50, this::updatePreferredSize);
    }

    @Override
    public void intervalRemoved(ListDataEvent listDataEvent) {
        SwingUI.invokeLater(50, this::updatePreferredSize);
    }

    @Override
    public void contentsChanged(ListDataEvent listDataEvent) {
        SwingUI.invokeLater(50, this::updatePreferredSize);
    }
}

