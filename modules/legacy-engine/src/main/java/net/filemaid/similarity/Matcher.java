package net.filemaid.similarity;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import net.filemaid.Logging;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.SimilarityMetric;

public class Matcher<V, C> {
    protected final List<V> values;
    protected final List<C> candidates;
    protected final boolean strict;
    protected final SimilarityMetric[] metrics;
    protected final DisjointMatchCollection<V, C> disjointMatchCollection;

    public Matcher(Collection<? extends V> collection, Collection<? extends C> collection2, boolean bl, SimilarityMetric[] similarityMetricArray) {
        this.values = new ArrayList<V>(collection);
        this.candidates = new ArrayList<C>(collection2);
        this.strict = bl;
        this.metrics = (SimilarityMetric[])similarityMetricArray.clone();
        this.disjointMatchCollection = new DisjointMatchCollection();
    }

    public synchronized List<Match<V, C>> match() {
        ArrayList<Match<V, C>> arrayList = new ArrayList<Match<V, C>>(this.values.size() * this.candidates.size());
        for (V value : this.values) {
            for (C candidate : this.candidates) {
                arrayList.add(Match.of(value, candidate));
            }
        }
        this.deepMatch(arrayList, 0);
        ArrayList<Match<V, C>> arrayList2 = new ArrayList<>();
        for (V value : this.values) {
            Match<V, C> match = this.disjointMatchCollection.getByValue(value);
            if (match == null) continue;
            arrayList2.add(match);
        }
        Iterator<Match<V, C>> iterator = arrayList2.iterator();
        while (iterator.hasNext()) {
            Match<V, C> match = iterator.next();
            this.values.remove(match.getValue());
            this.candidates.remove(match.getCandidate());
        }
        this.disjointMatchCollection.clear();
        return arrayList2;
    }

    public synchronized List<V> remainingValues() {
        return Collections.unmodifiableList(this.values);
    }

    public synchronized List<C> remainingCandidates() {
        return Collections.unmodifiableList(this.candidates);
    }

    protected void deepMatch(Collection<Match<V, C>> collection, int n) {
        if (n >= this.metrics.length || collection.isEmpty()) {
            if (!this.strict) {
                ArrayList<Match<V, C>> arrayList = new ArrayList<Match<V, C>>(collection);
                Collections.sort(arrayList, new Comparator<Match<V, C>>(){

                    @Override
                    public int compare(Match<V, C> match, Match<V, C> match2) {
                        return match.toString().compareToIgnoreCase(match2.toString());
                    }
                });
                this.disjointMatchCollection.addAll(arrayList);
            }
            return;
        }
        for (Set<Match<V, C>> set : this.mapBySimilarity(collection, this.metrics[n]).values()) {
            List<Match<V, C>> list = this.disjointMatches(set);
            if (!list.isEmpty()) {
                this.disjointMatchCollection.addAll(list);
                set.removeAll(list);
            }
            this.removeCollected(set);
            this.deepMatch(set, n + 1);
        }
    }

    protected void removeCollected(Collection<Match<V, C>> collection) {
        Iterator<Match<V, C>> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (this.disjointMatchCollection.disjoint(iterator.next())) continue;
            iterator.remove();
        }
    }

    protected SortedMap<Float, Set<Match<V, C>>> mapBySimilarity(Collection<Match<V, C>> collection, SimilarityMetric similarityMetric) {
        TreeMap<Float, Set<Match<V, C>>> treeMap = new TreeMap<>(Collections.reverseOrder());
        for (Match<V, C> match : collection) {
            float f2 = 0.0f;
            try {
                f2 = similarityMetric.getSimilarity(match.getCandidate(), match.getValue());
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(similarityMetric, exception));
            }
            if (f2 != 0.0f) {
                Logging.debug.finest(Logging.format("%s %.04f => %s", similarityMetric, Float.valueOf(f2), match));
            }
            treeMap.computeIfAbsent(Float.valueOf(f2), f -> new LinkedHashSet<Match<V, C>>()).add(match);
            if (!Thread.interrupted()) continue;
            throw new CancellationException();
        }
        return treeMap;
    }

    protected List<Match<V, C>> disjointMatches(Collection<Match<V, C>> collection) {
        HashMap<V, ArrayList<Match<V, C>>> hashMap = new HashMap<>();
        HashMap<C, ArrayList<Match<V, C>>> hashMap2 = new HashMap<>();
        for (Match<V, C> match : collection) {
            ArrayList<Match<V, C>> list = hashMap.get(match.getValue());
            ArrayList<Match<V, C>> arrayList = hashMap2.get(match.getCandidate());
            if (list == null) {
                list = new ArrayList<>();
                hashMap.put(match.getValue(), list);
            }
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                hashMap2.put(match.getCandidate(), arrayList);
            }
            list.add(match);
            arrayList.add(match);
        }
        ArrayList<Match<V, C>> result = new ArrayList<>();
        for (ArrayList<Match<V, C>> list : hashMap.values()) {
            if (list.size() != 1 || !list.equals(hashMap2.get(list.get(0).getCandidate()))) continue;
            result.add(list.get(0));
        }
        return result;
    }

    protected static class DisjointMatchCollection<V, C>
    extends AbstractList<Match<V, C>> {
        private final List<Match<V, C>> matches = new ArrayList<Match<V, C>>();
        private final Map<V, Match<V, C>> values = new IdentityHashMap<V, Match<V, C>>();
        private final Map<C, Match<V, C>> candidates = new IdentityHashMap<C, Match<V, C>>();

        protected DisjointMatchCollection() {
        }

        @Override
        public boolean add(Match<V, C> match) {
            if (this.disjoint(match)) {
                this.values.put(match.getValue(), match);
                this.candidates.put(match.getCandidate(), match);
                return this.matches.add(match);
            }
            return false;
        }

        public boolean disjoint(Match<V, C> match) {
            return !this.values.containsKey(match.getValue()) && !this.candidates.containsKey(match.getCandidate());
        }

        public Match<V, C> getByValue(V v) {
            return this.values.get(v);
        }

        public Match<V, C> getByCandidate(C c) {
            return this.candidates.get(c);
        }

        @Override
        public Match<V, C> get(int n) {
            return this.matches.get(n);
        }

        @Override
        public int size() {
            return this.matches.size();
        }

        @Override
        public void clear() {
            this.matches.clear();
            this.values.clear();
            this.candidates.clear();
        }
    }
}

