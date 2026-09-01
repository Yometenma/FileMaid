package net.filemaid.ui.rename;

import com.sun.jna.Platform;
import java.awt.Window;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.Logging;
import net.filemaid.StandardRenameAction;
import net.filemaid.platform.windows.FileOperation;

class NativeRenameWorker {
    private final Map<File, File> renameMap;
    private final Map<File, File> renameLog;
    private final StandardRenameAction action;
    private final Window owner;

    public NativeRenameWorker(Map<File, File> map, Map<File, File> map2, StandardRenameAction standardRenameAction, Window window) {
        this.renameMap = map;
        this.renameLog = map2;
        this.action = standardRenameAction;
        this.owner = window;
    }

    private File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Invalid file path", file, exception));
            return file.getAbsoluteFile();
        }
    }

    public Map<File, File> run() throws Exception {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        FileOperation fileOperation = new FileOperation();
        for (Map.Entry<File, File> entry : this.renameMap.entrySet()) {
            File file3 = this.canonicalize(entry.getKey());
            File file4 = this.action.resolve(file3, entry.getValue());
            file4 = new File(this.canonicalize(file4.getParentFile()), file4.getName());
            switch (this.action) {
                case MOVE: {
                    if (!this.action.canRename(file3, file4)) break;
                    fileOperation.move(file3, file4);
                    break;
                }
                case COPY: {
                    if (!this.action.canRename(file3, file4)) break;
                    fileOperation.copy(file3, file4);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException(this.action.name());
                }
            }
            linkedHashMap.put(file3, file4);
        }
        if (!fileOperation.perform(this.owner)) {
            Logging.log.warning("Failed to " + this.action.getDisplayName().toLowerCase() + " some files.");
        }
        linkedHashMap.forEach((file, file2) -> {
            if (file2.exists()) {
                this.renameLog.put((File)file, (File)file2);
            }
        });
        return this.renameLog;
    }

    public static boolean isSupported(StandardRenameAction standardRenameAction) {
        try {
            return Platform.isWindows() && (standardRenameAction == StandardRenameAction.MOVE || standardRenameAction == StandardRenameAction.COPY);
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
            return false;
        }
    }
}

