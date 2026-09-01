package net.filemaid;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import net.filemaid.ApplicationFolder;
import net.filemaid.License;
import net.filemaid.Logging;
import net.filemaid.ui.MainFrame;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PreferencesList;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.prefs.FilePreferencesFactory;
import net.filemaid.util.ui.SwingUI;

public class UserData {
    private final Preferences prefs;

    public static UserData root() {
        return UserData.forPackage(UserData.class);
    }

    public static UserData main() {
        return UserData.forPackage(MainFrame.class);
    }

    public static UserData forPackage(Class<?> clazz) {
        return new UserData(Preferences.userNodeForPackage(clazz));
    }

    private UserData(Preferences preferences) {
        this.prefs = preferences;
    }

    public UserData node(String string) {
        return new UserData(this.prefs.node(string));
    }

    public String get(String string) {
        return this.get(string, null);
    }

    public String get(String string, String string2) {
        return this.prefs.get(string, string2);
    }

    public void put(String string, String string2) {
        if (string2 != null) {
            this.prefs.put(string, string2);
        } else {
            this.remove(string);
        }
    }

    public void remove(String string) {
        this.prefs.remove(string);
    }

    public List<String> keys() {
        try {
            return Arrays.asList(this.prefs.keys());
        }
        catch (Exception exception) {
            Logging.trace(exception);
            return Collections.emptyList();
        }
    }

    public PreferencesMap.PreferencesEntry<String> entry(String string) {
        return new PreferencesMap.PreferencesEntry<String>(this.prefs, string, PreferencesMap.STRING);
    }

    public PreferencesMap.PreferencesEntry<Integer> entry(String string, int n) {
        return new PreferencesMap.PreferencesEntry<Integer>(this.prefs, string, PreferencesMap.INTEGER).defaultValue(n);
    }

    public <T> PreferencesMap.PreferencesEntry<T> entry(String string, Class<T> clazz) {
        return new PreferencesMap.PreferencesEntry<T>(this.prefs, string, new PreferencesMap.JsonAdapter<T>(clazz));
    }

    public <T> PreferencesMap.PreferencesEntry<T> entry(String string, T t, Function<String, T> function, Function<T, String> function2) {
        return new PreferencesMap.PreferencesEntry<T>(this.prefs, string, PreferencesMap.Adapter.create(function, function2)).defaultValue(t);
    }

    public <E extends Enum<E>> PreferencesMap.PreferencesEntry<E> entry(String string, E e) {
        return new PreferencesMap.PreferencesEntry(this.prefs, string, new PreferencesMap.EnumValueAdapter(e.getClass())).defaultValue(e);
    }

    public <E extends Enum<E>> PreferencesMap.PreferencesEntry<Set<E>> entrySet(String string, Class<E> clazz) {
        return new PreferencesMap.PreferencesEntry<Set<E>>(this.prefs, string, new PreferencesMap.EnumSetAdapter<E>(clazz));
    }

    public PreferencesMap<String> asMap() {
        return PreferencesMap.map(this.prefs);
    }

    public <T> PreferencesMap<T> asMap(Class<T> clazz) {
        return PreferencesMap.map(this.prefs, new PreferencesMap.JsonAdapter<T>(clazz));
    }

    public PreferencesList<String> asList() {
        return PreferencesList.map(this.prefs);
    }

    public <T> PreferencesList<T> asList(Class<T> clazz) {
        return PreferencesList.map(this.prefs, new PreferencesMap.JsonAdapter<T>(clazz));
    }

    public void restoreWindowBounds(Window window, Function<Window, Point> function) {
        PreferencesMap.PreferencesEntry<Rectangle> preferencesEntry = this.entry("window.position", Rectangle.class);
        PreferencesMap.PreferencesEntry<Integer> preferencesEntry2 = this.entry("window.state", 0);
        window.addWindowListener(SwingUI.windowClosed(windowEvent -> {
            if (window instanceof Frame) {
                Frame frame = (Frame)window;
                preferencesEntry2.setValue(frame.getExtendedState() != 0 ? 1 : 0);
                if (SwingUI.isMaximized(frame)) {
                    return;
                }
            }
            preferencesEntry.setValue(window.getBounds());
        }));
        Rectangle rectangle = preferencesEntry.getValue();
        if (rectangle != null) {
            window.setBounds(rectangle);
            if (window instanceof Frame) {
                Frame frame = (Frame)window;
                frame.setExtendedState((Integer)preferencesEntry2.getValue());
            }
            if (SwingUI.isOnScreen(rectangle)) {
                return;
            }
            Logging.debug.warning(Logging.message("Refuse to restore bad window position", rectangle));
        }
        if (function != null) {
            window.setLocation(function.apply(window));
        } else {
            window.setLocationByPlatform(true);
        }
    }

