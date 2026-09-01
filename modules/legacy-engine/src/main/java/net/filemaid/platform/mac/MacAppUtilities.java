package net.filemaid.platform.mac;

import ca.weblite.objc.Client;
import ca.weblite.objc.Proxy;
import ca.weblite.objc.util.CocoaUtils;
import com.sun.jna.platform.mac.CoreFoundation;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.desktop.QuitStrategy;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import net.filemaid.ApplicationFolder;
import net.filemaid.Logging;
import net.filemaid.platform.mac.CodeSigningServices;
import net.filemaid.platform.mac.DropToUnlock;
import net.filemaid.platform.mac.MASReceipt;
import net.filemaid.platform.mac.MASReceiptValidationFailure;
import net.filemaid.platform.mac.SecTaskRef;
import net.filemaid.platform.mac.WorkflowMenu;
import net.filemaid.platform.mac.WorkflowType;
import net.filemaid.ui.console.GroovyPad;
import net.filemaid.util.SystemProperty;

public class MacAppUtilities {
    public static String getSigningIdentifier() {
        SecTaskRef secTaskRef = CodeSigningServices.INSTANCE.SecTaskCreateFromSelf(null);
        try {
            CoreFoundation.CFBooleanRef cFBooleanRef;
            CoreFoundation.CFTypeRef cFTypeRef = CodeSigningServices.INSTANCE.SecTaskCopyValueForEntitlement(secTaskRef, CoreFoundation.CFStringRef.createCFString((String)"com.apple.security.app-sandbox"), null);
            if (cFTypeRef != null && (cFBooleanRef = new CoreFoundation.CFBooleanRef(cFTypeRef.getPointer())).booleanValue()) {
                try {
                    String string = CodeSigningServices.INSTANCE.SecTaskCopySigningIdentifier(secTaskRef, null).stringValue();
                    return string;
                }
                catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                    String string = "";
                    secTaskRef.release();
                    return string;
                }
            }
        }
        finally {
            secTaskRef.release();
        }
        return null;
    }

    public static String NSBundle_mainBundle_appStoreReceiptURL_path() {
        return Optional.ofNullable(Client.getInstance().sendProxy("NSBundle", "mainBundle", new Object[0]).sendProxy("appStoreReceiptURL", new Object[0])).map(proxy -> proxy.sendString("path", new Object[0])).orElse(null);
    }

    public static Object NSData_initWithBase64Encoding(String string) {
        return Client.getInstance().sendProxy("NSData", "alloc", new Object[0]).send("initWithBase64Encoding:", new Object[]{string});
    }

    public static String NSURL_bookmarkDataWithOptions(String string) {
        return Client.getInstance().sendProxy("NSURL", "fileURLWithPath:", new Object[]{string}).sendProxy("bookmarkDataWithOptions:includingResourceValuesForKeys:relativeToURL:error:", new Object[]{2048, null, null, null}).sendString("base64Encoding", new Object[0]);
    }

    public static Object NSURL_URLByResolvingBookmarkData_startAccessingSecurityScopedResource(String string) {
        return Client.getInstance().sendProxy("NSURL", "URLByResolvingBookmarkData:options:relativeToURL:bookmarkDataIsStale:error:", new Object[]{MacAppUtilities.NSData_initWithBase64Encoding(string), 1024, null, false, null}).send("startAccessingSecurityScopedResource", new Object[0]);
    }

    public static List<File> NSOpenPanel_openPanel_runModal(String string, boolean bl, boolean bl2, boolean bl3, String[] stringArray) {
        ArrayList<File> arrayList = new ArrayList<File>();
        EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        SecondaryLoop secondaryLoop = eventQueue.createSecondaryLoop();
        CocoaUtils.dispatch_async(() -> {
            try {
                String string2;
                int n;
                Proxy proxy;
                Proxy proxy2 = Client.getInstance().sendProxy("NSOpenPanel", "openPanel", new Object[0]);
                proxy2.send("retain", new Object[0]);
                proxy2.send("setTitle:", new Object[]{string});
                proxy2.send("setAllowsMultipleSelection:", new Object[]{bl ? 1 : 0});
                proxy2.send("setCanChooseDirectories:", new Object[]{bl2 ? 1 : 0});
                proxy2.send("setCanChooseFiles:", new Object[]{bl3 ? 1 : 0});
                if (stringArray != null) {
                    proxy = Client.getInstance().sendProxy("NSMutableArray", "arrayWithCapacity:", new Object[]{stringArray.length});
                    String[] stringArray2 = stringArray;
                    n = stringArray2.length;
                    for (int i = 0; i < n; ++i) {
                        string2 = stringArray2[i];
                        proxy.send("addObject:", new Object[]{string2});
                    }
                    proxy2.send("setAllowedFileTypes:", new Object[]{proxy});
                }
                if (proxy2.sendInt("runModal", new Object[0]) != 0) {
                    proxy = proxy2.getProxy("URLs");
                    int n2 = proxy.sendInt("count", new Object[0]);
                    for (n = 0; n < n2; ++n) {
                        Proxy proxy3 = proxy.sendProxy("objectAtIndex:", new Object[]{n});
                        string2 = proxy3.sendString("path", new Object[0]);
                        arrayList.add(new File(string2));
                    }
                }
            }
            catch (Throwable throwable) {
                Logging.trace("NSOpenPanel", throwable);
            }
            finally {
                secondaryLoop.exit();
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
        if (!secondaryLoop.enter()) {
            throw new IllegalStateException("SecondaryLoop");
        }
        return arrayList;
    }

    public static boolean isAppleInterfaceStyleDark() {
        return Boolean.parseBoolean(System.getProperty("DarkMode"));
    }

    public static void initializeApplication(Window window, JMenuBar jMenuBar, Consumer<List<File>> consumer) {
        UIManager.put("TitledBorder.border", UIManager.getBorder("InsetBorder.aquaVariant"));
        Desktop.getDesktop().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
        Desktop.getDesktop().setDefaultMenuBar(jMenuBar);
        Desktop.getDesktop().setOpenFileHandler(openFilesEvent -> {
            List<File> list = openFilesEvent.getFiles();
            if (list.size() > 0) {
                consumer.accept(list);
            }
        });
        SystemProperty.optional("apple.app.workflows", File::new).ifPresent(file -> {
            Desktop.getDesktop().setAboutHandler(aboutEvent -> GroovyPad.open(window, true));
            MacAppUtilities.initializeWorkflowServiceMenu(file, jMenuBar.getMenu(0));
        });
    }

    public static void initializeWorkflowServiceMenu(File file, JMenu jMenu) {
        jMenu.addSeparator();
        for (WorkflowType workflowType : WorkflowType.values()) {
            File file2 = new File(file, workflowType.getFolderName());
            File file3 = new File(ApplicationFolder.UserHome.getDirectory(), workflowType.getLibraryPath());
            if (!file2.exists()) continue;
            jMenu.add(new WorkflowMenu(workflowType.getFolderName(), file2, file3));
        }
    }

    public static boolean isLockedFolder(File file) {
        return file.isDirectory() && !file.canRead();
    }

    public static boolean askUnlockFolders(Window window, Collection<File> collection) {
        return DropToUnlock.showUnlockFoldersDialog(window, collection);
    }

    public static MASReceipt getAppStoreReceipt() throws FileNotFoundException, MASReceiptValidationFailure {
        return MASReceipt.read(MacAppUtilities.NSBundle_mainBundle_appStoreReceiptURL_path());
    }

    private MacAppUtilities() {
        throw new UnsupportedOperationException();
    }
}

