package net.filemaid.format;

import groovy.lang.Closure;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.InvalidInputException;
import net.filemaid.format.ExpressionFormatFunctions;
import net.filemaid.format.ReverseComparable;
import net.filemaid.format.StructuredFile;
import net.filemaid.media.MediaDetection;
import net.filemaid.similarity.ICU;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.DateTimeUtilities;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.util.StringUtil;

public class ExpressionFormatMethods {
    public static String lower(String string) {
        return string.toLowerCase();
    }

    public static String lower(String string2, String string3) {
        return StringUtilities.replaceAll(string2, Pattern.compile(string3), (n, string) -> ExpressionFormatMethods.lower(string));
    }

    public static String upper(String string) {
        return string.toUpperCase();
    }

    public static String upper(String string2, String string3) {
        return StringUtilities.replaceAll(string2, Pattern.compile(string3), (n, string) -> ExpressionFormatMethods.upper(string));
    }

    public static String pad(String string2, int ... nArray) {
        if (nArray == null || nArray.length == 0) {
            throw new InvalidInputException("1 padding length is required");
        }
        return StringUtilities.replaceAll(string2, RegularExpressions.DIGIT, (n, string) -> ExpressionFormatMethods.pad((CharSequence)string, nArray[n < nArray.length ? n : nArray.length - 1], "0"));
    }

    public static String pad(Number number, int n) {
        return ExpressionFormatMethods.pad((CharSequence)number.toString(), n, "0");
    }

    public static String pad(CharSequence charSequence, int n, CharSequence charSequence2) {
        return StringGroovyMethods.padLeft((CharSequence)charSequence, (Number)n, (CharSequence)charSequence2);
    }

    public static Number round(Number number, int n) {
        return DefaultGroovyMethods.round((BigDecimal)BigDecimal.valueOf(number.doubleValue()), (int)n);
    }

    public static String match(String string, String string2) {
        return ExpressionFormatMethods.match(string, string2, -1);
    }

    public static String match(String string, String string2, int n) {
        Matcher matcher = Pattern.compile(string2, 266).matcher(string);
        if (matcher.find()) {
            return ExpressionFormatMethods.firstCapturingGroup(matcher, n);
        }
        throw new InvalidInputException("Pattern not found: " + string2 + ": " + string);
    }

    public static List<String> matchAll(String string, String string2) {
        return ExpressionFormatMethods.matchAll(string, string2, -1);
    }

    public static List<String> matchAll(String string, String string2, int n) {
        ArrayList<String> arrayList = new ArrayList<String>();
        Matcher matcher = Pattern.compile(string2, 266).matcher(string);
        while (matcher.find()) {
            arrayList.add(ExpressionFormatMethods.firstCapturingGroup(matcher, n));
        }
        if (arrayList.isEmpty()) {
            throw new InvalidInputException("Pattern not found: " + string2 + ": " + string);
        }
        return arrayList;
    }

    private static String firstCapturingGroup(Matcher matcher, int n) {
        int n2 = n < 0 ? (matcher.groupCount() > 0 ? 1 : 0) : n;
        if (n2 == 0) {
            return matcher.group();
        }
        return IntStream.rangeClosed(n2, matcher.groupCount()).mapToObj(matcher::group).filter(Objects::nonNull).map(String::trim).filter(string -> string.length() > 0).findFirst().orElseThrow(() -> new InvalidInputException("Capturing group " + n2 + " not found"));
    }

    public static String replaceAll(String string, String string2) {
        return Pattern.compile(string2).matcher(string).replaceAll("");
    }

    public static String removeAll(String string, String string2) {
        return Pattern.compile(string2, 266).matcher(string).replaceAll("").trim();
    }

    public static String remove(String string3, String string4, String ... stringArray) {
        return Stream.of(stringArray).reduce(string3.replace(string4, ""), (string, string2) -> string.replace((CharSequence)string2, ""));
    }

    public static String removeIllegalCharacters(String string) {
        return FileUtilities.validateFileName(Normalization.normalizeQuotationMarks(string));
    }

    public static String replaceIllegalCharacters(String string) {
        return StringUtil.tr((String)string, (String)"<>:\"/|?*\\", (String)"\ufe64\ufe65\ua789\u201c\u2044\u2f01\uff1f\ufe61\u2216");
    }

