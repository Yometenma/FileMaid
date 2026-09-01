package net.filemaid.ui.list;

import ca.odell.glazedlists.EventList;
import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.Mode;
import net.filemaid.ui.TargetTransferable;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.list.ListExportHandler;
import net.filemaid.ui.list.ListItem;
import net.filemaid.ui.list.ListMode;
import net.filemaid.ui.list.ListTransferablePolicy;
import net.filemaid.ui.rename.FormatExpressionTextArea;
import net.filemaid.ui.transfer.LoadAction;
import net.filemaid.ui.transfer.SaveAction;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ListPanel
extends JComponent {
    private FormatExpressionTextArea editor = new FormatExpressionTextArea();
    private FileBotList<ListItem> list = new FileBotList();
    private ExpressionFormat format;
    private ListMode mode;

    public ListPanel() {
        PrototypeCellSize.fixedCellSize(this.list.getListComponent());
        ListTransferablePolicy listTransferablePolicy = new ListTransferablePolicy(this.list::setTitle, this::setFormatMode, this::createItemSequence);
        ListExportHandler listExportHandler = new ListExportHandler(this.list);
        this.list.setTransferablePolicy(listTransferablePolicy);
        this.list.setExportHandler(listExportHandler);
        this.list.getTransferHandler().setClipboardHandler(listExportHandler);
        this.list.getRemoveAction().setEnabled(true);
        JMenu jMenu = new JMenu("Send to");
        for (Mode object2 : Mode.textHandlerSequence()) {
            jMenu.add(SwingUI.newAction(object2.toString(), object2.getIcon(), actionEvent -> this.sendTo(object2)));
        }
        JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Lines");
        jPopupMenu.add(jMenu);
        jPopupMenu.addSeparator();
        jPopupMenu.add(SwingUI.newAction("Copy", ResourceManager.getIcon("rename.action.copy"), actionEvent -> SwingUI.invokeLater(50, this::copyToClipboard)));
        jPopupMenu.add(new SaveAction(this.list.getExportHandler()));
        this.list.getListComponent().setComponentPopupMenu(jPopupMenu);
        this.list.getListComponent().setCellRenderer(new DefaultFancyListCellRenderer(){

            @Override
            protected void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
                super.configureListCellRendererComponent(jList, object, n, bl, bl2);
                ListItem listItem = (ListItem)object;
                if (listItem.error()) {
                    this.setIcon(ResourceManager.getIcon("status.warning"));
                } else {
                    this.setIcon(null);
                }
                if (listItem.isPending()) {
                    listItem.schedule();
                }
            }
        });
        this.list.setTitle(ListMode.Sequence.toString());
        JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        JSpinner jSpinner2 = new JSpinner(new SpinnerNumberModel(24, 0, Integer.MAX_VALUE, 1));
        jSpinner.setEditor(new JSpinner.NumberEditor(jSpinner, "#"));
        jSpinner2.setEditor(new JSpinner.NumberEditor(jSpinner2, "#"));
        this.setLayout((LayoutManager)new MigLayout("nogrid, fill, insets dialog", "align center", "[center, growprio 0][fill]"));
        this.add((Component)new JLabel(""), "gapbefore indent");
        this.add((Component)this.wrapEditor(this.editor), "gap related, growx, h pref, sizegroupy editor");
        this.add((Component)new JLabel("From:"), "gap 5mm");
        this.add((Component)jSpinner, "gap related, wmax 15mm, sizegroup spinner, sizegroupy editor");
        this.add((Component)new JLabel("To:"), "gap 5mm");
        this.add((Component)jSpinner2, "gap related, wmax 15mm, sizegroup spinner, sizegroupy editor");
        this.add((Component)SwingUI.newButton("Sequence", ResourceManager.getIcon("action.export"), actionEvent -> {
            listTransferablePolicy.reset();
            this.createItemSequence((Integer)jSpinner.getValue(), (Integer)jSpinner2.getValue());
        }), "gap 7mm, gapafter indent, wrap paragraph");
        this.add((Component)new LoadingOverlayPane(this.list), "grow");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets rel, nogrid, novisualpadding, fill", "align center"));
        jComponent.add(SwingUI.newButton(new LoadAction(this.list::getTransferablePolicy)));
        jComponent.add(SwingUI.newButton(SwingUI.newAction("Copy", ResourceManager.getIcon("rename.action.copy"), actionEvent2 -> {
            ActionPopup actionPopup = new ActionPopup("Copy", ResourceManager.getIcon("rename.action.copy"));
            for (Mode mode : Mode.textHandlerSequence()) {
                actionPopup.add(SwingUI.newAction("Send to " + mode, ResourceManager.getIcon("rename.action.keeplink"), actionEvent -> this.sendTo(mode)));
            }
            actionPopup.addSeparator();
            actionPopup.add(SwingUI.newAction("Copy to Clipboard", ResourceManager.getIcon("action.paste"), actionEvent -> this.copyToClipboard()));
            SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent2);
        })));
        jComponent.add((Component)SwingUI.newButton(new SaveAction(this.list.getExportHandler())), "gap related");
        this.list.add((Component)jComponent, "South");
        SwingUI.invokeLater(0, () -> {
            this.setFormatMode(ListMode.Sequence);
            SwingUI.invokeLater(250, () -> {
                if (this.mode == ListMode.Sequence) {
                    this.createItemSequence((Integer)jSpinner.getValue(), (Integer)jSpinner2.getValue());
                }
            });
        });
    }

    private RTextScrollPane wrapEditor(FormatExpressionTextArea formatExpressionTextArea) {
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)formatExpressionTextArea, false);
        rTextScrollPane.setLineNumbersEnabled(false);
        rTextScrollPane.setFoldIndicatorEnabled(false);
        rTextScrollPane.setIconRowHeaderEnabled(false);
        rTextScrollPane.setVerticalScrollBarPolicy(20);
        rTextScrollPane.setHorizontalScrollBarPolicy(31);
        rTextScrollPane.setOpaque(true);
        Border border = ThemeSupport.getEditorBorder();
        Border border2 = BorderFactory.createLineBorder(ThemeSupport.getErrorColor(), 1);
        formatExpressionTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rTextScrollPane.setBorder(border);
        formatExpressionTextArea.onChange(50, documentEvent -> this.editFormat(this.mode, formatExpressionTextArea.getText().trim(), expressionFormat -> {
            rTextScrollPane.setBorder(border);
            rTextScrollPane.repaint();
        }, exception -> {
            rTextScrollPane.setBorder(border2);
            rTextScrollPane.repaint();
        }));
        rTextScrollPane.setBackground(ThemeSupport.getEditorBackground());
        rTextScrollPane.setMinimumSize(new Dimension(300, rTextScrollPane.getPreferredSize().height));
        return rTextScrollPane;
    }

    public void editFormat(ListMode listMode, String string, Consumer<ExpressionFormat> consumer, Consumer<Exception> consumer2) {
        if (this.format != null && this.format.sameExpression(string)) {
            consumer.accept(this.format);
            return;
        }
        SwingUI.onSwingWorker(() -> {
            ExpressionFormat expressionFormat = new ExpressionFormat(string);
            if (listMode != null) {
                listMode.persistentFormat().setValue(string);
            }
            return expressionFormat;
        }, consumer.andThen(this::setFormat), consumer2);
    }

    public void setFormat(String string) throws Exception {
        if (this.format != null && this.format.sameExpression(string)) {
            return;
        }
        this.setFormat(new ExpressionFormat(string));
    }

    public void setFormat(ExpressionFormat expressionFormat) {
        this.format = expressionFormat;
        ListItem.evaluatorPool().clearQueue();
        for (int i = 0; i < this.list.getModel().size(); ++i) {
            ListItem listItem = (ListItem)this.list.getModel().get(i);
            this.list.getModel().set(i, this.prepare(listItem.withFormat(expressionFormat)));
        }
    }

    public ListItem prepare(ListItem listItem) {
        EventList<ListItem> eventList = this.list.getModel();
        if (listItem.getIndex() < eventList.size()) {
            listItem.prefill((ListItem)eventList.get(listItem.getIndex()));
        } else {
            listItem.prefill(listItem.getObject().toString());
        }
        listItem.addPropertyChangeListener(propertyChangeEvent -> {
            if (listItem.isDone() && listItem.getIndex() < eventList.size() && listItem == eventList.get(listItem.getIndex())) {
                eventList.set(listItem.getIndex(), listItem);
            }
        });
        return listItem;
    }

    public ListItem newItem(int n, Object object, int n2, int n3, int n4, List<?> list) {
        return this.prepare(new ListItem(n, object, n2, n3, n4, list, this.format));
    }

    public void createItemSequence(List<?> list) {
        List list2 = IntStream.range(0, list.size()).mapToObj(n -> this.newItem(n, list.get(n), n + 1, 1, list.size(), list)).collect(Collectors.toList());
        this.list.getListComponent().clearSelection();
        this.list.getModel().clear();
        this.list.getModel().addAll(list2);
    }

    public void createItemSequence(int n, int n2) {
        this.setFormatMode(ListMode.Sequence);
        List list = IntStream.rangeClosed(n, n2).boxed().collect(Collectors.toList());
        List list2 = IntStream.range(0, list.size()).mapToObj(n3 -> this.newItem(n3, list.get(n3), (Integer)list.get(n3), n, n2, list)).collect(Collectors.toList());
        this.list.setTitle(ListMode.Sequence.toString());
        this.list.getListComponent().clearSelection();
        this.list.getModel().clear();
        this.list.getModel().addAll(list2);
    }

    public void setFormatMode(ListMode listMode) {
        if (this.mode == listMode) {
            return;
        }
        this.mode = listMode;
        String string = listMode.persistentFormat().getValue();
        if (string == null || string.isEmpty()) {
            string = listMode.getDefaultFormatExpression();
        }
        this.editor.setText(string);
        try {
            this.setFormat(string);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(exception));
        }
    }

    public void sendTo(Mode mode) {
        SwingEventBus.getInstance().post(new TargetTransferable(mode, new StringSelection(this.list.getExportHandler().export(true))));
    }

    public void copyToClipboard() {
        SwingUI.withWaitCursor((Object)this, () -> this.list.getTransferHandler().getClipboardHandler().exportToClipboard(this, Toolkit.getDefaultToolkit().getSystemClipboard(), 1));
    }

    @Subscribe
    public void handle(Transferable transferable) throws Exception {
        TransferablePolicy transferablePolicy = this.list.getTransferablePolicy();
        if (transferablePolicy != null) {
            transferablePolicy.importData(transferable, TransferablePolicy.TransferAction.PUT);
        }
    }
}

