package net.filemaid.infrastructure.metadata;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Preserves HTTP rate-limit guidance without exposing response bodies or credentials. */
public final class MetadataHttpException extends IllegalStateException {
    private final int statusCode;
    private final Duration retryAfter;

    public MetadataHttpException(String provider, int statusCode, HttpHeaders headers) {
        super(provider + " 请求失败（HTTP " + statusCode + "）");
        this.statusCode = statusCode;
        this.retryAfter = parseRetryAfter(headers.firstValue("Retry-After"));
    }

    public int statusCode() { return statusCode; }
    public Optional<Duration> retryAfter() { return Optional.ofNullable(retryAfter); }

    static Duration parseRetryAfter(Optional<String> value) {
        if (value.isEmpty() || value.get().isBlank()) return null;
        String raw = value.get().trim();
        try { return Duration.ofSeconds(Math.max(0, Long.parseLong(raw))); }
        catch (NumberFormatException ignored) {
            try {
                Duration duration = Duration.between(Instant.now(), ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (RuntimeException invalidDate) { return null; }
        }
    }
}
