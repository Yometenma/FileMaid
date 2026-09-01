package net.filemaid.similarity;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.MemoryCache;
import net.filemaid.util.RegularExpressions;

public class CommonSequenceMatcher {
    protected final int maxStartIndex;
    protected final boolean firstMatch;
    protected final Pattern delimiter;
    protected final Collator collator;
    protected final MemoryCache<String, CollationKey> keys;

    public static Collator getLenientCollator(Locale locale) {
        Collator collator = Collator.getInstance(locale);
        collator.setDecomposition(2);
        collator.setStrength(0);
        return collator;
    }

    public CommonSequenceMatcher(int n, boolean bl) {
        this(n, bl, RegularExpressions.NON_WORD, CommonSequenceMatcher.getLenientCollator(Locale.ENGLISH), MemoryCache.forMinutes());
    }

    public CommonSequenceMatcher(int n, boolean bl, Pattern pattern, Collator collator, MemoryCache<String, CollationKey> memoryCache) {
        this.maxStartIndex = n;
        this.firstMatch = bl;
        this.delimiter = pattern;
        this.collator = collator;
        this.keys = memoryCache;
    }

    public String matchFirstCommonSequence(String ... stringArray) {
        CollationKey[][] collationKeyArray = (CollationKey[][])Arrays.stream(stringArray).map(this::split).toArray(n -> new CollationKey[n][]);
        return this.synth(this.matchFirstCommonSequence(collationKeyArray));
    }

    public <E> E[] matchFirstCommonSequence(E[][] EArray) {
        E[] EArray2 = null;
        for (E[] EArray3 : EArray) {
            if (EArray2 == null) {
                EArray2 = EArray3;
                continue;
            }
            if ((EArray2 = this.firstCommonSequence(EArray2, EArray3)) != null) continue;
            return null;
        }
        return EArray2;
    }

    private String synth(CollationKey[] collationKeyArray) {
        return collationKeyArray == null ? null : Arrays.stream(collationKeyArray).map(CollationKey::getSourceString).collect(Collectors.joining(" "));
    }

    public CollationKey[] split(String string) {
        return (CollationKey[])this.delimiter.splitAsStream(string).map(this::getCollationKey).toArray(CollationKey[]::new);
    }

    protected CollationKey getCollationKey(String string) {
        return this.keys.get(string, this.collator::getCollationKey);
    }

    private <E> E[] firstCommonSequence(E[] EArray, E[] EArray2) {
        E[] EArray3 = null;
        for (int i = 0; i < EArray.length && i <= this.maxStartIndex; ++i) {
            for (int j = 0; j < EArray2.length && j <= this.maxStartIndex; ++j) {
                int n = 0;
                while (this.equals(EArray, i + n, EArray2, j + n)) {
                    ++n;
                }
                if (n <= (EArray3 == null ? 0 : EArray3.length)) continue;
                EArray3 = Arrays.copyOfRange(EArray, i, i + n);
                if (!this.firstMatch) continue;
                return EArray3;
            }
        }
        return EArray3;
    }

    private boolean equals(Object[] objectArray, int n, Object[] objectArray2, int n2) {
        if (n < objectArray.length && n2 < objectArray2.length) {
            Object object = objectArray[n];
            Object object2 = objectArray2[n2];
            return object2.equals(object);
        }
        return false;
    }
}

