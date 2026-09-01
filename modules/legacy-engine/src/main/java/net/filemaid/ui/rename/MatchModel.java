package net.filemaid.ui.rename;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.similarity.Match;

public class MatchModel<Value, Candidate> {
    private final EventList<Match<Value, Candidate>> source = new BasicEventList();
    private final MatchView<Value, Candidate> values = new MatchView<Value, Candidate>((EventList)this.source){

        @Override
        public Value getElement(Match<Value, Candidate> match) {
            return match.getValue();
        }

        @Override
        public Candidate getComplement(Match<Value, Candidate> match) {
            return match.getCandidate();
        }

        @Override
        public Match<Value, Candidate> createMatch(Value Value2, Candidate Candidate) {
            return Match.of(Value2, Candidate);
        }

        public String toString() {
            return "Values";
        }
    };
    private final MatchView<Candidate, Value> candidates = new MatchView<Candidate, Value>((EventList)this.source){

        @Override
        public Candidate getElement(Match<Value, Candidate> match) {
            return match.getCandidate();
        }

        @Override
        public Value getComplement(Match<Value, Candidate> match) {
            return match.getValue();
        }

        @Override
        public Match<Value, Candidate> createMatch(Candidate Candidate, Value Value2) {
            return Match.of(Value2, Candidate);
        }

        public String toString() {
            return "Candidates";
        }
    };

    public void clear() {
        this.source.clear();
        this.values.clear();
        this.candidates.clear();
    }

    public int size() {
        return this.source.size();
    }

    public Match<Value, Candidate> getMatch(int n) {
        return (Match)this.source.get(n);
    }

    public void removeMatch(int n) {
        if (n >= 0 && n < this.source.size()) {
            this.source.remove(n);
        }
    }

    public boolean hasComplement(int n) {
        if (n >= 0 && n < this.source.size()) {
            Match match = (Match)this.source.get(n);
            return match.getValue() != null && match.getCandidate() != null;
        }
        return false;
    }

    public int getComplementCount() {
        return Math.min(this.values.size(), this.candidates.size());
    }

    public EventList<Match<Value, Candidate>> matches() {
        return this.source;
    }

    public EventList<Value> values() {
        return this.values;
    }

    public EventList<Candidate> candidates() {
        return this.candidates;
    }

    public int insertMatch(Match<Value, Candidate> match) {
        if (match.getValue() != null && match.getCandidate() != null) {
            int n = this.getComplementCount();
            this.source.add(n, match);
            return n;
        }
        if (match.getValue() != null) {
            int n = this.values.size();
            this.values.add(n, match.getValue());
            return n;
        }
        if (match.getCandidate() != null) {
            int n = this.candidates.size();
            this.candidates.add(n, match.getCandidate());
            return n;
        }
        return -1;
    }

    public void addAll(Collection<Match<Value, Candidate>> collection) {
        this.source.addAll(collection);
    }

    public void addAll(Collection<? extends Value> collection, Collection<? extends Candidate> collection2) {
        if (this.values.size() != this.candidates.size()) {
            throw new IllegalStateException("Existing matches are not balanced");
        }
        Iterator<? extends Value> iterator = collection.iterator();
        Iterator<? extends Candidate> iterator2 = collection2.iterator();
        while (iterator.hasNext() || iterator2.hasNext()) {
            Value value = iterator.hasNext() ? iterator.next() : null;
            Candidate candidate = iterator2.hasNext() ? iterator2.next() : null;
            this.source.add(Match.of(value, candidate));
        }
    }

    private abstract class MatchView<Element, Complement>
    extends TransformedList<Match<Value, Candidate>, Element> {
        private int size;

        public MatchView(EventList<Match<Value, Candidate>> eventList) {
            super(eventList);
            this.size = 0;
            eventList.addListEventListener((ListEventListener)this);
        }

        public abstract Element getElement(Match<Value, Candidate> var1);

        public abstract Complement getComplement(Match<Value, Candidate> var1);

        public abstract Match<Value, Candidate> createMatch(Element var1, Complement var2);

        public Element get(int n) {
            return this.getElement(n);
        }

        public Element getElement(int n) {
            return this.getElement(this.source.get(n));
        }

        public Complement getComplement(int n) {
            return this.getComplement(this.source.get(n));
        }

        public boolean addAll(Collection<? extends Element> collection) {
            this.put(this.size(), collection);
            return true;
        }

        public boolean add(Element Element2) {
            this.put(this.size(), Collections.singleton(Element2));
            return true;
        }

        public void add(int n, Element Element2) {
            List<Element> list = this.tail(n);
            list.add(0, Element2);
            this.put(n, list);
        }

        public Element remove(int n) {
            List<Element> list = this.tail(n + 1);
            list.add(null);
            this.put(n, list);
            return null;
        }

        public Element set(int n, Element Element2) {
            Element Element3 = this.getElement(n);
            this.put(n, Collections.singleton(Element2));
            return Element3;
        }

        public void clear() {
            if (this.source.size() > 0 && this.source.stream().map(this::getElement).anyMatch(Objects::nonNull)) {
                this.updateEvent(() -> {
                    List<Complement> list = this.source.stream().map(this::getComplement).filter(Objects::nonNull).collect(Collectors.toList());
                    this.source.clear();
                    this.source.addAll(list.stream().map(complement -> this.createMatch(null, complement)).collect(Collectors.toList()));
                });
            }
            this.size = 0;
        }

        private void put(int n, Iterable<? extends Element> iterable) {
            this.updateEvent(() -> {
                int n2 = n;
                for (Element t : iterable) {
                    if (n2 < this.source.size()) {
                        Complement Complement = this.getComplement(n2);
                        if (t == null && Complement == null) {
                            this.source.remove(n2);
                            continue;
                        }
                        this.source.set(n2, this.createMatch(t, Complement));
                        ++n2;
                        continue;
                    }
                    if (t == null) continue;
                    this.source.add(n2, this.createMatch(t, null));
                    ++n2;
                }
            });
        }

        private void updateEvent(Runnable runnable) {
            this.updates.beginEvent(true);
            try {
                runnable.run();
            }
            catch (Exception exception) {
                Logging.trace((Object)this, exception);
            }
            this.updates.commitEvent();
        }

        private List<Element> tail(int n) {
            ArrayList arrayList = new ArrayList();
            arrayList.addAll(this.subList(n, this.size()));
            return arrayList;
        }

        protected boolean isWritable() {
            return false;
        }

        public int size() {
            return this.size;
        }

        public void listChanged(ListEvent<Match<Value, Candidate>> listEvent) {
            this.updateEvent(() -> {
                while (listEvent.next()) {
                    int n = listEvent.getIndex();
                    int n2 = listEvent.getType();
                    switch (n2) {
                        case 1: {
                            if (n == this.size && this.getElement(n) != null) {
                                this.updates.elementInserted(n, this.getElement(n));
                                ++this.size;
                                break;
                            }
                            if (n == this.size - 1 && this.getElement(n) == null) {
                                this.updates.elementDeleted(n, null);
                                --this.size;
                                break;
                            }
                            if (n >= this.size) break;
                            this.updates.elementUpdated(n, null, this.getElement(n));
                            break;
                        }
                        case 2: {
                            if (n > this.size || this.getElement(n) == null) break;
                            this.updates.elementInserted(n, this.getElement(n));
                            ++this.size;
                            break;
                        }
                        case 0: {
                            if (n >= this.size) break;
                            this.updates.elementDeleted(n, null);
                            --this.size;
                        }
                    }
                }
            });
        }
    }
}

