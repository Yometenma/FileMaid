package net.filemaid.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.ui.transfer.TextFileExportHandler;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.ListSearchKeyListener;
import net.filemaid.util.ui.SwingUI;

public class FileBotList<E>
extends JComponent {
    protected EventList<E> model = new BasicEventList();
    protected JList<E> list = new JList(new DefaultEventListModel(this.model));
    protected JScrollPane listScrollPane = new JScrollPane(this.list);
    private Action removeAction = SwingUI.newAction("Remove", actionEvent -> {
        int n;
        int[] nArray = this.list.getSelectedIndices();
        if (nArray.length == 0) {
            return;
        }
        for (n = nArray.length - 1; n >= 0; --n) {
            this.getModel().remove(nArray[n]);
        }
        n = nArray[0];
        int n2 = this.list.getModel().getSize() - 1;
        int n3 = n < n2 ? n : n2;
        this.list.setSelectedIndex(n3);
        this.list.ensureIndexIsVisible(n3);
    });
    private final Action removeHook = SwingUI.newAction("Remove", actionEvent -> {
        if (this.getRemoveAction() != null && this.getRemoveAction().isEnabled()) {
            this.getRemoveAction().actionPerformed((ActionEvent)actionEvent);
        }
    });

    public FileBotList() {
        this.setLayout(new BorderLayout());
        this.setBorder(new TitledBorder(this.getTitle()));
        this.list.setCellRenderer(new DefaultFancyListCellRenderer());
        this.list.setSelectionMode(2);
        this.list.setTransferHandler(new DefaultTransferHandler(null, null));
        this.list.setDragEnabled(false);
        this.add((Component)this.listScrollPane, "Center");
        this.list.addKeyListener(new ListSearchKeyListener());
        this.getRemoveAction().setEnabled(false);
        SwingUI.installAction((JComponent)this, 127, this.removeHook);
        SwingUI.installAction((JComponent)this, 127, 64, this.removeHook);
    }

    public EventList<E> getModel() {
        return this.model;
    }

    public void setModel(EventList<E> eventList) {
        this.model = eventList;
        this.list.setModel((ListModel<E>)new DefaultEventListModel(eventList));
    }

    public JList<E> getListComponent() {
        return this.list;
    }

    public JScrollPane getListScrollPane() {
        return this.listScrollPane;
    }

    @Override
    public DefaultTransferHandler getTransferHandler() {
        return (DefaultTransferHandler)this.list.getTransferHandler();
    }

    public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
        this.getTransferHandler().setTransferablePolicy(transferablePolicy);
    }

    public TransferablePolicy getTransferablePolicy() {
        return this.getTransferHandler().getTransferablePolicy();
    }

    public void setExportHandler(TextFileExportHandler textFileExportHandler) {
        this.getTransferHandler().setExportHandler(textFileExportHandler);
        this.list.setDragEnabled(textFileExportHandler != null);
    }

    public TextFileExportHandler getExportHandler() {
        return (TextFileExportHandler)this.getTransferHandler().getExportHandler();
    }

    public String getTitle() {
        return (String)this.getClientProperty("title");
    }

    public void setTitle(String string) {
        this.putClientProperty("title", string);
        if (this.getBorder() instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder)this.getBorder();
            titledBorder.setTitle(string);
            this.repaint();
        }
    }

    public Action getRemoveAction() {
        return this.removeAction;
    }

    public void setRemoveAction(Action action) {
        this.removeAction = action;
    }
}

