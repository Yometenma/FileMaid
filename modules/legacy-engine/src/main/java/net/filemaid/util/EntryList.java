package net.filemaid.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntryList<K, V>
extends AbstractMap<K, V> {
    private List<K> keys;
    private List<V> values;

    @SuppressWarnings("unchecked")
    public EntryList(List<? extends K> list, List<? extends V> list2) {
        this.keys = (List)(list != null ? list : Collections.emptyList());
        this.values = (List)(list2 != null ? list2 : Collections.emptyList());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>(){

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>(){
                    private Iterator<? extends K> keySeq;
                    private Iterator<? extends V> valueSeq;
                    {
                        this.keySeq = EntryList.this.keys.iterator();
                        this.valueSeq = EntryList.this.values.iterator();
                    }

                    @Override
                    public boolean hasNext() {
                        return this.keySeq.hasNext() || this.valueSeq.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        K k = this.keySeq.hasNext() ? this.keySeq.next() : null;
                        V v = this.valueSeq.hasNext() ? this.valueSeq.next() : null;
                        return new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
                    }
                };
            }

            @Override
            public int size() {
                return EntryList.this.keys.size();
            }
        };
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>(){

            @Override
            public Iterator<K> iterator() {
                return EntryList.this.keys.iterator();
            }

            @Override
            public int size() {
                return EntryList.this.keys.size();
            }
        };
    }

    @Override
    public List<V> values() {
        return this.values;
    }

    @Override
    public int size() {
        return Math.max(this.keys.size(), this.values.size());
    }

    public static <K, V> EntryList<K, V> of(List<? extends K> list, List<? extends V> list2) {
        return new EntryList<K, V>(list, list2);
    }
}

