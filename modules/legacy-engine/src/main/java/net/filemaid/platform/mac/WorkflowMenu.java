package net.filemaid.platform.mac;

import java.io.File;
import java.io.FileFilter;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import net.filemaid.UserInteraction;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;

public class WorkflowMenu
extends SwingUI.DynamicMenu {
    public static final FileFilter WORKFLOW = new ExtensionFileFilter("workflow");
    public File templateFolder;
    public File targetFolder;

    public WorkflowMenu(String string, File file, File file2) {
        super(string);
        this.templateFolder = file;
        this.targetFolder = file2;
    }

    @Override
    protected void populate() {
        for (File file : FileUtilities.getChildren(this.templateFolder, WORKFLOW)) {
            File file2 = new File(this.targetFolder, file.getName());
            if (file2.exists()) {
                this.add(this.createUninstallMenu(file2));
                continue;
            }
            this.add(this.createInstallMenu(file));
        }
    }

    protected JMenuItem createInstallMenu(File file) {
        JMenu jMenu = new JMenu(FileUtilities.getNameWithoutExtension(file.getName()));
        jMenu.add(SwingUI.newAction("Install", actionEvent -> UserInteraction.open(file)));
        jMenu.add(SwingUI.newAction("Reveal", actionEvent -> UserInteraction.reveal(file)));
        return jMenu;
    }

    protected JMenuItem createUninstallMenu(File file) {
        JMenu jMenu = new JMenu(FileUtilities.getNameWithoutExtension(file.getName()));
        jMenu.add(SwingUI.newAction("Uninstall", actionEvent -> UserInteraction.delete(file)));
        jMenu.add(SwingUI.newAction("Open", actionEvent -> UserInteraction.open(file)));
        jMenu.add(SwingUI.newAction("Reveal", actionEvent -> UserInteraction.reveal(file)));
        return jMenu;
    }
}

