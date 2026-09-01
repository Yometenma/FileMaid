package net.filemaid;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import net.filemaid.Settings;
import net.filemaid.util.SystemProperty;

public enum ApplicationFolder {
    UserHome(Settings.isMacSandbox() || Settings.isSnapSandbox() ? "UserHome" : "user.home", null),
    AppData("application.dir", UserHome.resolve(".filebot")),
    Cache("application.cache", AppData.resolve("cache")),
    Logs("application.logs", AppData.resolve("logs"));

    private final File directory;

    private ApplicationFolder(String string2, File file) {
        this.directory = SystemProperty.get(string2, File::new, file).getAbsoluteFile();
    }

    public File getDirectory() {
        return this.directory;
    }

    public File resolve(String string) {
        return new File(this.directory, string);
    }

    public File[] list(FileFilter fileFilter) throws IOException {
        File[] fileArray = this.directory.listFiles(fileFilter);
        if (fileArray == null) {
            throw new AccessDeniedException(this.directory.getPath());
        }
        return fileArray;
    }

    public boolean mkdir() throws IOException {
        if (this.directory.isDirectory()) {
            return false;
        }
        Files.createDirectories(this.directory.toPath(), new FileAttribute[0]);
        return true;
    }
}

