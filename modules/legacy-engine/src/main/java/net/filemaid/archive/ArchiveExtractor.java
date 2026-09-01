package net.filemaid.archive;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import net.filemaid.vfs.FileInfo;

public interface ArchiveExtractor {
    public List<FileInfo> listFiles() throws IOException;

    public void extract(File var1) throws IOException;

    public void extract(File var1, FileFilter var2) throws IOException;
}

