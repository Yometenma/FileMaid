package net.filemaid.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WeakValueHashMap<K, V>
extends AbstractMap<K, V> {
    private HashMap<K, WeakValue<V>> references;
    private ReferenceQueue<V> gcQueue;

    public WeakValueHashMap(int n) {
        this.references = new HashMap(n);
        this.gcQueue = new ReferenceQueue();
    }

    @Override
    public V put(K k, V v) {
        this.processQueue();
        return this.getReferenceValue(this.references.put(k, new WeakValue<V>(k, v, this.gcQueue)));
    }

    @Override
    public V get(Object object) {
        this.processQueue();
        return this.getReferenceValue(this.references.get(object));
    }

    @Override
    public V remove(Object object) {
        return this.getReferenceValue(this.references.remove(object));
    }

    @Override
    public void clear() {
        this.references.clear();
    }

    @Override
    public boolean containsKey(Object object) {
        this.processQueue();
        return this.references.containsKey(object);
    }

    @Override
    public boolean containsValue(Object object) {
        this.processQueue();
        for (WeakValue<V> weakValue : this.references.values()) {
            if (!object.equals(this.getReferenceValue(weakValue))) continue;
            return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        this.processQueue();
        return this.references.keySet();
    }

    @Override
    public int size() {
        this.processQueue();
        return this.references.size();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        this.processQueue();
        LinkedHashSet<Map.Entry<K, V>> linkedHashSet = new LinkedHashSet<Map.Entry<K, V>>(this.references.size());
        for (Map.Entry<K, WeakValue<V>> entry : this.references.entrySet()) {
            linkedHashSet.add(new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), this.getReferenceValue(entry.getValue())));
        }
        return linkedHashSet;
    }

    @Override
    public Collection<V> values() {
        this.processQueue();
        ArrayList<V> arrayList = new ArrayList<V>(this.references.size());
        for (WeakValue<V> weakValue : this.references.values()) {
            arrayList.add(this.getReferenceValue(weakValue));
        }
        return arrayList;
    }

    private V getReferenceValue(WeakValue<V> weakValue) {
        return weakValue == null ? null : (V)weakValue.get();
    }

    private void processQueue() {
        WeakValue weakValue;
        while ((weakValue = (WeakValue)this.gcQueue.poll()) != null) {
            this.references.remove(weakValue.getKey());
        }
    }

    private class WeakValue<T>
    extends WeakReference<T> {
        private final K key;

        private WeakValue(K k, T t, ReferenceQueue<T> referenceQueue) {
            super(t, referenceQueue);
            this.key = k;
        }

        private K getKey() {
            return this.key;
        }
    }
}

