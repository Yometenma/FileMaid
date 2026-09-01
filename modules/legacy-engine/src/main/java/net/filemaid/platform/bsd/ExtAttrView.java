package net.filemaid.platform.bsd;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.List;
import net.filemaid.platform.bsd.ExtAttrUtil;
import net.filemaid.util.XattrView;

public class ExtAttrView
implements XattrView {
    private final String path;

    public ExtAttrView(File file) {
        this.path = Normalizer.normalize(file.getAbsolutePath(), Normalizer.Form.NFC);
    }

    @Override
    public List<String> list() throws IOException {
        return ExtAttrUtil.list(this.path);
    }

    @Override
    public ByteBuffer read(String string) throws IOException {
        return ExtAttrUtil.get(this.path, string);
    }

    @Override
    public void write(String string, ByteBuffer byteBuffer) throws IOException {
        ExtAttrUtil.set(this.path, string, byteBuffer);
    }

    @Override
    public void delete(String string) throws IOException {
        ExtAttrUtil.delete(this.path, string);
    }
}

