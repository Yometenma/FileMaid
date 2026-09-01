package net.filemaid.hash;

import java.io.File;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.hash.VerificationFormat;

public class SfvFormat
extends VerificationFormat {
    private final Pattern pattern = Pattern.compile("^(.+)\\s+(\\p{XDigit}{8})$");

    @Override
    public String format(String string, String string2) {
        return string + " " + string2;
    }

    @Override
    public Map.Entry<File, String> parseObject(String string) throws ParseException {
        Matcher matcher = this.pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("Illegal input pattern", 0);
        }
        return this.entry(matcher.group(1), matcher.group(2));
    }
}

