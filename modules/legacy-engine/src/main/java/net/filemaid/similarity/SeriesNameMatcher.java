package net.filemaid.similarity;

import java.io.File;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.similarity.CommonSequenceMatcher;
import net.filemaid.similarity.DateMatcher;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;

public class SeriesNameMatcher {
    protected final SimilarityMetric metric;
    protected final SeasonEpisodeMatcher seasonEpisodeMatcher;
    protected final DateMatcher dateMatcher;
    protected final CommonSequenceMatcher commonSequenceMatcher;

    public SeriesNameMatcher(SeasonEpisodeMatcher seasonEpisodeMatcher, DateMatcher dateMatcher) {
        this(new NameSimilarityMetric(), seasonEpisodeMatcher, dateMatcher, new CommonSequenceMatcher(3, true));
    }

    public SeriesNameMatcher(SimilarityMetric similarityMetric, SeasonEpisodeMatcher seasonEpisodeMatcher, DateMatcher dateMatcher, CommonSequenceMatcher commonSequenceMatcher) {
        this.metric = similarityMetric;
        this.seasonEpisodeMatcher = seasonEpisodeMatcher;
        this.dateMatcher = dateMatcher;
        this.commonSequenceMatcher = commonSequenceMatcher;
    }

    public Collection<String> matchAll(Collection<File> collection, Function<File, String> function) {
        SeriesNameCollection seriesNameCollection = new SeriesNameCollection();
        Map map = collection.stream().collect(Collectors.groupingBy(function.compose(File::getParentFile), LinkedHashMap::new, Collectors.mapping(function, Collectors.collectingAndThen(Collectors.toList(), list -> list.toArray(new String[0])))));
        map.forEach((string, stringArray) -> {
            for (String string2 : this.matchAll((String[])stringArray)) {
                String string3 = this.commonSequenceMatcher.matchFirstCommonSequence(string2, (String)string);
                float f = string3 == null ? 0.0f : this.metric.getSimilarity(string3, string2);
                seriesNameCollection.add((double)f > 0.7 ? string3 : string2);
            }
        });
        return seriesNameCollection;
    }

    public Collection<String> matchAll(String[] stringArray) {
        SeriesNameCollection seriesNameCollection = new SeriesNameCollection();
        int n = stringArray.length < 5 ? stringArray.length : 5;
        SeriesNameCollection seriesNameCollection2 = new SeriesNameCollection();
        String[] stringArray2 = Arrays.copyOf(stringArray, stringArray.length);
        for (int i = 0; i < stringArray2.length; ++i) {
            String string = this.seasonEpisodeMatcher.head(stringArray2[i]);
            if (string != null && !string.isEmpty()) {
                stringArray2[i] = string;
                continue;
            }
            int n2 = this.dateMatcher.find(stringArray2[i], 0);
            if (n2 < 0) continue;
            stringArray2[i] = stringArray2[i].substring(0, n2);
        }
        seriesNameCollection2.addAll(this.deepMatchAll(stringArray2, n));
        seriesNameCollection.addAll(this.flatMatchAll(stringArray, Pattern.compile(StringUtilities.join(seriesNameCollection2, (CharSequence)"|"), 258), n));
        seriesNameCollection.addAll(seriesNameCollection2);
        return seriesNameCollection;
    }

    private Collection<String> flatMatchAll(String[] stringArray, Pattern pattern, int n) {
        ThresholdCollection<String> thresholdCollection = new ThresholdCollection<String>(n, Comparator.comparing(Normalization::normalizePunctuation, String.CASE_INSENSITIVE_ORDER));
        String[] stringArray2 = stringArray;
        int n2 = stringArray2.length;
        for (int i = 0; i < n2; ++i) {
            String string;
            Matcher matcher = pattern.matcher(string = stringArray2[i]);
            int n3 = matcher.find() ? matcher.end() : 0;
            int n4 = this.seasonEpisodeMatcher.find(string, n3);
            if (n4 > 0) {
                String string2 = string.substring(0, n4).trim();
                List<SeasonEpisodeMatcher.SxE> list = this.seasonEpisodeMatcher.match(string.substring(n3));
                if (list != null && list.size() == 1 && list.get((int)0).season >= 0) {
                    thresholdCollection.addDirect(string2);
                    continue;
                }
                thresholdCollection.add(string2);
                continue;
            }
            int n5 = this.dateMatcher.find(string, n3);
            if (n5 <= 0) continue;
            thresholdCollection.addDirect(string.substring(0, n5).trim());
        }
        return thresholdCollection;
    }

