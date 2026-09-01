package net.filemaid.ui;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import net.filemaid.Settings;
import net.filemaid.UserInteraction;
import net.filemaid.util.ui.SwingUI;

public class FileBotMenuBar {
    public static JMenuBar createHelp() {
        JMenu jMenu = new JMenu("Help");
        jMenu.add(FileBotMenuBar.createLink("Getting Started", Settings.getApplicationProperty("link.intro")));
        jMenu.add(FileBotMenuBar.createLink("FAQ", Settings.getApplicationProperty("link.faq")));
        jMenu.add(FileBotMenuBar.createLink("Forums", Settings.getApplicationProperty("link.forums")));
        jMenu.add(FileBotMenuBar.createLink("Discord Channel", Settings.getApplicationProperty("link.channel")));
        jMenu.addSeparator();
        jMenu.add(FileBotMenuBar.createLink("Report Bugs", Settings.getApplicationProperty("link.bugs")));
        jMenu.add(FileBotMenuBar.createLink("Request Help", Settings.getApplicationProperty("link.help")));
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(jMenu);
        return jMenuBar;
    }

    private static Action createLink(String string, String string2) {
        return SwingUI.newAction(string, null, actionEvent -> UserInteraction.browse(string2));
    }
}

