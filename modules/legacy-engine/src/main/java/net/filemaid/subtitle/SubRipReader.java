package net.filemaid.subtitle;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;

public class SubRipReader
extends SubtitleReader {
    private static final DateTimeFormatter HHmmssSSS = DateTimeFormatter.ofPattern("HH:mm:ss,SSS", Locale.ROOT).withZone(ZoneOffset.UTC);

    public SubRipReader(String string) {
        this(new Scanner(string));
    }

    public SubRipReader(Scanner scanner) {
        super(scanner);
    }

    @Override
    protected SubtitleElement readNext() throws Exception {
        String string;
        String string2 = this.scanner.nextLine();
        if (!this.isNumber(string2)) {
            return null;
        }
        long[] lArray = SubRipReader.parseInterval(this.scanner.nextLine());
        if (lArray == null) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList<String>(2);
        while (this.scanner.hasNextLine() && !(string = this.scanner.nextLine()).isEmpty()) {
            arrayList.add(string);
        }
        return new SubtitleElement(lArray[0], lArray[1], String.join((CharSequence)"\n", arrayList));
    }

    public boolean isNumber(String string) {
        try {
            return Integer.parseInt(string) >= 0;
        }
        catch (Exception exception) {
            return false;
        }
    }

    public static long[] parseInterval(CharSequence charSequence) {
        String[] stringArray = Pattern.compile(" --> ", 16).split(charSequence);
        if (stringArray.length != 2) {
            return null;
        }
        return Arrays.stream(stringArray).map(String::trim).mapToLong(string -> Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(string, HHmmssSSS)).toMillis()).toArray();
    }

    public static CharSequence formatInterval(long l, long l2) {
        StringBuilder stringBuilder = new StringBuilder(32);
        HHmmssSSS.formatTo(LocalTime.MIDNIGHT.plus(l, ChronoUnit.MILLIS), stringBuilder);
        stringBuilder.append(" --> ");
        HHmmssSSS.formatTo(LocalTime.MIDNIGHT.plus(l2, ChronoUnit.MILLIS), stringBuilder);
        return stringBuilder;
    }

    public static List<SubtitleElement> decode(String string) {
        return new SubRipReader(new Scanner(string)).decode();
    }
}

