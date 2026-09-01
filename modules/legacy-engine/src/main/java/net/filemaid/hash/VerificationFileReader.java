package net.filemaid.hash;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import net.filemaid.Logging;
import net.filemaid.hash.VerificationFormat;
import net.filemaid.util.FileUtilities;

public class VerificationFileReader
implements Iterator<Map.Entry<File, String>>,
Closeable {
    private final Scanner scanner;
    private final VerificationFormat format;
    private Map.Entry<File, String> buffer;
    private int lineNumber = 0;
    private static final String COMMENT = ";";
    private static final String COMMENT_CHARSET = "; charset=";

    public VerificationFileReader(Readable readable, VerificationFormat verificationFormat) {
        this.scanner = new Scanner(readable);
        this.format = verificationFormat;
    }

    @Override
    public boolean hasNext() {
        if (this.buffer == null) {
            this.buffer = this.nextEntry();
        }
        return this.buffer != null;
    }

    @Override
    public Map.Entry<File, String> next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            Map.Entry<File, String> entry = this.buffer;
            return entry;
        }
        finally {
            this.buffer = null;
        }
    }

    protected Map.Entry<File, String> nextEntry() {
        Map.Entry<File, String> object = null;
        while (object == null && this.scanner.hasNextLine()) {
            String string = this.scanner.nextLine().trim();
            if (!this.isComment(string)) {
                try {
                    object = this.format.parseObject(string);
                }
                catch (ParseException parseException) {
                    Logging.debug.warning(Logging.format("Illegal format on line %s: %s", this.lineNumber, string));
                }
            }
            ++this.lineNumber;
        }
        return object;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    protected boolean isComment(String string) {
        return string.isEmpty() || string.startsWith(COMMENT);
    }

    @Override
    public void close() throws IOException {
        this.scanner.close();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private static Charset probeCharsetHeader(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));
        String string = bufferedReader.readLine();
        while (string != null) {
            if (string.startsWith(COMMENT_CHARSET)) {
                return Charset.forName(string.substring(COMMENT_CHARSET.length()));
            }
            if (!string.startsWith(COMMENT)) {
                return null;
            }
            string = bufferedReader.readLine();
        }
        return null;
    }

    public static VerificationFileReader open(File file, VerificationFormat verificationFormat) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file), 65536);
        Charset charset = null;
        bufferedInputStream.mark(65536);
        try {
            charset = VerificationFileReader.probeCharsetHeader(bufferedInputStream);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Invalid charset header", file, exception));
        }
        bufferedInputStream.reset();
        if (charset != null) {
            return new VerificationFileReader(new InputStreamReader((InputStream)bufferedInputStream, charset), verificationFormat);
        }
        return new VerificationFileReader(FileUtilities.newTextFileReader(bufferedInputStream, true, StandardCharsets.UTF_8), verificationFormat);
    }
}

