package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import net.filemaid.Logging;
import net.filemaid.util.FileUtilities;

public class TrashFolder {
    private final File folder;

    public TrashFolder(File file) {
        this.folder = file;
    }

    public void trash(File file) throws IOException {
        File file2 = new File(this.folder, file.getName());
        int n = 1;
        while (file2.exists()) {
            file2 = new File(this.folder, file.getName() + "." + n);
            ++n;
        }
        Logging.debug.fine(Logging.format("[TRASH] from [%s] to [%s]", file, file2));
        FileUtilities.move(file, file2);
    }

    public static TrashFolder getTrashFolder(File file) throws IOException {
        File file2 = new File(FileUtilities.getMountPoint(file), "Trash");
        FileUtilities.createFolders(file2);
        return new TrashFolder(file2);
    }
}

