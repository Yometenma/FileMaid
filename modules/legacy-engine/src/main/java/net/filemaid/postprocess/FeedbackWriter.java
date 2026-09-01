package net.filemaid.postprocess;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.Consumer;

class FeedbackWriter
extends Writer {
    private final StringBuilder buffer = new StringBuilder();
    private final Consumer<String> sink;

    private FeedbackWriter(Consumer<String> consumer) {
        this.sink = consumer;
    }

    @Override
    public void write(char[] cArray, int n, int n2) throws IOException {
        StringBuilder stringBuilder = this.buffer;
        synchronized (stringBuilder) {
            this.buffer.append(cArray, n, n2);
        }
    }

    private String drain() {
        StringBuilder stringBuilder = this.buffer;
        synchronized (stringBuilder) {
            String string = this.buffer.toString();
            this.buffer.setLength(0);
            return string;
        }
    }

    @Override
    public void flush() throws IOException {
        String string = this.drain().trim();
        if (string.isEmpty()) {
            return;
        }
        this.sink.accept(string);
    }

    @Override
    public void close() throws IOException {
    }

    public static PrintWriter newPrintWriter(Consumer<String> consumer) {
        return new PrintWriter(new FeedbackWriter(consumer), true);
    }
}