    public static String clean(String string) {
        return FileUtilities.validateFileName(MediaDetection.stripReleaseInfo(string, true));
    }

    public static String space(String string, String string2) {
        return Normalization.normalizeSpace(string, string2);
    }

    public static String colon(String string, String string2) {
        return RegularExpressions.COLON.matcher(string).replaceAll(string2);
    }

    public static String colon(String string, String string2, String string3) {
        return RegularExpressions.COLON.matcher(RegularExpressions.RATIO.matcher(string).replaceAll(string3)).replaceAll(string2);
    }

    public static String slash(String string, String string2) {
        return RegularExpressions.SLASH.matcher(string).replaceAll(string2);
    }

    public static String upperInitial(String string) {
        return ExpressionFormatMethods.replaceHeadTail(string, String::toUpperCase, String::toString);
    }

    public static String lowerTrail(String string) {
        return ExpressionFormatMethods.replaceHeadTail(string, String::toString, String::toLowerCase);
    }

    private static String replaceHeadTail(String string, Function<String, String> function, Function<String, String> function2) {
        Matcher matcher = Pattern.compile("\\b((?<!['`\u00b4])\\p{Alnum}|['`\u00b4](?!m|s|re|ll)\\p{Alnum}(?=\\p{Alnum}))(\\p{Alnum}*)\\b", 256).matcher(string);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, function.apply(matcher.group(1)) + function2.apply(matcher.group(2)));
        }
        return matcher.appendTail(stringBuffer).toString();
    }

    public static String sortName(String string) {
        return Pattern.compile("^(The|A|An)\\s+", 2).matcher(string).replaceFirst("");
    }

    public static String sortName(String string, String string2) {
        Matcher matcher = Pattern.compile("^(The|A|An)\\s+(.+)", 2).matcher(string);
        Matcher matcher2 = Pattern.compile("\\s+[(]\\w+[)]$").matcher(string);
        if (matcher2.find()) {
            matcher = matcher.region(0, matcher2.start());
        }
        if (matcher.find()) {
            StringBuffer stringBuffer = new StringBuffer();
            matcher.appendReplacement(stringBuffer, string2);
            matcher.appendTail(stringBuffer);
            return stringBuffer.toString().trim();
        }
        return string;
    }

    public static String sortInitial(String string) {
        return ExpressionFormatMethods.sortName(string).codePoints().mapToObj(n -> {
            if (Character.isDigit(n)) {
                return "0-9";
            }
            if (Character.isLetter(n)) {
                String ch = new StringBuilder(2).appendCodePoint(n).toString();
                return ICU.ASCII.transform(ch).substring(0, 1).toUpperCase();
            }
            return null;
        }).filter(Objects::nonNull).findFirst().get();
    }

    public static String initialName(String string) {
        CharSequence[] charSequenceArray = RegularExpressions.SPACE.split(string);
        for (int i = 0; i < charSequenceArray.length - 1; ++i) {
            charSequenceArray[i] = ((String)charSequenceArray[i]).charAt(0) + ".";
        }
        return String.join((CharSequence)" ", charSequenceArray);
    }

    public static String truncate(String string, int n) {
        return Normalization.truncateText(string, n);
    }

    public static String truncate(String string, int n, String string2) {
        if (n >= string.length()) {
            return string;
        }
        int n2 = 0;
        Matcher matcher = Pattern.compile(string2, 258).matcher(string);
        while (matcher.find() && matcher.start() <= n) {
            n2 = matcher.start();
        }
        return ExpressionFormatMethods.truncate(string, n2);
    }

    public static String before(String string, String string2) {
        Matcher matcher = Pattern.compile(string2, 258).matcher(string);
        return matcher.find() ? string.substring(0, matcher.start()).trim() : string;
    }

    public static String after(String string, String string2) {
        Matcher matcher = Pattern.compile(string2, 258).matcher(string);
        return matcher.find() ? string.substring(matcher.end(), string.length()).trim() : string;
    }

    public static Matcher findMatch(String string, String string2) {
        if (string2 == null || string2.isEmpty()) {
            return null;
        }
        return Pattern.compile(string2, 258).matcher(string);
    }

    public static Matcher findWordMatch(String string, String string2) {
        if (string2 == null || string2.isEmpty()) {
            return null;
        }
        return ExpressionFormatMethods.findMatch(string, "(?<!\\p{Alnum})(?:" + string2 + ")(?!\\p{Alnum})");
    }

    public static List<String> matchBrackets(String string) {
        return ExpressionFormatMethods.matchAll(string, "[\\(\\[\\{]([^\\(\\[\\{\\)\\]\\}]+)[\\)\\]\\}]", 1);
    }

    public static String removeBrackets(String string) {
        return ExpressionFormatMethods.removeAll(string, "[\\(\\[\\{]([^\\(\\[\\{\\)\\]\\}]*)[\\)\\]\\}]");
    }

    public static String replaceTrailingBrackets(String string) {
        return ExpressionFormatMethods.replaceTrailingBrackets(string, "");
    }

    public static String replaceTrailingBrackets(String string, String string2) {
        return Pattern.compile("\\s*[(]([^)]*)[)]$", 256).matcher(string).replaceAll(string2);
    }

    public static String replacePart(String string) {
        return ExpressionFormatMethods.replacePart(string, "");
    }

    public static String replacePart(String string, String string2) {
        String[] stringArray;
        for (String string3 : stringArray = new String[]{"\\s*[(](\\w{1,3})[)]$", "\\W+Part (\\w+)\\W*$"}) {
            Matcher matcher = Pattern.compile(string3, 258).matcher(string);
            if (!matcher.find()) continue;
            return matcher.replaceAll(string2).trim();
        }
        return string;
    }

    public static String acronym(String string) {
        return Pattern.compile("\\s|\\B\\p{Alnum}+", 256).matcher(ExpressionFormatMethods.space(string, " ")).replaceAll("");
    }

    public static String roman(String string) {
        TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
        treeMap.put(10, "X");
        treeMap.put(9, "IX");
        treeMap.put(5, "V");
        treeMap.put(4, "IV");
        treeMap.put(1, "I");
        StringBuffer stringBuffer = new StringBuffer();
        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(string);
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group());
            matcher.appendReplacement(stringBuffer, n >= 1 && n <= 12 ? ExpressionFormatMethods.roman(n, treeMap) : matcher.group());
        }
        return matcher.appendTail(stringBuffer).toString();
    }

    public static String roman(Integer n, TreeMap<Integer, String> treeMap) {
        int n2 = treeMap.floorKey(n);
        if (n == n2) {
            return treeMap.get(n);
        }
        return treeMap.get(n2) + ExpressionFormatMethods.roman(n - n2, treeMap);
    }

    public static String transliterate(String string, String string2) {
        return ICU.getTransliterator(string2).transform(string);
    }

    public static String ascii(String string2) {
        Pattern pattern = Pattern.compile("\\P{ASCII}+");
        if (!pattern.matcher(string2).find()) {
            return string2;
        }
        Pattern pattern2 = Pattern.compile("/|\\\\");
        return pattern2.splitAsStream(string2).map(string -> {
            string = ICU.ASCII.transform(ExpressionFormatMethods.asciiQuotes(string));
            string = pattern.matcher((CharSequence)string).replaceAll(" ");
            string = pattern2.matcher((CharSequence)string).replaceAll(" ");
            return string.trim();
        }).collect(Collectors.joining("/"));
    }

    public static String asciiQuotes(String string) {
        return Normalization.normalizeQuotationMarks(string);
    }

    public static boolean isLatin(String string) {
        return RegularExpressions.LATIN.matcher(string).matches();
    }

    public static List<String> getGraphemeClusters(String string) {
        return ICU.tokenizeGraphemeClusters(string);
    }

    public static String replace(String string, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            for (Object e : DefaultGroovyMethods.flatten(Arrays.asList(entry.getKey()))) {
                Pattern pattern;
                Pattern pattern2 = pattern = e instanceof Pattern ? (Pattern)e : Pattern.compile(Pattern.quote(e.toString()));
                if (entry.getValue() instanceof Closure) {
                    string = StringGroovyMethods.replaceAll((CharSequence)string, (Pattern)pattern, (Closure)((Closure)entry.getValue()));
                    continue;
                }
                string = StringGroovyMethods.replaceAll((CharSequence)string, (Pattern)pattern, (CharSequence)entry.getValue().toString());
            }
        }
        return string;
    }

    public static Collection<String> replace(Collection<?> collection, Map<String, String> map) {
        return collection.stream().map(Objects::toString).map(string -> map.getOrDefault(string, (String)string)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static Object match(String string, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Pattern pattern = entry.getKey() instanceof Pattern ? (Pattern)entry.getKey() : Pattern.compile(entry.getKey().toString(), 266);
            if (!pattern.matcher(string).find()) continue;
            return entry.getValue();
        }
        return null;
    }

    public static Object match(Number number, Map<?, ?> map) {
        return ExpressionFormatMethods.match(number.toString(), map);
    }

    public static Object match(Collection<?> collection, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Pattern pattern = entry.getKey() instanceof Pattern ? (Pattern)entry.getKey() : Pattern.compile(entry.getKey().toString(), 266);
            for (Object obj : collection) {
                if (obj == null || !pattern.matcher(obj.toString()).find()) continue;
                return entry.getValue();
            }
        }
        return null;
    }

    public static String joining(Collection<?> collection, String string) {
        return ExpressionFormatMethods.joining(collection, string, "", "");
    }

    public static String joining(Collection<?> collection, String string2, String string3, String string4) {
        List<String> list = DefaultGroovyMethods.flatten(collection).stream().filter(Objects::nonNull).map(Objects::toString).map(string -> FileUtilities.replacePathSeparators(string, "")).filter(string -> !string.isEmpty()).collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new InvalidInputException("Collection did not yield any values: " + collection);
        }
        return list.stream().collect(Collectors.joining(string2, string3, string4));
    }

    public static String joiningDistinct(Collection<?> collection, String string, Closure<?> ... closureArray) {
        return ExpressionFormatMethods.joiningDistinct(collection, string, "", "", closureArray);
    }

    public static String joiningDistinct(Collection<?> collection, String string, String string2, String string3, Closure<?> ... closureArray) {
        List list = DefaultGroovyMethods.flatten(collection).stream().filter(Objects::nonNull).flatMap(object -> closureArray.length == 0 ? Stream.of(object) : Stream.of(closureArray).map(closure -> closure.call(object))).filter(Objects::nonNull).map(Objects::toString).distinct().collect(Collectors.toList());
        return ExpressionFormatMethods.joining(list, string, string2, string3);
    }

    public static List<?> bounds(Iterable<?> iterable) {
        return Stream.of(DefaultGroovyMethods.min(iterable), DefaultGroovyMethods.max(iterable)).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    public static Object check(Object object, Closure<?> closure) {
        if (DefaultTypeTransformation.castToBoolean((Object)closure.call(object))) {
            return object;
        }
        throw new InvalidInputException("Object failed check: " + object);
    }

    public static StructuredFile derive(StructuredFile structuredFile, Object object, Object ... objectArray) {
        return structuredFile.tag(ExpressionFormatFunctions.component(null, object, objectArray));
    }

    public static StructuredFile deriveFolder(StructuredFile structuredFile, Object object, Object ... objectArray) {
        return structuredFile.collection(ExpressionFormatFunctions.component(null, object, objectArray));
    }

    public static StructuredFile mod(StructuredFile structuredFile, Object object) {
        return ExpressionFormatMethods.derive(structuredFile, object, new Object[0]);
    }

    public static StructuredFile multiply(StructuredFile structuredFile, Object object) {
        return ExpressionFormatMethods.deriveFolder(structuredFile, object, new Object[0]);
    }

    public static StructuredFile power(StructuredFile structuredFile, Object object) {
        return ExpressionFormatMethods.deriveFolder(ExpressionFormatMethods.derive(structuredFile, object, new Object[0]), object, new Object[0]);
    }

    public static StructuredFile bitwiseNegate(StructuredFile structuredFile) {
        return structuredFile.category(null);
    }

    public static StructuredFile leftShift(StructuredFile structuredFile, Object object) {
        return structuredFile.group(ExpressionFormatFunctions.component(null, object, new Object[0]));
    }

    public static StructuredFile rightShift(StructuredFile structuredFile, Object object) {
        return structuredFile.library(ExpressionFormatFunctions.component(null, object, new Object[0]));
    }

    public static StructuredFile xor(StructuredFile structuredFile, Object object) {
        return structuredFile.suffix(null).suffix(ExpressionFormatFunctions.component(null, object, new Object[0]));
    }

    public static File ascii(File file) {
        return new File(ExpressionFormatMethods.ascii(file.getPath()));
    }

    public static long getDiskSpace(File file) {
        return FileUtilities.getDiskSpace(file);
    }

    public static File getRoot(File file) {
        return FileUtilities.listPath(file).get(0);
    }

    public static File getTail(File file) {
        return FileUtilities.getRelativePathTail(file, FileUtilities.listPath(file).size() - 1);
    }

    public static File head(File file, int n) {
        if (n < 1) {
            return null;
        }
        List<File> list = FileUtilities.listPath(file);
        return list.size() < n ? file : list.get(n - 1);
    }

    public static File tail(File file3, int n) {
        if (n < 1) {
            return null;
        }
        List<File> list = FileUtilities.listPath(file3);
        return list.size() < n ? file3 : (File)list.stream().skip(list.size() - n).reduce(null, (file, file2) -> new File((File)file, file2.getName()));
    }

    public static List<File> listPath(File file) {
        return FileUtilities.listPath(file);
    }

    public static List<File> listPath(File file, int n) {
        return FileUtilities.listPath(FileUtilities.getRelativePathTail(file, n));
    }

    public static File getRelativePathTail(File file, int n) {
        return FileUtilities.getRelativePathTail(file, n);
    }

    public static LocalDateTime toDate(Long l) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
    }

    public static LocalDateTime toDate(Long l, String string) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.of(string, ZoneId.SHORT_IDS));
    }

    public static File toFile(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        return new File(string);
    }

    public static File toFile(String string, String string2) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        File file = new File(string);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(string2, string);
    }

    public static Locale toLocale(String string) {
        return Locale.forLanguageTag(string);
    }

    public static String format(Temporal temporal, String string) {
        return ExpressionFormatMethods.format(temporal, string, Locale.US);
    }

    public static String format(Temporal temporal, String string, Locale locale) {
        return DateTimeUtilities.format(temporal, string, locale);
    }

    public static String format(TemporalAmount temporalAmount, String string) {
        return ExpressionFormatMethods.format(temporalAmount, string, Locale.US);
    }

    public static String format(TemporalAmount temporalAmount, String string, Locale locale) {
        return DateTimeUtilities.format(temporalAmount, string, locale);
    }

    public static String format(Date date, String string) {
        return ExpressionFormatMethods.format(date, string, Locale.US);
    }

    public static String format(Date date, String string, Locale locale) {
        return DateTimeUtilities.format(date, string, locale);
    }

    public static ZonedDateTime zone(Temporal temporal, String string) {
        return DateTimeUtilities.toDateTime(temporal).withZoneSameInstant(ZoneId.of(string));
    }

    public static Date parseDate(String string, String string2) {
        return ExpressionFormatMethods.parseDate(string, string2, Locale.US);
    }

    public static Date parseDate(String string, String string2, Locale locale) {
        return DateTimeUtilities.toDate(DateTimeUtilities.parseDateTime(string, string2, locale, ZoneId.systemDefault()));
    }

    public static Date parseDate(String string) {
        return DateTimeUtilities.toDate(DateTimeUtilities.parseDateTime(string, ZoneId.systemDefault()));
    }

    public static int compareTo(Temporal temporal, String string) {
        return DateTimeUtilities.toDateTime(temporal).toInstant().compareTo(DateTimeUtilities.parseDateTime(string, ZoneId.systemDefault()));
    }

    public static File plus(File file, String string) {
        return new File(file.getPath().concat(string));
    }

    public static File div(File file, String string) {
        return new File(file, string);
    }

    public static File div(String string, String string2) {
        return new File(string, string2);
    }

    public static File div(File file, File file2) {
        return new File(file, file2.getPath());
    }

    public static File div(String string, File file) {
        return new File(string, file.getPath());
    }

    public static File div(File file, List<Object> list) {
        for (Object object : list) {
            file = ExpressionFormatMethods.div(file, FileUtilities.replacePathSeparators(object.toString(), ""));
        }
        return file;
    }

    public static File mod(File file, Object object) {
        String string = FileUtilities.getNameWithoutExtension(file.getName());
        String string2 = file.getName().substring(string.length());
        return new File(file.getParentFile(), ExpressionFormatFunctions.component(null, string, object, string2));
    }

    public static String plus(String string, Closure closure) {
        return ExpressionFormatFunctions.concat(null, string, closure, new Object[0]);
    }

    public static ReverseComparable negative(Comparable comparable) {
        return new ReverseComparable(comparable);
    }

    private ExpressionFormatMethods() {
        throw new UnsupportedOperationException();
    }
}

