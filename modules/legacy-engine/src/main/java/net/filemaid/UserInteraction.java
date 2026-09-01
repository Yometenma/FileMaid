package net.filemaid;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import javax.swing.JPopupMenu;
import net.filemaid.ApplicationFolder;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Execute;
import net.filemaid.License;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.UserFiles;
import net.filemaid.platform.posix.XDG;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.ui.console.PropertyPad;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PGP;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ZipUtilities;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.WebRequest;

public class UserInteraction {
    public static void showUserDataPopup(ActionEvent actionEvent3) {
        ActionPopup actionPopup = new ActionPopup("Application Data", ResourceManager.getIcon("window.icon16"));
        actionPopup.add(SwingUI.newAction("Open Folder", ResourceManager.getIcon("action.load"), actionEvent -> UserInteraction.openAppDataFolder()));
        actionPopup.addSeparator();
        actionPopup.add(SwingUI.newAction("Export User Data", ResourceManager.getIcon("action.save"), actionEvent2 -> UserInteraction.exportUserData(actionEvent3)));
        actionPopup.add(SwingUI.newAction("Restore User Data", ResourceManager.getIcon("action.revert"), actionEvent2 -> UserInteraction.importUserData(actionEvent3)));
        actionPopup.addSeparator();
        actionPopup.add(SwingUI.newAction("Advanced Settings", ResourceManager.getIcon("action.settings"), actionEvent2 -> PropertyPad.showPropertyPad(UserData.getUserDefinedSystemProperties(), SwingUI.getWindow(actionEvent3))));
        if (Settings.LICENSE.isFile()) {
            actionPopup.addSeparator();
            actionPopup.add(SwingUI.newToolBarAction("Manage License", ResourceManager.getIcon("license.import"), actionEvent2 -> UserInteraction.showLicensePopup(actionEvent3)));
        }
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent3);
    }

    public static void showLicensePopup(ActionEvent actionEvent) {
        SwingUI.withWaitCursor((Object)actionEvent, () -> {
            try {
                UserInteraction.showLicensePopup("Manage License", Settings.LICENSE.check().toString(), actionEvent);
            }
            catch (Throwable throwable) {
                UserInteraction.showLicensePopup("Manage License", throwable.getMessage(), actionEvent);
            }
        });
    }

    public static void showLicensePopup(String string, String string2, ActionEvent actionEvent) {
        ActionPopup actionPopup = new ActionPopup(string, ResourceManager.getIcon("file.lock"));
        actionPopup.add(SwingUI.newAction("Paste License Key", ResourceManager.getIcon("license.import"), actionEvent2 -> UserInteraction.pasteLicenseKey(actionEvent)));
        actionPopup.add(SwingUI.newAction("Select License File", ResourceManager.getIcon("license.import"), actionEvent2 -> UserInteraction.selectLicenseFile(actionEvent)));
        String string3 = UserData.getLicenseKey().map(License::email).orElse(null);
        if (string3 == null) {
            actionPopup.add(SwingUI.newAction("Purchase License", ResourceManager.getIcon("license.purchase"), actionEvent2 -> UserInteraction.openPurchasePage(actionEvent, null, null)));
        } else {
            actionPopup.add(SwingUI.newAction("Renew License", ResourceManager.getIcon("license.purchase"), actionEvent2 -> GlassOptionPane.showInputDialog("Please confirm your email address:", string3, "Renew License", ResourceManager.getIcon("license.purchase"), SwingUI.getWindow(actionEvent), email -> UserInteraction.openPurchasePage(actionEvent, email, "P"))));
            actionPopup.add(SwingUI.newAction("Upgrade License", ResourceManager.getIcon("license.purchase"), actionEvent2 -> GlassOptionPane.showInputDialog("Please confirm your email address:", string3, "Upgrade License", ResourceManager.getIcon("license.purchase"), SwingUI.getWindow(actionEvent), email -> UserInteraction.openPurchasePage(actionEvent, email, "PX"))));
        }
        actionPopup.setStatus(string2);
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent);
    }

    public static void pasteLicenseKey(EventObject eventObject) {
        SwingUI.withWaitCursor((Object)eventObject, () -> {
            String string = UserInteraction.paste();
            if (string != null && !string.isEmpty()) {
                UserInteraction.configureLicense(string, true, eventObject);
            } else {
                Logging.log.warning("The clipboard is empty. Please copy your License Key first.");
            }
        });
    }

    public static void selectLicenseFile(EventObject eventObject) {
        SwingUI.withWaitCursor((Object)eventObject, () -> {
            List<File> list = UserFiles.showLoadDialogSelectFiles(false, false, null, new CategoryFileFilter("License", MediaTypes.LICENSE_FILES), "Select License", eventObject);
            if (list.size() > 0) {
                SwingUI.invokeLater(50, () -> SwingUI.withWaitCursor((Object)eventObject, () -> UserInteraction.configureLicense((File)list.get(0), true, eventObject)));
            }
        });
    }

    public static License configureLicense(File file, boolean bl, EventObject eventObject) {
        try {
            return UserInteraction.configureLicense(FileUtilities.readTextFile(file), bl, eventObject);
        }
        catch (NoSuchFileException noSuchFileException) {
            Logging.log.severe(Logging.message("File does not exist", file));
            Logging.debug.severe(noSuchFileException::toString);
        }
        catch (IOException iOException) {
            Logging.log.severe(Logging.message("File is not readable", file));
            Logging.debug.severe(iOException::toString);
        }
        catch (Throwable throwable) {
            Logging.log.severe(Logging.cause("Bad License File", throwable));
        }
        return null;
    }

    public static License configureLicense(String string, boolean bl, EventObject eventObject) {
        try {
            License license = License.importLicense(PGP.findClearSignMessage(string), bl);
            Logging.log.info(license + " has been activated successfully.");
            return license;
        }
        catch (NoSuchElementException noSuchElementException) {
            Logging.log.severe(Logging.message("Bad License Key", noSuchElementException.getMessage()));
            Logging.debug.severe(Logging.format("Bad License Key%n~~~%n%s%n~~~", string));
            if (eventObject != null) {
                String string2 = "<html>The clipboard does <u>not</u> contain a License Key.</html>";
                String string3 = SwingUI.formatHTML("<html><pre><code style='font-size:large; color:#45567C; background-color:#B3D7FE;'>%s</code></pre><br></html>", string);
                String string4 = SwingUI.formatHTML("<html><p>Please select and copy your License Key <u>completely from top to bottom</u> like so:</p><br><pre><code style='font-size:large; color:#45567C; background-color:#B3D7FE;'>%s</code></pre><br><p>* Note that your License Key starts with a human-readable section.</p><br></html>", "-----BEGIN PGP SIGNED MESSAGE-----\nHash: SHA512\n\nProduct: FileBot\nName: alice\nEmail: alice@example.com\nOrder: P123456789\nIssue-Date: 2018-06-01\nValid-Until: 2019-06-02\n-----BEGIN PGP SIGNATURE-----\n\n...\n-----END PGP SIGNATURE-----");
                GlassOptionPane.showConfirmDialog(string3, string2, ResourceManager.getIcon("license.import"), SwingUI.getWindow(eventObject), () -> GlassOptionPane.showConfirmDialog(string4, string2, ResourceManager.getIcon("license.import"), SwingUI.getWindow(eventObject), () -> UserInteraction.openLicenseHelp()));
            } else {
                Logging.help(Logging.format("Please select a valid License Key:%n%s%n", "-----BEGIN PGP SIGNED MESSAGE-----\nHash: SHA512\n\nProduct: FileBot\nName: alice\nEmail: alice@example.com\nOrder: P123456789\nIssue-Date: 2018-06-01\nValid-Until: 2019-06-02\n-----BEGIN PGP SIGNATURE-----\n\n...\n-----END PGP SIGNATURE-----"));
            }
        }
        catch (IllegalStateException illegalStateException) {
            Logging.log.severe(Logging.cause("Bad License Key", illegalStateException));
        }
        catch (IOException iOException) {
            Logging.log.severe(Logging.cause(iOException));
        }
        catch (Throwable throwable) {
            Logging.log.severe(Logging.cause("Unknown Error", throwable));
        }
        return null;
    }

    public static void exportUserData(EventObject eventObject) {
        File file = UserFiles.showSaveDialogSelectFile(new File("FileBot User Data.zip"), new CategoryFileFilter("User Data", MediaTypes.ZIP), "Export User Data", eventObject);
        if (file == null) {
            return;
        }
        SwingUI.onSwingWorker(() -> {
            UserData.backupUserData(0);
            File[] fileArray = ApplicationFolder.AppData.list(FileUtilities.FILES);
            return FileUtilities.writeFile(ZipUtilities.zip(fileArray), file);
        }, UserInteraction::reveal);
    }

    public static void importUserData(EventObject eventObject2) {
        CategoryFileFilter categoryFileFilter = new CategoryFileFilter("User Data", new ExtensionFileFilter[0]);
        categoryFileFilter.add("User Data", MediaTypes.ZIP);
        categoryFileFilter.add("Preferences", MediaTypes.XML);
        List<File> list = UserFiles.showLoadDialogSelectFiles(false, false, null, categoryFileFilter, "Import User Data", eventObject2);
        if (list.isEmpty()) {
            return;
        }
        SwingUI.onSwingWorker(() -> {
            File file = (File)list.get(0);
            if (MediaTypes.XML.accept(file)) {
                try {
                    UserData.root().restore(FileUtilities.readFile(file));
                    UserData.backupUserData(0);
                }
                catch (Exception exception) {
                    Logging.log.warning(Logging.cause("Invalid Preferences File", exception));
                }
                return eventObject2;
            }
            UserData.backupUserData(0);
            ZipUtilities.unzip(file, (zipEntry, byteBuffer) -> {
                try {
                    FileUtilities.writeFile(byteBuffer, ApplicationFolder.AppData.resolve(zipEntry.getName()));
                }
                catch (Exception exception) {
                    Logging.log.warning(Logging.cause(zipEntry, exception));
                }
            });
            UserData.restoreUserData();
            return eventObject2;
        }, eventObject -> UserInteraction.openAppDataFolder());
    }

    public static void openAppDataFolder() {
        if (Settings.isUWP()) {
            UserInteraction.open(WinAppUtilities.getPackageAppDataFolder());
        } else {
            UserInteraction.open(ApplicationFolder.AppData.getDirectory());
        }
    }

    public static void openErrorLog() {
        Logging.flushLog();
        if (Settings.isUWP()) {
            UserInteraction.open(WinAppUtilities.getPackageAppDataFolder());
        } else {
            UserInteraction.edit(UserData.getErrorLog());
        }
    }

    public static void openGettingStarted() {
        UserInteraction.browse(Settings.getApplicationProperty("link.app.help") + "#" + Settings.getApplicationDeployment());
    }

    public static void openPurchasePage(EventObject eventObject, String string, String string2) {
        SwingUI.withWaitCursor((Object)eventObject, () -> {
            try {
                License.checkServerStatus();
                if (string == null || string2 == null) {
                    UserInteraction.browse(Settings.getApplicationProperty("link.app.purchase") + "#" + Settings.getApplicationDeployment());
                    return;
                }
                if (UserInteraction.isEmailAddress(string)) {
                    UserInteraction.browse(Settings.getApplicationProperty("link.app.purchase") + "?" + WebRequest.encodeParameters("deployment", Settings.getApplicationDeployment(), "email", string, "type", string2));
                    return;
                }
                Logging.log.warning("\"" + string + "\" is not a valid email address.");
            }
            catch (Exception exception) {
                Logging.log.severe(Logging.cause(exception));
            }
        });
    }

    public static boolean isEmailAddress(String string) {
        return string.matches("[^@]+[@][^@]+[.][^@]+");
    }

    public static void openUserManual() {
        UserInteraction.browse(Settings.getApplicationProperty("link.faq"));
    }

    public static void openLicenseHelp() {
        UserInteraction.browse(Settings.getApplicationProperty("link.activate"));
    }

    public static boolean open(File file) {
        if (Settings.isLinuxContainer()) {
            return XDG.open(file);
        }
        try {
            Desktop.getDesktop().open(file);
            return true;
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
            return false;
        }
    }

    public static boolean edit(File file) {
        if (Settings.isLinuxContainer()) {
            return XDG.open(file);
        }
        if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
            try {
                Desktop.getDesktop().edit(file);
                return true;
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(exception));
            }
        }
        return UserInteraction.open(file);
    }

    public static boolean reveal(File file) {
        if (file.isHidden() && file.isDirectory()) {
            return UserInteraction.open(file);
        }
        if (Settings.isLinuxContainer()) {
            return XDG.reveal(file);
        }
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            try {
                Desktop.getDesktop().browseFileDirectory(file);
                return true;
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(exception));
            }
        }
        if (Settings.isWindowsApp()) {
            try {
                WinAppUtilities.openFolderAndSelectItem(file);
                return true;
            }
            catch (Throwable throwable) {
                Logging.debug.finest(Logging.cause(throwable));
            }
        }
        return UserInteraction.open(file.getParentFile());
    }

    public static boolean browse(String string) {
        if (Settings.isLinuxContainer()) {
            return XDG.open(string);
        }
        if (Settings.isWindowsApp() && string.startsWith("ms-windows-store://")) {
            try {
                Execute.powershell("Start-Process '" + string + "'");
                return true;
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        }
        try {
            Desktop.getDesktop().browse(URI.create(string));
            return true;
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
            return false;
        }
    }

    public static boolean delete(File file) {
        try {
            return UserFiles.getTrash().trash(file) || UserFiles.Trash.Delete.trash(file);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause("Failed to delete file", file, exception));
            return false;
        }
    }

    public static void revealFiles(Collection<File> collection) {
        collection.stream().filter(file -> {
            if (file.isHidden() && file.isDirectory()) {
                UserInteraction.open(file);
                return false;
            }
            return true;
        }).collect(Collectors.groupingBy(File::getParentFile, LinkedHashMap::new, Collectors.toList())).forEach((file, list) -> {
            if (Settings.isWindowsApp()) {
                try {
                    WinAppUtilities.openFolderAndSelectItems(list);
                }
                catch (Throwable throwable) {
                    Logging.debug.finest(Logging.cause(throwable));
                }
            } else {
                UserInteraction.reveal((File)list.get(0));
            }
        });
    }

    public static void copy(String string) {
        Object object;
        if (string == null || string.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
        long l = RegularExpressions.NEWLINE.splitAsStream(string).count();
        Object object2 = object = l == 1L ? "1 line has been copied to the clipboard." : l + " lines have been copied to the clipboard.";
        if (((String)object).equals(string)) {
            return;
        }
        Logging.log.info((String)object);
    }

    public static void copy(Clipboard clipboard, String string) {
        if (string == null || string.isEmpty()) {
            return;
        }
        clipboard.setContents(new StringSelection(string), null);
        long l = RegularExpressions.NEWLINE.splitAsStream(string).count();
        Logging.log.info((String)(l == 1L ? "1 line has been copied to the clipboard." : l + " lines have been copied to the clipboard."));
    }

    public static String paste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            try {
                String string = (String)clipboard.getData(DataFlavor.stringFlavor);
                if (string != null && string.length() > 0) {
                    return string;
                }
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        }
        return null;
    }
}

