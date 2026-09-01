package net.filemaid.ui.transfer;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import net.filemaid.CategoryFileFilter;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.ui.transfer.FileExportHandler;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;

public class SaveAction
extends AbstractAction {
    public static final String EXPORT_HANDLER = "ExportHandler";

    public SaveAction(FileExportHandler fileExportHandler) {
        this("Save as ...", ResourceManager.getIcon("action.save"), fileExportHandler);
    }

    public SaveAction(String string, Icon icon, FileExportHandler fileExportHandler) {
        this.putValue("Name", string);
        this.putValue("SmallIcon", icon);
        this.putValue(EXPORT_HANDLER, fileExportHandler);
    }

    public String getName() {
        return (String)this.getValue("Name");
    }

    public FileExportHandler getExportHandler() {
        return (FileExportHandler)this.getValue(EXPORT_HANDLER);
    }

    protected boolean canExport() {
        return this.getExportHandler().canExport();
    }

    protected void export(File file) throws Exception {
        this.getExportHandler().export(file);
        if (file.exists()) {
            UserInteraction.reveal(file);
        }
    }

    protected File getDefaultFile() {
        return new File(FileUtilities.validateFileName(this.getExportHandler().getDefaultFileName()));
    }

    protected CategoryFileFilter getFileFilter() {
        return this.getExportHandler().getFileFilter();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (this.canExport()) {
            File file = UserFiles.showSaveDialogSelectFile(this.getDefaultFile(), this.getFileFilter(), this.getName(), actionEvent);
            if (file == null) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
                return;
            }
            SwingUI.invokeLater(50, () -> {
                SwingUI.withWaitCursor((Object)actionEvent, () -> this.export(file));
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
            });
        }
    }
}

