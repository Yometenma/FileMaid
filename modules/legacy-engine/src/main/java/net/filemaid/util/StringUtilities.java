package net.filemaid.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.filemaid.Logging;
import net.filemaid.util.RegularExpressions;

public final class StringUtilities {
    public static List<Integer> matchIntegers(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return Collections.emptyList();
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        Matcher matcher = RegularExpressions.DIGIT.matcher(charSequence);
        while (matcher.find()) {
            try {
                arrayList.add(Integer.parseInt(matcher.group()));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return arrayList;
    }

    public static Integer matchInteger(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return null;
        }
        Matcher matcher = RegularExpressions.DIGIT.matcher(charSequence);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            }
            catch (NumberFormatException numberFormatException) {
                Logging.debug.finest(numberFormatException::toString);
            }
        }
        return null;
    }

    public static String matchLastOccurrence(CharSequence charSequence, Pattern pattern) {
        String string = null;
        Matcher matcher = pattern.matcher(charSequence);
        while (matcher.find()) {
            string = matcher.group();
        }
        return string;
    }

    public static Stream<String> tokenize(CharSequence charSequence) {
        return StringUtilities.tokenize(charSequence, RegularExpressions.SPACE);
    }

    public static Stream<String> tokenize(CharSequence charSequence, Pattern pattern) {
        return pattern.splitAsStream(charSequence).filter(string -> !string.isEmpty());
    }

    public static Stream<String> streamMatches(CharSequence charSequence, Pattern pattern) {
        return StringUtilities.streamMatches(charSequence, pattern, MatchResult::group);
    }

    public static <T> Stream<T> streamMatches(CharSequence charSequence, Pattern pattern, Function<MatchResult, T> function) {
        return StreamSupport.stream(new MatcherSpliterator(pattern.matcher(charSequence)), false).map(function);
    }

    public static Stream<String> streamCapturingGroups(MatchResult matchResult) {
        return IntStream.rangeClosed(1, matchResult.groupCount()).mapToObj(matchResult::group).filter(Objects::nonNull);
    }

    public static long lineCount(CharSequence charSequence) {
        return StringUtilities.streamMatches(charSequence, RegularExpressions.LINEBREAK).count() + 1L;
    }

    public static boolean find(String string, Pattern pattern) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        return pattern.matcher(string).find();
    }

    public static Optional<String> after(String string, Pattern pattern) {
        Matcher matcher = pattern.matcher(string);
        return matcher.find() ? Optional.of(string.substring(matcher.end()).trim()) : Optional.empty();
    }

    public static Optional<String> afterLast(String string, char c) {
        int n = string.lastIndexOf(c);
        return n >= 0 ? Optional.of(string.substring(n + 1).trim()) : Optional.empty();
    }

    public static String asString(Object object) {
        return object == null ? null : object.toString();
    }

    public static String asNonEmptyString(Object object) {
        String string;
        if (object != null && !(string = object.toString().trim()).isEmpty()) {
            return string;
        }
        return null;
    }

    public static boolean isEmpty(Object object) {
        return object == null || object.toString().length() == 0;
    }

    public static boolean nonEmpty(Object object) {
        return object != null && object.toString().length() > 0;
    }

    public static String join(Collection<?> collection, CharSequence charSequence) {
        return StringUtilities.join(collection.stream(), charSequence);
    }

    public static String join(Object[] objectArray, CharSequence charSequence) {
        return StringUtilities.join(Arrays.stream(objectArray), charSequence);
    }

    public static String join(Stream<?> stream, CharSequence charSequence) {
        return StringUtilities.join(stream, charSequence, "", "");
    }

    public static String join(Stream<?> stream, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return stream.map(StringUtilities::asNonEmptyString).filter(Objects::nonNull).collect(Collectors.joining(charSequence, charSequence2, charSequence3));
    }

    public static String replaceAll(CharSequence charSequence, Pattern pattern, BiFunction<Integer, String, String> biFunction) {
        Matcher matcher = pattern.matcher(charSequence);
        if (matcher.find()) {
            StringBuffer stringBuffer = new StringBuffer();
            matcher.appendReplacement(stringBuffer, biFunction.apply(0, matcher.group()));
            int n = 1;
            while (matcher.find()) {
                matcher.appendReplacement(stringBuffer, biFunction.apply(n, matcher.group()));
                ++n;
            }
            return matcher.appendTail(stringBuffer).toString();
        }
        return charSequence.toString();
    }

    public static String replaceInvisibleCharacters(CharSequence charSequence, IntFunction<String> intFunction) {
        return StringUtilities.replaceAll(charSequence, RegularExpressions.NON_PRINTABLE, (n, string) -> string.chars().mapToObj(intFunction).collect(Collectors.joining()));
    }

    public static String printable(CharSequence charSequence) {
        return StringUtilities.replaceInvisibleCharacters(charSequence, n -> String.format(Locale.ROOT, "<0x%02X %s>", n, Character.getName(n)));
    }

    public static boolean isLatin(CharSequence charSequence) {
        return RegularExpressions.LATIN.matcher(charSequence).matches();
    }

    private StringUtilities() {
        throw new UnsupportedOperationException();
    }

    public static class MatcherSpliterator
    extends Spliterators.AbstractSpliterator<MatchResult> {
        private final Matcher m;

        public MatcherSpliterator(Matcher matcher) {
            super(Long.MAX_VALUE, 1296);
            this.m = matcher;
        }

        @Override
        public boolean tryAdvance(Consumer<? super MatchResult> consumer) {
            if (!this.m.find()) {
                return false;
            }
            consumer.accept(this.m);
            return true;
        }
    }
}

