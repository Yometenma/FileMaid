package net.filemaid;

import com.sun.jna.platform.FileUtils;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.platform.posix.GnomeAppUtilities;
import net.filemaid.platform.posix.XDG;
import net.filemaid.platform.windows.FileDialog;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.TrashFolder;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.SwingUI;

public class UserFiles {
    private static final Trash trash = SystemProperty.optional("net.filemaid.UserFiles.trash", Trash::forName).orElse(Trash.System);
    private static final FileChooser defaultFileChooser = SystemProperty.optional("net.filemaid.UserFiles.fileChooser", FileChooser::valueOf).orElse(FileChooser.AUTO);
    private static final String PREF_KEY_PREFIX = "dialog.";
    private static final String KEY_OPEN_FILE = "open.file";
    private static final String KEY_OPEN_FOLDER = "open.folder";
    private static final String KEY_SAVE_FILE = "save.file";
    private static final String KEY_SAVE_FOLDER = "save.folder";

    public static Trash getTrash() {
        return trash;
    }

    public static void trash(File file) throws IOException {
        if (trash != Trash.Delete) {
            try {
                if (trash.trash(file)) {
                    return;
                }
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to move file to trash", file, exception));
            }
        }
        Trash.Delete.trash(file);
    }

    public static FileChooser getFileChooser() {
        return defaultFileChooser;
    }

    public static List<File> showLoadDialogSelectFiles(boolean bl, boolean bl3, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
        boolean bl4;
        boolean bl5 = bl4 = bl && categoryFileFilter != null;
        if (bl4 && !defaultFileChooser.isFileAndFolderSelectionSupported()) {
            return UserFiles.withModeSelect(bl2 -> UserFiles.selectSelectFiles(bl2, bl3, file, categoryFileFilter, bl2 != false ? "Select Folder" : "Select Files", eventObject), eventObject);
        }
        return UserFiles.selectSelectFiles(bl, bl3, file, categoryFileFilter, string, eventObject);
    }

    private static List<File> withModeSelect(Function<Boolean, List<File>> function, EventObject eventObject) {
        ArrayList<File> arrayList = new ArrayList<File>();
        SecondaryLoop secondaryLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        ActionPopup actionPopup = new ActionPopup("Load Files", ResourceManager.getIcon("action.load"));
        actionPopup.add(SwingUI.newAction("Select Folder", ResourceManager.getIcon("tree.closed"), actionEvent -> SwingUI.invokeLater(50, () -> {
            SwingUI.withWaitCursor((Object)eventObject, () -> arrayList.addAll((Collection)function.apply(true)));
            secondaryLoop.exit();
        })));
        actionPopup.add(SwingUI.newAction("Select Files", ResourceManager.getIcon("file.generic"), actionEvent -> SwingUI.invokeLater(50, () -> {
            SwingUI.withWaitCursor((Object)eventObject, () -> arrayList.addAll((Collection)function.apply(false)));
            secondaryLoop.exit();
        })));
        actionPopup.addPopupMenuListener(SwingUI.popupMenuCanceled(popupMenuEvent -> secondaryLoop.exit()));
        SwingUI.showDropDown((JPopupMenu)actionPopup, eventObject);
        secondaryLoop.enter();
        return arrayList;
    }

