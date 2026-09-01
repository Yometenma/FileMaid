package net.filemaid.media;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.filemaid.similarity.NameSimilarityMetric;

public class SubstringMatcher<T> {
    private final List<T> values;
    private final String[][] keys;
    public static final Pattern SPACING = Pattern.compile("(^(?:The|A)\\b)|[\\p{Punct}\\p{Space}]+|19[3-9][0-9]|20[0-2][0-9]", 2);

    public SubstringMatcher(List<T> list, Function<T, Collection<String>> function) {
        this.values = list;
        this.keys = this.prepare(list, function);
    }

    public String prepare(String string) {
        return SPACING.matcher(string).replaceAll("").toLowerCase();
    }

    public String[] prepare(Collection<String> collection) {
        return (String[])collection.stream().map(this::prepare).filter(string -> string.length() > 3).sorted().distinct().toArray(String[]::new);
    }

    private String[][] prepare(List<T> list, Function<T, Collection<String>> function) {
        String[][] stringArray = new String[list.size()][];
        for (int i = 0; i < stringArray.length; ++i) {
            stringArray[i] = this.prepare(function.apply(list.get(i)));
        }
        return stringArray;
    }

    public List<T> match(Collection<String> collection, boolean bl) {
        return this.match(collection, SubstringMatcher.minSimilarity(bl ? 0.8f : 0.4f));
    }

    public List<T> match(Collection<String> collection, BiPredicate<String, String> biPredicate) {
        String[] stringArray = this.prepare(collection);
        return IntStream.range(0, this.values.size()).filter(n -> {
            for (String string : stringArray) {
                for (String string2 : this.keys[n]) {
                    if (!string.contains(string2) || !biPredicate.test(string, string2)) continue;
                    return true;
                }
            }
            return false;
        }).mapToObj(this.values::get).collect(Collectors.toList());
    }

    public static BiPredicate<String, String> minSimilarity(float f) {
        NameSimilarityMetric nameSimilarityMetric = new NameSimilarityMetric();
        return (string, string2) -> nameSimilarityMetric.getSimilarity(string, string2) > f;
    }
}

