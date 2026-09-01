package net.filemaid.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PseudoConsole {
    private static PseudoConsole STDIO;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public static synchronized PseudoConsole getSystemConsole() {
        if (STDIO == null) {
            STDIO = new PseudoConsole(System.in, System.out, StandardCharsets.UTF_8);
        }
        return STDIO;
    }

    public PseudoConsole(InputStream inputStream, PrintStream printStream, Charset charset) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        this.writer = new PrintWriter(new OutputStreamWriter((OutputStream)printStream, charset), true);
    }

    public PrintWriter writer() {
        return this.writer;
    }

    public Reader reader() {
        return this.reader;
    }

    public PrintWriter format(String string, Object ... objectArray) {
        return this.writer.format(string, objectArray);
    }

    public PrintWriter printf(String string, Object ... objectArray) {
        return this.writer.printf(string, objectArray);
    }

    public String readLine(String string, Object ... objectArray) {
        throw new UnsupportedOperationException();
    }

    public String readLine() throws IOException {
        return this.reader.readLine();
    }

    public char[] readPassword(String string, Object ... objectArray) {
        throw new UnsupportedOperationException();
    }

    public char[] readPassword() {
        throw new UnsupportedOperationException();
    }

    public void flush() {
        this.writer.flush();
    }
}

