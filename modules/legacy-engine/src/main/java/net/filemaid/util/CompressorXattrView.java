package net.filemaid.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.XZ;
import net.filemaid.util.XattrView;

public class CompressorXattrView
implements XattrView {
    private final XattrView fs;
    private final int limit;
    public static final int XATTR_XZ_LIMIT = SystemProperty.optional("net.filemaid.xattr.xz.limit", Integer::parseInt).orElse(2048);

    private CompressorXattrView(XattrView xattrView, int n) {
        this.fs = xattrView;
        this.limit = n;
    }

    @Override
    public List<String> list() throws IOException {
        return this.fs.list();
    }

    @Override
    public void delete(String string) throws IOException {
        this.fs.delete(string);
    }

    @Override
    public ByteBuffer read(String string) throws IOException {
        ByteBuffer byteBuffer = this.fs.read(string);
        if (byteBuffer == null || !XZ.isXZ(byteBuffer)) {
            return byteBuffer;
        }
        return XZ.unxz(byteBuffer);
    }

    @Override
    public void write(String string, ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer.remaining() > this.limit && !XZ.isXZ(byteBuffer)) {
            byteBuffer = XZ.xz(byteBuffer);
        }
        this.fs.write(string, byteBuffer);
    }

    public static XattrView compress(XattrView xattrView, boolean bl) {
        if (xattrView instanceof CompressorXattrView) {
            xattrView = ((CompressorXattrView)xattrView).fs;
        }
        return new CompressorXattrView(xattrView, bl ? XATTR_XZ_LIMIT : 32);
    }
}