    private Collection<String> deepMatchAll(String[] stringArray, int n) {
        if (stringArray.length < 2 || stringArray.length < n) {
            return Collections.emptySet();
        }
        String string = this.commonSequenceMatcher.matchFirstCommonSequence(stringArray);
        if (string != null) {
            return Collections.singleton(string);
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.addAll(this.deepMatchAll(Arrays.copyOfRange(stringArray, 0, stringArray.length / 2), n));
        arrayList.addAll(this.deepMatchAll(Arrays.copyOfRange(stringArray, stringArray.length / 2, stringArray.length), n));
        return arrayList;
    }

    public String matchByEpisodeIdentifier(String string) {
        String string2 = this.seasonEpisodeMatcher.head(string);
        if (string2 != null && !string2.isEmpty()) {
            return string2;
        }
        int n = this.dateMatcher.find(string, 0);
        if (n > 0) {
            return string.substring(0, n);
        }
        return null;
    }

    public String matchByFirstCommonWordSequence(String ... stringArray) {
        if (stringArray.length < 2) {
            throw new IllegalArgumentException("Can't match common sequence from less than two names");
        }
        return this.commonSequenceMatcher.matchFirstCommonSequence(stringArray);
    }

    protected <T> T[] firstCommonSequence(T[] TArray, T[] TArray2, int n, Comparator<T> comparator) {
        for (int i = 0; i < TArray.length && i <= n; ++i) {
            for (int j = 0; j < TArray2.length && j <= n; ++j) {
                int n2 = 0;
                while (i + n2 < TArray.length && j + n2 < TArray2.length && comparator.compare(TArray[i + n2], TArray2[j + n2]) == 0) {
                    ++n2;
                }
                if (n2 <= 0) continue;
                if (i == 0 && n2 == TArray.length) {
                    return TArray;
                }
                return Arrays.copyOfRange(TArray, i, i + n2);
            }
        }
        return null;
    }

    private static class SeriesNameCollection
    extends AbstractCollection<String> {
        private final Map<String, String> data = new LinkedHashMap<String, String>();

        private SeriesNameCollection() {
        }

        private String key(Object object) {
            return object.toString().trim().toLowerCase();
        }

        @Override
        public boolean contains(Object object) {
            return this.data.containsKey(this.key(object));
        }

        @Override
        public boolean add(String string) {
            String string2 = this.key(string);
            if (string2.length() < 2) {
                return false;
            }
            String string3 = this.data.get(string2);
            if (string3 == null || this.firstCharacterCaseBalance(string3) < this.firstCharacterCaseBalance(string)) {
                this.data.put(string2, string);
                return true;
            }
            return false;
        }

        private float firstCharacterCaseBalance(String string) {
            int n = 0;
            int n2 = 0;
            for (String string2 : RegularExpressions.NON_WORD.split(string)) {
                if (string2.length() <= 0) continue;
                char c = string2.charAt(0);
                if (Character.isUpperCase(c)) {
                    ++n;
                    continue;
                }
                ++n2;
            }
            if (n == 0 && n2 == 0) {
                return 0.0f;
            }
            return ((float)n2 + (float)n * 1.01f) / (float)Math.abs(n2 - n);
        }

        @Override
        public Iterator<String> iterator() {
            return this.data.values().iterator();
        }

        @Override
        public int size() {
            return this.data.size();
        }
    }

    private static class ThresholdCollection<E>
    extends AbstractCollection<E> {
        private final Collection<E> heaven = new ArrayList();
        private final Map<E, Collection<E>> limbo;
        private final int threshold;

        public ThresholdCollection(int n, Comparator<E> comparator) {
            this.limbo = new TreeMap<E, Collection<E>>(comparator);
            this.threshold = n;
        }

        @Override
        public boolean add(E e) {
            Collection<E> collection = this.limbo.get(e);
            if (collection == null) {
                collection = new ArrayList(this.threshold);
                this.limbo.put(e, collection);
            }
            if (collection == this.heaven) {
                this.heaven.add(e);
                return true;
            }
            collection.add(e);
            if (collection.size() >= this.threshold) {
                this.heaven.addAll(collection);
                this.limbo.put(e, this.heaven);
                return true;
            }
            return false;
        }

        public boolean addDirect(E e) {
            return this.heaven.add(e);
        }

        @Override
        public Iterator<E> iterator() {
            return this.heaven.iterator();
        }

        @Override
        public int size() {
            return this.heaven.size();
        }
    }
}

