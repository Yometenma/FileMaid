package net.filemaid.ui;

import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Execute;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.WebRequest;

public enum SuggestionDialog {
    GettingStarted{

        @Override
        public void show() {
            SwingUtilities.invokeLater(() -> {
                String string = "Hello! Do you need help Getting Started?";
                String string2 = "<html><p>If you have never used FileBot before, please have a look at <br>the video tutorials first.</p></html>";
                GlassOptionPane.showConfirmDialog(string2, string, ResourceManager.getIcon("window.icon16"), SwingUI.getMainWindow(), UserInteraction::openGettingStarted);
            });
            this.counter().setValue(1);
            this.counter().flush();
        }

        @Override
        public PreferencesMap.PreferencesEntry<Integer> counter() {
            return UserData.main().entry("getting.started", 0);
        }
    }
    ,
    ContextMenu{

        @Override
        public boolean available() {
            return Settings.isWindowsApp() && super.available() && Math.random() < 0.1;
        }

        @Override
        public void show() {
            if (!this.isInstalled()) {
                Icon icon = SuggestionDialog.fetchImageIcon("https://app.filebot.net/screenshots/filebot-context-menu.png", 2.0);
                SwingUtilities.invokeLater(() -> {
                    JLabel jLabel = new JLabel(icon, 0);
                    JLabel jLabel2 = new JLabel("<html>Would you like to install the <b>Right-Click FileBot</b> context menu extension for Windows 11 File Explorer?</html>", 0);
                    JButton jButton = SwingUI.newButton("Learn More", actionEvent -> UserInteraction.browse("https://www.filebot.net/context-menu.html"));
                    GlassOptionPane glassOptionPane2 = new GlassOptionPane();
                    glassOptionPane2.initConfigurationDialog("Right-Click FileBot", ResourceManager.getIcon("window.icon16"), jLabel, jButton);
                    glassOptionPane2.initTrailer(jLabel2);
                    glassOptionPane2.confirm.putValue("Name", "Install");
                    glassOptionPane2.cancel.putValue("Name", "Skip");
                    glassOptionPane2.open(SwingUI.getMainWindow(), glassOptionPane -> {
                        if (glassOptionPane.isConfirmed()) {
                            UserInteraction.browse("ms-windows-store://pdp/?productid=9NNH21T1XBCH");
                        }
                    });
                });
            }
            this.counter().setValue(1);
            this.counter().flush();
        }

        private boolean isInstalled() {
            try {
                return Execute.powershell("Get-AppxPackage PointPlanck.FileBotContextMenu").length() > 0;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(exception));
                return false;
            }
        }

        @Override
        public PreferencesMap.PreferencesEntry<Integer> counter() {
            return UserData.forPackage(SuggestionDialog.class).entry("suggestion.context.menu", 0);
        }
    };


    public boolean available() {
        return this.counter().getValue() == 0;
    }

    public abstract void show();

    public abstract PreferencesMap.PreferencesEntry<Integer> counter();

    private static Icon fetchImageIcon(String string, double d) {
        Cache cache = Cache.getConcurrentCache("url", CacheType.Monthly);
        try {
            byte[] byArray = cache.url(WebRequest.newURL(string)).get();
            if (byArray == null || byArray.length == 0) {
                Icon icon = null;
                return icon;
            }
            BufferedImage bufferedImage = ResourceManager.readImage(byArray);
            if (bufferedImage == null) {
                Icon icon = null;
                return icon;
            }
            ImageIcon imageIcon = new ImageIcon(ResourceManager.getMultiResolutionImage(bufferedImage, d));
            return imageIcon;
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(exception));
        }
        finally {
            cache.flush();
        }
        return null;
    }
}

