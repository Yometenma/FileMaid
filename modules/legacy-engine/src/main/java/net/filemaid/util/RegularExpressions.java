package net.filemaid.util;

import java.util.regex.Pattern;

public class RegularExpressions {
    public static final Pattern DIGIT = Pattern.compile("\\d+");
    public static final Pattern NON_DIGIT = Pattern.compile("\\D+");
    public static final Pattern NON_WORD = Pattern.compile("\\P{Alnum}+", 256);
    public static final Pattern LINEBREAK = Pattern.compile("\\R");
    public static final Pattern NON_PRINTABLE = Pattern.compile("[^\\p{Graph} \t\r\n]", 256);
    public static final Pattern INVISIBLE_CONTROL_CHARACTERS = Pattern.compile("\\p{C}");
    public static final Pattern LATIN = Pattern.compile("[\\p{ASCII}\\p{script=Latin}]+");
    public static final Pattern PIPE = Pattern.compile("|", 16);
    public static final Pattern EQUALS = Pattern.compile("=", 16);
    public static final Pattern TAB = Pattern.compile("\t", 16);
    public static final Pattern SEMICOLON = Pattern.compile(";", 16);
    public static final Pattern COMMA = Pattern.compile("\\s*[,;:]\\s*", 256);
    public static final Pattern RATIO = Pattern.compile("(?<=\\w)[:\u2236\ua789](?=\\w)", 256);
    public static final Pattern COLON = Pattern.compile("\\s*[:\ua789]+\\s*", 256);
    public static final Pattern SLASH = Pattern.compile("\\s*[\\\\/]+\\s*", 256);
    public static final Pattern SPACE = Pattern.compile("\\s+", 256);
    public static final Pattern NEWLINE = Pattern.compile("\\R+", 256);
}

