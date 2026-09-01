package net.filemaid.ui.filter;

import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import net.filemaid.Logging;
import net.filemaid.UserData;
import net.filemaid.ui.filter.AttributeTool;
import net.filemaid.ui.filter.ExtractTool;
import net.filemaid.ui.filter.FileTreePanel;
import net.filemaid.ui.filter.MediaInfoTool;
import net.filemaid.ui.filter.SplitTool;
import net.filemaid.ui.filter.Tool;
import net.filemaid.ui.filter.TypeTool;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.miginfocom.swing.MigLayout;

public class FilterPanel
extends JComponent {
    private final FileTreePanel fileTreePanel = new FileTreePanel();
    private final JTabbedPane toolsPanel = new JTabbedPane();

    public FilterPanel() {
        this.setLayout((LayoutManager)new MigLayout("insets dialog, gapx 50, fill, nogrid"));
        this.add((Component)this.fileTreePanel, "grow 1, w 300:pref:500");
        this.add((Component)this.toolsPanel, "grow 2");
        this.fileTreePanel.addPropertyChangeListener("FILE_TREE", propertyChangeEvent -> {
            for (int i = 0; i < this.toolsPanel.getTabCount(); ++i) {
                Tool tool = (Tool)this.toolsPanel.getComponentAt(i);
                tool.setRoot(this.fileTreePanel.getFileTree().getRoot());
            }
        });
        this.add(new ExtractTool());
        this.add(new TypeTool());
        this.add(new SplitTool());
        this.add(new AttributeTool());
        this.add(new MediaInfoTool());
        UserData.forPackage(FilterPanel.class).restoreTabbedPane("tools", this.toolsPanel);
    }

    public void add(Tool<?> tool) {
        this.toolsPanel.addTab(tool.getName(), tool);
    }

    public void reload() {
        FileTransferable fileTransferable = new FileTransferable(this.fileTreePanel.getFileTree().getRoot());
        this.handle(fileTransferable);
    }

    @Subscribe
    public void handle(Transferable transferable) {
        TransferablePolicy transferablePolicy = this.fileTreePanel.getTransferablePolicy();
        if (transferablePolicy != null) {
            try {
                transferablePolicy.importData(transferable, TransferablePolicy.TransferAction.PUT);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
    }
}

