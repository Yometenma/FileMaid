package net.filemaid.similarity;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.SimpleDate;

public class DateMatcher {
    public static final DateFilter DEFAULT_SANITY = new DateFilter(1930, 2040);
    private final DatePattern[] patterns;
    private final int depth;

    public DateMatcher(DateFilter dateFilter, Locale ... localeArray) {
        this(1, dateFilter, localeArray);
    }

    public DateMatcher(int n, DateFilter dateFilter, Locale ... localeArray) {
        this.depth = n;
        this.patterns = this.compile(this.patterns(), dateFilter, localeArray);
    }

    protected String[] patterns() {
        return new String[]{"yyyy M d", "d M yyyy", "M d yyyy", "yyyy MMMM d", "yyyy MMM d", "d MMMM yyyy", "d MMMM yy", "d MMM yyyy", "d MMM yy", "MMMM d yyyy", "MMM d yyyy", "d M yy", "yyyyMMdd", "yyMMdd"};
    }

    protected String getPatternGroup(String string, Locale locale) {
        switch (string) {
            case "yyyy": {
                return "(\\d{4})";
            }
            case "yy": {
                return "(\\d{2})";
            }
            case "M": {
                return "(\\d{1,2})";
            }
            case "d": {
                return "(\\d{1,2})(?:th)?";
            }
            case "yyyyMMdd": {
                return "(\\d{8})";
            }
            case "yyMMdd": {
                return "(\\d{6})";
            }
            case "MMMM": {
                return this.getMonthNamePatternGroup(TextStyle.FULL, locale);
            }
            case "MMM": {
                return this.getMonthNamePatternGroup(TextStyle.SHORT, locale);
            }
        }
        throw new IllegalArgumentException(string);
    }

    protected String getMonthNamePatternGroup(TextStyle textStyle, Locale locale) {
        return Arrays.stream(Month.values()).map(month -> month.getDisplayName(textStyle, locale)).map(Pattern::quote).collect(Collectors.joining("|", "(", ")"));
    }

    protected DatePattern[] compile(String[] stringArray, DateFilter dateFilter, Locale ... localeArray) {
        return (DatePattern[])Arrays.stream(stringArray).flatMap(string -> Arrays.stream(localeArray).map(locale -> {
            String string3 = Arrays.stream(string.split(" ")).map(part -> this.getPatternGroup((String)part, (Locale)locale)).collect(Collectors.joining("\\P{Alnum}{1,2}", "(?<!\\p{Alnum})", "(?!\\p{Digit})"));
            return new DateFormatPattern(string3, (String)string, (Locale)locale, dateFilter);
        }).distinct()).toArray(DateFormatPattern[]::new);
    }

    public SimpleDate match(CharSequence charSequence) {
        for (DatePattern datePattern : this.patterns) {
            SimpleDate simpleDate = datePattern.match(charSequence);
            if (simpleDate == null) continue;
            return simpleDate;
        }
        return null;
    }

    public int find(CharSequence charSequence, int n) {
        for (DatePattern datePattern : this.patterns) {
            int n2 = datePattern.find(charSequence, n);
            if (n2 < 0) continue;
            return n2;
        }
        return -1;
    }

    public SimpleDate match(File file) {
        return this.tokenizeTail(file).map(this::match).filter(Objects::nonNull).findFirst().orElse(null);
    }

    protected Stream<String> tokenizeTail(File file2) {
        return FileUtilities.listPathTailReverse(file2, this.depth).stream().map(file -> FileUtilities.getName(file));
    }

    public static class DateFilter
    implements Predicate<LocalDate> {
        public final LocalDate min;
        public final LocalDate max;
        private final int minYear;
        private final int maxYear;

        public DateFilter(LocalDate localDate, LocalDate localDate2) {
            this.min = localDate;
            this.max = localDate2;
            this.minYear = localDate.getYear();
            this.maxYear = localDate2.getYear();
        }

        public DateFilter(int n, int n2) {
            this.min = LocalDate.of(n, Month.JANUARY, 1);
            this.max = LocalDate.of(n2, Month.JANUARY, 1);
            this.minYear = n;
            this.maxYear = n2;
        }

        @Override
        public boolean test(LocalDate localDate) {
            return localDate.isAfter(this.min) && localDate.isBefore(this.max);
        }

        public boolean acceptYear(int n) {
            return this.minYear <= n && n < this.maxYear;
        }

        public boolean acceptDate(int n, int n2, int n3) {
            return this.acceptYear(n) && this.test(LocalDate.of(n, n2, n3));
        }
    }

    public static interface DatePattern {
        public SimpleDate match(CharSequence var1);

        public int find(CharSequence var1, int var2);
    }

    public static class DateFormatPattern
    implements DatePattern {
        public static final String DELIMITER = " ";
        public final Pattern pattern;
        public final DateTimeFormatter format;
        public final DateFilter sanity;

        public DateFormatPattern(String string, String string2, Locale locale, DateFilter dateFilter) {
            this.pattern = Pattern.compile(string, 2);
            this.format = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(string2).toFormatter(locale);
            this.sanity = dateFilter;
        }

        protected SimpleDate process(MatchResult matchResult) {
            try {
                String string = StringUtilities.streamCapturingGroups(matchResult).collect(Collectors.joining(DELIMITER));
                LocalDate localDate = LocalDate.parse(string, this.format);
                if (this.sanity == null || this.sanity.test(localDate)) {
                    return new SimpleDate(localDate);
                }
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
            return null;
        }

        @Override
        public SimpleDate match(CharSequence charSequence) {
            Matcher matcher = this.pattern.matcher(charSequence);
            if (matcher.find()) {
                return this.process(matcher);
            }
            return null;
        }

        @Override
        public int find(CharSequence charSequence, int n) {
            Matcher matcher = this.pattern.matcher(charSequence).region(n, charSequence.length());
            if (matcher.find() && this.process(matcher) != null) {
                return matcher.start();
            }
            return -1;
        }

        public String toString() {
            return this.pattern.pattern();
        }

        public int hashCode() {
            return this.toString().hashCode();
        }

        public boolean equals(Object object) {
            return this.toString().equals(object.toString());
        }
    }
}

