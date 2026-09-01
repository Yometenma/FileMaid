package net.filemaid.cli;

import java.io.File;

public interface ConflictAction {
    public File conflict(File var1, File var2) throws Exception;
}

