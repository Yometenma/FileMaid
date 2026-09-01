package net.filemaid.subtitle;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;
import net.filemaid.subtitle.SubtitleTimeFormat;
import net.filemaid.util.StringUtilities;

public class SubViewerReader
extends SubtitleReader {
    private final DateFormat timeFormat = new SubtitleTimeFormat();
    private final Pattern newline = Pattern.compile(Pattern.quote("[br]"), 2);

    public SubViewerReader(Scanner scanner) {
        super(scanner);
    }

    @Override
    protected SubtitleElement readNext() throws Exception {
        String[] stringArray = this.scanner.nextLine().split(",", 2);
        if (stringArray.length < 2 || stringArray[0].startsWith("[")) {
            return null;
        }
        try {
            long l = this.timeFormat.parse(stringArray[0]).getTime();
            long l2 = this.timeFormat.parse(stringArray[1]).getTime();
            Object[] objectArray = this.newline.split(this.scanner.nextLine());
            return new SubtitleElement(l, l2, StringUtilities.join(objectArray, (CharSequence)"\n"));
        }
        catch (ParseException parseException) {
            return null;
        }
        catch (InputMismatchException inputMismatchException) {
            return null;
        }
    }

    public static List<SubtitleElement> decode(String string) {
        return new SubViewerReader(new Scanner(string)).decode();
    }
}

