package net.filemaid.similarity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeparatorMatcher {
    public static final Pattern DASH_NUMBER = Pattern.compile("[ _.]+[-]+[ _.]+[E]?[P]?[ _.]?[0-9]+", 2);
    public static final Pattern DASH = Pattern.compile("[ _.]+[-]+[ _.]+");
    private final Pattern[] patterns = new Pattern[]{DASH_NUMBER, DASH};

    public String match(String string) {
        int n = this.find(string, 0);
        if (n > 0) {
            return string.substring(0, n).trim();
        }
        return null;
    }

    public int find(String string, int n) {
        for (Pattern pattern : this.patterns) {
            Matcher matcher = pattern.matcher(string).region(n, string.length());
            if (!matcher.find()) continue;
            return matcher.start();
        }
        return -1;
    }
}

