package net.filemaid.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.filemaid.archive.ExtractOutProvider;
import net.filemaid.util.FileUtilities;

class FileMapper
implements ExtractOutProvider {
    private final File outputFolder;

    public FileMapper(File file) {
        this.outputFolder = file;
    }

    public File getOutputFolder() {
        return this.outputFolder;
    }

    public File getOutputFile(File file) {
        return new File(this.outputFolder, file.getPath());
    }

    @Override
    public OutputStream getStream(File file) throws IOException {
        File file2 = this.getOutputFile(file);
        File file3 = file2.getParentFile();
        FileUtilities.createFolders(file3);
        return new FileOutputStream(file2);
    }
}

