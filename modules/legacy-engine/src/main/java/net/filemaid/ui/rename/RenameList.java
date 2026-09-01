package net.filemaid.ui.rename;

import ca.odell.glazedlists.EventList;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import net.filemaid.ResourceManager;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class RenameList<E>
extends FileBotList<E> {
    private final JComponent buttonPanel;
    private final PrototypeCellSize prototypeCellSize;
    private final Action upAction = SwingUI.newAction("Align Up", ResourceManager.getIcon("action.up"), actionEvent -> this.moveSelection(-1));
    private final Action downAction = SwingUI.newAction("Align Down", ResourceManager.getIcon("action.down"), actionEvent -> this.moveSelection(1));
    private final MouseAdapter dndReorderMouseAdapter = new MouseAdapter(){
        private int lastIndex = -1;

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            this.lastIndex = RenameList.this.getListComponent().getSelectedIndex();
        }

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            int n = RenameList.this.getListComponent().getSelectedIndex();
            if (n != this.lastIndex && this.lastIndex >= 0 && n >= 0) {
                RenameList.this.moveSelection(this.lastIndex - n);
                this.lastIndex = n;
            }
        }
    };

    public RenameList(EventList<E> eventList) {
        this.setModel(eventList);
        this.list.setSelectionMode(0);
        this.prototypeCellSize = PrototypeCellSize.fixedCellSize(this.list, this::estimateCellSize);
        this.list.addMouseListener(this.dndReorderMouseAdapter);
        this.list.addMouseMotionListener(this.dndReorderMouseAdapter);
        this.getRemoveAction().setEnabled(true);
        this.buttonPanel = SwingUI.newPanel((LayoutManager)new MigLayout("insets 1.2mm, nogrid, novisualpadding, fill", "align center"));
        this.buttonPanel.add((Component)SwingUI.createImageButton(this.downAction), "gap 0, sg button");
        this.buttonPanel.add((Component)SwingUI.createImageButton(this.upAction), "gap 0, sg button");
        this.add((Component)this.buttonPanel, "South");
        this.listScrollPane.getViewport().setBackground(this.list.getBackground());
    }

    protected String estimateCellSize(Object object) {
        FormattedFuture formattedFuture;
        if (object instanceof File) {
            File file = (File)object;
            return file.getName();
        }
        if (object instanceof FormattedFuture && (formattedFuture = (FormattedFuture)object).isReady()) {
            return formattedFuture.getDisplayPath(false);
        }
        return String.valueOf(object);
    }

    public PrototypeCellSize getPrototypeCellSize() {
        return this.prototypeCellSize;
    }

    public JComponent getButtonPanel() {
        return this.buttonPanel;
    }

    public ListSelectionModel getSelectionModel() {
        return this.list.getSelectionModel();
    }

    public void setSelectedIndices(int ... nArray) {
        ListSelectionModel listSelectionModel = this.getSelectionModel();
        listSelectionModel.clearSelection();
        for (int n : nArray) {
            listSelectionModel.addSelectionInterval(n, n);
            this.list.ensureIndexIsVisible(n);
        }
    }

    public int[] getSelectedIndices() {
        return this.getSelectionModel().getSelectedIndices();
    }

    public int[] getSelectedIndices(MouseEvent mouseEvent) {
        Rectangle rectangle;
        int n;
        ListSelectionModel listSelectionModel = this.getSelectionModel();
        if (!listSelectionModel.isSelectionEmpty() && (n = this.list.locationToIndex(mouseEvent.getPoint())) >= 0 && this.list.isSelectedIndex(n) && (rectangle = this.list.getCellBounds(n, n)) != null && rectangle.contains(mouseEvent.getPoint())) {
            return listSelectionModel.getSelectedIndices();
        }
        return new int[0];
    }

    public List<E> getSelectedValues() {
        return this.list.getSelectedValuesList();
    }

    public List<E> getSelectedValues(int[] nArray) {
        return IntStream.of(nArray).filter(n -> n < this.model.size()).mapToObj(arg_0 -> this.model.get(arg_0)).collect(Collectors.toList());
    }

    protected void moveSelection(int n) {
        int[] nArray = this.getSelectedIndices();
        if (nArray.length == 0 || !IntStream.of(nArray).allMatch(n2 -> n2 + n >= 0 && n2 + n < this.model.size())) {
            return;
        }
        if (n < 0) {
            for (int i = 0; i < nArray.length; ++i) {
                Collections.swap(this.model, nArray[i], nArray[i] + n);
            }
        } else {
            for (int i = nArray.length - 1; i >= 0; --i) {
                Collections.swap(this.model, nArray[i], nArray[i] + n);
            }
        }
        this.getListComponent().setSelectedIndices(IntStream.of(nArray).map(n2 -> n2 + n).toArray());
        this.getListComponent().ensureIndexIsVisible(n < 0 ? nArray[0] + n : nArray[nArray.length - 1] + n);
    }
}

