package net.filemaid.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface XattrView {
    public List<String> list() throws IOException;

    public ByteBuffer read(String var1) throws IOException;

    public void write(String var1, ByteBuffer var2) throws IOException;

    public void delete(String var1) throws IOException;
}

