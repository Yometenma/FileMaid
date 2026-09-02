package net.filemaid.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Bounded in-memory application log feed for the authenticated Web UI. */
@Component
public final class WebLogBuffer extends AppenderBase<ILoggingEvent> {
    private static final int CAPACITY = 2_000;
    private static final Pattern SECRET = Pattern.compile("(?i)(api[_-]?key|password|token|pin|authorization)([=: ]+)([^&\\s,}]+)");
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private final AtomicLong sequence = new AtomicLong();
    private Logger root;

    @PostConstruct
    void install() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(context);
        setName("FILEMAID_WEB_LOG");
        start();
        root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(this);
        LoggerFactory.getLogger(WebLogBuffer.class).info("Web 日志缓冲已启用，最多保留 {} 条记录", CAPACITY);
    }

    @PreDestroy
    void uninstall() {
        if (root != null) root.detachAppender(this);
        stop();
    }

    @Override protected void append(ILoggingEvent event) {
        Entry entry = new Entry(sequence.incrementAndGet(), Instant.ofEpochMilli(event.getTimeStamp()).toString(),
                event.getLevel().toString(), abbreviate(event.getLoggerName()), sanitize(event.getFormattedMessage()), event.getThreadName());
        synchronized (entries) {
            entries.addLast(entry);
            while (entries.size() > CAPACITY) entries.removeFirst();
        }
    }

    public List<Entry> find(long after, String minimumLevel, String query, int requestedLimit) {
        Level threshold = parseLevel(minimumLevel);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int limit = Math.max(1, Math.min(500, requestedLimit));
        List<Entry> result = new ArrayList<>();
        synchronized (entries) {
            for (Entry entry : entries) {
                if (entry.id() <= after || Level.toLevel(entry.level()).isGreaterOrEqual(threshold) == false) continue;
                if (!needle.isEmpty() && !(entry.message() + " " + entry.logger()).toLowerCase(Locale.ROOT).contains(needle)) continue;
                result.add(entry);
            }
        }
        if (result.size() <= limit) return result;
        return after == 0 ? List.copyOf(result.subList(result.size() - limit, result.size()))
                : List.copyOf(result.subList(0, limit));
    }

    private Level parseLevel(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return Level.TRACE;
        return Level.toLevel(value.toUpperCase(Locale.ROOT), Level.INFO);
    }

    private String sanitize(String message) {
        if (message == null) return "";
        String clean = SECRET.matcher(message).replaceAll("$1$2***");
        return clean.length() <= 4_000 ? clean : clean.substring(0, 4_000) + "…";
    }

    private String abbreviate(String logger) {
        if (logger == null) return "";
        int dot = logger.lastIndexOf('.');
        return dot < 0 ? logger : logger.substring(dot + 1);
    }

    public record Entry(long id, String timestamp, String level, String logger, String message, String thread) { }
}
