package net.filemaid.util;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Matcher;
import net.filemaid.util.FileUtilities;

public class FileNameComparator
implements Comparator<File> {
    private final Comparator<String> comparator;

    public FileNameComparator(Comparator<String> comparator) {
        this.comparator = comparator;
    }

    private String[] tokenize(File file) {
        String string = file.getName();
        Matcher matcher = FileUtilities.EXTENSION.matcher(string);
        if (matcher.find()) {
            return new String[]{string.substring(0, matcher.start() - 1), matcher.group()};
        }
        return new String[]{string, ""};
    }

    @Override
    public int compare(File file, File file2) {
        String[] stringArray = this.tokenize(file);
        String[] stringArray2 = this.tokenize(file2);
        for (int i = 0; i < stringArray.length && i < stringArray2.length; ++i) {
            int n = this.comparator.compare(stringArray[i], stringArray2[i]);
            if (n == 0) continue;
            return n;
        }
        return Integer.compare(stringArray.length, stringArray2.length);
    }
}

