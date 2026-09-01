package net.filemaid.web;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.Logging;

public class SimpleDate
implements Serializable,
Comparable<Object> {
    protected int year;
    protected int month;
    protected int day;
    public static final Pattern DATE_FORMAT = Pattern.compile("(\\d{4})\\D(\\d{1,2})\\D(\\d{1,2})");

    public SimpleDate() {
    }

    public SimpleDate(int n, int n2, int n3) {
        this.year = n;
        this.month = n2;
        this.day = n3;
    }

    public SimpleDate(LocalDate localDate) {
        this.year = localDate.getYear();
        this.month = localDate.getMonthValue();
        this.day = localDate.getDayOfMonth();
    }

    public SimpleDate(int n) {
        this(n, 1, 1);
    }

    public int getYear() {
        return this.year;
    }

    public int getMonth() {
        return this.month;
    }

    public int getDay() {
        return this.day;
    }

    public boolean equals(Object object) {
        if (object instanceof SimpleDate) {
            SimpleDate simpleDate = (SimpleDate)object;
            return this.year == simpleDate.year && this.month == simpleDate.month && this.day == simpleDate.day;
        }
        if (object instanceof CharSequence) {
            return this.toString().equals(object.toString());
        }
        return super.equals(object);
    }

    @Override
    public int compareTo(Object object) {
        SimpleDate simpleDate;
        if (object instanceof SimpleDate) {
            return this.compareTo((SimpleDate)object);
        }
        if (object instanceof ChronoLocalDate) {
            return this.compareTo((ChronoLocalDate)object);
        }
        if (object instanceof CharSequence && (simpleDate = SimpleDate.parse(object.toString())) != null) {
            return this.compareTo(simpleDate);
        }
        throw new IllegalArgumentException("Bad Date: " + object);
    }

    public int compareTo(SimpleDate simpleDate) {
        return this.toLocalDate().compareTo(simpleDate.toLocalDate());
    }

    public int compareTo(ChronoLocalDate chronoLocalDate) {
        return this.toLocalDate().compareTo(chronoLocalDate);
    }

    public int compareTo(Instant instant) {
        return this.toInstant().compareTo(instant);
    }

    public SimpleDate plus(int n) {
        return new SimpleDate(this.toLocalDate().plusDays(n));
    }

    public SimpleDate minus(int n) {
        return new SimpleDate(this.toLocalDate().minusDays(n));
    }

    public long minus(SimpleDate simpleDate) {
        return this.getEpochDay() - simpleDate.getEpochDay();
    }

    public int hashCode() {
        return Objects.hash(this.year, this.month, this.day);
    }

    public SimpleDate clone() {
        return new SimpleDate(this.year, this.month, this.day);
    }

    public String format(String string) {
        return this.format(string, Locale.US);
    }

    public String format(String string, Locale locale) {
        return DateTimeFormatter.ofPattern(string, locale).format(this.toLocalDate());
    }

    public LocalDate toLocalDate() {
        try {
            return LocalDate.of(this.year, this.month, this.day);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Bad Date: " + this, exception);
        }
    }

    public Instant toInstant() {
        return this.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public long getEpochDay() {
        return ChronoUnit.DAYS.between(Instant.EPOCH, this.toInstant());
    }

    public long getTimeStamp() {
        return this.toInstant().toEpochMilli();
    }

    public String toString() {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", this.year, this.month, this.day);
    }

    public static SimpleDate now() {
        return new SimpleDate(LocalDate.now());
    }

    public static SimpleDate from(Instant instant) {
        return instant == null ? null : new SimpleDate(instant.atOffset(ZoneOffset.UTC).toLocalDate());
    }

    public static SimpleDate from(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null : new SimpleDate(zonedDateTime.toLocalDate());
    }

    public static SimpleDate of(int n, int n2, int n3) {
        return new SimpleDate(LocalDate.of(n, n2, n3));
    }

    public static SimpleDate parse(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher matcher = DATE_FORMAT.matcher(string);
        if (matcher.matches()) {
            int n = Integer.parseInt(matcher.group(1));
            int n2 = Integer.parseInt(matcher.group(2));
            int n3 = Integer.parseInt(matcher.group(3));
            try {
                return new SimpleDate(LocalDate.of(n, n2, n3));
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.message("Bad Date", string));
            }
        }
        return null;
    }
}

