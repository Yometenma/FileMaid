package net.filemaid.subtitle;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleReader;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;

public class MicroDVDReader
extends SubtitleReader {
    private double fps = 23.976;

    public MicroDVDReader(Scanner scanner) {
        super(scanner);
    }

    @Override
    public SubtitleElement readNext() throws Exception {
        int n;
        String string = this.scanner.nextLine();
        ArrayList<String> arrayList = new ArrayList<String>(2);
        int n2 = 0;
        while (n2 < string.length() && string.charAt(n2) == '{' && (n = string.indexOf(125, n2 + 1)) >= n2) {
            arrayList.add(string.substring(n2 + 1, n));
            n2 = n + 1;
        }
        if (arrayList.size() < 2) {
            return null;
        }
        n = Integer.parseInt((String)arrayList.get(0));
        int n3 = Integer.parseInt((String)arrayList.get(1));
        String string2 = string.substring(n2).trim();
        string2 = string2.replaceAll("\\{[^\\}]*\\}", "");
        if (n == 1 && n3 == 1) {
            this.fps = Double.parseDouble(string2);
            return null;
        }
        Object[] objectArray = RegularExpressions.PIPE.split(string2);
        return new SubtitleElement(Math.round((double)n * this.fps), Math.round((double)n3 * this.fps), StringUtilities.join(objectArray, (CharSequence)"\n"));
    }

    public static List<SubtitleElement> decode(String string) {
        return new MicroDVDReader(new Scanner(string)).decode();
    }
}

