package net.filemaid.util.prefs;

import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import net.filemaid.util.prefs.FilePreferences;

public class FilePreferencesFactory
implements PreferencesFactory {
    public static final String PREFS_FILE = System.getProperty("net.filemaid.util.prefs.file", "prefs.properties");
    public static final String PREFS_SYNC = System.getProperty("net.filemaid.util.prefs.sync", "false");
    public static final FilePreferences PREFS_ROOT = new FilePreferences(new File(PREFS_FILE), Boolean.parseBoolean(PREFS_SYNC));

    @Override
    public Preferences systemRoot() {
        return PREFS_ROOT;
    }

    @Override
    public Preferences userRoot() {
        return PREFS_ROOT;
    }
}

