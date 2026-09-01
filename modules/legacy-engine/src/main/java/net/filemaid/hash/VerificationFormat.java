package net.filemaid.hash;

import java.io.File;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerificationFormat
extends Format {
    private final String hashTypeHint;
    private final Pattern pattern = Pattern.compile("^(\\p{XDigit}+)\\s+(?:[?][^*]+)?[*](.+)$");

    public VerificationFormat() {
        this.hashTypeHint = "";
    }

    public VerificationFormat(String string) {
        this.hashTypeHint = string;
    }

    @Override
    public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        Map.Entry entry = (Map.Entry)object;
        String string = ((File)entry.getKey()).getPath();
        String string2 = (String)entry.getValue();
        return stringBuffer.append(this.format(string, string2));
    }

    public String format(String string, String string2) {
        if (this.hashTypeHint.isEmpty()) {
            return string2 + " *" + string;
        }
        return string2 + " ?" + this.hashTypeHint + "*" + string;
    }

    @Override
    public Map.Entry<File, String> parseObject(String string) throws ParseException {
        Matcher matcher = this.pattern.matcher(string);
        if (!matcher.find()) {
            throw new ParseException("Illegal input pattern", 0);
        }
        return this.entry(matcher.group(2), matcher.group(1));
    }

    @Override
    public Map.Entry<File, String> parseObject(String string, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    protected Map.Entry<File, String> entry(String string, String string2) {
        return new AbstractMap.SimpleImmutableEntry<File, String>(new File(string), string2);
    }
}

