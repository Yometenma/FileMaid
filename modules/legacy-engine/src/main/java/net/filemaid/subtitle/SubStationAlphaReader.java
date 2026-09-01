package net.filemaid.subtitle;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;
import net.filemaid.subtitle.SubtitleTimeFormat;

public class SubStationAlphaReader
extends SubtitleReader {
    private final DateFormat timeFormat = new SubtitleTimeFormat();
    private final Pattern newline = Pattern.compile(Pattern.quote("\\n"), 2);
    private final Pattern tags = Pattern.compile("[{]\\\\[^}]+[}]");
    private final Pattern drawingTags = Pattern.compile("\\\\[p][0-4]");
    private String[] format;
    private int formatIndexStart;
    private int formatIndexEnd;
    private int formatIndexText;

    public SubStationAlphaReader(Scanner scanner) {
        super(scanner);
    }

    private void readFormat() throws Exception {
        String string = this.scanner.nextLine();
        String[] stringArray = string.split(":", 2);
        if (!stringArray[0].equals("Format")) {
            throw new InputMismatchException("Illegal format header: " + string);
        }
        this.format = stringArray[1].split(",");
        for (int i = 0; i < this.format.length; ++i) {
            this.format[i] = this.format[i].trim().toLowerCase(Locale.ROOT);
        }
        List<String> list = Arrays.asList(this.format);
        this.formatIndexStart = list.indexOf("start");
        this.formatIndexEnd = list.indexOf("end");
        this.formatIndexText = list.indexOf("text");
    }

    @Override
    public SubtitleElement readNext() throws Exception {
        String[] stringArray;
        if (this.format == null) {
            boolean bl = false;
            while (!bl && this.scanner.hasNextLine()) {
                bl = this.scanner.nextLine().equals("[Events]");
            }
            if (!bl) {
                return null;
            }
            this.readFormat();
        }
        if ((stringArray = this.scanner.nextLine().split(":", 2)).length < 2 || !stringArray[0].equals("Dialogue")) {
            return null;
        }
        String[] stringArray2 = stringArray[1].split(",", this.format.length);
        long l = this.timeFormat.parse(stringArray2[this.formatIndexStart].trim()).getTime();
        long l2 = this.timeFormat.parse(stringArray2[this.formatIndexEnd].trim()).getTime();
        String string = stringArray2[this.formatIndexText].trim();
        if (this.drawingTags.matcher(string).find()) {
            return null;
        }
        return new SubtitleElement(l, l2, this.resolve(string));
    }

    protected String resolve(String string) {
        string = this.tags.matcher(string).replaceAll("");
        return this.newline.matcher(string).replaceAll("\n");
    }

    public static List<SubtitleElement> decode(String string) {
        return new SubStationAlphaReader(new Scanner(string)).decode();
    }
}

