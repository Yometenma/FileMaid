package net.filemaid.postprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.postprocess.Feedback;

public class FeedbackSpooler
implements Feedback {
    private final List<Record> records = new ArrayList<Record>();
    private final Consumer<String> message;
    private final BiConsumer<Integer, Integer> progress;
    private final Supplier<Boolean> cancelled;
    private final Function<Record, String> formatter;

    public FeedbackSpooler(Consumer<String> consumer, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) {
        this(consumer, biConsumer, supplier, Record::message);
    }

    public FeedbackSpooler(Consumer<String> consumer, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier, Function<Record, String> function) {
        this.message = consumer;
        this.progress = biConsumer;
        this.cancelled = supplier;
        this.formatter = function;
    }

    @Override
    public void record(Feedback.Type type, Object object, Object object2) {
        Record record = new Record(type, object, object2);
        Logging.debug.finest(Logging.message(record));
        if (object == null) {
            return;
        }
        List<Record> list = this.records;
        synchronized (list) {
            this.records.add(record);
        }
        if (type != Feedback.Type.TRACE) {
            this.message.accept(this.formatter.apply(record));
        }
    }

    public Stream<String> messages() {
        List<Record> list = this.records;
        synchronized (list) {
            return this.records.stream().map(Record::message).filter(Objects::nonNull);
        }
    }

    public Stream<File> files() {
        List<Record> list = this.records;
        synchronized (list) {
            return this.records.stream().map(Record::file).filter(Objects::nonNull);
        }
    }

    public Stream<String> warnings() {
        List<Record> list = this.records;
        synchronized (list) {
            return this.records.stream().map(Record::error).filter(Objects::nonNull);
        }
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled.get();
    }

    @Override
    public void progress(Integer n, Integer n2) {
        this.progress.accept(n, n2);
    }

    public static class Record {
        public final Feedback.Type type;
        public final Object message;
        public final Object path;

        public Record(Feedback.Type type, Object object, Object object2) {
            this.type = type;
            this.message = object;
            this.path = object2;
        }

        public String message() {
            return this.message.toString();
        }

        public String info() {
            if (this.path != null) {
                return this.message + ": " + this.path;
            }
            return this.message.toString();
        }

        public File file() {
            if (this.type == Feedback.Type.FILE && this.path instanceof File) {
                return (File)this.path;
            }
            return null;
        }

        public String error() {
            if (this.type == Feedback.Type.WARNING) {
                return this.message + " (" + this.path + ")";
            }
            return null;
        }

        public String toString() {
            return "[" + this.type + "] " + this.message + " (" + this.path + ")";
        }
    }
}

