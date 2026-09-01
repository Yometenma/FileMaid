package net.filemaid.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import net.filemaid.util.RegularExpressions;

public class DateTimeUtilities {
    private static final DateTimeFormatter[] UNIVERSAL_DATE_TIME_PATTERN = new DateTimeFormatter[]{DateTimeUtilities.ofPattern("[zzz][' ']uuuu[['-']MM['-']dd][['T'][' '][HH[':']mm[':']ss][X][' '][zzz]"), DateTimeUtilities.ofPattern("[zzz][' ']uuuuMMddHHmmss[X][' '][zzz]"), DateTimeUtilities.ofPattern("[zzz][' ']uuuuMMdd[X][' '][zzz]")};
    private static final int DATE_TIME_MIN_LENGTH = 4;

    public static Instant matchDateTime(String string2, ZoneId zoneId) {
        return RegularExpressions.SLASH.splitAsStream(string2).map(String::trim).map(string -> {
            try {
                return DateTimeUtilities.parseDateTime(string, zoneId);
            }
            catch (Exception exception) {
                return null;
            }
        }).filter(Objects::nonNull).filter(instant -> instant.isAfter(Instant.EPOCH)).min(Instant::compareTo).orElse(null);
    }

    public static Instant parseDateTime(String string, ZoneId zoneId) {
        if (string == null || string.length() < 4) {
            return null;
        }
        try {
            return Instant.parse(string);
        }
        catch (Exception exception) {
            for (DateTimeFormatter dateTimeFormatter : UNIVERSAL_DATE_TIME_PATTERN) {
                try {
                    return DateTimeUtilities.parseDateTime(string, dateTimeFormatter, zoneId);
                }
                catch (Exception exception2) {
                }
            }
            throw new DateTimeParseException(string, string, 0);
        }
    }

    public static Instant parseDateTime(String string, DateTimeFormatter dateTimeFormatter, ZoneId zoneId) {
        TemporalAccessor temporalAccessor = dateTimeFormatter.parseBest(string, ZonedDateTime::from, LocalDateTime::from, LocalDate::from, Year::from);
        if (temporalAccessor instanceof ZonedDateTime) {
            return ((ZonedDateTime)temporalAccessor).toInstant();
        }
        if (temporalAccessor instanceof LocalDateTime) {
            return ((LocalDateTime)temporalAccessor).atZone(zoneId).toInstant();
        }
        if (temporalAccessor instanceof LocalDate) {
            return ((LocalDate)temporalAccessor).atStartOfDay(zoneId).toInstant();
        }
        if (temporalAccessor instanceof Year) {
            return ((Year)temporalAccessor).atDay(1).atStartOfDay(zoneId).toInstant();
        }
        return null;
    }

    public static Instant parseDateTime(String string, String string2, Locale locale, ZoneId zoneId) {
        return DateTimeUtilities.parseDateTime(string, DateTimeFormatter.ofPattern(string2, locale), zoneId);
    }

    public static String format(Object object, String string, Locale locale) {
        return DateTimeFormatter.ofPattern(string, locale).format(DateTimeUtilities.toDateTime(object));
    }

    public static ZonedDateTime toDateTime(Object object) {
        if (object instanceof Date) {
            return ((Date)object).toInstant().atZone(ZoneId.systemDefault());
        }
        if (object instanceof LocalDateTime) {
            return ((LocalDateTime)object).atZone(ZoneId.systemDefault());
        }
        if (object instanceof Instant) {
            return ((Instant)object).atZone(ZoneOffset.UTC);
        }
        if (object instanceof TemporalAmount) {
            return Instant.EPOCH.plus((TemporalAmount)object).atZone(ZoneOffset.UTC);
        }
        return (ZonedDateTime)object;
    }

    public static Date toDate(Instant instant) {
        return new Date(instant.toEpochMilli());
    }

    private static DateTimeFormatter ofPattern(String string) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(string).toFormatter(Locale.US);
    }

    private DateTimeUtilities() {
        throw new UnsupportedOperationException();
    }
}

