package net.filemaid.similarity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.util.RegularExpressions;

public final class Normalization {
    public static final Pattern APOSTROPHE = Pattern.compile("['`\u00b4\u2018\u2019\u02bb]+");
    public static final Pattern PUNCTUATION_OR_SPACE = Pattern.compile("[\\p{Punct}\\p{Space}\\p{Cntrl}\\p{Cf}\\p{Sk}\\p{InSpacingModifierLetters}]+", 256);
    public static final Pattern WORD_SEPARATOR_PUNCTUATION = Pattern.compile("[:?._]");
    public static final Pattern TRAILING_PARENTHESIS = Pattern.compile("(?<!^)[(]([^)]*)[)]$");
    public static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[!?._ ]+$");
    public static final Pattern TRAILING_COLON = Pattern.compile("[:\ua789\u02d0](?=\\p{Space}*(?:\\p{Punct}|$))");
    public static final Pattern WORD_SLASH = Pattern.compile("(?<=\\w)[\\\\/]+(?=\\w)", 256);
    public static final Pattern DATE_SLASH = Pattern.compile("(?<=\\d)[\\\\/]+(?=\\d)");
    public static final Pattern NEWLINE_SLASH = Pattern.compile("(?<=\\p{Punct})\\p{Space}*[\\\\/]+");
    public static final Pattern EMBEDDED_CHECKSUM = Pattern.compile("[\\(\\[](\\p{XDigit}{8})[\\]\\)]");
    public static final Pattern EMBEDDED_GUID = Pattern.compile("\\p{Digit}{10,}|\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");
    private static final char[][] QUOTES = new char[][]{{'\'', '`', '\u00b4', '\u2018', '\u2019', '\u02bb'}, {'\"', '\u201c', '\u201d'}};

    public static String normalizeQuotationMarks(String string) {
        char[][] cArray = QUOTES;
        int n = cArray.length;
        for (int i = 0; i < n; ++i) {
            char[] cArray2;
            for (char c : cArray2 = cArray[i]) {
                string = string.replace(c, cArray2[0]);
            }
        }
        return string;
    }

    public static String trimTrailingPunctuation(CharSequence charSequence) {
        return Normalization.normalize(charSequence, TRAILING_PUNCTUATION, "");
    }

    public static String normalizePunctuation(String string) {
        return Normalization.normalizePunctuation(string, "", " ");
    }

    public static String normalizePunctuation(String string, String string2, String string3) {
        string = Normalization.normalize(string, APOSTROPHE, string2);
        string = Normalization.normalize(string, PUNCTUATION_OR_SPACE, string3);
        return string;
    }

    public static String normalizeSpace(String string, String string2) {
        string = Normalization.normalize(string, WORD_SEPARATOR_PUNCTUATION, " ");
        string = Normalization.normalize(string, RegularExpressions.SPACE, string2);
        return string;
    }

    public static String replaceSpace(CharSequence charSequence, String string) {
        return Normalization.normalize(charSequence, RegularExpressions.SPACE, string);
    }

    public static String replaceColon(String string, String string2, String string3, String string4) {
        string = Normalization.normalize(string, TRAILING_COLON, string4);
        string = Normalization.normalize(string, RegularExpressions.RATIO, string3);
        string = Normalization.normalize(string, RegularExpressions.COLON, string2);
        return string;
    }

    public static String replaceSlash(String string, String string2, String string3, String string4, String string5) {
        string = Normalization.normalize(string, NEWLINE_SLASH, string5);
        string = Normalization.normalize(string, DATE_SLASH, string4);
        string = Normalization.normalize(string, WORD_SLASH, string3);
        string = Normalization.normalize(string, RegularExpressions.SLASH, string2);
        return string;
    }

    public static String getEmbeddedChecksum(CharSequence charSequence) {
        Matcher matcher = EMBEDDED_CHECKSUM.matcher(charSequence);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String removeTrailingBrackets(CharSequence charSequence) {
        return Normalization.normalize(charSequence, TRAILING_PARENTHESIS, "");
    }

    private static String normalize(CharSequence charSequence, Pattern pattern, String string) {
        return pattern.matcher(charSequence).replaceAll(Matcher.quoteReplacement(string)).trim();
    }

    public static String truncateText(String string, int n) {
        String string2;
        if (string == null) {
            return null;
        }
        if (string.length() < n) {
            return string.trim();
        }
        String[] stringArray = RegularExpressions.SPACE.split(string);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stringArray.length && stringBuilder.length() + stringArray[i].length() < n; ++i) {
            if (i > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(stringArray[i]);
        }
        if (stringBuilder.length() > 0) {
            return stringBuilder.toString().trim();
        }
        string2 = stringArray[0];
        return string2.substring(0, string2.length() < n ? string2.length() : n).trim();
    }

    private Normalization() {
        throw new UnsupportedOperationException();
    }
}

