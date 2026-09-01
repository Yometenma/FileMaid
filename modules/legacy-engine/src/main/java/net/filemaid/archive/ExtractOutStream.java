package net.filemaid.archive;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

class ExtractOutStream
implements ISequentialOutStream,
Closeable {
    private OutputStream out;

    public ExtractOutStream(OutputStream outputStream) {
        this.out = outputStream;
    }

    public int write(byte[] byArray) throws SevenZipException {
        try {
            this.out.write(byArray);
        }
        catch (IOException iOException) {
            throw new SevenZipException((Throwable)iOException);
        }
        return byArray.length;
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
}

