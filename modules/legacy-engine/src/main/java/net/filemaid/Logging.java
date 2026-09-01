package net.filemaid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.EscapeCode;
import net.filemaid.GroovyEngine;
import net.filemaid.Settings;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.SystemProperty;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.HttpNetworkError;
import net.filemaid.web.HttpServerError;

public final class Logging {
    private static final SystemProperty<Level> debugLevel = SystemProperty.of("net.filemaid.logging.debug", Level::parse, Level.WARNING);
    private static final SystemProperty<Pattern> anonymizePattern = SystemProperty.of("net.filemaid.logging.anonymize", Pattern::compile, null);
    private static final SystemProperty<Boolean> sanitize = SystemProperty.of("net.filemaid.logging.sanitize", Boolean::parseBoolean, true);
    private static final SystemProperty<Boolean> timeStamp = SystemProperty.of("net.filemaid.logging.time", Boolean::parseBoolean, false);
    private static final SystemProperty<DateTimeFormatter> timeStampFormat = SystemProperty.of("net.filemaid.logging.time.format", DateTimeFormatter::ofPattern, DateTimeFormatter.ofPattern("'['uuuu-MM-dd HH:mm:ss.SSS']' "));
    public static final Logger log = Logging.createConsoleLogger("net.filemaid.console", System.out, Level.ALL);
    public static final Logger debug = Logging.createConsoleLogger("net.filemaid.debug", System.err, debugLevel.get());
    public static final Consumer<Integer> severe = Runtime.getRuntime()::halt;
    public static final Consumer<Object> stdout = System.out::println;

    public static Logger createConsoleLogger(String string, PrintStream printStream, Level level) {
        Logger logger = Logger.getLogger(string);
        logger.setUseParentHandlers(false);
        logger.setLevel(level);
        logger.addHandler(Logging.createConsoleHandler(printStream, level));
        return logger;
    }

    public static StreamHandler createSimpleFileHandler(File file, Level level) throws IOException {
        StreamHandler streamHandler = new StreamHandler(new FileOutputStream(file, true), new SimpleFormatter());
        streamHandler.setEncoding("UTF-8");
        streamHandler.setLevel(level);
        return streamHandler;
    }

    public static StreamHandler createLogFileHandler(File file, boolean bl, Level level) throws IOException {
        FileUtilities.createFile(file);
        try {
            FileUtilities.checkDiskSpace(file);
        }
        catch (Exception exception) {
            debug.warning(Logging.cause(exception));
        }
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        if (bl) {
            try {
                debug.config(Logging.format("Locking %s", file));
                fileChannel.lock();
            }
            catch (Exception exception) {
                throw new IOException("Failed to acquire lock: " + file, exception);
            }
        }
        StreamHandler streamHandler = new StreamHandler(Channels.newOutputStream(fileChannel), new ConsoleFormatter(anonymizePattern.get(), sanitize.get(), false, Logging.createTimeStampFormat()));
        streamHandler.setEncoding("UTF-8");
        streamHandler.setLevel(level);
        return streamHandler;
    }

    public static ConsoleHandler createConsoleHandler(PrintStream printStream, Level level) {
        ConsoleHandler consoleHandler = new ConsoleHandler(printStream);
        consoleHandler.setLevel(level);
        consoleHandler.setFormatter(new ConsoleFormatter(anonymizePattern.get(), sanitize.get(), EscapeCode.COLOR_TERMINAL, Logging.createTimeStampFormat()));
        return consoleHandler;
    }

    private static DateTimeFormatter createTimeStampFormat() {
        return timeStamp.get() != false ? timeStampFormat.get().withLocale(Locale.US).withZone(ZoneId.systemDefault()) : null;
    }

    public static Message format(String string, Object ... objectArray) {
        return Message.format(string, objectArray);
    }

    public static Message formatSingleLine(String string, Object ... objectArray) {
        return Message.formatSingleLine(string, objectArray);
    }

