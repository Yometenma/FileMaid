package net.filemaid;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.StreamHandler;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.filemaid.ApplicationFolder;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Deployment;
import net.filemaid.EscapeCode;
import net.filemaid.HistorySpooler;
import net.filemaid.License;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.cli.ArgumentBean;
import net.filemaid.cli.ArgumentProcessor;
import net.filemaid.platform.mac.MASReceiptValidationFailure;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.platform.posix.GnomeAppUtilities;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.BaseFrame;
import net.filemaid.ui.FileBotMenuBar;
import net.filemaid.ui.MainFrame;
import net.filemaid.ui.Mode;
import net.filemaid.ui.NotificationHandler;
import net.filemaid.ui.SinglePanelFrame;
import net.filemaid.ui.SuggestionDialog;
import net.filemaid.ui.SupportDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.XPathUtilities;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.WebRequest;
import net.miginfocom.swing.MigLayout;
import org.kohsuke.args4j.CmdLineException;
import org.w3c.dom.Document;

public class Main {
    public static void main(String[] stringArray) {
        try {
            ApplicationFolder applicationFolder;
            int n;
            ArgumentBean argumentBean = Settings.setApplicationArguments(stringArray);
            if (argumentBean.printVersion()) {
                System.out.println(Settings.getBundleIdentifier());
                System.exit(0);
            }
            if (argumentBean.printHelp()) {
                System.out.println(Settings.getApplicationIdentifier());
                System.out.println();
                System.out.println(argumentBean.usage());
                System.out.println();
                System.out.println(EscapeCode.color(EscapeCode.ORANGE_ONE, "Please read the manual for details and usage examples:"));
                System.out.println(EscapeCode.color(EscapeCode.ORANGE_ONE, Settings.getApplicationProperty("link.help.manpage")));
                System.out.println();
                System.exit(0);
            }
            if (Settings.isUWP()) {
                WinAppUtilities.requirePackageIdentity();
            }
            ApplicationFolder[] applicationFolders = ApplicationFolder.values();
            for (n = 0; n < applicationFolders.length; ++n) {
                applicationFolder = applicationFolders[n];
                try {
                    if (!applicationFolder.mkdir()) continue;
                    Logging.debug.warning(Logging.format("Initialize %s folder: %s", applicationFolder.name(), applicationFolder.getDirectory()));
                    continue;
                }
                catch (Exception exception) {
                    Logging.log.severe(Logging.format("%s folder does not exist and could not be created: %s", applicationFolder.name(), applicationFolder.getDirectory()));
                    throw exception;
                }
            }
            if (argumentBean.clearCache() || argumentBean.clearUserData() || argumentBean.clearHistory()) {
                if (argumentBean.clearCache()) {
                    Logging.log.info("Clear cache");
                    File[] files = ApplicationFolder.Cache.list(FileUtilities.FOLDERS);
                    for (n = 0; n < files.length; ++n) {
                        File file = files[n];
                        if (System.console() == null && FileUtilities.getChildren(file, FileUtilities.FILES).stream().allMatch(f -> FileUtilities.lastModifiedWithin(f, Cache.ONE_WEEK))) {
                            Duration duration = FileUtilities.getChildren(file, FileUtilities.FILES).stream().map(f -> FileUtilities.getFileAge(f)).max(Duration::compareTo).orElse(Duration.ZERO);
                            Logging.log.fine(Logging.format("* Keep %s (%02d:%02d)", new Object[]{file, duration.toHours(), duration.toMinutes() % 60L}));
                            continue;
                        }
                        long l = FileUtilities.getChildren(file, FileUtilities.FILES).stream().mapToLong(File::length).sum();
                        Logging.log.fine(Logging.format("* Delete %s (%s)", new Object[]{file, FileUtilities.formatSize(l)}));
                        try {
                            FileUtilities.delete(file);
                            continue;
                        }
                        catch (Exception exception) {
                            Logging.log.severe(Logging.cause(exception));
                        }
                    }
                }
                if (argumentBean.clearHistory()) {
                    Logging.log.info("Reset history");
                    File[] historyFiles = UserData.getUserHistory();
                    for (n = 0; n < historyFiles.length; ++n) {
                        File file = historyFiles[n];
                        Logging.log.fine(Logging.format("* Delete %s", new Object[]{file}));
                        try {
                            FileUtilities.delete(file);
                            continue;
                        }
                        catch (Exception exception) {
                            Logging.log.severe(Logging.cause(exception));
                        }
                    }
                }
                if (argumentBean.clearUserData()) {
                    Logging.log.info("Reset preferences");
                    UserData.root().clear();
                    File[] userFiles = UserData.getUserFiles();
                    for (n = 0; n < userFiles.length; ++n) {
                        File file = userFiles[n];
                        Logging.log.fine(Logging.format("* Delete %s", new Object[]{file}));
                        try {
                            FileUtilities.delete(file);
                            continue;
                        }
                        catch (Exception exception) {
                            Logging.log.severe(Logging.cause(exception));
                        }
                    }
                }
                System.exit(0);
            }
            Main.initializeSystemProperties(argumentBean);
            Main.initializeLogging(argumentBean);
            if (Logging.debug.isLoggable(Level.INFO)) {
                Main.printEnvironment();
            }
            if (Settings.isMacSandbox()) {
                try {
                    MacAppUtilities.getAppStoreReceipt().check("net.filemaid.FileBot");
                }
                catch (FileNotFoundException | MASReceiptValidationFailure throwable) {
                    Logging.trace("Failed to validate MASReceipt", throwable);
                    MASReceiptValidationFailure.relaunch();
                }
            }
            if (Settings.isPortableApp() && !FileUtilities.UNIX) {
                ApplicationFolder[] applicationFolders2 = ApplicationFolder.values();
                for (n = 0; n < applicationFolders2.length; ++n) {
                    applicationFolder = applicationFolders2[n];
                    if (!FileUtilities.isUNC(applicationFolder.getDirectory())) continue;
                    throw new IllegalStateException("Cannot run application from a UNC network path: " + applicationFolder.getDirectory());
                }
            }
            Cache.DISK_STORE.acquire();
            if (Settings.useRenameHistory()) {
                HistorySpooler.HISTORY.setPersistentHistoryFile(UserData.getHistoryFile());
                HistorySpooler.HISTORY.commitOnExit();
            }
            WebServices.login();
            if (argumentBean.runCLI()) {
                String licenseKey = argumentBean.getLicenseKey();
                if (Settings.LICENSE.isFile() && licenseKey != null) {
                    License license = UserInteraction.configureLicense(licenseKey, System.console() != null, null);
                    Main.shutdown(license != null ? 0 : 2);
                }
                int n3 = new ArgumentProcessor(argumentBean).run();
                Main.shutdown(n3);
            }
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println(Settings.getBundleIdentifier());
                System.out.println();
                System.out.println(argumentBean.usage());
                Main.shutdown(1);
            }
            SwingUtilities.invokeLater(() -> Main.startUserInterface(argumentBean));
        }
        catch (CmdLineException cmdLineException) {
            Logging.help(ArgumentProcessor.formatArgumentHelp(stringArray, true));
            Logging.log.log(Level.SEVERE, Logging.cause(cmdLineException));
            Main.shutdown(1);
        }
        catch (Throwable throwable) {
            Logging.log.log(Level.SEVERE, throwable, Logging.cause("An unexpected error occurred during startup", throwable));
            Main.shutdown(Main.onStartError(throwable));
        }
    }

    private static void onStart(ArgumentBean argumentBean) throws Exception {
        List<File> list = argumentBean.getFileArguments();
        if (list.size() > 0) {
            SwingEventBus.getInstance().post(new FileTransferable(list));
        }
        if (Settings.LICENSE.isFile()) {
            try {
                String string = argumentBean.getLicenseKey();
                if (string != null) {
                    UserInteraction.configureLicense(string, false, null);
                }
            }
            catch (Throwable throwable) {
                Logging.trace(throwable);
            }
        }
        if (UserData.main().keys().isEmpty()) {
            UserData.restoreUserData();
        }
        if (!"skip".equals(System.getProperty("application.help"))) {
            try {
                Main.checkGettingStarted();
            }
            catch (Throwable throwable) {
                Logging.trace("Failed to show Getting Started help", throwable);
            }
        }
        if (!"skip".equals(System.getProperty("application.update"))) {
            try {
                Main.checkUpdate();
            }
            catch (Throwable throwable) {
                Logging.trace("Failed to check for updates", throwable);
            }
        }
    }

    private static void startUserInterface(ArgumentBean argumentBean) {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ThemeSupport.setTheme();
        Mode[] modeArray = argumentBean.getPanelMode();
        BaseFrame baseFrame = modeArray.length > 1 ? new MainFrame(modeArray) : new SinglePanelFrame(modeArray[0]);
        baseFrame.setDefaultCloseOperation(2);
        UserData.forPackage(MainFrame.class).restoreWindowBounds(baseFrame, null);
        baseFrame.addWindowListener(SwingUI.windowClosed(windowEvent -> {
            baseFrame.setVisible(false);
            UserData.backupUserData(60);
            if (Settings.isAppStore()) {
                SupportDialog.AppStoreReview.maybeShow();
            }
            Main.shutdown(0);
        }));
        if (Settings.isMacApp()) {
            MacAppUtilities.initializeApplication(baseFrame, FileBotMenuBar.createHelp(), list -> {
                if (Settings.LICENSE.isFile() && list.size() == 1 && FileUtilities.containsOnly(list, (FileFilter)MediaTypes.LICENSE_FILES)) {
                    SwingUI.onSwingWorker(() -> UserInteraction.configureLicense((File)list.get(0), false, null));
                } else {
                    SwingEventBus.getInstance().post(new FileTransferable((Collection<File>)list));
                }
            });
        } else if (Settings.isWindowsApp()) {
            WinAppUtilities.initializeApplication(Settings.isUWP() ? null : Settings.getApplicationName(), () -> Main.shutdown(0));
            baseFrame.setIconImages(ResourceManager.getApplicationIconImages());
        } else {
            GnomeAppUtilities.initializeApplication();
            baseFrame.setIconImages(ResourceManager.getApplicationIconImages());
        }
        SwingUtilities.invokeLater(() -> {
            baseFrame.getRootPane().setDoubleBuffered(false);
            baseFrame.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                baseFrame.getRootPane().setDoubleBuffered(true);
                SwingUI.onSwingWorker(() -> Main.onStart(argumentBean));
            });
        });
    }

    private static void checkUpdate() throws Exception {
        Cache cache = Cache.getCache("data", CacheType.Monthly);
        try {
            Document document = cache.xml("update.url", string -> WebRequest.newURL(Settings.getApplicationProperty(string))).expire(Cache.ONE_WEEK).retry(0).get();
            Map<String, String> map = XPathUtilities.streamElements(document.getFirstChild()).collect(Collectors.toMap(node -> node.getNodeName(), node -> node.getTextContent().trim()));
            int n = Integer.parseInt(map.get("revision"));
            int n2 = Settings.getApplicationRevisionNumber();
            int n3 = Optional.ofNullable(cache.get("update.revision")).map(Integer.class::cast).orElse(0);
            if (n > n2 && n > n3) {
                SwingUtilities.invokeLater(() -> {
                    BaseDialog baseDialog = new BaseDialog(SwingUI.getMainWindow(), (String)map.get("title"), Dialog.ModalityType.DOCUMENT_MODAL);
                    JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill, nogrid, insets dialog"));
                    baseDialog.setContentPane(jPanel);
                    jPanel.add((Component)new JLabel(ResourceManager.getIcon("window.icon.medium")), "aligny top");
                    jPanel.add((Component)new JLabel((String)map.get("message")), "aligny top, gap 10, wrap paragraph:push");
                    jPanel.add((Component)SwingUI.newButton("Download", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
                        UserInteraction.browse((String)map.get("download"));
                        baseDialog.setVisible(false);
                    }), "tag ok");
                    jPanel.add((Component)SwingUI.newButton("Details", ResourceManager.getIcon("action.report"), actionEvent -> UserInteraction.browse((String)map.get("discussion"))), "tag left");
                    jPanel.add((Component)SwingUI.newButton("Ignore", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
                        cache.put("update.revision", n);
                        cache.flush();
                        baseDialog.setVisible(false);
                    }), "tag cancel");
                    baseDialog.pack();
                    baseDialog.setLocation(SwingUI.getOffsetLocation(baseDialog));
                    SwingUI.showLater(baseDialog);
                });
            }
        }
        finally {
            cache.flush();
        }
    }

    private static void checkGettingStarted() throws Exception {
        SwingUtilities.invokeLater(() -> SwingUI.installAction(SwingUI.getMainWindow(), 112, SwingUI.newAction("Help", actionEvent -> UserInteraction.openUserManual())));
        for (SuggestionDialog suggestionDialog : SuggestionDialog.values()) {
            if (!suggestionDialog.available()) continue;
            suggestionDialog.show();
            break;
        }
        UserData.getLicenseKey().ifPresent(license -> license.remainingDays().ifPresent(n -> {
            if (n < 0) {
                Logging.log.info(Logging.format("%s has expired %s %s ago.", license, -n.intValue(), -n.intValue() == 1 ? "day" : "days"));
            } else if (n < 3) {
                Logging.log.info(Logging.format("%s will expire in %s %s.", license, n, n == 1 ? "day" : "days"));
            }
        }));
    }

    public static void initializeSystemProperties(ArgumentBean argumentBean) throws Exception {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        Locale.setDefault(Locale.Category.DISPLAY, Locale.US);
        System.setProperty("unixfs", "false");
        System.setProperty("useExtendedFileAttributes", "true");
        System.setProperty("useCreationDate", "false");
        System.setProperty("jna.nosys", Settings.isLinuxApp() ? "false" : "true");
        System.setProperty("jna.nounpack", "true");
        System.setProperty("jna.noclasspath", "true");
        System.setProperty("http.agent", Settings.getApplicationName() + "/" + Settings.getApplicationVersion());
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "60000");
        System.setProperty("java.net.useSystemProxies", Settings.isLinuxApp() ? "false" : "true");
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        if (Settings.DEPLOYMENT.is(Deployment.APPX) || Runtime.version().feature() < 17) {
            System.setProperty("net.filemaid.web.WebRequest.v1", "true");
        }
        if (Settings.isWindowsApp()) {
            System.setProperty("swing.crossplatformlaf", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        System.setProperty("grape.root", ApplicationFolder.AppData.resolve("grape").getPath());
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        System.setProperty("jdk.logger.packages", "net.filemaid.Logging");
        UserData.restoreUserDefinedSystemProperties(System::setProperty);
        if (Settings.isMacApp() && ThemeSupport.getTheme().isDark()) {
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        }
        if (argumentBean.unixfs) {
            System.setProperty("unixfs", "true");
        }
        if (argumentBean.disableExtendedAttributes) {
            System.setProperty("useExtendedFileAttributes", "false");
            System.setProperty("useCreationDate", "false");
        }
        if (argumentBean.disableMediaParser) {
            System.setProperty("net.filemaid.media.parser", "none");
            System.setProperty("net.filemaid.files.heuristics", "true");
        }
        if (argumentBean.disableMediaIndex) {
            System.setProperty("net.filemaid.media.index", "false");
        }
        if (argumentBean.disableHistory) {
            System.setProperty("application.rename.history", "false");
        }
    }

    public static void initializeLogging(ArgumentBean argumentBean) throws Exception {
        StreamHandler streamHandler;
        File file = argumentBean.getLogFile();
        if (argumentBean.runCLI()) {
            Logging.log.setLevel(argumentBean.getLogLevel());
            if (file == null && System.console() == null) {
                streamHandler = Logging.createLogFileHandler(UserData.getProcessLock(), true, Level.WARNING);
                Logging.log.addHandler(streamHandler);
                Logging.debug.addHandler(streamHandler);
            }
        } else {
            Logging.log.setLevel(Level.INFO);
            Logging.log.addHandler(new NotificationHandler(Settings.getApplicationName(), Settings.isMacApp()));
            try {
                streamHandler = Logging.createSimpleFileHandler(UserData.getErrorLog(), Level.WARNING);
                Logging.log.addHandler(streamHandler);
                Logging.debug.addHandler(streamHandler);
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause("Failed to initialize error log", exception));
            }
        }
        if (file != null) {
            streamHandler = Logging.createLogFileHandler(file, true, Level.ALL);
            Logging.log.addHandler(streamHandler);
            Logging.debug.addHandler(streamHandler);
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Logging.trace("Uncaught Exception in " + thread.getName(), throwable));
    }

    public static void printEnvironment() {
        Logging.debug.info("# Application Bundle");
        Logging.debug.info(String.join((CharSequence)" / ", Settings.getApplicationIdentifier(), Settings.getGroovyRuntimeIdentifier(), Settings.getJavaRuntimeIdentifier(), Settings.getSystemIdentifier()));
        Logging.debug.info("# Local Time");
        Logging.debug.info(new Date().toString());
        Logging.debug.info("# Process Tree");
        try {
            Deque<String> commands = new ArrayDeque<>();
            ProcessHandle processHandle = ProcessHandle.current();
            while (processHandle != null) {
                Optional<String> optional = processHandle.info().command();
                if (optional.isPresent()) {
                    commands.push(optional.get());
                }
                processHandle = processHandle.parent().orElse(null);
            }
            commands.forEach(string -> Logging.debug.info(Logging.formatSingleLine("\u2514\u2500 %s", string)));
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause(throwable));
        }
        Logging.debug.info("# Environment Variables");
        System.getenv().keySet().stream().sorted().forEach(string -> Logging.debug.info(Logging.formatSingleLine("%s = %s", string, System.getenv(string))));
        Logging.debug.info("# Java System Properties");
        System.getProperties().keySet().stream().map(Object::toString).sorted().forEach(string -> Logging.debug.info(Logging.formatSingleLine("%s = %s", string, System.getProperty(string))));
        Logging.debug.info("# Arguments");
        for (String string2 : Settings.getApplicationArguments().getArgumentArray()) {
            Logging.debug.info(Logging.formatSingleLine("\u2514\u2500 %s", string2));
        }
    }

    private static int onStartError(Throwable throwable) {
        try {
            if (System.console() == null && !GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null, Logging.cause(throwable), Settings.getApplicationIdentifier(), 0));
            }
        }
        catch (Throwable throwable2) {
            // empty catch block
        }
        return 1;
    }

    public static void shutdown(int n) {
        HistorySpooler.HISTORY.commit();
        Cache.DISK_STORE.shutdown();
        UserData.root().flush();
        System.exit(n);
    }
}

