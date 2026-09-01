package net.filemaid.web;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.simmetrics.MultisetMetric;
import org.simmetrics.StringMetric;
import org.simmetrics.builders.StringMetricBuilder;
import org.simmetrics.metrics.BlockDistance;
import org.simmetrics.tokenizers.Tokenizers;

public class LocalSearch<T> {
    private final StringMetric metric = StringMetricBuilder.with((MultisetMetric)new BlockDistance()).tokenize(Tokenizers.qGramWithPadding((int)3)).build();
    private final float resultMinimumSimilarity = 0.6f;
    private final int resultSetSize = 20;
    private final List<T> objects;
    private final Set<String>[] fields;
    private static final Pattern PUNCTUATION_OR_SPACE = Pattern.compile("[\\p{Punct}\\p{Space}]+");

    public LocalSearch(List<T> list, Function<T, Collection<String>> function) {
        this.objects = list;
        this.fields = list.stream().parallel().map(function).map(this::normalize).toArray(Set[]::new);
    }

    public Stream<T> stream() {
        return this.objects.stream();
    }

    public int size() {
        return this.objects.size();
    }

    public List<T> search(String string) {
        String string2 = this.normalize(string);
        return IntStream.range(0, this.objects.size()).parallel().mapToObj(n -> {
            T t = this.objects.get(n);
            Set<String> set = this.fields[n];
            float f = this.similarity(string2, set);
            if (f >= 0.6f || this.match(string2, set)) {
                return this.entry(t, f);
            }
            return null;
        }).filter(Objects::nonNull).sorted(this.order(string2)).limit(20L).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    protected Map.Entry<T, Float> entry(T t, float f) {
        return new AbstractMap.SimpleImmutableEntry<T, Float>(t, Float.valueOf(f));
    }

    protected Comparator<Map.Entry<T, Float>> order(String string) {
        Comparator<Map.Entry<T, Float>> comparator = Comparator.comparing(Map.Entry::getValue);
        Comparator<Map.Entry<T, Float>> comparator2 = Comparator.comparing(entry -> Float.valueOf(this.metric.compare(string, this.normalize(entry.getKey().toString()))));
        return Collections.reverseOrder(comparator.thenComparing(comparator2));
    }

    protected boolean match(String string, Set<String> set) {
        return set.stream().anyMatch(string2 -> string2.contains(string));
    }

    protected float similarity(String string, Set<String> set) {
        float f = 0.0f;
        for (String string2 : set) {
            float f2 = this.metric.compare(string, string2);
            if (string2.startsWith(string)) {
                f2 += 0.2f;
            }
            if (!(f2 > f)) continue;
            if (f2 >= 1.0f) {
                return f2;
            }
            f = f2;
        }
        return f;
    }

    protected Set<String> normalize(Collection<String> collection) {
        return collection.stream().map(this::normalize).collect(Collectors.toSet());
    }

    protected String normalize(String string) {
        return PUNCTUATION_OR_SPACE.matcher(string).replaceAll(" ").trim().toLowerCase(Locale.ENGLISH);
    }
}

