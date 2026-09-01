package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.ResourceManager;
import net.filemaid.ui.subtitle.upload.Status;

class StatusRenderer
extends DefaultTableCellRenderer {
    StatusRenderer() {
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
        String string = null;
        Icon icon = null;
        switch ((Status)((Object)object)) {
            case IllegalInput: {
                string = "Please select video matching video file";
                icon = ResourceManager.getIcon("status.error");
                break;
            }
            case CheckPending: {
                string = "Pending...";
                icon = ResourceManager.getIcon("worker.pending");
                break;
            }
            case Checking: {
                string = "Checking database...";
                icon = ResourceManager.getIcon("database.go");
                break;
            }
            case CheckFailed: {
                string = "Failed to check database";
                icon = ResourceManager.getIcon("database.error");
                break;
            }
            case AlreadyExists: {
                string = "Subtitle already exists in database";
                icon = ResourceManager.getIcon("database.ok");
                break;
            }
            case Identifying: {
                string = "Identifying...";
                icon = ResourceManager.getIcon("action.export");
                break;
            }
            case IdentificationRequired: {
                string = "Please select Movie / Series and Language";
                icon = ResourceManager.getIcon("dialog.continue.invalid");
                break;
            }
            case UploadReady: {
                string = "Ready for upload";
                icon = ResourceManager.getIcon("dialog.continue");
                break;
            }
            case Uploading: {
                string = "Uploading...";
                icon = ResourceManager.getIcon("database.go");
                break;
            }
            case UploadComplete: {
                string = "Upload successful";
                icon = ResourceManager.getIcon("database.ok");
                break;
            }
            case UploadFailed: {
                string = "Upload failed";
                icon = ResourceManager.getIcon("database.error");
            }
        }
        this.setText(string);
        this.setIcon(icon);
        return this;
    }
}