    public void restoreToggleButton(AbstractButton abstractButton) {
        PreferencesMap.PreferencesEntry<Integer> preferencesEntry = this.entry(abstractButton.getName() + ".toggle", 0);
        abstractButton.setSelected(preferencesEntry.getValue() > 0);
        abstractButton.addItemListener(itemEvent -> preferencesEntry.setValue(abstractButton.isSelected() ? 1 : 0));
    }

    public void restoreTabbedPane(String string, JTabbedPane jTabbedPane) {
        PreferencesMap.PreferencesEntry<Integer> preferencesEntry = this.entry(string + ".tab.index", 0);
        jTabbedPane.setSelectedIndex(preferencesEntry.getValue());
        jTabbedPane.addChangeListener(changeEvent -> preferencesEntry.setValue(jTabbedPane.getSelectedIndex() != 0 ? 1 : 0));
    }

    public void clear() {
        try {
            for (String string : this.prefs.childrenNames()) {
                this.prefs.node(string).removeNode();
            }
            this.prefs.clear();
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
    }

    public void flush() {
        try {
            this.prefs.flush();
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
    }

    public ByteBuffer export() throws Exception {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        this.prefs.exportSubtree(byteBufferOutputStream);
        return byteBufferOutputStream.getByteBuffer();
    }

    public void restore(byte[] byArray) throws Exception {
        Preferences.importPreferences(new ByteArrayInputStream(byArray));
    }

    public Object getBackingStore() {
        switch (this.prefs.getClass().getSimpleName()) {
            case "WindowsPreferences": {
                return "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs";
            }
            case "MacOSXPreferences": {
                return System.getProperty("user.home") + "/Library/Preferences";
            }
            case "FileSystemPreferences": {
                return System.getProperty("user.home") + "/.java/.userPrefs";
            }
            case "FilePreferences": {
                return FilePreferencesFactory.PREFS_ROOT.getBackingStore();
            }
        }
        return null;
    }

    public String toString() {
        return this.prefs.toString() + " at " + this.getBackingStore();
    }

    public static String getUID() {
        return SystemProperty.get("user.name", Object::toString, null);
    }

    public static File getLicenseFile() {
        return SystemProperty.get("net.filemaid.license", File::new, ApplicationFolder.AppData.resolve("license.txt"));
    }

    public static Optional<License> getLicenseKey() {
        return Optional.of(UserData.getLicenseFile()).filter(File::exists).map(License::parseLicenseFile);
    }

    public static File getHistoryFile() {
        return ApplicationFolder.AppData.resolve("history.xml");
    }

    public static File getPreferencesFile() {
        return ApplicationFolder.AppData.resolve("preferences.backup.xml");
    }

    public static File getUserDefinedSystemProperties() {
        return ApplicationFolder.AppData.resolve("system.properties");
    }

    public static File getErrorLog() {
        return ApplicationFolder.Logs.resolve("error.log");
    }

    public static File getProcessLock() {
        return ApplicationFolder.Logs.resolve("lock.log");
    }

    public static File getGroovyPad() {
        return ApplicationFolder.AppData.resolve("pad.groovy");
    }

    public static void restoreUserData() {
        try {
            if (UserData.getPreferencesFile().exists()) {
                Logging.log.info("Restore user preferences");
                UserData.root().restore(FileUtilities.readFile(UserData.getPreferencesFile()));
            }
        }
        catch (Throwable throwable) {
            Logging.trace("Failed to restore user preferences", throwable);
        }
    }

    public static void backupUserData(int n) {
        try {
            File file = UserData.getPreferencesFile();
            if (n <= 0 || !file.exists() || !FileUtilities.lastModifiedWithin(file, Duration.ofDays(n))) {
                ByteBuffer byteBuffer = UserData.root().export();
                if (n <= 0 || (long)byteBuffer.remaining() > 2000L) {
                    FileUtilities.writeFile(byteBuffer, file);
                }
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to store preferences", exception));
        }
    }

    public static void restoreUserDefinedSystemProperties(BiConsumer<String, String> biConsumer) throws Exception {
        File file = UserData.getUserDefinedSystemProperties();
        if (!file.isFile()) {
            return;
        }
        try (Reader reader = FileUtilities.newTextFileReader(file, false, StandardCharsets.UTF_8);){
            Properties properties = new Properties();
            properties.load(reader);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String string = entry.getKey().toString();
                String string2 = entry.getValue().toString();
                if (string.isEmpty() || string2.isEmpty()) continue;
                biConsumer.accept(string, string2);
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to read system properties", file, exception));
        }
    }

    public static File[] getUserFiles() {
        return (File[])Stream.of(UserData.getPreferencesFile(), UserData.getUserDefinedSystemProperties(), UserData.getGroovyPad(), UserData.getLicenseFile()).filter(File::isFile).toArray(File[]::new);
    }

    public static File[] getUserHistory() {
        return (File[])Stream.of(UserData.getHistoryFile()).filter(File::isFile).toArray(File[]::new);
    }
}

