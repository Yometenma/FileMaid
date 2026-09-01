package net.filemaid.ui.sfv;

import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import net.filemaid.ResourceManager;
import net.filemaid.hash.HashType;
import net.filemaid.ui.sfv.ChecksumButton;
import net.filemaid.ui.sfv.ChecksumCell;
import net.filemaid.ui.sfv.ChecksumComputationService;
import net.filemaid.ui.sfv.ChecksumComputationTask;
import net.filemaid.ui.sfv.ChecksumRow;
import net.filemaid.ui.sfv.ChecksumTable;
import net.filemaid.ui.sfv.ChecksumTableExportHandler;
import net.filemaid.ui.sfv.ChecksumTableModel;
import net.filemaid.ui.sfv.ChecksumTableTransferablePolicy;
import net.filemaid.ui.sfv.TotalProgressPanel;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.ui.transfer.FileExportHandler;
import net.filemaid.ui.transfer.LoadAction;
import net.filemaid.ui.transfer.SaveAction;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class SfvPanel
extends JComponent {
    private final ChecksumComputationService computationService = new ChecksumComputationService();
    private final ChecksumTable table = new ChecksumTable();
    private final ChecksumTableTransferablePolicy transferablePolicy = new ChecksumTableTransferablePolicy(this.table, this.computationService);
    private final ChecksumTableExportHandler exportHandler = new ChecksumTableExportHandler(this.table.getModel());
    private final LoadAction loadAction = new LoadAction(this::getTransferablePolicy);
    private final Action saveAction = SwingUI.newAction("Save as ...", ResourceManager.getIcon("action.save"), actionEvent -> {
        List<Action> list = this.table.getModel().getChecksumColumns().stream().filter(File::isDirectory).map(file -> new ChecksumColumnExportAction(file.getName(), null, this.exportHandler, (File)file)).collect(Collectors.toList());
        if (list.isEmpty()) {
            return;
        }
        if (list.size() == 1) {
            ((Action)list.get(0)).actionPerformed((ActionEvent)actionEvent);
            return;
        }
        ActionPopup actionPopup = new ActionPopup("Select Column", ResourceManager.getIcon("action.save"));
        list.forEach(actionPopup::add);
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent);
    });
    private final Action clearAction = SwingUI.newAction("Clear", ResourceManager.getIcon("action.clear"), actionEvent -> {
        this.transferablePolicy.reset();
        this.computationService.reset();
        this.table.getModel().clear();
    });
    private final Action removeAction = SwingUI.newAction("Remove", actionEvent -> {
        int n;
        int[] nArray = this.table.getSelectedRows();
        if (nArray.length == 0) {
            return;
        }
        for (n = 0; n < nArray.length; ++n) {
            nArray[n] = this.table.getRowSorter().convertRowIndexToModel(nArray[n]);
        }
        this.table.getModel().remove(nArray);
        this.computationService.purge();
        n = Math.min(nArray[0], this.table.getRowCount() - 1);
        this.table.getSelectionModel().setSelectionInterval(n, n);
    });

    public SfvPanel() {
        this.table.setTransferHandler(new DefaultTransferHandler(this.transferablePolicy, this.exportHandler));
        JComponent jComponent = SwingUI.newPanel("SFV", (LayoutManager)new MigLayout("insets 0, nogrid, novisualpadding, fill"));
        this.setLayout((LayoutManager)new MigLayout("insets dialog, fill"));
        this.add((Component)jComponent, "grow");
        jComponent.add((Component)new LoadingOverlayPane(new JScrollPane(this.table), this.transferablePolicy), "grow");
        JComponent jComponent2 = SwingUI.newPanel((LayoutManager)new MigLayout("insets rel, nogrid, novisualpadding, fill", "align center"));
        jComponent.add((Component)jComponent2, "dock south");
        jComponent2.add((Component)SwingUI.newButton(this.loadAction), "gap left 15px");
        jComponent2.add(SwingUI.newButton(this.saveAction));
        jComponent2.add((Component)SwingUI.newButton(this.clearAction), "gap right 40px");
        ButtonGroup buttonGroup = new ButtonGroup();
        for (HashType hashType : HashType.values()) {
            ChecksumButton checksumButton = new ChecksumButton(new ChangeHashTypeAction(hashType));
            buttonGroup.add(checksumButton);
            jComponent2.add(checksumButton);
        }
        jComponent2.add((Component)new TotalProgressPanel(this.computationService), "gap left 35px:push, gap right 7px, hidemode 1");
        this.table.getModel().addPropertyChangeListener(propertyChangeEvent -> {
            if ("hashType".equals(propertyChangeEvent.getPropertyName())) {
                this.restartComputation((HashType)((Object)((Object)propertyChangeEvent.getNewValue())));
            }
        });
        SwingUI.installAction((JComponent)this, 127, this.removeAction);
    }

    public TransferablePolicy getTransferablePolicy() {
        return this.transferablePolicy;
    }

    protected void restartComputation(HashType hashType) {
        this.computationService.reset();
        ChecksumTableModel checksumTableModel = this.table.getModel();
        HashMap<File, ExecutorService> hashMap = new HashMap<File, ExecutorService>(4);
        for (ChecksumRow object : checksumTableModel.rows()) {
            for (ChecksumCell checksumCell : object.values()) {
                if (checksumCell.getChecksum(hashType) == null && checksumCell.getRoot().isDirectory()) {
                    ChecksumComputationTask checksumComputationTask = new ChecksumComputationTask(new File(checksumCell.getRoot(), checksumCell.getName()), hashType);
                    checksumCell.putTask(checksumComputationTask);
                    hashMap.computeIfAbsent(checksumCell.getRoot(), file -> this.computationService.newExecutor()).execute(checksumComputationTask);
                    continue;
                }
                checksumCell.putTask(null);
            }
        }
        for (ExecutorService executorService : hashMap.values()) {
            executorService.shutdown();
        }
    }

    @Subscribe
    public void handle(Transferable transferable) throws Exception {
        TransferablePolicy transferablePolicy = this.getTransferablePolicy();
        if (transferablePolicy != null) {
            transferablePolicy.importData(transferable, TransferablePolicy.TransferAction.PUT);
        }
    }

    protected class ChangeHashTypeAction
    extends AbstractAction
    implements PropertyChangeListener {
        private ChangeHashTypeAction(HashType hashType) {
            super(hashType.toString());
            this.putValue("hashType", (Object)hashType);
            this.propertyChange(new PropertyChangeEvent(this, "hashType", null, (Object)SfvPanel.this.table.getModel().getHashType()));
            SfvPanel.this.transferablePolicy.addPropertyChangeListener(this);
            SfvPanel.this.table.getModel().addPropertyChangeListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            SfvPanel.this.table.getModel().setHashType((HashType)((Object)this.getValue("hashType")));
        }

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            if ("loading".equals(propertyChangeEvent.getPropertyName())) {
                this.setEnabled((Boolean)propertyChangeEvent.getNewValue() == false);
            } else if ("hashType".equals(propertyChangeEvent.getPropertyName())) {
                this.putValue("SwingSelectedKey", propertyChangeEvent.getNewValue() == this.getValue("hashType"));
            }
        }
    }

    protected class ChecksumColumnExportAction
    extends SaveAction {
        public static final String COLUMN = "Column";

        public ChecksumColumnExportAction(String string, Icon icon, FileExportHandler fileExportHandler, File file) {
            super(string, icon, fileExportHandler);
            this.putValue(COLUMN, file);
        }

        public File getColumn() {
            return (File)this.getValue(COLUMN);
        }

        @Override
        public ChecksumTableExportHandler getExportHandler() {
            return (ChecksumTableExportHandler)super.getExportHandler();
        }

        @Override
        protected boolean canExport() {
            return this.getExportHandler().canExport(this.getColumn());
        }

        @Override
        protected void export(File file) throws IOException {
            this.getExportHandler().export(file, this.getColumn());
        }

        @Override
        protected File getDefaultFile() {
            return new File(this.getColumn(), FileUtilities.validateFileName(this.getExportHandler().getDefaultFileName(this.getColumn())));
        }
    }
}

