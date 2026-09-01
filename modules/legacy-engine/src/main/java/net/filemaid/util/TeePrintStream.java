package net.filemaid.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class TeePrintStream
extends PrintStream {
    private final PrintStream sink;

    public TeePrintStream(OutputStream outputStream, boolean bl, String string, PrintStream printStream) throws UnsupportedEncodingException {
        super(outputStream, bl, string);
        this.sink = printStream;
    }

    public PrintStream getSink() {
        return this.sink;
    }

    @Override
    public void flush() {
        super.flush();
        this.sink.flush();
    }

    @Override
    public void write(byte[] byArray, int n, int n2) {
        super.write(byArray, n, n2);
        this.sink.write(byArray, n, n2);
    }

    @Override
    public void write(int n) {
        super.write(n);
        this.sink.write(n);
    }

    @Override
    public void write(byte[] byArray) throws IOException {
        super.write(byArray);
        this.sink.write(byArray);
    }

    public static TeePrintStream pipe(OutputStream outputStream, Charset charset, PrintStream printStream) {
        try {
            return new TeePrintStream(outputStream, false, charset.name(), printStream);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}

