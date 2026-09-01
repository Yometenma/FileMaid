package net.filemaid;

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.RenameAction;
import net.filemaid.UserFiles;
import net.filemaid.platform.mac.APFS;
import net.filemaid.platform.posix.BTRFS;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.SystemProperty;

public enum StandardRenameAction implements RenameAction
{
    MOVE{

        @Override
        public File rename(File file, File file2) throws Exception {
            try {
                if (Platform.isLinux() && BTRFS.cloneable(file, file2)) {
                    return BTRFS.move(file, file2);
                }
            }
            catch (Throwable throwable) {
                Logging.debug.finest(Logging.cause(CLONE, throwable));
            }
            return FileUtilities.move(file, file2);
        }

        @Override
        public boolean canRename(File file, File file2) {
            return !file.getName().equals(file2.getName()) || !FileUtilities.sameFile(file, file2);
        }
    }
    ,
    COPY{

        @Override
        public File rename(File file, File file2) throws Exception {
            try {
                if (Platform.isLinux() && BTRFS.cloneable(file, file2)) {
                    return BTRFS.clone(file, file2);
                }
                if (Platform.isMac()) {
                    return APFS.copy(file, file2);
                }
            }
            catch (Throwable throwable) {
                Logging.debug.finest(Logging.cause(CLONE, throwable));
            }
            return FileUtilities.copy(file, file2);
        }
    }
    ,
    KEEPLINK{

        @Override
        public File rename(File file, File file2) throws Exception {
            File file3 = MOVE.rename(file, file2);
            SYMLINK.rename(file3, file);
            return file3;
        }
    }
    ,
    SYMLINK{
        private final boolean absolute = SystemProperty.get("net.filemaid.symlink", "absolute"::equals, false);
        private final boolean relative = SystemProperty.get("net.filemaid.symlink", "relative"::equals, false);

        @Override
        public File rename(File file, File file2) throws Exception {
            if (this.absolute) {
                return FileUtilities.symlink(file, file2);
            }
            File file3 = FileUtilities.relativize(file2.getParentFile(), file);
            if (this.relative) {
                return FileUtilities.symlink(file3, file2);
            }
            if (file2.isAbsolute() && file3.getPath().endsWith(file2.getPath())) {
                return FileUtilities.symlink(file, file2);
            }
            if (FileUtilities.sameFileStore(file.getParentFile(), file2.getParentFile())) {
                return FileUtilities.symlink(file3, file2);
            }
            return FileUtilities.symlink(file, file2);
        }
    }
    ,
    HARDLINK{

        @Override
        public File rename(File file, File file2) throws Exception {
            return FileUtilities.hardlink(file, file2);
        }
    }
    ,
    CLONE{

        @Override
        public File rename(File file, File file2) throws Exception {
            if (Platform.isLinux()) {
                return BTRFS.clone(file, file2);
            }
            if (Platform.isMac()) {
                return APFS.clone(file, file2);
            }
            throw new UnsupportedOperationException("clone");
        }
    }
    ,
    DUPLICATE{

        @Override
        public File rename(File file, File file2) throws Exception {
            try {
                if (Platform.isLinux() && BTRFS.cloneable(file, file2)) {
                    return BTRFS.clone(file, file2);
                }
                if (Platform.isMac()) {
                    return APFS.clone(file, file2);
                }
            }
            catch (Throwable throwable) {
                Logging.debug.finest(Logging.cause(CLONE, throwable));
            }
            try {
                return FileUtilities.hardlink(file, file2);
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(HARDLINK, exception));
                return FileUtilities.copy(file, file2);
            }
        }
    }
    ,
    TEST{

        @Override
        public File resolve(File file, File file2) throws Exception {
            return FileUtilities.resolveSibling(file, file2);
        }

        @Override
        public File rename(File file, File file2) throws IOException {
            return null;
        }
    };


    @Override
    public File resolve(File file, File file2) throws Exception {
        file2 = FileUtilities.resolveSibling(file, file2);
        FileUtilities.createFolders(file2.getParentFile());
        return file2;
    }

    @Override
    public boolean canRename(File file, File file2) {
        return !FileUtilities.sameFile(file, file2);
    }

    public String getDisplayName() {
        switch (this) {
            case MOVE: {
                return "Rename";
            }
            case COPY: {
                return "Copy";
            }
            case KEEPLINK: {
                return "Keeplink";
            }
            case SYMLINK: {
                return "Symlink";
            }
            case HARDLINK: {
                return "Hardlink";
            }
            case CLONE: {
                return "Clone";
            }
            case DUPLICATE: {
                return "Duplicate";
            }
        }
        return "Test";
    }

    public String getDisplayVerb() {
        switch (this) {
            case MOVE: {
                return "Moving";
            }
            case COPY: {
                return "Copying";
            }
            case KEEPLINK: 
            case SYMLINK: 
            case HARDLINK: {
                return "Linking";
            }
            case CLONE: {
                return "Cloning";
            }
            case DUPLICATE: {
                return "Duplicating";
            }
        }
        return "Testing";
    }

    public String getDisplayStatus(int n) {
        String string = String.format("%,d %s", n, n == 1 ? "file" : "files");
        switch (this) {
            case MOVE: {
                return string + " renamed.";
            }
            case COPY: {
                return string + " copied.";
            }
            case KEEPLINK: 
            case SYMLINK: 
            case HARDLINK: {
                return string + " linked.";
            }
        }
        return string + " processed.";
    }

    public static List<String> names() {
        return Arrays.stream(StandardRenameAction.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static StandardRenameAction forName(String string) {
        for (StandardRenameAction standardRenameAction : StandardRenameAction.values()) {
            if (!standardRenameAction.name().equalsIgnoreCase(string)) continue;
            return standardRenameAction;
        }
        throw new IllegalArgumentException(string + " not in " + StandardRenameAction.names());
    }

    public static File revert(File file, File file2) throws Exception {
        if (!file.exists()) {
            throw new FileNotFoundException("Cannot revert file: " + file);
        }
        if (file.equals(file2)) {
            if (file.getName().equals(file2.getName())) {
                return file2;
            }
            return FileUtilities.move(file, file2);
        }
        if (!file2.exists()) {
            return MOVE.rename(file, MOVE.resolve(file, file2));
        }
        BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributes basicFileAttributes2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (basicFileAttributes.isSymbolicLink() && !basicFileAttributes2.isSymbolicLink()) {
            UserFiles.trash(file);
            return file2;
        }
        if (basicFileAttributes2.isSymbolicLink() && !basicFileAttributes.isSymbolicLink()) {
            UserFiles.trash(file2);
            return MOVE.rename(file, MOVE.resolve(file, file2));
        }
        if (basicFileAttributes.isRegularFile() && basicFileAttributes2.isRegularFile()) {
            UserFiles.trash(file);
            return file2;
        }
        if (basicFileAttributes.isDirectory() && basicFileAttributes2.isDirectory()) {
            UserFiles.trash(file);
            return file2;
        }
        throw new IllegalStateException("Cannot revert file: " + file + " => " + file2);
    }
}