    public static Message cause(Throwable throwable) {
        return Message.cause(throwable, new Object[0]);
    }

    public static Message cause(Object object, Throwable throwable) {
        return Message.cause(throwable, object);
    }

    public static Message cause(Object object, Object object2, Throwable throwable) {
        return Message.cause(throwable, object, object2);
    }

    public static Message cause(Object object, Object object2, Object object3, Throwable throwable) {
        return Message.cause(throwable, object, object2, object3);
    }

    public static Message message(Object ... objectArray) {
        return Message.join(objectArray);
    }

    public static String getRootCauseMessage(Throwable throwable) {
        if ((throwable = Logging.getRootCause(throwable)) instanceof Error) {
            if (throwable instanceof NoClassDefFoundError) {
                return "An application component is damaged or missing: " + throwable.getMessage();
            }
            return throwable.toString();
        }
        String string = throwable.getMessage();
        if (string == null || string.isEmpty()) {
            string = Logging.defaultMessage(throwable);
        }
        if (throwable instanceof NullPointerException) {
            return "Undefined Variable: " + string;
        }
        if (throwable instanceof HttpClientError) {
            HttpClientError httpClientError = (HttpClientError)throwable;
            if (httpClientError.isErrorResponse()) {
                if (httpClientError.isRateLimited()) {
                    return "Network Connection Error: " + string + "\n" + httpClientError.getResponseContent() + "\nYour current VPN server / public IP has been temporarily banned due to abuse. Please use a different VPN server / public IP.";
                }
                return "Server Error: " + string + "\n" + httpClientError.getResponseContent();
            }
            return "Server Error: " + string;
        }
        if (throwable instanceof HttpServerError) {
            return "Server Error: " + string;
        }
        if (throwable instanceof HttpNetworkError) {
            return "Network Error: " + string;
        }
        if (throwable instanceof UnknownHostException) {
            return "Network Connection Error: DNS server failed to resolve IP for host name: " + string + "\nPlease use CloudFlare DNS 1.1.1.1 or Google DNS 8.8.8.8 instead of the default DNS server provided by your ISP.";
        }
        if (throwable instanceof SocketException || throwable instanceof SocketTimeoutException) {
            return "Network Connection Error: " + string;
        }
        if (throwable instanceof AccessDeniedException) {
            return Logging.checkPermissions((AccessDeniedException)throwable);
        }
        if (throwable instanceof FileNotFoundException || throwable instanceof NoSuchFileException || throwable instanceof InvalidPathException) {
            return "No Such File: " + string;
        }
        if (throwable instanceof FileAlreadyExistsException) {
            return "File already exists: " + string;
        }
        if (throwable instanceof FileSystemException) {
            return Logging.checkFileSystemOperation((FileSystemException)throwable);
        }
        return string;
    }

    private static String defaultMessage(Throwable throwable) {
        StackTraceElement[] stackTraceElementArray = throwable.getStackTrace();
        if (stackTraceElementArray != null) {
            return Stream.of(stackTraceElementArray).filter(stackTraceElement -> stackTraceElement.getClassName().startsWith("net.filemaid.") && !stackTraceElement.getClassName().startsWith("net.filemaid.Cache") && !stackTraceElement.getMethodName().contains("$")).map(stackTraceElement -> stackTraceElement.getMethodName()).distinct().limit(10L).collect(Collectors.joining(" < ", throwable + " at ", ""));
        }
        return throwable + " at (Unknown Location)";
    }

    private static String checkPermissions(AccessDeniedException accessDeniedException) {
        String string = "Access Denied: " + accessDeniedException.getMessage();
        if (FileUtilities.UNIX) {
            String string2 = Stream.of(accessDeniedException.getFile(), accessDeniedException.getOtherFile()).filter(Objects::nonNull).map(File::new).map(file -> Logging.stat(file)).collect(Collectors.joining(" -> "));
            if (string2.length() > 0) {
                string = string + " (" + string2 + ")";
            }
            if (Settings.isSnapSandbox()) {
                string = string + " [Snap Confinement: Strict]";
            }
        }
        return string;
    }

