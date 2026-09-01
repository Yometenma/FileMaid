package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.util.XattrView;

public class PlainFileXattrView
implements XattrView {
    private final Path root;
    private final Path node;

    public PlainFileXattrView(File file, File file2) throws IOException {
        this.root = this.getXattrFolder(file.toPath(), file2.toPath());
        this.node = this.root.resolve(file.getName());
    }

    private Path getXattrFolder(Path path, Path path2) {
        if (path2.isAbsolute()) {
            return path2;
        }
        return path.getParent().resolve(path2);
    }

    @Override
    public List<String> list() throws IOException {
        if (Files.isDirectory(this.node, new LinkOption[0])) {
            return Files.list(this.node).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public ByteBuffer read(String string) throws IOException {
        Path path = this.node.resolve(string);
        if (Files.exists(path, new LinkOption[0])) {
            try {
                return ByteBuffer.wrap(Files.readAllBytes(path));
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.message("Failed to read xattr file", path, throwable));
            }
        }
        return null;
    }

    @Override
    public void write(String string, ByteBuffer byteBuffer) throws IOException {
        if (!Files.isDirectory(this.node, new LinkOption[0])) {
            Files.createDirectories(this.node, new FileAttribute[0]);
            if (Settings.isWindowsApp()) {
                try {
                    Files.setAttribute(this.root, "dos:hidden", true, new LinkOption[0]);
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Set dos:hidden failed", exception));
                }
            }
        }
        try (FileChannel fileChannel = FileChannel.open(this.node.resolve(string), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
            fileChannel.write(byteBuffer);
        }
    }

    @Override
    public void delete(String string) throws IOException {
        Files.deleteIfExists(this.node.resolve(string));
    }

    public String toString() {
        return this.node.toString();
    }
}

