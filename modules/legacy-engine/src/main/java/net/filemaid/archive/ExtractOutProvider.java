package net.filemaid.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface ExtractOutProvider {
    public OutputStream getStream(File var1) throws IOException;
}

