package net.filemaid.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class AlphanumComparator
implements Comparator<String> {
    protected Collator collator;

    public static AlphanumComparator getInstance() {
        return new AlphanumComparator(Locale.ENGLISH);
    }

    public AlphanumComparator(Collator collator) {
        this.collator = collator;
    }

    public AlphanumComparator(Locale locale) {
        this.collator = Collator.getInstance(locale);
        this.collator.setDecomposition(2);
        this.collator.setStrength(0);
    }

    protected boolean isDigit(String string, int n) {
        return Character.isDigit(string.charAt(n));
    }

    protected int getNumericValue(String string, int n) {
        return Character.getNumericValue(string.charAt(n));
    }

    protected String getChunk(String string, int n) {
        int n2 = n;
        int n3 = string.length();
        boolean bl = this.isDigit(string, n2++);
        while (n2 < n3 && bl == this.isDigit(string, n2)) {
            ++n2;
        }
        return string.substring(n, n2);
    }

    @Override
    public int compare(String string, String string2) {
        int n = string.length();
        int n2 = string2.length();
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        while (n5 == 0 && n3 < n && n4 < n2) {
            String string3 = this.getChunk(string, n3);
            n3 += string3.length();
            String string4 = this.getChunk(string2, n4);
            n4 += string4.length();
            if (this.isDigit(string3, 0) && this.isDigit(string4, 0)) {
                int n6;
                int n7;
                int n8 = string3.length();
                int n9 = string4.length();
                for (n7 = 0; n7 < n8 && this.getNumericValue(string3, n7) == 0; ++n7) {
                }
                for (n6 = 0; n6 < n9 && this.getNumericValue(string4, n6) == 0; ++n6) {
                }
                n5 = n8 - n7 - (n9 - n6);
                int n10 = n7;
                int n11 = n6;
                while (n5 == 0 && n10 < n8 && n11 < n9) {
                    n5 = this.getNumericValue(string3, n10++) - this.getNumericValue(string4, n11++);
                }
                if (n5 != 0) continue;
                n5 = n10 - n11;
                continue;
            }
            n5 = this.collator.compare(string3, string4);
        }
        if (n5 == 0) {
            n5 = n - n2;
        }
        return Integer.signum(n5);
    }
}