    private static String checkFileSystemOperation(FileSystemException fileSystemException) {
        String string = fileSystemException.getReason();
        if (fileSystemException.getFile() != null) {
            File file = new File(fileSystemException.getFile());
            if (fileSystemException.getOtherFile() != null) {
                File file2 = new File(fileSystemException.getOtherFile());
                if ("Invalid cross-device link".equals(string) || "Cross-device link".equals(string)) {
                    try {
                        FileStore fileStore = FileUtilities.getFileStore(file);
                        FileStore fileStore2 = FileUtilities.getFileStore(file2);
                        return "Invalid cross-device link from " + fileStore2 + " to " + fileStore;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                return string + ": " + Logging.stat(file) + " -> " + Logging.stat(file2);
            }
            return string + ": " + Logging.stat(file);
        }
        return string;
    }

    private static String stat(File file) {
        if (FileUtilities.UNIX) {
            try {
                for (File file2 = file; file2 != null; file2 = file2.getParentFile()) {
                    if (!file2.exists()) continue;
                    return FileUtilities.getPermissions(file2) + " " + FileUtilities.getUID(file2) + ":" + FileUtilities.getGID(file2) + " " + Logging.getFileName(file2);
                }
            }
            catch (Exception exception) {
                return Logging.getFileName(file) + " [" + exception + "]";
            }
        }
        return Logging.getFileName(file);
    }

    private static String getFileName(File file) {
        String string = file.getName();
        if (string.length() > 0) {
            return string;
        }
        return file.toString();
    }

    public static Throwable getRootCause(Throwable throwable) {
        while (throwable.getCause() != null && (throwable instanceof RuntimeException || throwable instanceof ExecutionException || throwable instanceof InvocationTargetException)) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    public static <T extends Throwable> T findCause(Throwable throwable, Class<T> clazz) {
        while (throwable != null) {
            if (clazz.isInstance(throwable)) {
                return (T)((Throwable)clazz.cast(throwable));
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    public static boolean help() {
        return log.isLoggable(Level.ALL);
    }

    public static void help(String string) {
        log.log(Level.ALL, string);
    }

    public static void help(Supplier<String> supplier) {
        log.log(Level.ALL, supplier);
    }

    public static void trace(Throwable throwable) {
        debug.log(Logging.severity(throwable), throwable, Logging.cause(throwable));
    }

    public static void trace(Object object, Throwable throwable) {
        debug.log(Logging.severity(throwable), throwable, Logging.cause(object, throwable));
    }

    public static void error(Object ... objectArray) {
        if (debug == null) {
            System.err.println(Logging.message(objectArray));
        } else {
            debug.log(Level.SEVERE, Logging.message(objectArray));
        }
    }

    private static Level severity(Throwable throwable) {
        return Logging.isCancellation(throwable) ? Level.ALL : (throwable instanceof Error ? Level.SEVERE : Level.WARNING);
    }

    public static boolean isCancellation(Throwable throwable) {
        for (Throwable throwable2 = throwable; throwable2 != null; throwable2 = throwable2.getCause()) {
            if (!(throwable2 instanceof InterruptedException) && !(throwable2 instanceof CancellationException)) continue;
            return true;
        }
        return false;
    }

    public static void flushLog() {
        for (Handler handler : log.getHandlers()) {
            handler.flush();
        }
        for (Handler handler : debug.getHandlers()) {
            handler.flush();
        }
        Runtime.getRuntime().gc();
    }

    public static class ConsoleHandler
    extends Handler {
        private final PrintStream out;

        public ConsoleHandler(PrintStream printStream) {
            this.out = printStream;
        }

        @Override
        public void publish(LogRecord logRecord) {
            this.out.print(this.getFormatter().format(logRecord));
            this.out.flush();
        }

        @Override
        public void flush() {
            this.out.flush();
        }

        @Override
        public void close() {
        }
    }

    public static class Message
    implements Supplier<String> {
        private final Supplier<String> value;

        public Message(Supplier<String> supplier) {
            this.value = supplier;
        }

        @Override
        public String get() {
            return this.value.get();
        }

        public String toString() {
            return this.value.get();
        }

        public static Message format(String string, Object ... objectArray) {
            return Message.lazy(() -> String.format(Locale.ROOT, string, objectArray));
        }

        public static Message formatSingleLine(String string, Object ... objectArray) {
            return Message.lazy(() -> String.format(Locale.ROOT, string, (Object[])Stream.of(objectArray).map(Objects::toString).map(s -> RegularExpressions.NEWLINE.matcher((CharSequence)s).replaceAll(" \u21b2 ")).map(String::trim).toArray(Object[]::new)));
        }

        public static Message join(Object ... objectArray) {
            return Message.lazy(() -> Stream.of(objectArray).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(": ")));
        }

        public static Message cause(Throwable throwable, Object ... objectArray) {
            if (debug.isLoggable(Level.ALL)) {
                throwable.printStackTrace();
            }
            return Message.lazy(() -> Stream.concat(Stream.of(objectArray), Stream.of(Logging.getRootCauseMessage(throwable))).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(": ")));
        }

        public static Message lazy(Supplier<String> supplier) {
            return new Message(supplier);
        }
    }

    public static class ConsoleFormatter
    extends Formatter {
        private final Pattern anonymize;
        private final boolean sanitize;
        private final boolean colorize;
        private final DateTimeFormatter dateTimeFormatter;

        public ConsoleFormatter(Pattern pattern, boolean bl, boolean bl2, DateTimeFormatter dateTimeFormatter) {
            this.anonymize = pattern;
            this.sanitize = bl;
            this.colorize = bl2;
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public String format(LogRecord logRecord) {
            Object object;
            EscapeCode escapeCode = this.getColor(logRecord.getLevel().intValue());
            StringWriter stringWriter = new StringWriter(80);
            if (this.dateTimeFormatter != null) {
                this.dateTimeFormatter.formatTo(logRecord.getInstant(), stringWriter);
            }
            stringWriter.append(escapeCode.begin);
            String string = logRecord.getMessage();
            if (string != null && this.anonymize != null) {
                object = this.anonymize.matcher(string);
                while (((Matcher)object).find()) {
                    ((Matcher)object).appendReplacement(stringWriter.getBuffer(), "");
                }
                ((Matcher)object).appendTail(stringWriter.getBuffer());
            } else {
                stringWriter.append(string);
            }
            object = logRecord.getThrown();
            if (object != null) {
                stringWriter.append(System.lineSeparator());
                if (this.sanitize) {
                    try {
                        object = GroovyEngine.sanitizeStackTrace((Throwable)object);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                ((Throwable)object).printStackTrace(new PrintWriter(stringWriter));
            }
            return stringWriter.append(escapeCode.end).append(System.lineSeparator()).toString();
        }

        public EscapeCode getColor(int n) {
            if (this.colorize) {
                if (n < Level.FINE.intValue()) {
                    return n > Level.ALL.intValue() ? EscapeCode.ROYAL_BLUE : EscapeCode.ORANGE_ONE;
                }
                if (n < Level.INFO.intValue()) {
                    return EscapeCode.LIME_GREEN;
                }
                if (n < Level.WARNING.intValue()) {
                    return EscapeCode.NONE;
                }
                if (n < Level.SEVERE.intValue()) {
                    return EscapeCode.ORANGE_RED;
                }
                return EscapeCode.CHERRY_RED;
            }
            return EscapeCode.NONE;
        }
    }
}

