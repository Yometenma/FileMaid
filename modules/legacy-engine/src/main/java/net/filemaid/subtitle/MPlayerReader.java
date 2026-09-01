package net.filemaid.subtitle;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;
import net.filemaid.util.RegularExpressions;

public class MPlayerReader
extends SubtitleReader {
    private final Pattern lineFormat = Pattern.compile("\\[(\\d+)\\]\\[(\\d+)\\](.+)");

    public MPlayerReader(Scanner scanner) {
        super(scanner);
    }

    @Override
    public SubtitleElement readNext() throws Exception {
        String string = this.scanner.nextLine();
        Matcher matcher = this.lineFormat.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        int n = Integer.parseInt(matcher.group(1)) * 100;
        int n2 = Integer.parseInt(matcher.group(2)) * 100;
        String string2 = RegularExpressions.PIPE.matcher(matcher.group(3)).replaceAll("\n").trim();
        return new SubtitleElement(n, n2, string2);
    }

    public static List<SubtitleElement> decode(String string) {
        return new MPlayerReader(new Scanner(string)).decode();
    }
}

