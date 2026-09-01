package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.filemaid.ApplicationFolder;
import net.filemaid.Logging;
import net.filemaid.util.FileUtilities;

public final class TemporaryFolder {
    private static final Map<String, TemporaryFolder> folders = new HashMap<String, TemporaryFolder>();
    private final File folder;

    public static synchronized TemporaryFolder getFolder(String string) {
        return folders.computeIfAbsent(string.toLowerCase(Locale.ROOT), string2 -> new TemporaryFolder(new File(TemporaryFolder.root(), string + "." + ProcessHandle.current().pid())));
    }

    public static File root() {
        return ApplicationFolder.AppData.resolve("tmp");
    }

    private TemporaryFolder(File file) {
        this.folder = file;
        try {
            FileUtilities.createFolders(file);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Create temporary folder", file, exception));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::clean, "TemporaryFolderShutdownHook"));
    }

    private void clean() {
        try {
            FileUtilities.delete(this.folder);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Delete temporary folder", this.folder, exception));
        }
    }

    public File resolve(String string) {
        return new File(this.folder, string);
    }

    public File createFile(String string) throws IOException {
        File file = this.resolve(string);
        file.createNewFile();
        return file;
    }

    public File createFile(String string, String string2) throws IOException {
        return File.createTempFile(string, string2, this.folder);
    }

    public String toString() {
        return this.folder.getPath();
    }
}

