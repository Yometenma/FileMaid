package net.filemaid.similarity;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transliterator;
import java.util.ArrayList;
import java.util.List;

public final class ICU {
    public static final Transliterator ASCII = ICU.getTransliterator("Any-Latin;[\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df]DE-ASCII;Latin-ASCII");

    public static Transliterator getTransliterator(String string) {
        return Transliterator.getInstance((String)string);
    }

    public static List<String> tokenizeGraphemeClusters(String string) {
        ArrayList<String> arrayList = new ArrayList<String>(string.length());
        BreakIterator breakIterator = BreakIterator.getCharacterInstance();
        breakIterator.setText(string);
        int n = 0;
        int n2 = 0;
        while ((n2 = breakIterator.next()) != -1) {
            arrayList.add(string.substring(n, n2));
            n = breakIterator.current();
        }
        return arrayList;
    }

    private ICU() {
        throw new UnsupportedOperationException();
    }
}

