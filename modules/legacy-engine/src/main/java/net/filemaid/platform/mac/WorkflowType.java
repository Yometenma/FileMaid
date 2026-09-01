package net.filemaid.platform.mac;

public enum WorkflowType {
    QuickAction,
    FolderAction;


    public String getFolderName() {
        switch (this) {
            case QuickAction: {
                return "Quick Actions";
            }
        }
        return "Folder Actions";
    }

    public String getLibraryPath() {
        switch (this) {
            case QuickAction: {
                return "Library/Services";
            }
        }
        return "Library/Workflows/Applications/Folder Actions";
    }
}

