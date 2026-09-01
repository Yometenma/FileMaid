package net.filemaid.media;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.MemoryCache;
import net.filemaid.media.IndexEntry;
import net.filemaid.similarity.CommonSequenceMatcher;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.Movie;
import net.filemaid.web.Score;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;

class HighPerformanceMatcher
extends CommonSequenceMatcher {
    private final Function<String, String> collationKeyMapper;

    public HighPerformanceMatcher(Locale locale, MemoryCache<String, CollationKey> memoryCache) {
        this(0, true, RegularExpressions.NON_WORD, HighPerformanceMatcher.getLenientCollator(locale), string -> string.toLowerCase(locale), memoryCache);
    }

    private HighPerformanceMatcher(int n, boolean bl, Pattern pattern, Collator collator, Function<String, String> function, MemoryCache<String, CollationKey> memoryCache) {
        super(n, bl, pattern, collator, memoryCache);
        this.collationKeyMapper = function;
    }

    public HighPerformanceMatcher maxStartIndex(int n) {
        return new HighPerformanceMatcher(n, this.firstMatch, this.delimiter, this.collator, this.collationKeyMapper, this.keys);
    }

    @Override
    public CollationKey[] split(String string) {
        throw new UnsupportedOperationException("requires ahead-of-time collation");
    }

    @Override
    protected CollationKey getCollationKey(String string) {
        return this.keys.get(this.collationKeyMapper.apply(string), this.collator::getCollationKey);
    }

    public CollationKey[] prepare(String string) {
        return super.split(string);
    }

    public List<CollationKey[]> prepare(Collection<String> collection) {
        return collection.stream().filter(Objects::nonNull).map(string -> this.prepare((String)string)).collect(Collectors.toList());
    }

    public List<IndexEntry<Movie>> prepareMovie(Movie movie) {
        return movie.getEffectiveNamesWithoutYear().stream().map(string -> new IndexEntry<Movie>(movie, (String)string, string + " " + movie.getYear(), this::prepare)).collect(Collectors.toList());
    }

    public List<IndexEntry<Series>> prepareSeries(Series series) {
        return series.getEffectiveNames().stream().map(string -> new IndexEntry<Series>(series, (String)string, string + " " + series.getYear(), this::prepare)).collect(Collectors.toList());
    }

    public List<IndexEntry<SearchResult>> prepareSearchResult(SearchResult searchResult) {
        return searchResult.getEffectiveNames().stream().map(string -> new IndexEntry<SearchResult>(searchResult, (String)string, null, this::prepare)).collect(Collectors.toList());
    }

    public <T> List<T> match(Collection<String> collection, boolean bl, List<IndexEntry<T>> list) throws Exception {
        ArrayList<Score<T>> arrayList = new ArrayList<Score<T>>();
        List<CollationKey[]> list2 = this.prepare(collection);
        for (IndexEntry<T> indexEntry : list) {
            for (CollationKey[] collationKeyArray : list2) {
                CollationKey[] collationKeyArray2;
                CollationKey[] collationKeyArray3;
                CollationKey[] collationKeyArray4 = this.matchFirstCommonSequence(new CollationKey[][]{collationKeyArray, collationKeyArray3 = indexEntry.getLenientKey()});
                if (collationKeyArray4 == null || collationKeyArray4.length < collationKeyArray3.length) continue;
                CollationKey[] collationKeyArray5 = indexEntry.getStrictKey();
                CollationKey[] collationKeyArray6 = collationKeyArray2 = collationKeyArray5 == null ? null : this.matchFirstCommonSequence(new CollationKey[][]{collationKeyArray, collationKeyArray5});
                if (collationKeyArray2 != null && collationKeyArray2.length >= collationKeyArray5.length) {
                    arrayList.add(Score.of(indexEntry.getObject(), indexEntry.getStrictName().length()));
                    continue;
                }
                if (collationKeyArray5 != null && bl) continue;
                arrayList.add(Score.of(indexEntry.getObject(), indexEntry.getLenientName().length()));
            }
        }
        return arrayList.stream().sorted(Score.descending()).map(Score::getValue).collect(Collectors.toList());
    }
}

