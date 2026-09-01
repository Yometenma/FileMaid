package net.filemaid.cli;

import java.io.File;
import java.util.Arrays;
import net.filemaid.Execute;
import net.filemaid.InvalidInputException;
import net.filemaid.RenameAction;

public class ExecutableRenameAction
implements RenameAction {
    private final File executable;
    private final File directory;

    public ExecutableRenameAction(File file, File file2) {
        this.executable = file;
        this.directory = file2;
    }

    @Override
    public File rename(File file, File file2) throws Exception {
        Execute.system(this.executable.getPath(), Arrays.asList(file.getAbsolutePath(), file2.getPath()), this.getWorkingDirectory(file, file2), null);
        return file2.exists() ? file2 : null;
    }

    public File getWorkingDirectory(File file, File file2) throws Exception {
        return this.directory != null ? this.directory : file.getParentFile();
    }

    public String toString() {
        return this.executable.getName();
    }

    public static ExecutableRenameAction executable(File file, File file2) {
        if (!file.exists()) {
            throw new InvalidInputException("File not found: " + file.getAbsolutePath());
        }
        if (!file.canExecute()) {
            throw new InvalidInputException("File not executable: " + file.getAbsolutePath());
        }
        return new ExecutableRenameAction(file, file2);
    }
}

