package net.filemaid.subtitle;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

class SubtitleTimeFormat
extends DateFormat {
    private final Pattern delimiter = Pattern.compile("[:.]");

    public SubtitleTimeFormat() {
        this.calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
    }

    @Override
    public StringBuffer format(Date date, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        this.calendar.setTime(date);
        stringBuffer.append(String.format(Locale.ROOT, "%02d", this.calendar.get(11)));
        stringBuffer.append(':').append(String.format(Locale.ROOT, "%02d", this.calendar.get(12)));
        stringBuffer.append(':').append(String.format(Locale.ROOT, "%02d", this.calendar.get(13)));
        String string = String.format(Locale.ROOT, "%03d", this.calendar.get(14));
        stringBuffer.append('.').append(string.substring(0, 2));
        return stringBuffer;
    }

    @Override
    public Date parse(String string, ParsePosition parsePosition) {
        String[] stringArray = this.delimiter.split(string, 4);
        this.calendar.clear();
        try {
            this.calendar.set(11, Integer.parseInt(stringArray[0]));
            this.calendar.set(12, Integer.parseInt(stringArray[1]));
            this.calendar.set(13, Integer.parseInt(stringArray[2]));
            this.calendar.set(14, Integer.parseInt(stringArray[3]) * 10);
        }
        catch (Exception exception) {
            parsePosition.setErrorIndex(0);
            return null;
        }
        parsePosition.setIndex(string.length());
        return this.calendar.getTime();
    }
}

