package net.filemaid.platform.mac;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.List;
import net.filemaid.platform.mac.XAttrUtil;
import net.filemaid.util.XattrView;

public class MacXattrView
implements XattrView {
    private final String path;
    private final int options;

    public MacXattrView(File file) {
        this(file, 0);
    }

    public MacXattrView(File file, int n) {
        this.path = Normalizer.normalize(file.getAbsolutePath(), Normalizer.Form.NFD);
        this.options = n;
    }

    @Override
    public List<String> list() {
        return XAttrUtil.list(this.path, this.options);
    }

    @Override
    public ByteBuffer read(String string) {
        return XAttrUtil.get(this.path, string, this.options);
    }

    @Override
    public void write(String string, ByteBuffer byteBuffer) {
        XAttrUtil.set(this.path, string, byteBuffer, this.options);
    }

    @Override
    public void delete(String string) {
        XAttrUtil.remove(this.path, string, this.options);
    }
}

