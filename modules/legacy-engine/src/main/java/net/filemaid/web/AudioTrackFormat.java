package net.filemaid.web;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import net.filemaid.web.AudioTrack;

public class AudioTrackFormat
extends Format {
    public static final AudioTrackFormat DEFAULT = new AudioTrackFormat();

    @Override
    public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        AudioTrack audioTrack = (AudioTrack)object;
        stringBuffer.append(audioTrack);
        return stringBuffer;
    }

    @Override
    public AudioTrack parseObject(String string, ParsePosition parsePosition) {
        String[] stringArray = string.split(" - ", 2);
        if (stringArray.length == 2) {
            parsePosition.setIndex(string.length());
            return new AudioTrack(stringArray[0].trim(), stringArray[1].trim(), "VA", null);
        }
        parsePosition.setErrorIndex(0);
        return null;
    }
}