    private static List<File> selectSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
        String string2 = bl ? KEY_OPEN_FOLDER : KEY_OPEN_FILE;
        List<File> list = defaultFileChooser.showLoadDialogSelectFiles(bl, bl2, UserFiles.getFileChooserDefaultFile(string2, file), categoryFileFilter, string, eventObject);
        if (list.size() > 0) {
            UserFiles.setFileChooserDefaultFile(string2, list.get(0));
        }
        return list;
    }

    public static File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
        File file2 = defaultFileChooser.showSaveDialogSelectFile(UserFiles.getFileChooserDefaultFile(KEY_SAVE_FILE, file), categoryFileFilter, string, eventObject);
        if (file2 != null) {
            UserFiles.setFileChooserDefaultFile(KEY_SAVE_FILE, file2);
        }
        return file2;
    }

    public static File showOpenDialogSelectFolder(File file, String string, EventObject eventObject) {
        File file2 = defaultFileChooser.showLoadDialogSelectFiles(true, false, UserFiles.getFileChooserDefaultFile(KEY_SAVE_FOLDER, file), null, string, eventObject).stream().findFirst().orElse(null);
        if (file2 != null) {
            UserFiles.setFileChooserDefaultFile(KEY_SAVE_FOLDER, file2);
        }
        return file2;
    }

    protected static File getFileChooserDefaultFile(String string, File file) {
        if (file != null && file.getParentFile() != null) {
            return file;
        }
        String string2 = UserData.main().get(PREF_KEY_PREFIX + string);
        if (string2 == null || string2.isEmpty()) {
            return file;
        }
        try {
            File file2;
            File file3 = new File(string2).getCanonicalFile();
            for (file2 = file3.getParentFile(); file2 != null && !file2.exists(); file2 = file2.getParentFile()) {
                file3 = file2;
            }
            if (file2 != null) {
                return new File(file2, file == null ? file3.getName() : file.getName());
            }
        }
        catch (Exception exception) {
            Logging.trace(string, exception);
        }
        return file;
    }

    protected static void setFileChooserDefaultFile(String string, File file) {
        UserData.main().put(PREF_KEY_PREFIX + string, file.getAbsolutePath());
    }

    public static enum Trash {
        System{

            @Override
            public boolean trash(File file) throws IOException {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                    return Desktop.getDesktop().moveToTrash(file);
                }
                return false;
            }
        }
        ,
        JNA{

            @Override
            public boolean trash(File file) throws IOException {
                if (FileUtils.getInstance().hasTrash()) {
                    FileUtils.getInstance().moveToTrash(new File[]{file});
                    return true;
                }
                return false;
            }
        }
        ,
        XDG{

            @Override
            public boolean trash(File file) throws IOException {
                return net.filemaid.platform.posix.XDG.trash(file);
            }
        }
        ,
        Move{

            @Override
            public boolean trash(File file) throws IOException {
                TrashFolder.getTrashFolder(file).trash(file);
                return true;
            }
        }
        ,
        Hide{

            @Override
            public boolean trash(File file) throws IOException {
                File file2 = file.getParentFile();
                File file3 = new File(file2, "." + file.getName());
                while (file3.exists()) {
                    file3 = new File(file2, "." + file3.getName());
                }
                Logging.debug.fine(Logging.format("[TRASH] from [%s] to [%s]", file, file3));
                FileUtilities.move(file, file3);
                return true;
            }
        }
        ,
        Delete{

            @Override
            public boolean trash(File file) throws IOException {
                FileUtilities.delete(file);
                return true;
            }
        };


        public abstract boolean trash(File var1) throws IOException;

        public static List<String> names() {
            return Arrays.stream(Trash.values()).map(Enum::name).collect(Collectors.toList());
        }

        public static Trash forName(String string) {
            for (Trash trash : Trash.values()) {
                if (!trash.name().equalsIgnoreCase(string)) continue;
                return trash;
            }
            throw new IllegalArgumentException(string + " not in " + Trash.names());
        }
    }

    public static enum FileChooser {
        Swing{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string2, EventObject eventObject) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setDialogTitle(string2);
                jFileChooser.setMultiSelectionEnabled(bl2);
                jFileChooser.setFileSelectionMode(bl ? (categoryFileFilter == null ? 1 : 2) : 0);
                if (file != null) {
                    if (file.isFile()) {
                        jFileChooser.setSelectedFile(file);
                    } else if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                        jFileChooser.setCurrentDirectory(file.getParentFile());
                    }
                }
                if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
                    categoryFileFilter.each((string, extensionFileFilter) -> jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter((String)string, extensionFileFilter.array())));
                    jFileChooser.setAcceptAllFileFilterUsed(false);
                }
                if (jFileChooser.showOpenDialog(SwingUI.getWindow(eventObject)) == 0) {
                    if (jFileChooser.getSelectedFiles().length > 0) {
                        return Arrays.asList(jFileChooser.getSelectedFiles());
                    }
                    if (jFileChooser.getSelectedFile() != null) {
                        return Arrays.asList(jFileChooser.getSelectedFile());
                    }
                }
                return Arrays.asList(new File[0]);
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return true;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string2, EventObject eventObject) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setDialogTitle(string2);
                jFileChooser.setMultiSelectionEnabled(false);
                jFileChooser.setSelectedFile(file);
                if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
                    categoryFileFilter.each((string, extensionFileFilter) -> jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter((String)string, extensionFileFilter.array())));
                    jFileChooser.setAcceptAllFileFilterUsed(false);
                }
                if (jFileChooser.showSaveDialog(SwingUI.getWindow(eventObject)) == 0) {
                    return jFileChooser.getSelectedFile();
                }
                return null;
            }
        }
        ,
        AWT{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                java.awt.FileDialog fileDialog = this.createFileDialog(SwingUI.getWindow(eventObject), string, 0, bl);
                fileDialog.setTitle(string);
                fileDialog.setMultipleMode(bl2);
                if (file != null) {
                    if (bl && file.isDirectory()) {
                        fileDialog.setDirectory(file.getPath());
                    } else if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                        fileDialog.setDirectory(file.getParentFile().getPath());
                        fileDialog.setFile(file.getName());
                    }
                }
                if (categoryFileFilter != null && !categoryFileFilter.acceptAny()) {
                    fileDialog.setFilenameFilter(categoryFileFilter);
                }
                fileDialog.setVisible(true);
                return Arrays.asList(fileDialog.getFiles());
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return false;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                java.awt.FileDialog fileDialog = this.createFileDialog(SwingUI.getWindow(eventObject), string, 1, false);
                fileDialog.setTitle(string);
                fileDialog.setMultipleMode(false);
                if (file != null) {
                    if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                        fileDialog.setDirectory(file.getParentFile().getPath());
                    }
                    fileDialog.setFile(file.getName());
                }
                if (categoryFileFilter != null && !categoryFileFilter.acceptAny()) {
                    fileDialog.setFilenameFilter(categoryFileFilter);
                }
                fileDialog.setVisible(true);
                File[] fileArray = fileDialog.getFiles();
                return fileArray.length > 0 ? fileArray[0] : null;
            }

            public java.awt.FileDialog createFileDialog(Window window, String string, int n, boolean bl) {
                System.setProperty("apple.awt.fileDialogForDirectories", String.valueOf(bl));
                if (window instanceof Frame) {
                    return new java.awt.FileDialog((Frame)window, string, n);
                }
                if (window instanceof Dialog) {
                    return new java.awt.FileDialog((Dialog)window, string, n);
                }
                return new java.awt.FileDialog((Frame)null, string, n);
            }
        }
        ,
        COCOA{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return MacAppUtilities.NSOpenPanel_openPanel_runModal(string, bl2, bl, !bl || categoryFileFilter != null, categoryFileFilter == null || categoryFileFilter.acceptAny() ? null : categoryFileFilter.array());
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return true;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return AWT.showSaveDialogSelectFile(file, categoryFileFilter, string, eventObject);
            }
        }
        ,
        COM{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string2, EventObject eventObject) {
                FileDialog fileDialog = new FileDialog();
                fileDialog.setTitle(string2);
                fileDialog.setFolderSelectionEnabled(bl);
                fileDialog.setMultiSelectionEnabled(bl2);
                if (file != null) {
                    boolean bl3;
                    File file2 = file.getParentFile();
                    boolean bl4 = bl3 = file2 != null && file2.isDirectory();
                    if (bl) {
                        if (file.isDirectory()) {
                            fileDialog.setFile(file.getName());
                        }
                        if (bl3) {
                            fileDialog.setFolder(file2);
                        }
                    } else if (bl3) {
                        if (file.isFile()) {
                            fileDialog.setFile(file.getName());
                        }
                        fileDialog.setFolder(file2);
                    }
                }
                if (categoryFileFilter != null && categoryFileFilter.hasTypes() && !bl) {
                    categoryFileFilter.each((string, extensionFileFilter) -> fileDialog.addFilter((String)string, extensionFileFilter.glob()));
                }
                return fileDialog.showOpenDialog(SwingUI.getWindow(eventObject));
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return false;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string2, EventObject eventObject) {
                FileDialog fileDialog = new FileDialog();
                fileDialog.setTitle(string2);
                if (file != null) {
                    if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                        fileDialog.setFolder(file.getParentFile());
                    }
                    fileDialog.setFile(file.getName());
                }
                if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
                    categoryFileFilter.each((string, extensionFileFilter) -> fileDialog.addFilter((String)string, extensionFileFilter.glob()));
                }
                return fileDialog.showSaveDialog(SwingUI.getWindow(eventObject));
            }
        }
        ,
        JavaFX{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return SwingUI.invokeJavaFX(() -> {
                    if (bl) {
                        File file2;
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setTitle(string);
                        if (file != null && file.getParentFile() != null && file.getParentFile().isDirectory()) {
                            directoryChooser.setInitialDirectory(file.getParentFile());
                        }
                        return (file2 = directoryChooser.showDialog(null)) == null ? Collections.emptyList() : Collections.singletonList(file2);
                    }
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle(string);
                    if (file != null) {
                        if (file.isFile()) {
                            fileChooser.setInitialDirectory(file.getParentFile());
                            fileChooser.setInitialFileName(file.getName());
                        } else if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                            fileChooser.setInitialDirectory(file.getParentFile());
                        }
                    }
                    if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
                        categoryFileFilter.each((description, extensionFileFilter) -> fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(description, extensionFileFilter.glob())));
                    }
                    if (bl2) {
                        List list = fileChooser.showOpenMultipleDialog(null);
                        if (list != null) {
                            return list;
                        }
                    } else {
                        File file3 = fileChooser.showOpenDialog(null);
                        if (file3 != null) {
                            return Collections.singletonList(file3);
                        }
                    }
                    return Collections.emptyList();
                });
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return false;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return SwingUI.invokeJavaFX(() -> {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle(string);
                    if (file != null) {
                        if (file.getParentFile() != null && file.getParentFile().isDirectory()) {
                            fileChooser.setInitialDirectory(file.getParentFile());
                        }
                        fileChooser.setInitialFileName(file.getName());
                    }
                    if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
                        categoryFileFilter.each((description, extensionFileFilter) -> fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(description, extensionFileFilter.glob())));
                    }
                    return fileChooser.showSaveDialog(null);
                });
            }
        }
        ,
        Zenity{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return GnomeAppUtilities.zenity.selectFiles(bl, bl2, file, bl ? null : categoryFileFilter, string, eventObject);
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return false;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                return GnomeAppUtilities.zenity.selectFile(file, null, string, true, eventObject);
            }
        }
        ,
        AUTO{

            @Override
            public List<File> showLoadDialogSelectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                if (GnomeAppUtilities.useZenity()) {
                    return Zenity.showLoadDialogSelectFiles(bl, bl2, file, categoryFileFilter, string, eventObject);
                }
                if (SwingUI.useJavaFX()) {
                    return JavaFX.showLoadDialogSelectFiles(bl, bl2, file, categoryFileFilter, string, eventObject);
                }
                if (bl) {
                    return Swing.showLoadDialogSelectFiles(true, bl2, file, null, string, eventObject);
                }
                return AWT.showLoadDialogSelectFiles(false, bl2, file, categoryFileFilter, string, eventObject);
            }

            @Override
            public boolean isFileAndFolderSelectionSupported() {
                return false;
            }

            @Override
            public File showSaveDialogSelectFile(File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
                if (GnomeAppUtilities.useZenity()) {
                    return Zenity.showSaveDialogSelectFile(file, categoryFileFilter, string, eventObject);
                }
                if (SwingUI.useJavaFX()) {
                    return JavaFX.showSaveDialogSelectFile(file, categoryFileFilter, string, eventObject);
                }
                return AWT.showSaveDialogSelectFile(file, categoryFileFilter, string, eventObject);
            }
        };


        public abstract List<File> showLoadDialogSelectFiles(boolean var1, boolean var2, File var3, CategoryFileFilter var4, String var5, EventObject var6);

        public abstract File showSaveDialogSelectFile(File var1, CategoryFileFilter var2, String var3, EventObject var4);

        public abstract boolean isFileAndFolderSelectionSupported();
    }
}

