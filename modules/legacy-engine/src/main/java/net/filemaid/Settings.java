package net.filemaid;

import groovy.lang.GroovySystem;
import java.net.URI;
import java.util.Locale;
import java.util.ResourceBundle;
import net.filemaid.Deployment;
import net.filemaid.LicenseModel;
import net.filemaid.cli.ArgumentBean;
import net.filemaid.util.IntSet;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.SystemProperty;

public final class Settings {
    public static final Deployment DEPLOYMENT = SystemProperty.of("application.deployment", Deployment::get, Deployment.JAR).get();
    public static final LicenseModel LICENSE = Settings.getLicenseModel();
    private static ArgumentBean applicationArguments;

    public static String getApplicationName() {
        return Settings.getApplicationProperty("application.name");
    }

    public static String getApplicationVersion() {
        return Settings.getApplicationProperty("application.version");
    }

    public static int getApplicationRevisionNumber() {
        try {
            return Integer.parseInt(Settings.getApplicationProperty("application.revision"));
        }
        catch (Exception exception) {
            return 0;
        }
    }

    public static String getApplicationProperty(String string) {
        return ResourceBundle.getBundle(Settings.class.getName(), Locale.ROOT).getString(string);
    }

    public static URI getApiResource(String string) {
        return URI.create("https://api.filebot.net/" + Settings.getApplicationRevisionNumber() + "/" + string);
    }

    public static boolean isUnixFS() {
        return Boolean.parseBoolean(System.getProperty("unixfs"));
    }

    public static boolean useNativeShell() {
        return Boolean.parseBoolean(System.getProperty("useNativeShell"));
    }

    public static boolean useGVFS() {
        return Boolean.parseBoolean(System.getProperty("useGVFS"));
    }

    public static boolean useExtendedFileAttributes() {
        return Boolean.parseBoolean(System.getProperty("useExtendedFileAttributes"));
    }

    public static boolean useCreationDate() {
        return Boolean.parseBoolean(System.getProperty("useCreationDate"));
    }

    public static boolean useRenameHistory() {
        return Boolean.parseBoolean(System.getProperty("application.rename.history", "true"));
    }

    public static String getApplicationDeployment() {
        return DEPLOYMENT.name().toLowerCase(Locale.ROOT);
    }

    public static boolean isAppStore() {
        return DEPLOYMENT.is(Deployment.APPX, Deployment.MAS);
    }

    public static boolean isWindowsApp() {
        return DEPLOYMENT.is(Deployment.MSI, Deployment.ZIP, Deployment.APPX);
    }

    public static boolean isLinuxApp() {
        return DEPLOYMENT.is(Deployment.DEB, Deployment.RPM, Deployment.AUR, Deployment.TAR, Deployment.SPK, Deployment.QPKG, Deployment.SNAP, Deployment.FLATPAK, Deployment.UNRAID, Deployment.DOCKER);
    }

    public static boolean isLinuxContainer() {
        return DEPLOYMENT.is(Deployment.SNAP, Deployment.FLATPAK, Deployment.UNRAID, Deployment.DOCKER);
    }

    public static boolean isDockerContainer() {
        return DEPLOYMENT.is(Deployment.UNRAID, Deployment.DOCKER);
    }

    public static boolean isLinuxPackage() {
        return DEPLOYMENT.is(Deployment.DEB, Deployment.RPM, Deployment.AUR, Deployment.SPK, Deployment.QPKG);
    }

    public static boolean isDebianPackage() {
        return DEPLOYMENT.is(Deployment.DEB);
    }

    public static boolean isMacApp() {
        return DEPLOYMENT.is(Deployment.PKG, Deployment.APP, Deployment.MAS);
    }

    public static boolean isMacSandbox() {
        return DEPLOYMENT.is(Deployment.MAS);
    }

    public static boolean isSnapSandbox() {
        return DEPLOYMENT.is(Deployment.SNAP);
    }

    public static boolean isUWP() {
        return DEPLOYMENT.is(Deployment.APPX);
    }

    public static boolean isNAS() {
        return DEPLOYMENT.is(Deployment.SPK, Deployment.QPKG);
    }

    public static boolean isPortableApp() {
        return DEPLOYMENT.is(Deployment.ZIP, Deployment.TAR);
    }

    public static boolean isAutoUpdateEnabled() {
        return DEPLOYMENT.is(Deployment.APPX, Deployment.MAS, Deployment.SNAP);
    }

    public static IntSet getPreferredParallelism() {
        return SystemProperty.get("parallelism", string -> IntSet.of(RegularExpressions.NON_DIGIT.splitAsStream((CharSequence)string).mapToInt(Integer::parseInt).toArray()), IntSet.of(1, Runtime.getRuntime().availableProcessors()));
    }

    public static IntSet getPreferredThreadPriority() {
        return SystemProperty.get("priority", string -> IntSet.of(RegularExpressions.NON_DIGIT.splitAsStream((CharSequence)string).mapToInt(Integer::parseInt).toArray()), IntSet.of(1, 5));
    }

    public static LicenseModel getLicenseModel() {
        if (Settings.isUWP()) {
            return LicenseModel.MicrosoftStore;
        }
        if (Settings.isMacSandbox()) {
            return LicenseModel.MacAppStore;
        }
        return LicenseModel.PGPSignedMessage;
    }

    public static String getWindowTitle() {
        if (Settings.isPortableApp()) {
            return Settings.getApplicationName() + " " + Settings.getApplicationVersion();
        }
        return Settings.getApplicationName();
    }

    public static String getBundleIdentifier() {
        if (Settings.isWindowsApp() || Settings.isMacApp() || Settings.isNAS() || Settings.isLinuxContainer()) {
            return Settings.getApplicationIdentifier();
        }
        if (Settings.isLinuxPackage()) {
            return String.join((CharSequence)" / ", Settings.getApplicationIdentifier(), Settings.getJavaRuntimeIdentifier());
        }
        return String.join((CharSequence)" / ", Settings.getApplicationIdentifier(), Settings.getGroovyRuntimeIdentifier(), Settings.getJavaRuntimeIdentifier(), Settings.getSystemIdentifier());
    }

    public static String getApplicationIdentifier() {
        return Settings.getApplicationName() + " " + Settings.getApplicationVersion() + " (r" + Settings.getApplicationRevisionNumber() + ")";
    }

    public static String getJavaRuntimeIdentifier() {
        return System.getProperty("java.runtime.name") + " " + System.getProperty("java.version");
    }

    public static String getGroovyRuntimeIdentifier() {
        return "Groovy " + GroovySystem.getVersion();
    }

    public static String getSystemIdentifier() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
    }

    public static ArgumentBean setApplicationArguments(String ... stringArray) throws Exception {
        applicationArguments = ArgumentBean.parse(stringArray);
        return applicationArguments;
    }

    public static ArgumentBean getApplicationArguments() {
        return applicationArguments;
    }
}

