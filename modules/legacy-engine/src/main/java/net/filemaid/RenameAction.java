package net.filemaid;

import java.io.File;

public interface RenameAction {
    default public File resolve(File file, File file2) throws Exception {
        return file2;
    }

    default public boolean canRename(File file, File file2) {
        return true;
    }

    public File rename(File var1, File var2) throws Exception;
}

