package net.filemaid.util.ui;

import java.util.function.Function;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class PrototypeCellSize
implements ListDataListener {
    private final JList list;
    private final Function<Object, String> metrics;
    private int maxSize = 0;

    private PrototypeCellSize(JList jList, Function<Object, String> function) {
        this.list = jList;
        this.metrics = function;
        this.reset();
    }

    public void reset() {
        this.maxSize = 0;
        this.list.setPrototypeCellValue(null);
        this.list.setFixedCellHeight(28);
        this.list.setFixedCellWidth(280);
        if (this.list.getModel().getSize() > 0) {
            this.union(this.model(0, Integer.MAX_VALUE));
        }
    }

    protected int size(Object object) {
        return this.list.getFontMetrics(this.list.getFont()).stringWidth(this.metrics.apply(object));
    }

    protected Object[] model(int n, int n2) {
        ListModel listModel = this.list.getModel();
        n = Math.max(0, n);
        n2 = Math.min(listModel.getSize() - 1, n2);
        if (n2 < n) {
            return new Object[0];
        }
        Object[] objectArray = new Object[n2 - n + 1];
        for (int i = 0; i < objectArray.length; ++i) {
            objectArray[i] = listModel.getElementAt(n + i);
        }
        return objectArray;
    }

    protected void union(Object ... objectArray) {
        int n = 0;
        Object object = null;
        for (Object object2 : objectArray) {
            int n2 = this.size(object2);
            if (n2 <= n) continue;
            n = n2;
            object = object2;
        }
        if (n > this.maxSize) {
            if (object == this.list.getPrototypeCellValue()) {
                this.list.setPrototypeCellValue(null);
            }
            this.maxSize = n;
            this.list.setPrototypeCellValue(object);
        }
    }

    @Override
    public void intervalAdded(ListDataEvent listDataEvent) {
        this.contentsChanged(listDataEvent);
    }

    @Override
    public void intervalRemoved(ListDataEvent listDataEvent) {
        if (this.list.getModel().getSize() == 0) {
            this.reset();
        }
    }

    @Override
    public void contentsChanged(ListDataEvent listDataEvent) {
        if (listDataEvent.getIndex0() == 0 && this.list.getModel().getSize() == 0) {
            this.reset();
        } else {
            this.union(this.model(listDataEvent.getIndex0(), listDataEvent.getIndex1()));
        }
    }

    public static PrototypeCellSize fixedCellSize(JList jList) {
        return PrototypeCellSize.fixedCellSize(jList, String::valueOf);
    }

    public static PrototypeCellSize fixedCellSize(JList jList, Function<Object, String> function) {
        PrototypeCellSize prototypeCellSize = new PrototypeCellSize(jList, function);
        jList.getModel().addListDataListener(prototypeCellSize);
        return prototypeCellSize;
    }
}

