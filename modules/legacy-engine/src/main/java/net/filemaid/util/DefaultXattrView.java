package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import net.filemaid.Logging;
import net.filemaid.util.XattrView;

public class DefaultXattrView
implements XattrView {
    private final UserDefinedFileAttributeView attr;

    public DefaultXattrView(File file) throws IOException {
        this.attr = Files.getFileAttributeView(file.toPath(), UserDefinedFileAttributeView.class, new LinkOption[0]);
        if (this.attr == null) {
            throw new IOException("UserDefinedFileAttributeView is not supported");
        }
    }

    @Override
    public List<String> list() throws IOException {
        return this.attr.list();
    }

    @Override
    public ByteBuffer read(String string) throws IOException {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(this.attr.size(string));
            this.attr.read(string, byteBuffer);
            byteBuffer.flip();
            return byteBuffer;
        }
        catch (IOException iOException) {
            return null;
        }
    }

    @Override
    public void write(String string, ByteBuffer byteBuffer) throws IOException {
        this.attr.write(string, byteBuffer);
    }

    @Override
    public void delete(String string) throws IOException {
        this.attr.delete(string);
    }

    public static class PreserveLastModified
    extends DefaultXattrView {
        private final File path;

        public PreserveLastModified(File file) throws IOException {
            super(file);
            this.path = file;
        }

        @Override
        public void write(String string, ByteBuffer byteBuffer) throws IOException {
            FileTime fileTime = this.getLastModified();
            try {
                super.write(string, byteBuffer);
            }
            finally {
                this.setLastModified(fileTime);
            }
        }

        private FileTime getLastModified() {
            try {
                return Files.getLastModifiedTime(this.path.toPath(), new LinkOption[0]);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to record Last-Modified time", this.path, exception));
                return null;
            }
        }

        private void setLastModified(FileTime fileTime) {
            if (fileTime == null) {
                return;
            }
            try {
                Files.setLastModifiedTime(this.path.toPath(), fileTime);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to restore Last-Modified time", this.path, exception));
            }
        }
    }
}

