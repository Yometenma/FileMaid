package net.filemaid.subtitle;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;
import net.filemaid.util.RegularExpressions;

public class TMPlayerReader
extends SubtitleReader {
    private final Pattern lineFormat = Pattern.compile("(\\d+):(\\d+):(\\d+):(.+)");

    public TMPlayerReader(Scanner scanner) {
        super(scanner);
    }

    @Override
    public SubtitleElement readNext() throws Exception {
        String string = this.scanner.nextLine();
        Matcher matcher = this.lineFormat.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        int n = Integer.parseInt(matcher.group(1)) * 60 * 60 * 1000;
        int n2 = Integer.parseInt(matcher.group(2)) * 60 * 1000;
        int n3 = Integer.parseInt(matcher.group(3)) * 1000;
        int n4 = n + n2 + n3;
        int n5 = n4 + 3000;
        String string2 = RegularExpressions.PIPE.matcher(matcher.group(4)).replaceAll("\n").trim();
        return new SubtitleElement(n4, n5, string2);
    }

    public static List<SubtitleElement> decode(String string) {
        return new TMPlayerReader(new Scanner(string)).decode();
    }
}

